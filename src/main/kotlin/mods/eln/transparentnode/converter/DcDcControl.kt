package mods.eln.transparentnode.converter

import mods.eln.i18n.I18N.tr
import mods.eln.sim.power.*
import net.minecraft.nbt.CompoundTag
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/** Explicit names in saves; missing version is always the historical signal mapping. */
class DcDcControl {
    var version = 2
    var mode = "SIGNAL"
    var value = 1.0
    var enabled = true

    internal fun validSettings(): Boolean = version in 1..2 && value.isFinite() && when (mode) {
        "SIGNAL" -> true
        "RATIO" -> value in (1.0 / 256)..256.0
        // V1 one-way converters have no voltage-target controller. Do not silently run them at a ratio.
        "VOLTAGE" -> version == 2 && value in 0.0..120_000.0
        else -> false
    }

    fun load(tag: CompoundTag) {
        version = if (tag.contains("converterControlVersion")) tag.getIntOr("converterControlVersion",1) else 1
        mode = if (tag.contains("converterControlMode")) tag.getStringOr("converterControlMode","SIGNAL") else "SIGNAL"
        value = if (tag.contains("converterControlValue")) tag.getDoubleOr("converterControlValue",1.0) else 1.0
        enabled = (!tag.contains("converterEnabled") || tag.getBooleanOr("converterEnabled",true)) && validSettings()
    }

    fun save(tag: CompoundTag) {
        tag.putInt("converterControlVersion", version)
        tag.putString("converterControlMode", mode)
        tag.putDouble("converterControlValue", value)
        tag.putBoolean("converterEnabled", enabled)
    }

    fun ratio(kind: ConverterKind, normalized: Double): Double {
        if (!enabled) return 1.0
        require(validSettings()) { "Invalid converter control settings." }
        return when (mode) {
            "RATIO" -> value
            // The process supplies voltageTarget separately. The inactive signal MUST NOT be evaluated.
            "VOLTAGE" -> 1.0
            else -> ControlMapping.ratio(kind, normalized,
                if (version == 1) MappingVersion.LEGACY_LINEAR_V1 else MappingVersion.LOGARITHMIC_V2)
        }
    }

    fun write(stream: DataOutputStream) {
        stream.writeInt(version); stream.writeUTF(mode); stream.writeDouble(value); stream.writeBoolean(enabled)
    }

    fun read(stream: DataInputStream) {
        // Read a complete snapshot before changing state; a truncated packet cannot leave mixed settings.
        val nextVersion = stream.readInt()
        val nextMode = stream.readUTF()
        val nextValue = stream.readDouble()
        val nextEnabled = stream.readBoolean()
        version = nextVersion; mode = nextMode; value = nextValue
        enabled = nextEnabled && validSettings()
    }

    /** Packet validation precedes mutation. IDs deliberately do not overlap base grounding. */
    fun handle(id: Byte, stream: DataInputStream): Boolean {
        if (id.toInt() !in 40..43) return false
        try {
            when (id.toInt()) {
                40 -> {
                    val next = stream.readInt()
                    if (next !in 0..2 || (next == 2 && version != 2)) return true
                    mode = arrayOf("SIGNAL", "RATIO", "VOLTAGE")[next]
                    // Do not reinterpret a voltage as a gain (or a gain as volts) when changing units.
                    value = if (mode == "VOLTAGE") 800.0 else 1.0
                }
                41 -> {
                    val next = stream.readDouble()
                    val valid = next.isFinite() && when (mode) {
                        "RATIO" -> next in (1.0 / 256)..256.0
                        "VOLTAGE" -> version == 2 && next in 0.0..120_000.0
                        else -> false
                    }
                    if (valid) value = next
                }
                42 -> {
                    val next = stream.readBoolean()
                    enabled = next && validSettings()
                }
                43 -> { version = 2; mode = "RATIO"; value = 1.0; enabled = false }
            }
        } catch (_: IOException) {
            // Malformed/truncated client input is rejected without mutating the accepted setting.
        }
        return true
    }
}

