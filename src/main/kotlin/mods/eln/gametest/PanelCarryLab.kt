package mods.eln.gametest

import mods.eln.Eln
import mods.eln.misc.*
import mods.eln.node.NodeManager
import mods.eln.node.NodeMenus
import mods.eln.node.transparent.*
import mods.eln.sixnode.lampsocket.DecorativeLampElement
import mods.eln.transparentnode.cord.*
import mods.eln.transparentnode.ups.*
import mods.eln.utility.BreakerPanelElement
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component

/** Operator-only live scene using normal saved hardware, inventories and paid cord claims. */
object PanelCarryLab {
    @JvmStatic fun build(src:CommandSourceStack,origin:BlockPos):Int {
        val c=WorldTestContext(src.level,origin)
        for(x in 0..12)for(z in 0..8)c.floor(c.at(x,0,z))
        val player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(src.level)
        val at=c.at(4,2,3);for(y in -1..1)c.floor(at.east().offset(0,y,0))
        val desc=Eln.transparentNodeItem.getDescriptor(7988)!!
        val panel=(desc.parentItem as TransparentNodeItem).placeAt(desc.newItemStack(),player,src.level,at,Direction.XP)!!.element as BreakerPanelElement
        listOf(5,10,15,20,30,40,50,60,100,125,200,400).forEachIndexed {i,amps->panel.getInventory().setItem(i,Eln.findItemStack("${amps}A Electrical Breaker",1))}
        panel.networkUnserialize(c.stream{it.writeByte(81);it.writeUTF("0\nPanel lab")})
        val out=c.at(2,1,6);c.floor(out.north())
        val od=Eln.transparentNodeItem.getDescriptor(7968)!!
        val outlet=(od.parentItem as TransparentNodeItem).placeAt(od.newItemStack(),player,src.level,out,Direction.ZN)!!.element as OutletElement
        val ups=c.placeTransparent(8076,c.at(6,1,6)) as CompactUpsElement
        val cell=Eln.findItemStack("UPS AGM Battery Cell",1);val battery=upsBattery(cell)!!
        battery.setEnergy(cell,battery.getEnergyMax(cell)*.5);ups.inventory.setItem(0,cell)
        check(ups.cord.attach(outlet.anchor(0),true))
        val lamp=c.placeSix(4118,c.at(9,1,6)) as DecorativeLampElement
        lamp.inventory.setItem(0,Eln.findItemStack("120V LED Light Bulb",1));lamp.inventoryChange(lamp.inventory)
        check(CordSupport.device(lamp)!!.cord.attach(ups.anchor(0),true))
        src.sendSuccess({Component.literal("Panel and carrying scene at ${origin.toShortString()}: panel ${at.toShortString()}, UPS ${ups.node.coordonate.pos().toShortString()}, desk lamp ${lamp.coordonate.pos().toShortString()}")},true)
        return 4
    }
    @JvmStatic fun menu(src:CommandSourceStack,lookup:Int):Int {
        val player=src.playerOrException
        val node=NodeManager.instance.nodes.asSequence().filterIsInstance<TransparentNode>()
            .filter{it.coordonate.dimention==Dimensions.id(src.level)&&it.element?.descriptor?.parentItemDamage==lookup}
            .minByOrNull{player.distanceToSqr(it.coordonate.x+.5,it.coordonate.y+.5,it.coordonate.z+.5)}
            ?:run{src.sendFailure(Component.literal("No resident transparent device $lookup"));return 0}
        val p=node.coordonate
        player.teleportTo(src.level,p.x+2.5,p.y.toDouble(),p.z+.5,emptySet<net.minecraft.world.entity.Relative>(),player.yRot,player.xRot,true)
        return if(NodeMenus.open(node,player,Direction.YN))1 else 0
    }
}
