package mods.eln.transparentnode.kitchen

import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sound.SoundCommand
import mods.eln.transparentnode.cord.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.abs

class KitchenElement(node: TransparentNode,descriptor: TransparentNodeDescriptor) : TransparentNodeElement(node,descriptor),CordDevice {
    val type=(descriptor as KitchenDescriptor).type
    private val items=object : TransparentNodeElementInventory(type.slotCount,64,this) {
        override fun startOpen(player: net.minecraft.world.entity.ContainerUser) {
            if(type.refrigerator && player is net.minecraft.server.level.ServerPlayer && viewers.add(player.uuid))needPublish()
        }
        override fun stopOpen(player: net.minecraft.world.entity.ContainerUser) {
            if(player is net.minecraft.server.level.ServerPlayer && viewers.remove(player.uuid))needPublish()
        }
        override fun getAccessibleSlotsFromSide(side: Int)=IntArray(containerSize) { it }
        override fun canInsertItem(slot: Int,stack: ItemStack,side: Int)=stack!=null && KitchenRecipe.allowed(type,slot,stack)
        override fun canExtractItem(slot: Int,stack: ItemStack,side: Int)=slot==2 || slot==1 && (stack?.`is`(net.minecraft.world.item.Items.BUCKET)==true || stack?.`is`(net.minecraft.world.item.Items.GLASS_BOTTLE)==true) || slot>=3
    }
    override fun getInventory()=items
    val load=NbtElectricalLoad("applianceInput")
    val resistor=Resistor(load,null)
    override val cord=ApplianceCord(this,this,load)
    override fun acceptsCord(rating: CordRating)=rating.accepts(type.volts)
    override val cordPortable get()=type!=KitchenType.FRIDGE
    val work=KitchenWork()
    val batteryHeat=MicrowaveBatteryHeat()
    val batteryJob get()=type==KitchenType.MICROWAVE && MicrowaveBatteryHeat.accepts(inventory.getItem(0))
    var detonating=false
        private set
    private var cachedInputReference: ItemStack?=null
    var enabled=true
    var active=false
        private set
    var temperature=20.0
        private set
    private val viewers=HashSet<java.util.UUID>()
    val doorOpen get()=type.refrigerator && viewers.isNotEmpty()
    private var roomAmbient=20.0
    private var roomClock=0.0
    var status="IDLE"
        private set
    val coldStorage get()=type.refrigerator && temperature<=5.0
    private var foodClock=-1L
    private var foodWasCold=false
    private var recipe: KitchenRecipe?=null
    private var cachedInput=ItemStack.EMPTY
    private var cacheReady=false
    private var publishedStatus=""
    private var paidJoules=0.0
    init {
        load.rs=.002
        electricalLoadList.add(load);electricalComponentList.add(resistor)
        electricalProcessList.add(IProcess { dt->
            val p=resistor.p
            if(requested && dt.isFinite() && dt>0 && p.isFinite() && p>0 && abs(load.u) in type.volts*.8..type.volts*1.2 && resistor.r<1e8)paidJoules+=dt*p.coerceAtMost(type.watts*1.1)
        })
        slowProcessList.add(IProcess(::tick))
        slowProcessList.add(NodePeriodicPublishProcess(node,.5,0.0))
    }
    override fun initialize() { rebuildRecipe(false);configure();connect() }
    private fun rebuildRecipe(reset: Boolean) {
        val input=inventory.getItem(0)
        if(reset && (input!==cachedInputReference || input.count!=cachedInput.count || !ItemStack.isSameItemSameComponents(input,cachedInput))) {
            work.joules=0.0;paidJoules=0.0;batteryHeat.reset()
        }
        recipe=(coordonate().world() as? ServerLevel)?.let { KitchenRecipe.find(it,type,inventory) }
        cachedInput=input.copy();cachedInputReference=input;cacheReady=true
    }
    override fun inventoryChange(inventory: Container?) { rebuildRecipe(true);configure();needPublish() }
    private val requested get()=enabled && !detonating && (batteryJob || recipe?.canFit(inventory)==true || type.refrigerator && temperature>4.5)
    private fun configure() {
        val connected=requested && abs(load.u)<=type.volts*1.2
        active=connected && abs(load.u)>=type.volts*.8
        resistor.r=if(connected)type.volts*type.volts/type.watts else 1e9
    }
    private fun tick(seconds: Double) {
        if(!cacheReady)rebuildRecipe(false)
        val input=inventory.getItem(0)
        if(input!==cachedInputReference || input.count!=cachedInput.count || !ItemStack.isSameItemSameComponents(input,cachedInput))rebuildRecipe(true)
        configure()
        val paid=paidJoules;paidJoules=0.0
        val power=if(seconds.isFinite()&&seconds>0)paid/seconds else 0.0
        if(type.refrigerator) {
            roomClock-=seconds
            if(roomClock<=0.0) {
                roomClock=1.0
                val world=node?.coordonate?.world() as? ServerLevel
                if(world!=null) {
                    if(viewers.removeIf { id ->
                        val p=world.server.playerList.getPlayer(id)
                        p==null || p.level()!==world || (p.containerMenu as? mods.eln.gui.ElnNodeMenu)?.deviceInventory!==inventory
                    })needPublish()
                    roomAmbient=20.0
                }
            }
            temperature=RefrigeratorThermal.advance(temperature,roomAmbient,power/type.watts,doorOpen,seconds)
        }
        if(batteryJob && active && power>0 && batteryHeat.advance(seconds,power))detonateBattery()
        val job=recipe
        status=when {
            detonating -> "EXPLODING"
            !enabled -> "OFF"
            abs(load.u)>type.volts*1.2 -> "OVERVOLTAGE"
            requested && abs(load.u)<type.volts*.8 -> "NO_POWER"
            doorOpen -> "DOOR_OPEN"
            batteryJob && batteryHeat.hissing -> "HISSING"
            batteryJob -> "HEATING_BATTERY"
            job!=null && !job.canFit(inventory) -> "BLOCKED"
            type.refrigerator && temperature>5.0 -> "COOLING"
            job!=null && power>1 -> "WORKING"
            else -> "IDLE"
        }
        if(job!=null && job.canFit(inventory) && enabled && power>0 && (!type.refrigerator || temperature<=5.0)) {
            if(work.advance(seconds,power,type.watts*type.seconds)) {
                work.joules=0.0;job.complete(inventory)
                play(SoundCommand(if(type==KitchenType.TOASTER)"eln:appliance_toaster_pop" else "eln:appliance_done").verySmallRange().mulVolume(.35f,1f))
            }
        }
        if(publishedStatus!=status) { publishedStatus=status;needPublish() }
    }
    private fun detonateBattery() {
        if(detonating)return
        detonating=true;active=false;resistor.r=1e9;needPublish()
        net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer()?.execute {
            if(!CordSupport.active(this))return@execute
            val c=coordonate();val world=c.world() as? ServerLevel ?: return@execute
            inventory.getItem(0).shrink(1)
            node.physicalSelfDestruction(0f)
            world.explode(null as net.minecraft.world.entity.Entity?,c.x+.5,c.y+.5,c.z+.5,2f,false,net.minecraft.world.level.Level.ExplosionInteraction.NONE)
        }
    }
    override fun onBreakElement() {
        if(cord.moving) { if(useUuid())stop(getUuid());return }
        cord.onBroken()
        if(!detonating) { super.onBreakElement();return }
        // A ruptured microwave is destroyed; spare contents drop once, not the appliance.
        if(useUuid())stop(getUuid())
        node!!.dropInventory(inventory)
    }
    override fun getElectricalLoad(side: Direction,lrdu: LRDU)=if(!cord.plugged && side.isNotY && lrdu==LRDU.Down)load else null
    override fun getConnectionMask(side: Direction,lrdu: LRDU)=if(getElectricalLoad(side,lrdu)!=null)NodeBase.maskElectricalPower else 0
    override fun getThermalLoad(side: Direction,lrdu: LRDU)=null
    override fun thermoMeterString(side:Direction)=""
    override fun onBlockActivated(player:Player,side:Direction,x:Float,y:Float,z:Float)=false
    override fun hasGui()=true
    override fun networkUnserialize(stream: DataInputStream): Byte {
        val id=super.networkUnserialize(stream)
        if(id.toInt()==80) { enabled=!enabled;configure();needPublish() }
        return id
    }
    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeBoolean(enabled);stream.writeBoolean(active);stream.writeDouble(if(batteryJob)batteryHeat.joules/MicrowaveBatteryHeat.BLAST_JOULES else work.joules/(type.watts*type.seconds))
        stream.writeDouble(load.u);stream.writeDouble(resistor.p);stream.writeDouble(temperature);stream.writeUTF(status)
        node.lrduCubeMask.getTranslate(Direction.YN).serialize(stream);
        stream.writeBoolean(doorOpen)
    }
    override fun readFromNBT(nbt: CompoundTag) {
        super.readFromNBT(nbt)
        enabled=!nbt.contains("kitchenEnabled") || nbt.getBooleanOr("kitchenEnabled",false)
        work.joules=nbt.getDoubleOr("kitchenJoules",0.0).takeIf { it.isFinite() }?.coerceIn(0.0,type.watts*type.seconds) ?: 0.0
        temperature=if(nbt.contains("kitchenTemperature"))nbt.getDoubleOr("kitchenTemperature",0.0).takeIf { it.isFinite() }?.coerceIn(3.5,80.0) ?: 20.0 else 20.0
        viewers.clear();roomClock=0.0
        batteryHeat.read(nbt);cacheReady=false
        foodClock=if(nbt.contains("fridgeFoodClock"))nbt.getLongOr("fridgeFoodClock",0L) else -1L
        foodWasCold=nbt.getBooleanOr("fridgeFoodWasCold",false)
    }
    override fun writeToNBT(nbt: CompoundTag) {
        super.writeToNBT(nbt);nbt.putLong("fridgeFoodClock",foodClock);nbt.putBoolean("fridgeFoodWasCold",foodWasCold);nbt.putBoolean("kitchenEnabled",enabled);nbt.putDouble("kitchenJoules",work.joules);nbt.putDouble("kitchenTemperature",temperature);batteryHeat.write(nbt)
    }
    override fun multiMeterString(side: Direction)=Utils.plotVolt("U:",load.u)+Utils.plotPower("P:",resistor.p)
    override fun getWaila()=linkedMapOf(tr("State") to kitchenStatus(status),tr("Voltage") to Utils.plotVolt("",load.u),tr("Power") to Utils.plotPower("",resistor.p))
}
