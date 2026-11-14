package mods.eln.sixnode.electricalcable

import mods.eln.cable.CableRenderDescriptor
import mods.eln.generic.ItemNbt
import mods.eln.i18n.I18N.tr
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Player
import kotlin.math.floor

enum class UtilityCableMaterial(val label: String,val meltingPointCelsius: Double) { COPPER("Copper",1085.0),ALUMINUM("Aluminum",660.0) }

/** Save-stable metre spool, with independent colored conductors in intact multicore cable. */
class UtilityCableDescriptor(val sizeLabel: String,val material: UtilityCableMaterial,val conductorAreaMm2: Double,val ampacity: Double,
    val insulated: Boolean,val insulationVoltageRating: Double,val melted: Boolean,val meltTemperatureCelsius: Double,
    name: String,render: CableRenderDescriptor,val conductorCount: Int=1,
    val colorPalettes: Array<UtilityCablePalette> = arrayOf(UtilityCablePalette("Single",intArrayOf(0))),
    val flatStyle: Boolean=false,val totalConductorAreaMm2: Double=conductorAreaMm2): ElectricalCableDescriptor(name,render,if(conductorCount==1)"Single-conductor utility cable" else "Multi-conductor utility cable",false) {
    var meltedDescriptor: UtilityCableDescriptor?=null
    val actsAsSingleConductor get()=conductorCount==1
    init { ElementClass=UtilityCableElement::class.java;RenderClass=UtilityCableRender::class.java }
    override fun itemProperties(p: net.minecraft.world.item.Item.Properties)=p.stacksTo(1)
    fun resistanceOhms(meters: Double=1.0,celsius: Double=20.0)=WirePhysics.resistance(material,conductorAreaMm2,meters,celsius)
    fun defaultLengthMeters()=floor(10.0/WirePhysics.massKg(material,totalConductorAreaMm2,1.0)).coerceAtLeast(1.0)
    override fun getDefaultNBT()=CompoundTag().apply { putDouble("utilityLengthMeters",defaultLengthMeters());putString("utilityLengthUid",java.util.UUID.randomUUID().toString()) }
    fun getRemainingLengthMeters(stack: ItemStack): Double {
        if(stack.isEmpty)return 0.0
        val raw=ItemNbt.get(stack)?.getDoubleOr("utilityLengthMeters",defaultLengthMeters()) ?: defaultLengthMeters()
        return raw.takeIf { it.isFinite() && it>=0 } ?: 0.0
    }
    fun setRemainingLengthMeters(stack: ItemStack,meters: Double) {
        require(meters.isFinite() && meters>=0)
        if(stack.isEmpty)return
        val t=ItemNbt.get(stack) ?: getDefaultNBT();t.putDouble("utilityLengthMeters",meters);ItemNbt.set(stack,t)
    }
    fun segment(stack: ItemStack,meters: Double): ItemStack {
        require(meters.isFinite() && meters>0)
        return stack.copyWithCount(1).also { setRemainingLengthMeters(it,meters) }
    }
    fun consume(stack: ItemStack,meters: Double): Boolean {
        if(!meters.isFinite() || meters<=0 || stack.count!=1)return false
        val remaining=getRemainingLengthMeters(stack)
        if(remaining+LENGTH_METERS_EPSILON<meters)return false
        val next=(remaining-meters).coerceAtLeast(0.0);setRemainingLengthMeters(stack,next)
        if(next<LENGTH_METERS_EPSILON)stack.shrink(1)
        return true
    }
    fun creativeStack()=newItemStack().also { setRemainingLengthMeters(it,128.0) }
    override fun addInformation(stack: ItemStack,player: Player?,list: MutableList<Any?>,advanced: Boolean) {
        list.add(tr("Conductor: %1$ %2$",material.label,sizeLabel));list.add(tr("Equivalent area: %1$ mm2",conductorAreaMm2))
        list.add(tr("Conductors: %1$",conductorCount));if(conductorCount>1)list.add(tr("Each core is electrically independent"))
        list.add(tr("Nominal current: %1$ A",ampacity));list.add(tr("Resistance per core at 20 C: %1$ ohm/m",resistanceOhms()));list.add(tr("Remaining length: %1$ m",getRemainingLengthMeters(stack)))
        list.add(tr("Insulation: %1$",if(insulated)tr("%1$ V",insulationVoltageRating) else tr("Bare")))
        list.add(tr("Max temperature: %1$ C",meltTemperatureCelsius));list.add(tr("Each placed cable segment uses 1 m"))
    }
    companion object { const val LENGTH_METERS_EPSILON=1e-6 }
}

data class UtilityCablePalette(val displayName: String,val conductorColors: IntArray)
