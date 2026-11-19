package mods.eln.utility


import mods.eln.Eln
import mods.eln.gridnode.*
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sound.SoundCommand
import mods.eln.transparentnode.standby.TransferContact
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.io.DataInputStream
import java.io.DataOutputStream


class SubstationElement(node: TransparentNode,descriptor: TransparentNodeDescriptor):
    GridElement(node,descriptor,(descriptor as SubstationDescriptor).kind.range) {
    val kind=(descriptor as SubstationDescriptor).kind
    private val items=TransparentNodeElementInventory(0,1,this)
    override fun getInventory()=items
    val protection=SubstationProtection()
    val inputs=List(3) { NbtElectricalLoad("transmissionInput$it").apply { rs=.00001 } }
    val outputs=if(kind.station)List(3) { NbtElectricalLoad("transmissionOutput$it").apply { rs=.00001 } } else inputs
    val services=emptyList<NbtElectricalLoad>()
    var primaryClosed=true;private set
    var secondaryClosed=true;private set
    private var selected=52
    private val primaryContacts=ArrayList<TransferContact>()
    private val secondaryContacts=ArrayList<TransferContact>()
    val taps=ArrayList<ServiceTap>()
    init {
        electricalLoadList.addAll(inputs)
        electricalLoadList.addAll(services)
        if(outputs!==inputs)electricalLoadList.addAll(outputs)
        if(kind.transformer)repeat(3) { phase->
            val primary=NbtElectricalLoad("windingPrimary$phase")
            val secondary=NbtElectricalLoad("windingSecondary$phase")
            val output=NbtElectricalLoad("windingOutput$phase")
            electricalLoadList.addAll(listOf(primary,secondary,output))
            primaryContacts+=TransferContact(inputs[phase],primary)
            taps+=ServiceTap(primary,secondary,kind.ratio)
            // 0.5% copper loss at nominal design loading. Pure passive linear
            // physics: fluctuations and reverse power are not hidden by a regulator.
            electricalComponentList+=Resistor(secondary,output).apply {
                r=kind.outputVolts*kind.outputVolts/kind.wattsPerPhase*.005
            }
            secondaryContacts+=TransferContact(output,outputs[phase])
        } else if(kind.disconnect)repeat(3) { primaryContacts+=TransferContact(inputs[it],outputs[it]) }
        electricalLoadList.filterIsInstance<NbtElectricalLoad>().forEach{electricalComponentList.add(Resistor(it,null).apply{r=1e12})}
        electricalComponentList.addAll(taps);electricalComponentList.addAll(primaryContacts);electricalComponentList.addAll(secondaryContacts)
        slowProcessList.add(NodePeriodicPublishProcess(node,1.0,.5))
        electricalProcessList.add(mods.eln.sim.IProcess { dt->
            val voltage=(inputs.map{ kotlin.math.abs(it.u)/kind.inputVolts }+outputs.map{kotlin.math.abs(it.u)/kind.outputVolts}).maxOrNull()?:0.0
            val primaryCurrent=primaryContacts.maxOfOrNull{kotlin.math.abs(it.current)/(kind.wattsPerPhase/kind.inputVolts)}?:0.0
            val secondaryCurrent=secondaryContacts.maxOfOrNull{kotlin.math.abs(it.current)/(kind.wattsPerPhase/kind.outputVolts)}?:0.0
            if(protection.sample(dt,voltage,maxOf(primaryCurrent,secondaryCurrent))) { primaryClosed=false;secondaryClosed=false;applyContacts();needPublish() }
        })
        applyContacts()
    }
    private fun applyContacts() {
        primaryContacts.forEach { it.closed=primaryClosed }
        secondaryContacts.forEach { it.closed=secondaryClosed }
    }
    fun operate(primary: Boolean) {
        if(!kind.station || !primary && !kind.transformer)return
        if(protection.tripped) {
            val voltage=(inputs.map{kotlin.math.abs(it.u)/kind.inputVolts}+outputs.map{kotlin.math.abs(it.u)/kind.outputVolts}).maxOrNull()?:0.0
            if(!primary||!protection.reset(voltage))return
        }
        if(primary)primaryClosed=!primaryClosed else secondaryClosed=!secondaryClosed
        applyContacts();needPublish()
        if(coordonate().world() is net.minecraft.server.level.ServerLevel)
            play(SoundCommand("eln:utility_recloser_bump").smallRange().mulVolume(.65))
    }
    override fun getGridElectricalLoad(side: Direction): ElectricalLoad?=inputs.first()
    override fun selectedGridTerminal(side: Direction)=selected
    override fun gridLoads(terminal: Int): List<ElectricalLoad> = when {
        !kind.station && terminal==50->services
        !kind.station && terminal in 40..42->listOf(services[terminal-40])
        // Keep the direction-based GridElement API consistent with its exposed
        // single-load adapter. Interactive links use the explicit phase terminals.
        terminal in 0..5->listOf(inputs.first())
        terminal in 32..34->listOf(inputs[terminal-32])
        terminal in 36..38 && kind.station->listOf(outputs[terminal-36])
        terminal==52->inputs
        terminal==53 && kind.station->outputs
        else->emptyList()
    }
    override fun renderWireCount(terminal: Int)=if(terminal==50 || terminal in 52..53)3 else 1
    override fun gridPhase(terminal: Int): Int?=when(terminal) { in 32..34->terminal-32;in 36..38->terminal-36;else->null }
    override fun gridCircuitKey(terminal: Int)=when(terminal) { in 32..34->"HV${terminal-32}";in 36..38->"HV${terminal-36}";52,53->"HV_THREE_PHASE";in 0..5->"HV0";else->"INVALID" }
    override fun gridCablePoint(terminal: Int,wire: Int): Vec3 {
        if(!kind.station && terminal in 40..42 || terminal==50)return ServiceCableMount.point(terminal,wire)
        if(terminal==52)return gridCablePoint(32+wire,0)
        if(terminal==53)return gridCablePoint(36+wire,0)
        val phase=gridPhase(terminal) ?: 0
        return if(kind.station)Vec3(if(terminal in 36..38)2.5 else -2.5,kind.height-.65,(phase-1)*kind.spread)
            else Vec3(0.0,kind.height-.75,(phase-1)*kind.spread)
    }
    override fun renderedGridCablePoint(terminal:Int,wire:Int)=gridCablePoint(terminal,wire).yRot(Math.toRadians(TransmissionKind.facingAngle(front).toDouble()).toFloat())
    private fun modelHit(hit: Vec3)=hit.yRot(-Math.toRadians(TransmissionKind.facingAngle(front).toDouble()).toFloat())
    override fun getThermalLoad(side:Direction,lrdu:LRDU):mods.eln.sim.ThermalLoad?=null
    override fun getElectricalLoad(side: Direction,lrdu: LRDU): ElectricalLoad? {
        if(!kind.station && side==Direction.YN)return services[0]
        if(!kind.riser || lrdu!=LRDU.Down || side.isY)return null
        return when(side) { front.left()->inputs[0];front.right()->inputs[1];front.inverse->inputs[2];else->null }
    }
    override fun getConnectionMask(side: Direction,lrdu: LRDU)=if(getElectricalLoad(side,lrdu)!=null)NodeBase.maskElectricalPower else 0
    override fun hasGui()=true
    override fun onBlockActivated(player:Player,side:Direction,x:Float,y:Float,z:Float)=false
    override fun multiMeterString(side: Direction)=inputs.mapIndexed { i,p->"${listOf("A","B","C")[i]} ${Utils.plotVolt("",p.u)} ${Utils.plotAmpere("",p.current)}" }.joinToString("\n")+
        if(kind.station)outputs.mapIndexed { i,p->"\n${tr("Output")} ${listOf("A","B","C")[i]} ${Utils.plotVolt("",p.u)} ${Utils.plotAmpere("",p.current)}" }.joinToString("") else services.mapIndexed { i,p->"\n${"Service"} ${Utils.plotVolt("",p.u)} ${Utils.plotAmpere("",p.current)}" }.joinToString("")
    override fun thermoMeterString(side: Direction)=""
    private fun writeSettings(tag: CompoundTag) { protection.write(tag); tag.putBoolean("transmissionPrimaryClosed",primaryClosed);tag.putBoolean("transmissionSecondaryClosed",secondaryClosed);tag.putInt("transmissionTerminal",selected) }
    private fun readSettings(tag: CompoundTag) {
        protection.read(tag)
        primaryClosed=!tag.contains("transmissionPrimaryClosed") || tag.getBooleanOr("transmissionPrimaryClosed",false)
        secondaryClosed=!tag.contains("transmissionSecondaryClosed") || tag.getBooleanOr("transmissionSecondaryClosed",false)
        val terminal=tag.getIntOr("transmissionTerminal",0);selected=if(gridLoads(terminal).isNotEmpty())terminal else 52
        if(protection.tripped){primaryClosed=false;secondaryClosed=false}
        applyContacts()
    }
    override fun writeToNBT(tag: CompoundTag) { super.writeToNBT(tag);writeSettings(tag) }
    override fun readFromNBT(tag: CompoundTag) { super.readFromNBT(tag);readSettings(tag) }
    override fun getItemStackNBT(): CompoundTag=CompoundTag().also(::writeSettings)
    override fun readItemStackNBT(tag: CompoundTag?) { super.readItemStackNBT(tag);if(tag!=null)readSettings(tag) }
    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream);stream.writeBoolean(primaryClosed);stream.writeBoolean(secondaryClosed);stream.writeInt(selected)
        inputs.forEach { stream.writeDouble(it.u) };outputs.forEach { stream.writeDouble(it.u) };stream.writeBoolean(protection.tripped);stream.writeDouble(protection.heat)
    }
    override fun networkUnserialize(stream: DataInputStream): Byte {
        val event=super.networkUnserialize(stream)
        when(event.toInt()) {
            80->operate(true);81->operate(false)
            82->{ val next=stream.readInt();if(gridLoads(next).isNotEmpty()) { selected=next;needPublish() } }
        }
        return event
    }
}
