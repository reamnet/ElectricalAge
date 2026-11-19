package mods.eln.vehicle

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.ItemNbt
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.power.ConservativePowerProcess
import mods.eln.transparentnode.cord.CordNbt
import mods.eln.transparentnode.cord.CordSupport
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.io.DataOutputStream
import java.lang.ref.WeakReference
import java.util.UUID
import kotlin.math.*

object VehicleChargers {
    private val loaded=HashMap<UUID,WeakReference<VehicleChargerElement>>()
    fun register(charger:VehicleChargerElement) {
        loaded.entries.removeIf {it.value.get()?.let{owner->owner.identity!=it.key}?:true}
        val old=loaded[charger.identity]?.get()
        if(old==null||old===charger||!CordSupport.resident(old))loaded[charger.identity]=WeakReference(charger)
    }
    fun find(id:UUID)=loaded[id]?.get()?.takeIf {it.identity==id&&it.isLoaded()}
    fun connected(vehicle:RoadVehicleEntity)=vehicle.charger?.let(::find)?.let {it.vehicle()===vehicle}==true
    fun remove(charger:VehicleChargerElement) {if(loaded[charger.identity]?.get()===charger)loaded.remove(charger.identity)}
}
class VehicleLeadItem:GenericItemUsingDamageDescriptor(TITLE,"flexibleappliancecord") {
    companion object {val TITLE=I18N.TR_NAME(I18N.Type.NONE,"100 A Vehicle Charging Lead")}
    fun bind(stack:ItemStack,charger:VehicleChargerElement) {ItemNbt.edit(stack){CordNbt.putUuid(it,"vehicleCharger",charger.identity)}}
    fun connect(stack:ItemStack,player:Player,vehicle:RoadVehicleEntity) {
        if(player.isShiftKeyDown&&vehicle.parked) {vehicle.charger?.let(VehicleChargers::find)?.unplug();vehicle.charger=null;return}
        val charger=ItemNbt.get(stack)?.let {CordNbt.uuid(it,"vehicleCharger")}?.let(VehicleChargers::find)
        val ok=charger?.attach(vehicle)==true
        player.sendOverlayMessage(Component.literal(if(ok)tr("Charging lead connected")else tr("Select a loaded vehicle charger first, then a parked vehicle within six blocks")))
    }
    override fun addInformation(stack:ItemStack?,player:Player?,lines:MutableList<Any?>,advanced:Boolean) {
        lines.add(tr("Use on a 240 V 100 A Vehicle Charger, then right-click a parked vehicle within six blocks"))
        lines.add(tr("Sneak-right-click the vehicle with the lead to disconnect"))
    }
}

/** Native measured load; solver retries can alter its resistor but never charge a battery. */
class VehicleChargerElement(node:TransparentNode,descriptor:TransparentNodeDescriptor):TransparentNodeElement(node,descriptor) {
    private val items=TransparentNodeElementInventory(0,1,this)
    override fun getInventory()=items
    val input=NbtElectricalLoad("vehicleChargerInput").apply {rs=.0001}
    val sink=Resistor(input,null).apply {r=1e19}
    var identity=UUID.randomUUID();private set
    private var target:UUID?=null
    var enabled=true;private set
    var watts=0.0;private set
    var stored=0.0;private set
    var state="IDLE";private set
    private var seconds=.05
    private var trialVolts=0.0
    val controller=object:ConservativePowerProcess {
        override fun beginStep(dt:Double) {seconds=dt.takeIf{it.isFinite()&&it>0}?:.05;trialVolts=abs(input.u)}
        override fun prepareStep() {prepare(trialVolts)}
        override fun rootSystemPreStepProcess() {prepare(trialVolts)}
        override fun acceptsCandidate():Boolean {
            val volts=abs(input.subSystem?.pendingValue(input)?:input.u)
            val e=vehicle()
            val expected=if(enabled&&e!=null&&VehicleChargers.find(identity)===this@VehicleChargerElement)RoadPhysics.chargerResistance(volts,e.chargeDemand(seconds))else 1e19
            val matches=volts.isFinite()&&abs(sink.r-expected)<=maxOf(1e-10,expected*1e-9)
            if(!matches){trialVolts=volts;return false}
            return sink.r>=1e18 || volts/sink.r<=100.0+1e-7 && volts*volts/sink.r<=24000.0+1e-5
        }
        override fun failClosed() {sink.r=1e19;state="WAIT_POWER"}
        override fun connectedSystems()=setOfNotNull(input.subSystem)
        override fun isolatesLoadOnFailure()=true
    }
    init {
        electricalLoadList.add(input);electricalComponentList.add(sink)
        electricalProcessList.add(IProcess(::account));slowProcessList.add(NodePeriodicPublishProcess(node,1.0,.5))
    }
    private fun loadedWorld():ServerLevel? {
        val c=node.coordonate;val world=c.world() as? ServerLevel ?: return null
        return world.takeIf {it.chunkSource.getChunkNow(c.x shr 4,c.z shr 4)!=null}
    }
    fun isLoaded()=CordSupport.active(this)&&loadedWorld()!=null
    fun vehicle():RoadVehicleEntity? {
        val e=target?.let{loadedWorld()?.getEntity(it) as? RoadVehicleEntity}?:return null
        val p=node.coordonate
        return e.takeIf{it.isAlive&&it.parked&&!it.isVehicle&&it.charger==identity&&it.position().distanceToSqr(net.minecraft.world.phys.Vec3(p.x+.5,p.y+.5,p.z+.5))<=36.0}
    }
    fun attach(e:RoadVehicleEntity):Boolean {
        VehicleChargers.register(this)
        if(VehicleChargers.find(identity)!==this||loadedWorld()!==e.level()||!e.parked||e.isVehicle||e.charger!=null&&e.charger!=identity)return false
        val p=node.coordonate
        if(e.position().distanceToSqr(net.minecraft.world.phys.Vec3(p.x+.5,p.y+.5,p.z+.5))>36)return false
        unplug();target=e.uuid;e.charger=identity;prepare();needPublish();return true
    }
    fun unplug() {target?.let{loadedWorld()?.getEntity(it) as? RoadVehicleEntity}?.takeIf{it.charger==identity}?.charger=null;target=null;sink.r=1e19;watts=0.0;needPublish()}
    override fun initialize() {VehicleChargers.register(this);connect()}
    override fun connectJob() {super.connectJob();Eln.simulator.mna.addProcess(controller)}
    override fun disconnectJob() {Eln.simulator.mna.removeProcess(controller);watts=0.0;super.disconnectJob()}
    internal fun prepare(v:Double=abs(input.u)) {
        val e=vehicle();val demand=e?.chargeDemand(seconds)?:0.0
        val owned=VehicleChargers.find(identity)===this
        state=when{!owned->"DUPLICATE";!enabled->"OFF";e==null->"IDLE";demand<.01->"FULL";!v.isFinite()||v>264->"OVERVOLTAGE";v<200->"WAIT_POWER";else->"CHARGING"}
        sink.r=if(owned&&enabled&&e!=null)RoadPhysics.chargerResistance(v,demand)else 1e19
        needPublish()
    }
    internal fun account(dt:Double) {
        if(!dt.isFinite()||dt<=0)return
        val e=vehicle();val v=abs(input.u)
        watts=if(e!=null&&enabled&&VehicleChargers.find(identity)===this&&v in 200.0..264.0)sink.p.takeIf{it.isFinite()}?.coerceIn(0.0,min(24000.0,v*100))?:0.0 else 0.0
        if(watts>0&&e!=null)stored+=e.acceptCharge(watts*dt,dt)
    }
    fun setRemoteEnabled(value:Boolean){enabled=value;prepare();needPublish()}
    override fun onBlockActivated(player:Player,side:Direction,vx:Float,vy:Float,vz:Float):Boolean {
        val lead=GenericItemUsingDamageDescriptor.getDescriptor(player.mainHandItem) as? VehicleLeadItem
        if(lead!=null){lead.bind(player.mainHandItem,this);player.sendOverlayMessage(Component.literal(tr("Vehicle charger selected; use the lead on a parked vehicle")));return true}
        player.sendOverlayMessage(Component.literal(tr("Vehicle charger: %1$ V, %2$ A, %3$ W",abs(input.u).toInt(),abs(input.current).toInt(),watts.toInt())));return true
    }
    override fun getElectricalLoad(side:Direction,lrdu:LRDU)=if((side==front.left()||side==Direction.YN)&&lrdu==LRDU.Down)input else null
    override fun getConnectionMask(side:Direction,lrdu:LRDU)=if(getElectricalLoad(side,lrdu)!=null)NodeBase.maskElectricalPower else 0
    override fun getThermalLoad(side:Direction,lrdu:LRDU)=null
    override fun multiMeterString(side:Direction)=Utils.plotUIP(input.u,input.current)
    override fun thermoMeterString(side:Direction)=""
    override fun getWaila()=linkedMapOf(tr("Supply")to Utils.plotUIP(input.u,input.current),tr("Power")to Utils.plotPower("",watts),tr("Status")to chargerStatus(state),tr("Stored this session")to Utils.plotEnergy("",stored))
    override fun networkSerialize(out:DataOutputStream){super.networkSerialize(out);out.writeDouble(input.u);out.writeDouble(watts);out.writeDouble(stored);out.writeUTF(state);out.writeLong(target?.let{loadedWorld()?.getEntity(it)?.id}?.toLong()?:-1)}
    override fun writeToNBT(tag:CompoundTag){super.writeToNBT(tag);CordNbt.putUuid(tag,"vehicleChargerIdentity",identity);tag.putBoolean("vehicleChargerEnabled",enabled)}
    override fun readFromNBT(tag:CompoundTag){super.readFromNBT(tag);identity=CordNbt.uuid(tag,"vehicleChargerIdentity")?:identity;enabled=tag.getBooleanOr("vehicleChargerEnabled",true)}
    override fun onBreakElement(){unplug();VehicleChargers.remove(this);super.onBreakElement()}
}
fun chargerStatus(state:String)=when(state){"CHARGING"->tr("Charging");"FULL"->tr("Fully charged");"OFF"->tr("Disabled");"OVERVOLTAGE"->tr("Overvoltage disconnected");"WAIT_POWER"->tr("Waiting for 240 V supply");"DUPLICATE"->tr("Duplicate charger identity");else->tr("Connect a parked vehicle")}
