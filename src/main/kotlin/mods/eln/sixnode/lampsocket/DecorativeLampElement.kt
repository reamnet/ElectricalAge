package mods.eln.sixnode.lampsocket

import mods.eln.item.LampDescriptor
import mods.eln.misc.Direction
import mods.eln.misc.Utils
import mods.eln.node.AutoAcceptInventoryProxy
import mods.eln.node.six.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.io.DataOutputStream
import java.io.DataInputStream
import net.minecraft.nbt.CompoundTag
import mods.eln.sim.mna.component.Resistor
import mods.eln.transparentnode.fan.FanDynamics
import kotlin.math.abs

class DecorativeLampElement(node: SixNode, side: Direction, descriptor: SixNodeDescriptor) :
    LampSocketElement(node,side,descriptor) {
    val style = (descriptor as DecorativeLampDescriptor).style
    val aim = style.newAim()
    val suspension = WarehouseSuspension()
    var fanEnabled = true
        private set
    private val rotor = FanDynamics()
    val fanPower get() = rotor.speed
    val fanMotor = Resistor(null,null)
    var motionMode = 0
        private set
    private var motionEnergized = false
    private val motion = MotionLightControl()
    private val hazard = CeilingFanHazard(this)
    private var publishedSpeed = 0.0
    init {
        acceptingInventory = AutoAcceptInventoryProxy(SixNodeElementInventory(2,style.bulbs,this))
        slowProcessList.remove(lampProcess)
        lampProcess = DecorativeLampProcess(this)
        slowProcessList.add(lampProcess)
    }
    override fun getInstalledBulbs() = if(Utils.getItemObject(inventory.getItem(0)) is LampDescriptor)
        inventory.getItem(0).count.coerceIn(0,style.bulbs) else 0
    override fun getLampResistance(bulb: LampDescriptor) = if(installedBulbs>0 && isLampEnergized) bulb.r / installedBulbs else Double.POSITIVE_INFINITY
    override fun getNominalLampPower(bulb: LampDescriptor) = bulb.nominalP * maxOf(1,installedBulbs)

    override fun isWirelessPower() = poweredByLampSupply && machineCord()?.cord?.plugged!=true
    // Native sockets contain their LV cable; cord mode supplies a paid physical lead as in the source.
    val hasPowerLead get() = !isWirelessPower
    override fun isLampEnergized() = style != DecorativeLampStyle.MOTION || motionEnergized
    val fanVoltage get() = (Utils.getItemObject(inventory.getItem(0)) as? LampDescriptor)?.nominalU ?: 120.0
    override fun getWirelessResistance(): Double {
        val bulbs = super.getWirelessResistance()
        val motor = if(style.fan && fanEnabled && hasPowerLead) style.fanWatts/(fanVoltage*fanVoltage) else 0.0
        return 1.0/(1.0/bulbs+motor)
    }
    override fun computeElectricalLoad() {
        super.computeElectricalLoad()
        fanMotor.r = if(style.fan && fanEnabled && hasPowerLead) fanVoltage*fanVoltage/style.fanWatts else Double.POSITIVE_INFINITY
    }
    fun beforePhysicalStep(seconds: Double) {
        if(style==DecorativeLampStyle.MOTION) {
            val next=motion.update(this,seconds)
            if(next!=motionEnergized) { motionEnergized=next;computeElectricalLoad();needPublish() }
        }
    }
    fun afterPhysicalStep(seconds: Double) {
        if(!style.fan)return
        val fraction=abs(positiveLoad.u)/fanVoltage
        rotor.step(seconds,if(fanEnabled && hasPowerLead && fraction>=.5) fraction.coerceAtMost(1.0) else 0.0)
        if(style==DecorativeLampStyle.FAN_INDUSTRIAL)hazard.tick()
        if(abs(fanPower-publishedSpeed)>.009 || fanPower==0.0 && publishedSpeed!=0.0) {
            publishedSpeed=fanPower;needPublish()
        }
    }
    override fun readControl(event: Int,stream: DataInputStream) {
        when {
            event==MOTION_MODE && style==DecorativeLampStyle.MOTION -> {
                motionMode=stream.readUnsignedByte().coerceIn(0,3);beforePhysicalStep(0.0);needPublish()
            }
            event==FAN_SWITCH && style.fan -> { fanEnabled=!fanEnabled;computeElectricalLoad();needPublish() }
            event==SUSPENSION && style.hangingWarehouse -> { if(suspension.set(stream.readDouble()))needPublish() }
            event in AIM_FIRST until AIM_FIRST+2*aim.heads -> {
                val index=event-AIM_FIRST
                if(aim.set(index/2,index%2,stream.readDouble()))needPublish()
            }
            event==tooglePowerSupplyType && style.wirelessOnly -> Unit
            else -> super.readControl(event,stream)
        }
    }
    override fun readFromNBT(tag: CompoundTag) {
        super.readFromNBT(tag)
        aim.read(tag);suspension.read(tag);rotor.read(tag)
        fanEnabled=tag.getBooleanOr("fanEnabled",true)
        motionMode=tag.getIntOr("motionMode",0).coerceIn(0,3)
    }
    override fun writeToNBT(tag: CompoundTag) {
        super.writeToNBT(tag)
        aim.write(tag);suspension.write(tag);rotor.write(tag)
        tag.putBoolean("fanEnabled",fanEnabled);tag.putInt("motionMode",motionMode)
    }
    override fun disconnectJob() {
        super.disconnectJob()
        electricalComponentList.remove(fanMotor)
        fanMotor.connectTo(null,null)
    }
    override fun onBlockActivated(player: Player, side: Direction, x: Float, y: Float, z: Float): Boolean {
        val held = player.mainHandItem
        val bulb = Utils.getItemObject(held) as? LampDescriptor
        if(bulb != null) {
            if(bulb.socket != socketDescriptor.socketType || installedBulbs >= style.bulbs) return false
            val current = inventory.getItem(0)
            // Preserve bulb condition/components; never turn a used bulb into a fresh stack.
            if(!current.isEmpty && !ItemStack.isSameItemSameComponents(current,held)) return false
            if(current.isEmpty) inventory.setItem(0,held.copyWithCount(1)) else current.grow(1)
            held.shrink(1)
            inventoryChange(inventory)
            needPublish()
            return true
        }
        return super.onBlockActivated(player,side,x,y,z)
    }
    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeByte(installedBulbs)
        if(style==DecorativeLampStyle.MOTION)stream.writeByte(motionMode)
        aim.write(stream)
        if(style.hangingWarehouse)stream.writeDouble(suspension.drop)
        if(style.fan) { stream.writeBoolean(fanEnabled);stream.writeDouble(fanPower) }
    }
    override fun connectJob() {
        if(style.wirelessOnly) poweredByLampSupply = true
        computeElectricalLoad() // Mode/cord transitions must configure the motor before its first physical step.
        if(style.fan && !isWirelessPower) { fanMotor.connectTo(positiveLoad,null);electricalComponentList.add(fanMotor) }
        super.connectJob()
    }
    override fun getElectricalLoad(lrdu: mods.eln.misc.LRDU) = if(style.wirelessOnly || machineCord()?.cord?.plugged==true) null else super.getElectricalLoad(lrdu)
    override fun getConnectionMask(lrdu: mods.eln.misc.LRDU) = if(style.wirelessOnly || machineCord()?.cord?.plugged==true) 0 else super.getConnectionMask(lrdu)
    companion object {
        const val MOTION_MODE=16 // Native event 4 remains the channel string; source motion event 4 cannot be reused.
        const val FAN_SWITCH=9
        const val SUSPENSION=10
        const val AIM_FIRST=32
    }
}

/** Native voltage, aging and channel logic, with source fixture-specific light positions. */
class DecorativeLampProcess(private val fixture: DecorativeLampElement) : LampSocketProcess(fixture) {
    override fun process(time: Double) {
        fixture.beforePhysicalStep(time)
        super.process(time)
        fixture.afterPhysicalStep(time)
    }
    override fun placeSpot(newLight: Int) {
        val value = if(fixture.installedBulbs==0 || !fixture.isLampEnergized) 0 else newLight
        // Keep native packet publication and owner-block light recalculation.
        setLightAt(myCoord(),value)
        if(value>0) DecorativeLampLighting.update(fixture,value)
    }
}
