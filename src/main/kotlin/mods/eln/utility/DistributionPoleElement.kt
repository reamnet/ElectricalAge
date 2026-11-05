package mods.eln.utility

import mods.eln.Eln
import mods.eln.gridnode.*
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.transparentnode.utilitylight.StreetLightMount
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.io.*

class DistributionPoleElement(node: TransparentNode,d: TransparentNodeDescriptor): GridElement(node,d,64) {
    val kind=(d as DistributionPoleDescriptor).kind
    val unit=DistributionPoleUnit(kind,electricalLoadList,electricalComponentList,electricalProcessList)
    val hardware=object: TransparentNodeElementInventory(if(kind.twinLight)2 else if(kind.transformer)1 else 0,1,this) {
        override fun getAccessibleSlotsFromSide(side: Int)=if(kind.twinLight)intArrayOf(0,1) else if(kind.transformer)intArrayOf(0) else intArrayOf()
        override fun canInsertItem(slot: Int,stack: ItemStack?,side: Int)=kind.transformer && slot==0 && PoleHardware.transformerWatts(stack)!=null
        override fun canExtractItem(slot: Int,stack: ItemStack?,side: Int)=if(kind.twinLight)slot in 0..1 else kind.transformer && slot==0
        override fun setInventorySlotContents(slot: Int,stack: ItemStack) { super.setInventorySlotContents(slot,stack);markDirty() }
    }
    private var twinRecovered=false
    override fun getInventory()=hardware
    private fun installTransformer() { if(kind.transformer)unit.installTransformer(PoleHardware.transformerWatts(hardware.getItem(0))) }
    override fun initialize() { installTransformer();super.initialize() }
    override fun inventoryChange(inventory: Container?) { installTransformer();needPublish() }
    var terminal=if(kind.lvOnly)50 else 32;private set
    init { unit.wiredSplitAllowed={gridLinkList.isEmpty() && gridLinksBooting.isEmpty()};unit.transformer?.let { thermalLoadList.add(it.thermal) };unit.changed={ event->needPublish();play(mods.eln.sound.SoundCommand("eln:$event").smallRange().mulVolume(.5)) };slowProcessList.add(NodePeriodicPublishProcess(node,1.0,.5));slowProcessList.add(IProcess { refreshLight() }) }
    private fun refreshLight() {
        val lamp=unit.lamp ?: return
        if(!lamp.installed)return
        val at=node.coordonate ?: return
        val world=at.world() as? net.minecraft.server.level.ServerLevel ?: return
        lamp.night=world.skyDarken>=4
        if(lamp.sample())needPublish()
        if(lamp.light>0)for(head in kind.lampHeads) {
            val offset=StreetLightMount.adjust(head,front,lamp.heightStep,lamp.yaw,lamp.pitch)
            val pos=at.pos().offset(kotlin.math.floor(offset.x+.5).toInt(),kotlin.math.floor(offset.y+.5).toInt(),kotlin.math.floor(offset.z+.5).toInt())
            if(world.chunkSource.getChunkNow(pos.x shr 4,pos.z shr 4)!=null)mods.eln.sixnode.lampsocket.LightBlockEntity.addLight(world,pos.x,pos.y,pos.z,lamp.light,10)
        }
    }
    override fun acceptsSavedGridTerminal(t: Int)=(!kind.twinLight || t==50 || t in 40..42) && super.acceptsSavedGridTerminal(t)
    override fun allowsNewGridLinks()=!kind.underground
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
        t==36 && kind.singlePhaseTap->Vec3(-.68,5.85,if(kind.transformer).65 else 0.0)
        t in 36..38 && kind.cutout->Vec3(-.68,5.85,phaseZ(t-36))
        t in 36..38 && kind.regulator->Vec3(.65,5.50,phaseZ(t-36)*.73)
        t in 36..38->Vec3(.35,6.53,phaseZ(t-36))
        else->Vec3(0.0,6.63,phaseZ((t-32).coerceIn(0,kind.phases-1)))
    }
    private fun phaseZ(i: Int)=if(kind.phases==1)0.0 else (i-1)*.95
    override fun getElectricalLoad(side: Direction,lrdu: LRDU): ElectricalLoad? {
        if(side==Direction.YN) { if(kind.pedestal)return unit.undergroundInput[when(lrdu) { LRDU.Left->0;LRDU.Right->1;else->2 }];if(kind.singlePrimaryRiser && lrdu==LRDU.Up)return unit.outs[0];return unit.services[when(lrdu) { LRDU.Left->0;LRDU.Right->1;else->2 }] }
        if(kind.underground && !kind.pedestal) {
            if(side==front)return unit.services[when(lrdu) { LRDU.Left->0;LRDU.Right->1;else->2 }]
            val i=when(side) { front.left()->0;front.right()->1;front.inverse->2;else->return null }
            if(i>=kind.phases)return null
            return when(lrdu) { LRDU.Down->unit.lines[i];LRDU.Up->unit.outs[i];else->null }
        }
        if(side.isY || lrdu!=LRDU.Down)return null
        val i=when(side) { front.left()->0;front.right()->1;front.inverse->2;else->return null }
        return if(kind.pedestal)unit.services[i] else if(kind==DistributionKind.FEEDER_THREE)unit.lines[i] else if(kind.singlePrimaryRiser)when(i) { 0->unit.outs[0];1->unit.services[2];else->unit.services[0] } else unit.services[i]
    }
    override fun getConnectionMask(side: Direction,l: LRDU)=if(getElectricalLoad(side,l)!=null)NodeBase.maskElectricalPower else 0
    override fun getThermalLoad(side: Direction,l: LRDU)=null
    override fun hasGui()=true
    fun selectPhase(value: Int): Boolean {
        if(value !in unit.lines.indices || !(kind.singlePhaseTap || kind.transformer))return false
        for(link in gridLinkList)if(link.getTerminal(this)==36) {
            val other=link.getOtherElement(this) ?: continue
            if(GridPhaseRouting.phases(other,link.getTerminal(other),this).any { it!=value })return false
        }
        val changed=unit.selectPhase(value);if(changed)needPublish();return changed
    }
    override fun onBlockActivated(p: Player,side: Direction,x: Float,y: Float,z: Float): Boolean {
        if(!kind.light && !kind.underground && UtilityItems.part(p.mainHandItem)==3) {
            if(unit.lamp?.install()==true) { if(!p.isCreative)p.mainHandItem.shrink(1);p.inventory.setChanged();needPublish() };return true
        }
        if(kind.transformer && PoleHardware.transformerWatts(p.mainHandItem)!=null && hardware.getItem(0).isEmpty) {
            hardware.setItem(0,p.mainHandItem.copyWithCount(1));if(!p.isCreative)p.mainHandItem.shrink(1)
            inventoryChange(hardware);p.inventory.setChanged();return true
        }
        if(PoleHardware.extract(p,unit.fuses)) { unit.applyContacts();needPublish();return true }
        if(UtilityItems.isFuse(p.mainHandItem)) {
            val f=unit.fuses.firstOrNull { !it.hasCartridge || it.blown }
            if(f!=null && f.insertCartridge(p.mainHandItem)) { if(!p.isCreative)p.mainHandItem.shrink(1);unit.applyContacts();p.inventory.setChanged();needPublish();return true }
        }
        return super.onBlockActivated(p,side,x,y,z)
    }
    override fun multiMeterString(side: Direction)=buildList {
        if(!kind.lvOnly)addAll(unit.lines.mapIndexed { i,l->tr("Phase %1$: %2$",i+1,Utils.plotUIP(l.u,l.i)) })
        if(kind.pedestal)add(tr("Incoming 240 V: %1$",Utils.plotVolt("",unit.undergroundInput[2].u)))
        addAll(unit.services.mapIndexed { i,l->tr("Service %1$: %2$",i+1,Utils.plotUIP(l.u,l.i)) })
    }.joinToString("\n")
    override fun thermoMeterString(side: Direction): String?=unit.transformer?.let { Utils.plotCelsius("",it.thermal.Tc) }
    override fun getWaila()=linkedMapOf(tr("Cable terminal") to tr("Terminal %1$",terminal),tr("Protection") to when { unit.recloser.locked->tr("Recloser locked out");unit.fuses.any { it.blown }->tr("Fuse blown");else->tr("Ready") }).apply { unit.transformer?.let { put(tr("Transformer rating"),Utils.plotPower("",it.ratedWatts.toDouble()));put(tr("Transformer status"),poleTransformerStatus(it.status));put(tr("Temperature"),Utils.plotCelsius("",it.thermal.Tc)) } }
    private fun settings(t: CompoundTag) { unit.write(t);t.putInt("utilityTerminal",terminal) }
    private fun readSettings(t: CompoundTag) { val settings=TwinPoleMigration.settings(kind,t);unit.read(settings);terminal=settings.getIntOr("utilityTerminal",if(kind.lvOnly)50 else 32).takeIf { it in terminals() } ?: terminals().first() }
    override fun writeToNBT(t: CompoundTag) { super.writeToNBT(t);settings(t) }
    override fun readFromNBT(t: CompoundTag) { super.readFromNBT(t);recoverTwinHardware(t);if(kind.transformer)PoleHardware.migrate(hardware,t.getIntOr("mainsRatedWatts",10000));readSettings(t);installTransformer() }
    override fun getItemStackNBT()=CompoundTag().also { settings(it);if(!kind.light)it.getCompoundOrEmpty("poleStreetlightAttachment").putBoolean("installed",false) }
    override fun onBreakElement() { if(unit.lamp?.remove()==true)node.dropItem(Eln.findItemStack(UtilityItems.STREETLIGHT_NAME,1));super.onBreakElement() }
    override fun readItemStackNBT(t: CompoundTag?) { if(t!=null) { recoverTwinHardware(t);readSettings(t) } }
    private fun recoverTwinHardware(t: CompoundTag) {
        if(twinRecovered || !TwinPoleMigration.legacy(kind,t))return
        twinRecovered=true
        PoleHardware.migrate(hardware,t.getIntOr("mainsRatedWatts",10000))
        // A control/rating snapshot alone is not an inventory. A former fuse record is required.
        if(hardware.getItem(1).isEmpty && (t.contains("poleFuseCartridge") || t.contains("poleFuseClosed") || t.contains("poleFuseBlown"))) {
            val old=CutoutFuse();old.read(t,"poleFuse")
            if(old.hasCartridge)hardware.setItem(1,old.removeCartridge())
        }
    }
    override fun networkSerialize(out: DataOutputStream) { super.networkSerialize(out);DistributionDisplay().apply { capture(this@DistributionPoleElement);write(out) } }
    override fun networkUnserialize(s: DataInputStream): Byte {
        val id=super.networkUnserialize(s)
        try { when(id.toInt()) {
            107->if(!kind.light || kind.dualLampVoltage)unit.lamp?.chooseSupply(s.readInt())
            108->{ if(unit.lamp?.remove()==true)node.dropItem(Eln.findItemStack(UtilityItems.STREETLIGHT_NAME,1)) }
            102->unit.lamp?.let { it.configure(s.readBoolean(),it.automatic) }
            103->unit.lamp?.let { it.configure(it.enabled,s.readBoolean()) }
            104->unit.lamp?.let { lamp->val move=s.readInt();if(move in listOf(-1,1))lamp.aim((lamp.heightStep+move).coerceIn(StreetLightMount.MIN_STEP,StreetLightMount.MAX_STEP),lamp.yaw,lamp.pitch) }
            105->unit.lamp?.let { lamp->val move=s.readInt();if(move in listOf(-5,-1,1,5))lamp.aim(lamp.heightStep,StreetLightMount.turn(lamp.yaw,move),lamp.pitch) }
            106->unit.lamp?.let { lamp->val move=s.readInt();if(move in listOf(-5,-1,1,5))lamp.aim(lamp.heightStep,lamp.yaw,(lamp.pitch+move).coerceIn(-60,60)) }
            80->unit.configureTransformer(s.readInt(),unit.transformer?.enabled ?: true)
            81->unit.configureTransformer(unit.transformer?.outputVoltage ?: 120,s.readBoolean())
            82->unit.resetTransformer()
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
