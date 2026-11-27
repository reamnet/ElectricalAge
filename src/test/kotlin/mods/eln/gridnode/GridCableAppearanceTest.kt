package mods.eln.gridnode

import mods.eln.cable.CableRenderDescriptor
import mods.eln.gridnode.GridCableAppearance.Surface
import mods.eln.sixnode.electricalcable.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.*

class GridCableAppearanceTest {
    private fun wire(metal: UtilityCableMaterial, area: Double, volts: Double) = UtilityCableDescriptor(
        "fixture", metal, area, 20.0, volts > 0, volts, false, 180.0, "fixture",
        CableRenderDescriptor("eln", "sprites/white.png", .65f, .65f))

    @Test fun overheadDiameterUsesCoreAreaAndTheSourceJacketAllowance() {
        for (metal in UtilityCableMaterial.entries) {
            val thin = GridCableAppearance.of(wire(metal, Math.PI, 0.0))
            val thick = GridCableAppearance.of(wire(metal, 25 * Math.PI, 0.0))
            assertEquals(.006, thin.radius().toDouble(), 1e-7)
            assertEquals(5.0, thick.radius().toDouble() / thin.radius(), 1e-6)
            assertEquals(if (metal == UtilityCableMaterial.COPPER) 0xffd96b2e.toInt() else 0xffc7ccd6.toInt(), thin.tint())
            assertEquals(Surface.AWG_METAL, thin.surface())
            for (volts in listOf(300.0, 5000.0, 150000.0)) {
                val insulated = GridCableAppearance.of(wire(metal, Math.PI, volts))
                assertEquals(Surface.JACKET, insulated.surface())
                assertEquals(thin.tint(), insulated.tint())
                assertEquals(thin.radius(), insulated.radius())
                assertEquals(.006 * 1.18 + .002, insulated.outsideRadius().toDouble(), 1e-7)
            }
        }
    }

    @Test fun codecRoundTripsAndRejectsTruncatedOrInvalidGeometry() {
        fun encoded(appearance: GridCableAppearance) =
            ByteArrayOutputStream().also { appearance.write(DataOutputStream(it)) }.toByteArray()
        fun read(bytes: ByteArray, old: Boolean = false) =
            GridCableAppearance.read(DataInputStream(ByteArrayInputStream(bytes)), old)

        for (surface in Surface.entries) {
            val appearance = GridCableAppearance(.02f, 0xffd96b2e.toInt(), surface)
            val bytes = encoded(appearance)
            assertEquals(appearance, read(bytes))
            if (surface == Surface.LEGACY_TEXTURE || surface == Surface.NATIVE_METAL)
                assertEquals(appearance, read(bytes, old = true))
            else assertThrows(IOException::class.java) { read(bytes, old = true) }
            for (size in bytes.indices)
                assertThrows(IOException::class.java) { read(bytes.copyOf(size)) }
        }
        for (radius in listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, -.1f, 1f, 1.01f))
            assertThrows(IOException::class.java) { read(encoded(GridCableAppearance(radius, -1, Surface.JACKET))) }
        val bytes = encoded(GridCableAppearance(.02f, -1, Surface.JACKET))
        for (bad in listOf(bytes.copyOf().also { it[4] = 0 }, bytes.copyOf().also { it[8] = 4 }))
            assertThrows(IOException::class.java) { read(bad) }
    }
}
