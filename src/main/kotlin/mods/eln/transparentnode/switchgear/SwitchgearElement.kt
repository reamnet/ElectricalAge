package mods.eln.transparentnode.switchgear

import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.*
import mods.eln.node.transparent.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import java.io.*

class SwitchgearElement(node: TransparentNode,d: TransparentNodeDescriptor):TransparentNodeElement(node,d) {
    val kind=(d as SwitchgearDescriptor).kind
    val unit=SwitchgearUnit(kind,electricalLoadList,electricalComponentList,electricalProcessList)
    private val slots=TransparentNodeElementInventory(0,1,this)
    override fun getInventory()=slots
    init {
        unit.changed={needPublish()};unit.click={play(mods.eln.sound.SoundCommand("eln:click").smallRange().mulVolume(if(kind.changeover).16 else .32).mulBlockAttenuation(0.0))}
        slowProcessList.add(NodePeriodicPublishProcess(node,.5,.25))
    }
    override fun initialize() { connect() }
    override fun getElectricalLoad(side: Direction,lrdu: LRDU)=when(side) {
        front.left()->unit.commons.getOrNull(lrdu.dir);front.right()->unit.normallyOpen.getOrNull(lrdu.dir);front.inverse->unit.normallyClosed.getOrNull(lrdu.dir)
        front->when(lrdu) { LRDU.Left->unit.coilA;LRDU.Right->unit.coilB;LRDU.Up->unit.gate;else->null };else->null
    }
    override fun getConnectionMask(side: Direction,lrdu: LRDU)=when(getElectricalLoad(side,lrdu)) { null->0;unit.gate->NodeBase.maskElectricalInputGate;else->NodeBase.maskElectricalPower }
    override fun getThermalLoad(side: Direction,lrdu: LRDU)=null
    override fun hasGui()=true
    override fun onBlockActivated(p: Player,s: Direction,x: Float,y: Float,z: Float)=false
    override fun multiMeterString(side: Direction)=unit.commons.indices.joinToString("\n") { i->tr("Contact %1$: %2$; NO %3$; NC %4$",i+1,Utils.plotVolt("",unit.commons[i].u),Utils.plotAmpere("",unit.noContacts[i].current),Utils.plotAmpere("",unit.ncContacts.getOrNull(i)?.current ?: 0.0)) }
    override fun thermoMeterString(side: Direction)=tr("Case temperature: %1$ C",String.format(java.util.Locale.ROOT,"%.1f",unit.temperature))
    override fun getWaila()=linkedMapOf(tr("Control") to unit.control.label(),tr("Position") to switchgearStatus(unit.enabled,unit.energized,unit.transitioning,unit.tripped))
    override fun writeToNBT(t: CompoundTag) { super.writeToNBT(t);unit.write(t) }
    override fun readFromNBT(t: CompoundTag) { super.readFromNBT(t);unit.read(t) }
    override fun getItemStackNBT()=CompoundTag().also(unit::write)
    override fun readItemStackNBT(t: CompoundTag?) { if(t!=null)unit.read(t) }
    override fun networkSerialize(out: DataOutputStream) { super.networkSerialize(out);SwitchgearDisplay(kind).apply { capture(unit);listOf(front.left(),front.right(),front.inverse,front).forEachIndexed { i,s->connections[i].set(node.lrduCubeMask.getTranslate(s).mask) };write(out) } }
    override fun networkUnserialize(input: DataInputStream): Byte {
        val id=super.networkUnserialize(input)
        try { when(id.toInt()) { 80->RelayControl.entries.getOrNull(input.readInt())?.let(unit::setControl);81->unit.setEnabled(input.readBoolean());82->unit.resetTrip() } }catch(_: IOException) {}
        needPublish();return id
    }
}
