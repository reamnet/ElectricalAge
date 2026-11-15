package mods.eln.sixnode.lampsocket

import mods.eln.item.LampDescriptor
import mods.eln.misc.Direction
import mods.eln.misc.Utils
import mods.eln.node.AutoAcceptInventoryProxy
import mods.eln.node.six.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.io.DataOutputStream

class DecorativeLampElement(node: SixNode, side: Direction, descriptor: SixNodeDescriptor) :
    LampSocketElement(node,side,descriptor) {
    val style = (descriptor as DecorativeLampDescriptor).style
    init {
        acceptingInventory = AutoAcceptInventoryProxy(SixNodeElementInventory(2,style.bulbs,this))
        slowProcessList.remove(lampProcess)
        lampProcess = DecorativeLampProcess(this)
        slowProcessList.add(lampProcess)
    }
    override fun getInstalledBulbs() = if(Utils.getItemObject(inventory.getItem(0)) is LampDescriptor)
        inventory.getItem(0).count.coerceIn(0,style.bulbs) else 0
    override fun getLampResistance(bulb: LampDescriptor) = if(installedBulbs>0) bulb.r / installedBulbs else Double.POSITIVE_INFINITY
    override fun getNominalLampPower(bulb: LampDescriptor) = bulb.nominalP * maxOf(1,installedBulbs)

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
    }
    override fun connectJob() {
        if(style.wirelessOnly) poweredByLampSupply = true
        super.connectJob()
    }
    override fun getElectricalLoad(lrdu: mods.eln.misc.LRDU) = if(style.wirelessOnly) null else super.getElectricalLoad(lrdu)
    override fun getConnectionMask(lrdu: mods.eln.misc.LRDU) = if(style.wirelessOnly) 0 else super.getConnectionMask(lrdu)
}

/** Native voltage, aging and channel logic, with source fixture-specific light positions. */
class DecorativeLampProcess(private val fixture: DecorativeLampElement) : LampSocketProcess(fixture) {
    override fun placeSpot(newLight: Int) {
        val value = if(fixture.installedBulbs==0) 0 else newLight
        // Keep native packet publication and owner-block light recalculation.
        setLightAt(myCoord(),value)
        if(value>0) DecorativeLampLighting.update(fixture,value)
    }
}
