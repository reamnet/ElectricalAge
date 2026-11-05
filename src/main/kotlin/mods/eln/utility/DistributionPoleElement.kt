package mods.eln.utility

import mods.eln.Eln
import mods.eln.gridnode.*
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.io.*

class DistributionPoleElement(node: TransparentNode,d: TransparentNodeDescriptor): GridElement(node,d,64) {
    val kind=(d as DistributionPoleDescriptor).kind
    val unit=DistributionPoleUnit(kind,electricalLoadList,electricalComponentList,electricalProcessList)
    var terminal=if(kind.lvOnly)50 else 32;private set
    init { unit.changed={ event->needPublish();play(mods.eln.sound.SoundCommand("eln:$event").smallRange().mulVolume(.5)) };slowProcessList.add(NodePeriodicPublishProcess(node,1.0,.5)) }
    override fun selectedGridTerminal(side: Direction)=terminal
    fun terminals()=buildList {
        if(!kind.lvOnly) { repeat(kind.phases) { add(32+it) };if(kind.phases==3)add(52);if(kind.protectedLine) { repeat(kind.outputPhases) { add(36+it) };if(kind.phases==3 && !kind.singlePhaseTap)add(53) } }
        add(50);addAll(40..42)
    }
    fun selectTerminal(value: Int): Boolean { if(value !in terminals())return false;terminal=value;needPublish();return true }
    override fun getGridElectricalLoad(side: Direction)=if(kind.lvOnly)unit.services[0] else unit.lines[0]
    override fun gridLoads(t: Int): List<ElectricalLoad> = when {
        t in 0..5->listOf(getGridElectricalLoad(Direction.XN))
        !kind.lvOnly && t in 32 until 32+kind.phases->listOf(unit.lines[t-32])
        !kind.lvOnly && kind.protectedLine && t in 36 until 36+kind.outputPhases->listOf(unit.outs[t-36])
        t in 40..42->listOf(unit.services[t-40]);t==50->unit.services
        t==52 && kind.phases==3 && !kind.lvOnly->unit.lines
        t==53 && kind.phases==3 && kind.protectedLine && !kind.singlePhaseTap->unit.outs
        else->emptyList()
    }
    override fun gridCircuitKey(t: Int)=when {
        t in 0..5->"LEGACY";t==50->"SERVICE_BUNDLE";t==40->"SERVICE_L1";t==41->"SERVICE_L2";t==42->"SERVICE_240"
        t in 52..53->"HV_THREE_PHASE";t in listOf(32,36) && kind.phases==1->"HV_SINGLE"
        t==36 && kind.singlePhaseTap->"HV${unit.phase}";else->"HV${(t-32)%4}"
    }
    override fun gridPhase(t: Int): Int?=when {
        kind.phases!=3 || kind.lvOnly->null;t in 0..5->0;t in 32..34->t-32;t==36 && kind.singlePhaseTap->unit.phase
        t in 36..38 && kind.protectedLine->t-36;else->null
    }
    override fun phaseBusTerminals(t: Int)=if(kind.phases==1 && (t in 0..5 || t in listOf(32,36)))listOf(32,36).filter { gridLoads(it).isNotEmpty() } else listOf(t)
    override fun renderWireCount(t: Int)=if(t in listOf(50,52,53))3 else 1
    override fun gridCablePoint(t: Int,wire: Int): Vec3=when {
        t==52->gridCablePoint(32+wire,0);t==53->gridCablePoint(36+wire,0)
        t==50 || t in 40..42->ServiceCableMount.point(t,wire)
        t==32 && kind.phases==1 || t==33 && kind.phases==3->Vec3(0.0,7.15,0.0)
        t==36 && kind.singlePhaseTap->Vec3(-.68,5.85,0.0)
        t in 36..38 && kind.cutout->Vec3(-.68,5.85,phaseZ(t-36))
        t in 36..38 && kind.regulator->Vec3(.65,5.50,phaseZ(t-36)*.73)
        t in 36..38->Vec3(.35,6.53,phaseZ(t-36))
        else->Vec3(0.0,6.63,phaseZ((t-32).coerceIn(0,kind.phases-1)))
    }
    private fun phaseZ(i: Int)=if(kind.phases==1)0.0 else (i-1)*.95
    override fun getElectricalLoad(side: Direction,lrdu: LRDU): ElectricalLoad? {
        if(side==Direction.YN)return unit.services[when(lrdu) { LRDU.Left->0;LRDU.Right->1;else->2 }]
        if(side.isY || lrdu!=LRDU.Down)return null
        val i=when(side) { front.left()->0;front.right()->1;front.inverse->2;else->return null }
        return if(kind==DistributionKind.FEEDER_THREE)unit.lines[i] else unit.services[i]
    }
    override fun getConnectionMask(side: Direction,l: LRDU)=if(getElectricalLoad(side,l)!=null)NodeBase.maskElectricalPower else 0
    override fun getThermalLoad(side: Direction,l: LRDU)=null
    override fun hasGui()=true
    fun selectPhase(value: Int): Boolean {
        if(value !in unit.lines.indices || !kind.singlePhaseTap)return false
        for(link in gridLinkList)if(link.getTerminal(this)==36) {
            val other=link.getOtherElement(this) ?: continue
            if(GridPhaseRouting.phases(other,link.getTerminal(other),this).any { it!=value })return false
        }
        val changed=unit.selectPhase(value);if(changed)needPublish();return changed
    }
    override fun onBlockActivated(p: Player,side: Direction,x: Float,y: Float,z: Float): Boolean {
        if(PoleHardware.extract(p,unit.fuses)) { unit.applyContacts();needPublish();return true }
        if(UtilityItems.isFuse(p.mainHandItem)) {
            val f=unit.fuses.firstOrNull { !it.hasCartridge || it.blown }
            if(f!=null && f.insertCartridge(p.mainHandItem)) { if(!p.isCreative)p.mainHandItem.shrink(1);unit.applyContacts();p.inventory.setChanged();needPublish();return true }
        }
        return super.onBlockActivated(p,side,x,y,z)
    }
    override fun multiMeterString(side: Direction)=unit.lines.mapIndexed { i,l->tr("Phase %1$: %2$",i+1,Utils.plotUIP(l.u,l.i)) }.joinToString("\n")
    override fun thermoMeterString(side: Direction): String?=null
    override fun getWaila()=linkedMapOf(tr("Cable terminal") to tr("Terminal %1$",terminal),tr("Protection") to when { unit.recloser.locked->tr("Recloser locked out");unit.fuses.any { it.blown }->tr("Fuse blown");else->tr("Ready") })
    private fun settings(t: CompoundTag) { unit.write(t);t.putInt("utilityTerminal",terminal) }
    private fun readSettings(t: CompoundTag) { unit.read(t);terminal=t.getIntOr("utilityTerminal",if(kind.lvOnly)50 else 32).takeIf { it in terminals() } ?: terminals().first() }
    override fun writeToNBT(t: CompoundTag) { super.writeToNBT(t);settings(t) }
    override fun readFromNBT(t: CompoundTag) { super.readFromNBT(t);readSettings(t) }
    override fun getItemStackNBT()=CompoundTag().also(::settings)
    override fun readItemStackNBT(t: CompoundTag?) { if(t!=null)readSettings(t) }
    override fun networkSerialize(out: DataOutputStream) { super.networkSerialize(out);DistributionDisplay().apply { capture(this@DistributionPoleElement);write(out) } }
    override fun networkUnserialize(s: DataInputStream): Byte {
        val id=super.networkUnserialize(s)
        try { when(id.toInt()) {
            87->selectTerminal(s.readInt());88->selectPhase(s.readInt());89->{ val index=s.readInt();val loaded=kotlin.math.abs(unit.current(index))>.02;val closed=unit.fuses.getOrNull(index)?.closed==true
                if(unit.operateFuse(index) && loaded && closed)play(mods.eln.sound.SoundCommand("eln:utility_arc_open").smallRange().mulVolume(.5)) }
            91->unit.configureRecloser(s.readDouble(),unit.recloser.retrySeconds,unit.recloser.maxFailures)
            92->unit.configureRecloser(unit.recloser.pickupAmps,s.readDouble(),unit.recloser.maxFailures)
            93->unit.configureRecloser(unit.recloser.pickupAmps,unit.recloser.retrySeconds,s.readInt())
            94->unit.resetRecloser();95->unit.switch(!if(kind.recloser)unit.recloser.closed else unit.gangClosed)
            96->unit.configureRegulator(s.readDouble(),unit.regulatorAutomatic);97->unit.configureRegulator(unit.regulatorTarget,s.readBoolean())
        } } catch(_: IOException) {}
        unit.applyContacts();needPublish();return id
    }
}
