package mods.eln.transparentnode.converter

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.ItemNbt
import mods.eln.i18n.I18N.tr
import mods.eln.item.FerromagneticCoreDescriptor
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.heater.ElectricalHeatAccumulator
import mods.eln.sixnode.electricalcable.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import kotlin.math.*

/** Source winding metres/turns, with the destination's paid spool and thermal geometry. */
data class DcDcWinding(val descriptor:ElectricalCableDescriptor,val amount:Double) {
    val utility get()=descriptor as? UtilityCableDescriptor
    val maximumVoltage get()=utility?.takeIf { it.insulated }?.insulationVoltageRating?.coerceAtMost(120000.0) ?: 120000.0
    val maximumCurrent get()=descriptor.electricalMaximalCurrent.takeIf { it.isFinite() && it>0 } ?: 5.0
    val renderedCount get()=ceil(amount).toInt().coerceIn(1,256)
    val renderedThickness get()=utility?.let { sqrt(it.totalConductorAreaMm2/it.conductorCount.coerceAtLeast(1)/33.631).toFloat().coerceIn(.05f,1.85f) }
        ?: (descriptor.render.width*16f/1.95f).coerceIn(.35f,1.85f)
    companion object {
        @JvmStatic fun of(stack:ItemStack?):DcDcWinding? {
            if(stack==null || stack.isEmpty)return null
            val d=GenericItemBlockUsingDamageDescriptor.getDescriptor(stack) as? ElectricalCableDescriptor ?: return null
            if(d.signalWire || (d is UtilityCableDescriptor && (d.melted || stack.count!=1)))return null
            val amount=if(d is UtilityCableDescriptor)d.getRemainingLengthMeters(stack) else stack.count.toDouble()
            return if(amount.isFinite() && amount>UtilityCableDescriptor.LENGTH_METERS_EPSILON)DcDcWinding(d,amount) else null
        }
        @JvmStatic fun construction(core:ItemStack,primary:ItemStack,secondary:ItemStack,version:Int):String {
            if(GenericItemUsingDamageDescriptor.getDescriptor(core) !is FerromagneticCoreDescriptor)return tr("Needs Core")
            val p=of(primary) ?: return tr("Needs Primary Winding")
            val s=of(secondary) ?: return tr("Needs Secondary Winding")
            if(version==1 && abs(p.amount-s.amount)>UtilityCableDescriptor.LENGTH_METERS_EPSILON)return tr("Primary and secondary windings must match")
            return tr("Operational")
        }
    }
}

/** Electrical heat is delivered once per native slow thermal step; Tc uses the fixed native 20 C reference. */
class DcDcWindingThermal(val load:NbtThermalLoad,val resistance:Resistor,private val amps:()->Double,private val terminalLoss:()->Double) {
    var wire:DcDcWinding?=null;private set
    private var held:ItemStack?=null
    private var configured=false
    private var physics:WireThermalPhysics?=null
    val absoluteCelsius get()=load.Tc+20.0
    val heating=ElectricalHeatAccumulator({
        val i=amps();val watts=i*i*resistance.r+terminalLoss()
        if(watts.isFinite())watts.coerceAtLeast(0.0) else 0.0
    },load)
    fun stamp(stack:ItemStack) {
        if(stack!==held || stack.isEmpty)return
        val temperature=absoluteCelsius+heating.pendingJoules/load.C
        if(temperature.isFinite()) {
            val tag=ItemNbt.get(stack) ?: CompoundTag();tag.putDouble("windingTemperatureCelsius",temperature);ItemNbt.set(stack,tag)
        }
    }
    fun configure(stack:ItemStack) {
        heating.flushIntoLoad()
        if(configured && held!==stack) {
            held?.let(::stamp)
            val temperature=ItemNbt.get(stack)?.getDoubleOr("windingTemperatureCelsius",20.0) ?: 20.0
            load.Tc=if(temperature.isFinite() && temperature>=-273.15)temperature-20.0 else 0.0
        }
        held=stack;configured=true;wire=DcDcWinding.of(stack)
        physics=wire?.let { w->w.utility?.let { WireThermalPhysics(it.material,it.conductorAreaMm2,w.amount) } }
        update()
    }
    fun update() {
        val w=wire
        if(w==null) { resistance.highImpedance();load.setHighImpedance();return }
        val t=absoluteCelsius.coerceIn(-273.0,5000.0)
        val next=(w.utility?.resistanceOhms(w.amount,t) ?: (w.descriptor.electricalRs*2*w.amount)).coerceAtLeast(1e-9)
        if(abs(resistance.r-next)>next*.001)resistance.r=next
        val physical=physics
        if(physical!=null) {
            val cooling=physical.coolingConductance(t,load.ambient+20.0,w.utility!!.insulated)/sqrt(w.amount.coerceAtLeast(1.0))
            load.set(physical.endpointThermalResistance,1/cooling,physical.capacity(t))
        }else { w.descriptor.applyTo(load);load.C*=w.amount }
    }
    fun melting():UtilityCableDescriptor? = wire?.utility?.takeIf {
        absoluteCelsius>=it.material.meltingPointCelsius || (it.insulated && absoluteCelsius>=it.meltTemperatureCelsius)
    }
}

/** Keep item and installed-wire temperatures at the same precision; older float tags remain readable. */
class WindingThermalLoad(name:String):NbtThermalLoad(name) {
    private val key=name+"Tc"
    override fun readFromNBT(tag:CompoundTag,prefix:String) {
        val value=tag.getDoubleOr(prefix+key,0.0)
        Tc=if(value.isFinite())value else 0.0
    }
    override fun writeToNBT(tag:CompoundTag,prefix:String) { tag.putDouble(prefix+key,Tc) }
}
