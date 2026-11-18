package mods.eln.gametest

import mods.eln.Eln
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sixnode.lampsupply.LampSupplyElement
import mods.eln.transparentnode.festive.*
import mods.eln.transparentnode.floodlight.*
import mods.eln.transparentnode.worklight.*
import mods.eln.transparentnode.cord.*
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component

/** Reproducible live lighting gallery, powered by ordinary saved supplies and paid cords. */
object FinalLightingLab {
    @JvmStatic fun build(src:CommandSourceStack,origin:BlockPos):Int {
        val c=WorldTestContext(src.level,origin)
        val player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(src.level)
        var count=0
        for((i,id)in listOf(4352,4353,4416,4417,7936,7994,7995,8006,8007).withIndex()){
            val base=origin.offset(i%3*16,0,i/3*16)
            for(x in (base.x shr 4)..((base.x+13)shr 4))for(z in (base.z shr 4)..((base.z+13)shr 4))src.level.getChunk(x,z)
            for(x in 0..12)for(z in 0..12)c.floor(base.offset(x,0,z))
            val at=base.offset(4,1,4)
            val desc=Eln.transparentNodeItem.getDescriptor(id)!!
            val stack=desc.newItemStack()
            val volts=if(id==4353)240 else 120
            if(desc is FairyLightsDescriptor)mods.eln.generic.ItemNbt.edit(stack){
                it.putInt("festiveVolts",120)
                it.putDouble("fairyStartX",at.x-3.0);it.putDouble("fairyStartY",at.y+2.0);it.putDouble("fairyStartZ",at.z+.5)
                it.putDouble("fairyEndX",at.x+.5);it.putDouble("fairyEndY",at.y+.5);it.putDouble("fairyEndZ",at.z+.5)
            }
            val e=(desc.parentItem as TransparentNodeItem).placeAt(stack,player,src.level,at,Direction.YN)?.element
                ?:run{src.sendFailure(Component.literal("Could not place lighting $id at ${at.toShortString()}"));continue}
            val supplyPos=base.offset(10,1,10)
            val supply=c.placeSix(if(volts==120)4162 else 4161,supplyPos)as LampSupplyElement
            val channel="final lighting ${origin.toShortString()} $id"
            supply.networkUnserialize(c.stream{it.writeByte(LampSupplyElement.setPowerName.toInt());it.writeByte(0);it.writeUTF(channel)})
            supply.front=Direction.YN.getLRDUGoingTo(Direction.XP)
            val power=c.placeSix(192,supplyPos.east())as mods.eln.sixnode.electricalsource.ElectricalSourceElement
            power.networkUnserialize(c.stream{it.writeByte(1);it.writeFloat(volts.toFloat())})
            supply.reconnect();supply.needPublish()
            when(e){
                is StringLightsElement->e.networkUnserialize(c.stream{it.writeByte(81);it.writeUTF(channel)})
                is WorkLightElement->{e.inventory.setItem(0,Eln.findItemStack("120V LED Light Bulb",2));e.inventory.setChanged();e.networkUnserialize(c.stream{it.writeByte(82);it.writeUTF(channel)});e.networkUnserialize(c.stream{it.writeByte(81)})}
                is FloodlightElement->{
                    repeat(2){e.inventory.setItem(it,Eln.findItemStack("${volts}V LED Light Bulb"))};e.inventory.setChanged()
                    val outletPos=at.offset(4,0,0);c.floor(outletPos.north())
                    val od=Eln.transparentNodeItem.getDescriptor(if(volts==120)7968 else 7969)!!
                    val outlet=(od.parentItem as TransparentNodeItem).placeAt(od.newItemStack(),player,src.level,outletPos,Direction.ZN)!!.element as OutletElement
                    outlet.networkUnserialize(c.stream{it.writeByte(83);it.writeUTF(channel)});outlet.networkUnserialize(c.stream{it.writeByte(82)})
                    check(CordSupport.device(e)!!.cord.attach(outlet.anchor(0),true))
                }
            }
            e.needPublish();count++
        }
        src.sendSuccess({Component.literal("Final lighting gallery: $count/9 at ${origin.toShortString()}; /elnlab devicemenu <lookup> opens controls")},true)
        return count
    }
}
