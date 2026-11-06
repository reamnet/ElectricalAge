package mods.eln.sixnode.variableinductor

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.item.FerromagneticCoreDescriptor
import mods.eln.misc.*
import mods.eln.misc.series.SerieEE
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import mods.eln.registry.ElnDeviceRegistry
import mods.eln.sim.*
import mods.eln.sim.mna.component.Inductor
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.*
import mods.eln.sixnode.powerinductorsix.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Player
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

/** Source E6 control quantization; the native fixed inductor keeps its existing identity and geometry. */
class VariableInductorDescriptor(name:String,model:Obj3D?):PowerInductorSixDescriptor(name,model,SerieEE.newE6(-1.0)) {
    companion object { const val ID=6179;const val MAX_TURNS=19 }
    init { ElementClass=VariableInductorElement::class.java }
    fun inductanceFromControl(control:Double):Double {
        val normalized=if(control.isFinite())control.coerceIn(0.0,1.0) else 1.0
        return getlValue((normalized*(MAX_TURNS-1)).roundToInt()+1)
    }
    override fun getRsValue(inventory:Container):Double {
        val core=FerromagneticCoreDescriptor.getDescriptor(inventory.getItem(0)) as? FerromagneticCoreDescriptor
        return if(core==null)MnaConst.highImpedance else ElnDeviceRegistry.lowVoltageCableDescriptor.electricalRs*core.cableMultiplicator
    }
    override fun addInformation(stack:ItemStack?,player:Player?,lines:MutableList<String>,advanced:Boolean) {
        super.addInformation(stack,player,lines,advanced)
        lines.add(tr("Signal controlled inductor"));lines.add(tr("Connect a signal wire to modulate inductance"))
        lines.add(tr("Inductance range: %1$-%2$",Utils.plotValue(getlValue(1),"H"),Utils.plotValue(getlValue(MAX_TURNS),"H")))
    }
}

class VariableInductorElement(node:SixNode,side:Direction,d:SixNodeDescriptor):SixNodeElement(node,side,d) {
    val descriptor=d as VariableInductorDescriptor
    val positive=NbtElectricalLoad("positiveLoad")
    val negative=NbtElectricalLoad("negativeLoad")
    val control=NbtElectricalGateInput("controlLoad")
    val inductor=Inductor("inductor",positive,negative)
    private val items=SixNodeElementInventory(1,64,this)
    private var restoring=false
    private var restoredInductance:Double?=null
    val normalizedControl:Double get()=if(control.connectedComponents.isEmpty() || !control.u.isFinite())1.0 else control.normalized
    init {
        electricalLoadList.add(positive);electricalLoadList.add(negative);electricalLoadList.add(control)
        electricalComponentList.add(inductor);positive.setAsMustBeFarFromInterSystem()
        slowProcessList.add(IProcess { updateControl() })
    }
    fun updateControl() {
        val next=descriptor.inductanceFromControl(normalizedControl)
        if(next!=inductor.l) { inductor.l=next;needPublish() }
    }
    override fun getInventory():Container=items
    override fun hasGui()=true
    override fun getElectricalLoad(lrdu:LRDU):ElectricalLoad?=when(lrdu) { front.right()->positive;front.left()->negative;front->control;else->null }
    override fun getConnectionMask(lrdu:LRDU)=when(lrdu) { front.right(),front.left()->NodeBase.maskElectricalPower;front->NodeBase.maskElectricalInputGate;else->0 }
    override fun getThermalLoad(lrdu:LRDU):ThermalLoad?=null
    override fun initialize()=setupPhysical()
    override fun inventoryChanged() { super.inventoryChanged();setupPhysical() }
    private fun setupPhysical() {
        val rs=descriptor.getRsValue(items);positive.rs=rs;negative.rs=rs
        if(restoring) {
            restoring=false
            // Keep the saved field/current until the first physical control boundary rebuilds the connected signal.
            inductor.l=restoredInductance ?: descriptor.inductanceFromControl(normalizedControl)
        }else { inductor.resetStates();updateControl() }
    }
    override fun readFromNBT(tag:CompoundTag) {
        super.readFromNBT(tag);restoring=true
        val saved=tag.getDoubleOr("variableInductance",Double.NaN)
        restoredInductance=if(saved.isFinite() && saved>=descriptor.getlValue(1) && saved<=descriptor.getlValue(VariableInductorDescriptor.MAX_TURNS))saved else null
        if(!inductor.current.isFinite())inductor.resetStates()
    }
    override fun writeToNBT(tag:CompoundTag) { super.writeToNBT(tag);tag.putDouble("variableInductance",inductor.l) }
    override fun networkSerialize(stream:DataOutputStream) { super.networkSerialize(stream);stream.writeDouble(inductor.l);stream.writeDouble(normalizedControl) }
    override fun multiMeterString()=Utils.plotVolt("U",abs(inductor.u))+Utils.plotAmpere("I",inductor.current)+Utils.plotValue(inductor.l,"H")+" "+Utils.plotPercent("Ctl",normalizedControl)
    override fun thermoMeterString()=""
    override fun getWaila():Map<String,String> = linkedMapOf(tr("Inductance") to Utils.plotValue(inductor.l,"H"),tr("Charge") to Utils.plotEnergy("",inductor.e),tr("Control") to Utils.plotPercent("",normalizedControl))
}

/** Reuses the destination's shortened, voxel-aware winding mesh and adds the source's signal port. */
class VariableInductorRender(tile:SixNodeEntity,side:Direction,d:SixNodeDescriptor):PowerInductorSixRender(tile,side,d) {
    var inductance=0.0;private set
    var normalizedControl=1.0;private set
    override fun publishUnserialize(stream:DataInputStream) {
        super.publishUnserialize(stream);inductance=stream.readDouble();normalizedControl=stream.readDouble()
    }
    override fun getCableRender(lrdu:LRDU):CableRenderDescriptor?=when(lrdu) {
        front->ElnDeviceRegistry.signalCableDescriptor.render
        front.right(),front.left()->ElnDeviceRegistry.lowVoltageCableDescriptor.render
        else->null
    }
}
