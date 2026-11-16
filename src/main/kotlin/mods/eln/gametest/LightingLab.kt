package mods.eln.gametest

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mods.eln.Eln
import mods.eln.misc.*
import mods.eln.node.NodeManager
import mods.eln.node.NodeMenus
import mods.eln.node.six.SixNode
import mods.eln.sixnode.lampsocket.*
import mods.eln.sixnode.lampsupply.LampSupplyElement
import mods.eln.transparentnode.cord.*
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import java.nio.file.Files
import java.nio.file.Path

/** Operator-only reproducible client scene. Every load uses native saved sources/supplies/cords. */
object LightingLab {
    @JvmStatic fun build(src: CommandSourceStack,origin: BlockPos): Int {
        val c=WorldTestContext(src.level,origin)
        val records=JsonArray()
        var placed=0
        for(style in DecorativeLampStyle.entries) {
            val index=style.sourceOrdinal
            val base=c.at(index%9*16,0,index/9*16)
            // Scene construction intentionally requests chunks; fixture runtime queries remain resident-only.
            for(x in (base.x shr 4)..((base.x+12) shr 4))for(z in (base.z shr 4)..((base.z+12) shr 4))src.level.getChunk(x,z)
            val d=Eln.sixNodeItem.getDescriptor(style.lookup) as DecorativeLampDescriptor
            val side=requireNotNull(d.placeDirection).first()
            val p=base.offset(4,if(side==Direction.YP)9 else if(side==Direction.YN)1 else 4,4)
            try {
                c.floor(p.relative(side.toEnumFacing()))
                val fixture=c.placeSix(style.lookup,p,side) as DecorativeLampElement
                fixture.inventory.setItem(0,Eln.findItemStack("120V LED Light Bulb",style.bulbs));fixture.inventoryChange(fixture.inventory)
                val channel="lighting lab ${origin.toShortString()} $index"
                fixture.channel=channel
                if(style==DecorativeLampStyle.MOTION)fixture.networkUnserialize(c.stream { it.writeByte(DecorativeLampElement.MOTION_MODE);it.writeByte(2) })
                val supplyPos=base.offset(10,1,10);c.floor(supplyPos.below());c.floor(supplyPos.east().below())
                val supply=c.placeSix(4162,supplyPos) as LampSupplyElement
                supply.networkUnserialize(c.stream { it.writeByte(LampSupplyElement.setPowerName.toInt());it.writeByte(0);it.writeUTF(channel) })
                val source=c.placeSix(192,supplyPos.east()) as mods.eln.sixnode.electricalsource.ElectricalSourceElement
                source.networkUnserialize(c.stream { it.writeByte(1);it.writeFloat(120f) })
                if(style.fan) {
                    val outletPos=p.offset(4,0,4);c.floor(outletPos.north())
                    val outlet=mods.eln.gametest.cases.Wp10bCases.placeFacing(c,7968,outletPos,Direction.ZP) as OutletElement
                    outlet.networkUnserialize(c.stream { it.writeByte(83);it.writeUTF(channel) })
                    outlet.networkUnserialize(c.stream { it.writeByte(82) })
                    check(requireNotNull(CordSupport.device(fixture)).cord.attach(outlet.anchor(0),true))
                }
                fixture.needPublish()
                records.add(JsonObject().apply { addProperty("id",style.lookup);addProperty("style",style.name);addProperty("x",p.x);addProperty("y",p.y);addProperty("z",p.z);addProperty("side",side.name);addProperty("panelHeight",d.panelHeight) })
                placed++
            }catch(e: RuntimeException) { c.check("lighting scene ${style.name}",false,e.message ?: e.javaClass.simpleName) }
        }
        Files.writeString(Path.of("eln-lighting-gallery.json"),GsonBuilder().setPrettyPrinting().create().toJson(records)+"\n")
        src.sendSuccess({ Component.literal("Lighting scene: $placed/108 fixtures; ${c.failures} failures; cameras in eln-lighting-gallery.json") },true)
        return placed
    }
    @JvmStatic fun menu(src: CommandSourceStack,lookup: Int): Int {
        val player=src.playerOrException
        val fixture=NodeManager.instance.nodes.asSequence().filterIsInstance<SixNode>()
            .filter { it.coordonate.dimention==mods.eln.misc.Dimensions.id(src.level) }
            .flatMap { it.sideElementList.asSequence().filterIsInstance<DecorativeLampElement>() }
            .firstOrNull { it.style.lookup==lookup } ?: run { src.sendFailure(Component.literal("No resident lamp $lookup; build /elnlab lamps first"));return 0 }
        val p=fixture.coordonate
        player.teleportTo(p.x+2.5,p.y.toDouble(),p.z+.5)
        return if(NodeMenus.open(fixture.sixNode,player,fixture.side))1 else 0
    }
}
