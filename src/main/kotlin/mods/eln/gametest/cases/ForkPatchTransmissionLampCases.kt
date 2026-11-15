package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gl.GlFrame
import mods.eln.gridnode.GridLink
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.utility.*
import net.minecraft.nbt.CompoundTag
import java.io.*

object ForkPatchTransmissionLampCases {
    @JvmStatic fun cases(): List<TestCase> = TransmissionKind.entries.flatMap { kind -> (0..4).map { Lamp(kind,it) } }
    private class Lamp(val kind: TransmissionKind,val mode: Int) : TestCase {
        lateinit var e: TransmissionElement
        lateinit var feed: VoltageSource
        var cleaned=false
        override fun id()="forkpatch_transmission_lamp_${kind.name.lowercase()}_"+listOf("paid120_reload","paid240_remove","primary_only_dark","bounded_controls","break_refund")[mode]
        override fun width()=32; override fun height()=28; override fun depth()=16; override fun floorDepth()=16
        override fun ticks()=100
        override fun build(c: TestContext) {
            e=c.placeTransparent(kind.id,c.at(6,1,7)) as TransmissionElement
            val player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(id().toByteArray()),"steelLamp$mode"))
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL)
            val stock=Eln.findItemStack(UtilityItems.STREETLIGHT_NAME,2);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stock)
            e.onBlockActivated(player,Direction.XN,.5f,.5f,.5f)
            c.check("transmission lamp: real survival install spends one attachment",e.lamp.installed && stock.count==1,kind.name)
            e.onBlockActivated(player,Direction.XN,.5f,.5f,.5f)
            c.check("transmission lamp: duplicate install cannot spend the remaining attachment",stock.count==1,kind.name)
            val source=when(mode) { 1 -> e.services[2];2 -> e.inputs[0];else -> e.services[0] }
            feed=VoltageSource("steel pole lamp",source,null).setU(when(mode) { 1 -> 240.0;2 -> kind.volts;else -> 120.0 })
            Eln.simulator.addElectricalComponent(feed)
            if (mode==1) e.networkUnserialize(c.stream { it.writeByte(107);it.writeInt(240) })
        }
        private fun clean() { if(!cleaned) { Eln.simulator.removeElectricalComponent(feed);feed.breakConnection();cleaned=true } }
        private fun refunds(c: TestContext)=c.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java,net.minecraft.world.phys.AABB(e.node.coordonate.pos()).inflate(2.0)).filter { UtilityItems.part(it.item)==3 }.sumOf { it.item.count }
        override fun measure(c: TestContext) {
            try {
                if (mode==2) c.check("transmission lamp: energized primary cannot power an isolated service fixture",e.lamp.light==0 && e.lamp.watts<1e-7 && e.services.all { kotlin.math.abs(it.u)<1e-8 },kind.name)
                else c.check("transmission lamp: one actual secondary circuit pays the 70 W lamp load",e.lamp.light>=14 && e.lamp.watts in 69.0..70.1 && e.lamp.selected==if(mode==1)240 else 120,"${kind.name} ${e.lamp.watts} W")
                when (mode) {
                    0 -> {
                        val carry=TransmissionElement(TransparentNode(),e.descriptor);carry.readItemStackNBT(e.itemStackNBT)
                        c.check("transmission lamp: carried pole cannot duplicate installed hardware",!carry.lamp.installed,kind.name)
                        clean(); val pos=e.node.coordonate.pos(); Reload.node(c.level(),pos)
                        e=GridLink.getElementFromCoordinate(Coordonate(pos,c.level())) as TransmissionElement
                        c.check("transmission lamp: actual owner reload retains the paid installed fixture",e.lamp.installed,kind.name)
                    }
                    1 -> {
                        e.networkUnserialize(c.stream { it.writeByte(108) })
                        c.check("transmission lamp: removal extinguishes both loads and refunds once",!e.lamp.installed && e.lamp.light==0 && e.lamp.watts<1e-7 && refunds(c)==1,kind.name)
                        e.networkUnserialize(c.stream { it.writeByte(108) })
                        c.check("transmission lamp: repeated removal cannot duplicate the physical item",refunds(c)==1,kind.name)
                    }
                    3 -> controls(c)
                    4 -> {
                        val carried=e.dropItemStack
                        clean(); c.level().destroyBlock(e.node.coordonate.pos(),true)
                        c.check("transmission lamp: actual pole destruction refunds the attachment exactly once",refunds(c)==1 && !e.lamp.installed && !e.lamp.remove(),kind.name)
                        val saved=requireNotNull(mods.eln.generic.ItemNbt.get(carried))
                        c.check("transmission lamp: dropped pole has no second installed attachment",!saved.getCompoundOrEmpty("poleStreetlightAttachment").getBooleanOr("installed",true),kind.name)
                    }
                }
            } finally { clean() }
        }
        private fun controls(c: TestContext) {
            fun move(id: Int,delta: Int) { e.networkUnserialize(c.stream { it.writeByte(id);it.writeInt(delta) }) }
            move(104,-1);move(105,-15);move(106,-5);move(107,120)
            c.check("transmission lamp: native controls preserve source height, yaw and pitch steps",e.lamp.heightStep==-1 && e.lamp.yaw==345 && e.lamp.pitch==-5 && e.lamp.supply==120,kind.name)
            val before=CompoundTag().also(e::writeToNBT)
            for (id in 104..107) {
                move(id,Int.MAX_VALUE);e.networkUnserialize(c.stream { it.writeByte(id) })
                c.check("transmission lamp: invalid and truncated packets cannot alter saved controls",CompoundTag().also(e::writeToNBT)==before,"$kind command=$id")
            }
            val tag=CompoundTag().also(e::writeToNBT);val copy=TransmissionElement(TransparentNode(),e.descriptor);copy.readFromNBT(tag)
            c.check("transmission lamp: owner NBT keeps source-compatible attachment settings",copy.lamp.installed && copy.lamp.heightStep==-1 && copy.lamp.yaw==345 && copy.lamp.pitch==-5 && copy.lamp.supply==120,kind.name)
            val d=TransmissionLampDisplay().apply { capture(e.lamp) };val bytes=ByteArrayOutputStream();d.write(DataOutputStream(bytes))
            val display=TransmissionLampDisplay();val input=DataInputStream(ByteArrayInputStream(bytes.toByteArray()));display.read(input)
            c.check("transmission lamp: fixed publication preserves every live control and meter",input.available()==0 && display.installed && display.height==-1 && display.yaw==345 && display.pitch==-5 && display.supply==120 && display.watts==e.lamp.watts,kind.name)
            fun geometry(data: TransmissionLampDisplay): Int {
                GlFrame.beginRenderFrame();val f=GlFrame.open()
                try { (e.descriptor as TransmissionDescriptor).drawEquipment(e.front,data);return f.batches().values.fold(0) { a,b -> 31*a+b.data.copyOf(b.floats).contentHashCode() } } finally { GlFrame.close() }
            }
            c.check("transmission lamp: fixture installation, aim and lit lens affect native geometry",geometry(TransmissionLampDisplay())!=geometry(display),kind.name)
            e.networkUnserialize(c.stream { it.writeByte(102);it.writeBoolean(false) })
            c.check("transmission lamp: switching off opens both real load paths",!e.lamp.requested && e.lamp.light==0 && e.lamp.watts<1e-7,kind.name)
            e.networkUnserialize(c.stream { it.writeByte(102);it.writeBoolean(true) })
            e.networkUnserialize(c.stream { it.writeByte(103);it.writeBoolean(true) })
            e.lamp.night=false;e.lamp.sample()
            c.check("transmission lamp: automatic daylight operation draws no useful power",e.lamp.automatic && !e.lamp.requested && e.lamp.watts<1e-7,kind.name)
            e.lamp.night=true;e.lamp.sample()
            c.check("transmission lamp: dusk operation closes only the selected secondary load",e.lamp.requested && e.lamp.load.r<1000 && e.lamp.load240!!.r>=1e19,kind.name)
        }
    }
}
