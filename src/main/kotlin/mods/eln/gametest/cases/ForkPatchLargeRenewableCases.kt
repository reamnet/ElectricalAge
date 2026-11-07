package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gl.GlFrame
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.mna.component.Resistor
import mods.eln.transparentnode.renewable.*
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LiquidBlock
import java.io.*
import kotlin.math.abs

object ForkPatchLargeRenewableCases {
    @JvmStatic fun cases():List<TestCase> = listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP).map { Water(it) }+Wind()+Identity()
    private open class Base:TestCase {
        override fun id()="forkpatch_large_renewable_base"
        override fun width()=24;override fun height()=20;override fun depth()=24;override fun floorDepth()=24
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {}
        fun place(c: TestContext,water: Boolean,front: Direction):LargeRenewableElement {
            val d=requireNotNull(Eln.transparentNodeItem.getDescriptor(if(water)LargeRenewableDescriptor.WATER_ID else LargeRenewableDescriptor.WIND_ID))
            val fp=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
            val pitch=fp.xRot;val yaw=fp.yRot
            val placed=try {
                fp.xRot=0f;fp.yRot=when(front) { Direction.XN->270f;Direction.XP->90f;Direction.ZN->0f;else->180f }
                requireNotNull((d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),fp,c.level(),c.at(12,1,12),Direction.YN)).element as LargeRenewableElement
            }finally { fp.xRot=pitch;fp.yRot=yaw }
            c.check("large renewable: oriented placement preserves shared test-player facing",fp.xRot==pitch && fp.yRot==yaw,"")
            return placed
        }
        fun offset(e: LargeRenewableElement,x:Int,y:Int,z:Int):BlockPos=Coordonate(x,y,z,0).apply { applyTransformation(e.front,e.node.coordonate) }.pos()
        fun published(c: TestContext,e:LargeRenewableElement) {
            mods.eln.client.ClientRenders.registerAll()
            val pos=e.node.coordonate.pos();val client=TransparentNodeEntity(pos,c.level().getBlockState(pos));client.setLevel(c.level())
            try {
                val input=DataInputStream(ByteArrayInputStream(e.node.publishPacket.toByteArray()));input.readByte();repeat(4) { input.readInt() };input.readUTF();client.serverPublishUnserialize(input)
                val r=client.elementRender as? LargeRenewableRender
                c.check("large renewable: actual publication constructs the renderer and consumes all bytes",r!=null && input.available()==0,"")
                if(r!=null) {
                    c.check("large renewable: live environmental power reaches client",abs(r.availableWatts-e.availableWatts)<1e-8 && abs(r.watts-e.source.p)<1e-8,"")
                    GlFrame.beginRenderFrame();val frame=GlFrame.open();try { r.refresh(.1f);r.draw();c.check("large renewable: source tower/wheel geometry renders",frame.batches().values.sumOf { it.vertices() }>100,"") }finally { GlFrame.close() }
                    val bounds=r.renderBounds();c.check("large renewable: client bounds retain the tall tower/rotor",bounds!=null && bounds.maxY>=pos.y+15,"")
                }
            }finally { client.clientDestroy() }
        }
    }
    private class Water(val orientation:Direction):Base() {
        lateinit var e:LargeRenewableElement;lateinit var load:Resistor
        override fun id()="forkpatch_large_watermill_"+orientation.name.lowercase()
        override fun ticks()=20
        override fun build(c: TestContext) {
            e=place(c,true,orientation)
            for(z in listOf(-3,0,3))c.level().setBlock(offset(e,2,0,z),Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,1),2)
            e.sampleEnvironment();load=Resistor(e.output,null).setR(3200.0*3200/1000);Eln.simulator.addElectricalComponent(load)
        }
        override fun measure(c: TestContext) {
            try {
                c.check("watermill: player placement rotates its real environmental ports",e.front==orientation,"")
                c.check("watermill: actual flowing-water environment supplies a real cable-side load",e.availableWatts>9999 && load.p>990 && e.source.p in 990.0..10000.01,"${e.availableWatts}, ${load.p}, ${e.source.p}")
                published(c,e)
                for(z in listOf(-3,0,3))c.level().setBlock(offset(e,2,0,z),Blocks.WATER.defaultBlockState(),2)
                e.sampleEnvironment();c.check("watermill: still water cannot produce power",e.availableWatts==0.0,"")
                for(z in listOf(-3,0,3))c.level().setBlock(offset(e,2,0,z),Blocks.AIR.defaultBlockState(),2)
                e.sampleEnvironment();c.check("watermill: removing water stops available generation",e.availableWatts==0.0,"")
                c.level().setBlock(offset(e,2,0,0),Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,1),2);e.sampleEnvironment()
                c.check("watermill: one of three moving-water samples gives one third capacity",abs(e.availableWatts-10000.0/3)<1e-8,"")
                c.check("watermill: output accepts only the front floor cable",e.getElectricalLoad(orientation,LRDU.Down)===e.output && e.getElectricalLoad(orientation,LRDU.Up)==null && e.getElectricalLoad(orientation.inverse,LRDU.Down)==null,"")
            }finally { Eln.simulator.removeElectricalComponent(load);load.breakConnection() }
        }
    }
    private class Wind:Base() {
        lateinit var e:LargeRenewableElement;lateinit var load:Resistor;val wind=CompoundTag()
        override fun id()="forkpatch_large_wind_weather_clearance_and_real_output"
        override fun ticks()=20
        override fun build(c: TestContext) {
            Eln.wind.writeToNBT(wind,"");Eln.wind.readFromNBT(CompoundTag().apply { putDouble("windHit",12.0);putDouble("windTarget",12.0);putFloat("windTargetFiltred",12f) },"")
            e=place(c,false,Direction.XN);e.sampleEnvironment();load=Resistor(e.output,null).setR(3200.0*3200/1000);Eln.simulator.addElectricalComponent(load)
        }
        override fun measure(c: TestContext) {
            try {
                c.check("wind: actual sky/wind environment supplies a real output load",e.availableWatts>1000 && e.availableWatts<=20000 && load.p>990,"${e.availableWatts}, ${load.p}")
                published(c,e)
                c.check("wind: rotor reserves actual tall ghost clearance",c.level().getBlockState(offset(e,-1,14,3)).block===Eln.ghostBlock,"")
                for(y in listOf(6,9,12))for(z in listOf(-3,0,3))c.level().setBlock(offset(e,-3,y,z),Blocks.STONE.defaultBlockState(),2)
                e.sampleEnvironment();c.check("wind: blocked sparse air samples curtail generation",e.availableWatts==0.0,"")
                val tag=CompoundTag();e.writeToNBT(tag);val copy=LargeRenewableElement(TransparentNode(),e.descriptor);copy.readFromNBT(tag)
                c.check("wind: reload cannot restore cached environmental generation",copy.availableWatts==0.0 && !copy.source.enabled,"")
            }finally { Eln.wind.readFromNBT(wind,"");Eln.simulator.removeElectricalComponent(load);load.breakConnection() }
        }
    }
    private class Identity:Base() {
        override fun id()="forkpatch_large_renewable_registry_and_identity_conflict"
        override fun measure(c: TestContext) {
            c.check("renewable registry: established UPS lookup is retained",Eln.transparentNodeItem.getDescriptor(7947) is mods.eln.transparentnode.ups.UpsDescriptor,"")
            c.check("renewable registry: source wind and relocated watermill are distinct",(Eln.transparentNodeItem.getDescriptor(7946) as? LargeRenewableDescriptor)?.water==false && (Eln.transparentNodeItem.getDescriptor(16383) as? LargeRenewableDescriptor)?.water==true,"")
            for(id in listOf(7946,16383)) { val d=requireNotNull(Eln.transparentNodeItem.getDescriptor(id));c.check("renewable registry: substantial source clearance retained",d.getGhostGroup(Direction.XN)!!.size()>50,d.name) }
        }
    }
}
