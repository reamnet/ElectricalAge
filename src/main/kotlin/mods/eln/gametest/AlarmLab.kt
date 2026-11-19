package mods.eln.gametest

import mods.eln.Eln
import mods.eln.misc.Direction
import mods.eln.node.transparent.TransparentNodeItem
import mods.eln.transparentnode.firealarm.*
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component

/** Operator scene powered by a normal saved120V source, rather than transient test components. */
object AlarmLab {
    @JvmStatic fun build(src:CommandSourceStack,origin:BlockPos):Int {
        val c=WorldTestContext(src.level,origin)
        val player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(src.level)
        for(x in 0..24)for(z in 0..12)c.floor(c.at(x,0,z))
        var panel:AlarmElement?=null
        val rows=com.google.gson.JsonArray()
        for(kind in AlarmKind.entries) {
            val at=c.at(3+kind.ordinal%4*5,3,3+kind.ordinal/4*5);c.floor(at.west())
            val descriptor=Eln.transparentNodeItem.getDescriptor((125 shl 6)+kind.subId)!!
            src.level.setBlockAndUpdate(at,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState())
            val alarm=(descriptor.parentItem as TransparentNodeItem).placeAt(descriptor.newItemStack(),player,src.level,at,Direction.XN)?.element as? AlarmElement
            c.check("fire alarm ${kind.name} placed",alarm!=null,"${at.toShortString()} -> ${src.level.getBlockState(at)}")
            if(alarm!=null){alarm.changeChannel("Alarm lab ${origin.toShortString()}");if(kind==AlarmKind.PANEL)panel=alarm}
            rows.add(com.google.gson.JsonObject().apply{addProperty("kind",kind.name);addProperty("id",(125 shl 6)+kind.subId);addProperty("x",at.x);addProperty("y",at.y);addProperty("z",at.z)})
        }
        val mains=try{
            val panelElement=panel?:throw IllegalStateException("fire alarm panel did not place")
            val input=panelElement.node.coordonate.pos().south();c.floor(input.below())
            src.level.setBlockAndUpdate(input,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState())
            val source=c.placeSix(192,input) as mods.eln.sixnode.electricalsource.ElectricalSourceElement
            source.networkUnserialize(c.stream{it.writeByte(1);it.writeFloat(120f)})
            c.check("source120V mains placed",true,input.toShortString());source
        }catch(t:Throwable){c.check("source120V mains placed",false,"${t.message}");null}
        java.nio.file.Files.writeString(java.nio.file.Path.of("eln-alarm-gallery.json"),com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows)+"\n")
        src.sendSuccess({Component.literal("Seven native fire alarms (${rows.size()}/7 placed); mains ${if(mains!=null)"up" else "failed"}; ${c.failures} failures; positions in eln-alarm-gallery.json")},true)
        return 7
    }
}
