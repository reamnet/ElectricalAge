package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gridnode.GridLink
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.utility.*
import net.minecraft.nbt.CompoundTag
import java.io.*

object ForkPatchPoleAttachmentCases {
    @JvmStatic fun cases(): List<TestCase> = listOf(Attachment(0),Attachment(1),Attachment(2),Attachment(3))
    private class Attachment(val mode: Int): TestCase {
        lateinit var e: DistributionPoleElement;lateinit var feed: VoltageSource;var clock: IProcess?=null;var elapsed=0.0;var removed=false;var lit=false
        override fun id()="forkpatch_pole_attachment_"+listOf("paid_install_reload","removal","manual_240_supply","break_refund")[mode]
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun ticks()=100
        override fun build(c: TestContext) {
            e=c.placeTransparent(DistributionPoles.id(DistributionKind.SINGLE),c.at(6,1,6)) as DistributionPoleElement
            val player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(id().toByteArray()),"attachment_$mode"))
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);val stack=Eln.findItemStack(UtilityItems.STREETLIGHT_NAME,2);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack)
            e.onBlockActivated(player,Direction.XN,.5f,.5f,.5f)
            c.check("pole attachment: installation consumes exactly one physical item",e.unit.lamp!!.installed && stack.count==1,"")
            e.onBlockActivated(player,Direction.XN,.5f,.5f,.5f);c.check("pole attachment: duplicate installation cannot consume another item",stack.count==1,"")
            val line=e.unit.services[if(mode==2)2 else 0];feed=VoltageSource("attachment service",line,null).setU(if(mode==2)240.0 else 120.0);Eln.simulator.addElectricalComponent(feed)
            if(mode==2)e.unit.lamp!!.chooseSupply(240)
            clock=IProcess { dt->elapsed+=dt;val lamp=e.unit.lamp!!;if(lamp.watts>69 && lamp.light>=14)lit=true
                if(mode==1 && !removed && elapsed>=1.5) { c.check("pole attachment: actual secondary load lights the fixture before removal",lit,"");e.networkUnserialize(DataInputStream(ByteArrayInputStream(byteArrayOf(108.toByte()))));removed=true } }
            Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c: TestContext) {
            try {
                val lamp=e.unit.lamp!!
                if(mode==1)c.check("pole attachment: native removal opens both loads and extinguishes fixture",removed && !lamp.installed && lamp.light==0 && lamp.watts<1e-6,"")
                else c.check("pole attachment: real service powers only one selected lamp load",lit && lamp.watts in 69.0..70.1 && lamp.selected==(if(mode==2)240 else 120),"${lamp.watts} W")
                if(mode==0) {
                    val carry=e.itemStackNBT;val carried=DistributionPoleElement(TransparentNode(),e.descriptor);carried.readItemStackNBT(carry)
                    c.check("pole attachment: carried settings cannot duplicate an installed paid attachment",!carried.unit.lamp!!.installed,"")
                    Eln.simulator.removeElectricalComponent(feed);feed.breakConnection();clock?.let(Eln.simulator::removeElectricalProcess);clock=null
                    val pos=e.node.coordonate.pos();Reload.node(c.level(),pos);e=GridLink.getElementFromCoordinate(Coordonate(pos,c.level())) as DistributionPoleElement
                    c.check("pole attachment: actual placed reload keeps paid installation",e.unit.lamp!!.installed,"")
                }
                if(mode==3) {
                    // Break the actual node, then prove its carried pole has no paid attachment and repeated removal is inert.
                    val stack=e.dropItemStack;val pos=e.node.coordonate.pos();c.level().destroyBlock(pos,true)
                    val refunds=c.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java,net.minecraft.world.phys.AABB(pos).inflate(2.0)).filter { UtilityItems.part(it.item)==3 }.sumOf { it.item.count }
                    c.check("pole attachment: actual block break refunds one physical item",refunds==1,"refunds=$refunds")
                    c.check("pole attachment: breaking refunds the attachment once and removes installed state",!e.unit.lamp!!.installed && !e.unit.lamp!!.remove() && !requireNotNull(mods.eln.generic.ItemNbt.get(stack)).getCompoundOrEmpty("poleStreetlightAttachment").getBooleanOr("installed",true),"")
                }
            }finally { clock?.let(Eln.simulator::removeElectricalProcess);if(mode!=0) { Eln.simulator.removeElectricalComponent(feed);feed.breakConnection() } }
        }
    }
}
