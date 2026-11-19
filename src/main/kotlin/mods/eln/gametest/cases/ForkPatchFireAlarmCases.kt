package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.transparentnode.firealarm.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.VoltageSource
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.Blocks
import java.io.DataInputStream
import kotlin.math.abs

/** Real source-power, loaded environmental sensing and ownership/lifecycle checks. */
object ForkPatchFireAlarmCases {
    @JvmStatic fun cases():List<TestCase> = AlarmKind.entries.map(::Geometry)+listOf(Physical(false),Physical(true),Environment(),Network(),Backup(),Controls())
    private open class Base:TestCase {
        override fun id()="forkpatch_fire_alarm_base"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c:TestContext) {}
        override fun measure(c:TestContext) {}
        fun alarm(c:TestContext,kind:AlarmKind,x:Int=4,z:Int=4):AlarmElement {
            val d=Eln.transparentNodeItem.getDescriptor((125 shl 6)+kind.subId)!!
            val p=c.at(x,3,z);c.floor(p.west())
            return (d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level()),c.level(),p,Direction.XN)!!.element as AlarmElement
        }
    }
    private class Geometry(val kind:AlarmKind):Base() {
        override fun id()="forkpatch_fire_alarm_${kind.name.lowercase()}_model_menu_and_publication"
        override fun build(c:TestContext) {
            val e=alarm(c,kind);val d=e.descriptor as AlarmDescriptor
            val spec=mods.eln.gui.MenuSpecs.of(d)
            c.check("fire alarm menu: source controls and inventory coordinates",spec.slots().isEmpty()&&spec.width()==300&&spec.height()==382&&spec.inventoryX()==8&&spec.inventoryY()==300,"")
            for(front in Direction.values()) {
                val box=d.orientedShape(front).bounds()
                c.check("fire alarm $front: original model has a thin oriented outline",minOf(box.xsize,box.ysize,box.zsize)<.3 && box.minX>=-1e-6&&box.minY>=-1e-6&&box.minZ>=-1e-6&&box.maxX<=1.000001&&box.maxY<=1.000001&&box.maxZ<=1.000001,"$box")
            }
            val at=e.node.coordonate.pos();val state=c.level().getBlockState(at)
            c.check("fire alarm collision: source devices do not block movement",state.getCollisionShape(c.level(),at).isEmpty,"")
            if(kind==AlarmKind.PULL) {
                val player=TransportTestPlayer(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"alarmGesture"))
                player.setPos(at.x+.5,at.y.toDouble(),at.z+.5)
                var opened=false;val previous=mods.eln.node.NodeMenus.openedHook
                mods.eln.node.NodeMenus.openedHook=java.util.function.BiConsumer {who,menu->if(who===player)opened=true;previous?.accept(who,menu)}
                try {
                    val hit=net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at),net.minecraft.core.Direction.WEST,at,false)
                    player.setShiftKeyDown(false);state.useWithoutItem(c.level(),player,hit)
                    c.check("fire pull interaction: ordinary empty-hand click operates without opening setup",e.pulled&&!opened,"")
                    player.setShiftKeyDown(true);state.useWithoutItem(c.level(),player,hit)
                    c.check("fire pull interaction: source sneak gesture opens the real native setup menu",e.pulled&&opened,"")
                    player.closeContainer()
                }finally{mods.eln.node.NodeMenus.openedHook=previous;player.setShiftKeyDown(false)}
            }
            mods.eln.client.ClientRenders.registerAll()
            val tile=TransparentNodeEntity(at,state);tile.setLevel(c.level())
            try {
                val data=DataInputStream(e.node.publishPacket.toByteArray().inputStream());data.readByte();repeat(4){data.readInt()};data.readUTF();tile.serverPublishUnserialize(data)
                val render=tile.elementRender as? AlarmRender
                c.check("fire alarm publication: native factory reads every control and meter",render!=null&&render.kind==kind&&data.available()==0,"remaining=${data.available()}")
                if(render!=null) {
                    mods.eln.gl.GlFrame.beginRenderFrame();val frame=mods.eln.gl.GlFrame.open()
                    try {render.draw();c.check("fire alarm rendering: original fixture mesh emits geometry",frame.batches().values.sumOf{it.vertices()}>100,"")}finally{mods.eln.gl.GlFrame.close()}
                }
            }finally{tile.clientDestroy()}
        }
    }
    private class Physical(val silent:Boolean):Base() {
        lateinit var panel:AlarmElement;lateinit var pull:AlarmElement;lateinit var horn:AlarmElement
        lateinit var source:VoltageSource;lateinit var clock:IProcess
        var elapsed=0.0;var phase=0
        override fun id()="forkpatch_fire_alarm_actual_panel_${if(silent)"silenced_strobe" else "audible_alarm"}_paid_power"
        override fun ticks()=120
        override fun build(c:TestContext) {
            panel=alarm(c,AlarmKind.PANEL);pull=alarm(c,AlarmKind.PULL,8);horn=alarm(c,AlarmKind.HORN,12)
            val channel="alarm physical "+java.util.UUID.randomUUID()
            listOf(panel,pull,horn).forEach{it.changeChannel(channel)}
            source=VoltageSource("real alarm panel mains",panel.supply,null).setU(120.0);Eln.simulator.addElectricalComponent(source)
            clock=IProcess {dt->elapsed+=dt
                if(phase==0&&elapsed>=1.5){check(pull.operatePull());phase=1}
                if(silent&&phase==1&&elapsed>=3.0){panel.networkUnserialize(c.stream{it.writeByte(84)});phase=2}
            };Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c:TestContext) {try {
            val expected=5.0+5.0+.05+.1+if(silent)3.0 else 8.0
            val paid=abs(source.u*source.current)
            c.check("fire alarm network: a real pull latches the powered panel and annunciator",panel.powered&&panel.panel.latched&&pull.pulled&&horn.alarming&&horn.silenced==silent,"powered=${panel.powered},latched=${panel.panel.latched},horn=${horn.alarming},silent=${horn.silenced}")
            c.check("fire alarm power: mains pays field load, controller and backup charging exactly once",abs(paid-expected)<1e-7&&panel.panel.energy>0&&abs(pull.consumer.p)+abs(horn.consumer.p)<1e-9,"source=$paid,expected=$expected,backup=${panel.panel.energy}")
            panel.networkUnserialize(c.stream{it.writeByte(85)})
            c.check("fire alarm reset: an active pulled station prevents reset",panel.panel.latched,"")
            check(pull.operatePull());panel.networkUnserialize(c.stream{it.writeByte(85)})
            c.check("fire alarm reset: clear source permits explicit reset",!panel.panel.latched&&!panel.alarming,"")
            val at=pull.node.coordonate.pos();pull.node.disconnect()
            c.check("fire alarm lifecycle: disconnect removes field lease immediately",AlarmNetwork.snapshot(c.level(),channelFor(panel),panel.node.coordonate.pos(),c.level().gameTime,false,false).members.none{it.pos==at},"")
        }finally{Eln.simulator.removeElectricalProcess(clock);Eln.simulator.removeElectricalComponent(source);source.breakConnection()} }
        private fun channelFor(e:AlarmElement)=e.channel
    }
    private class Environment:Base() {
        override fun id()="forkpatch_fire_alarm_loaded_smoke_occlusion_and_radiant_heat"
        override fun build(c:TestContext) {
            val at=c.at(10,4,10);val fire=at.below(2)
            c.floor(fire.below());c.level().setBlockAndUpdate(fire,Blocks.FIRE.defaultBlockState())
            c.check("smoke: actual fire is sensed through a bounded connected air path",FireEnvironment.smoke(c.level(),at,6),"")
            for(face in net.minecraft.core.Direction.entries)c.floor(at.relative(face))
            c.check("smoke: a completely closed partition blocks that source",!FireEnvironment.smoke(c.level(),at,6),"")
            c.check("heat: short-range radiation remains measurable behind a partition",abs(FireEnvironment.heat(c.level(),at,4)-80.0)<1e-9,"")
            val far=BlockPos(28_000_000,100,-28_000_000)
            c.check("alarm sensing: unloaded queries do not request chunks",c.level().chunkSource.getChunkNow(far.x shr 4,far.z shr 4)==null&&FireEnvironment.state(c.level(),far)==null&&c.level().chunkSource.getChunkNow(far.x shr 4,far.z shr 4)==null,"")
        }
    }
    private class Network:Base() {
        override fun id()="forkpatch_fire_alarm_world_range_lease_and_single_panel_billing"
        override fun build(c:TestContext) {
            val world=Any();val other=Any();val name=" isolated alarm "
            val p=BlockPos(0,1,0);val p2=BlockPos(8,1,0);val field=BlockPos(1,1,0)
            AlarmNetwork.publishPanel(world,name,AlarmNetwork.Panel(p,10,true,true,false));AlarmNetwork.publishPanel(world,name,AlarmNetwork.Panel(p2,10,true,true,false))
            val lease=AlarmNetwork.lease(world,name,field,10)
            AlarmNetwork.publishField(world,name,AlarmNetwork.Field(field,10,"Horn",false,true,false,lease.owner,.1,8.0,3.0,false))
            c.check("alarm network: nearest powered panel owns a field lease",lease.owner==p.asLong()&&lease.alarm,"")
            c.check("alarm network: two panels never both pay the same leased field",abs(AlarmNetwork.snapshot(world,name,p,10,true,false).watts-8.1)<1e-9&&AlarmNetwork.snapshot(world,name,p2,10,true,false).watts==0.0,"")
            c.check("alarm network: foreign worlds and out-of-range panels cannot power fields",!AlarmNetwork.lease(other,name,field,10).powered&&!AlarmNetwork.lease(world,name,field.offset(200,0,0),10).powered,"")
            c.check("alarm network: expired leases cannot sustain phantom alarm power",!AlarmNetwork.lease(world,name,field,61).powered&&AlarmNetwork.snapshot(world,name,p,61,true,false).members.isEmpty(),"")
            AlarmNetwork.remove(world,name,p);AlarmNetwork.remove(world,name,p2);AlarmNetwork.remove(world,name,field)
        }
    }
    private class Backup:Base() {
        override fun id()="forkpatch_fire_alarm_backup_physical_energy_and_test_duration"
        override fun build(c:TestContext) {
            val p=AlarmPanelState();p.account(10.0,10.0,0.0)
            c.check("alarm backup: accepted input minus actual idle draw is stored",p.powered&&p.energy==50.0,"")
            p.trigger(true);p.test();repeat(100){p.account(.05,0.0,3.0)}
            c.check("alarm backup: field loads spend stored energy with SI units",p.powered&&abs(p.energy-10.0)<1e-9&&abs(p.testSeconds-5.0)<1e-9&&p.latched,"")
            repeat(26){p.account(.05,0.0,3.0)}
            c.check("alarm backup: exhaustion cannot provide free field power",!p.powered&&p.energy<1e-9,"")
            c.check("alarm latch: removing power does not silently clear a fire alarm",p.latched&&!p.reset(true)&&p.reset(false)&&!p.latched,"")
        }
    }
    private class Controls:Base() {
        override fun id()="forkpatch_fire_alarm_saved_controls_and_malformed_limits"
        override fun build(c:TestContext) {
            val e=alarm(c,AlarmKind.HEAT)
            for(value in listOf(Double.NaN,Double.POSITIVE_INFINITY,34.0,201.0))e.networkUnserialize(c.stream{it.writeByte(82);it.writeDouble(value)})
            for(value in listOf(0,9,Int.MAX_VALUE))e.networkUnserialize(c.stream{it.writeByte(89);it.writeInt(value)})
            c.check("alarm controls: invalid thresholds/radii preserve source defaults",e.threshold==60.0&&e.range==4,"")
            e.networkUnserialize(c.stream{it.writeByte(82);it.writeDouble(75.0)});e.networkUnserialize(c.stream{it.writeByte(89);it.writeInt(8)})
            e.changeChannel(" saved alarm ");val saved=CompoundTag();e.writeToNBT(saved)
            val restored=AlarmElement(TransparentNode(),e.descriptor);restored.readFromNBT(saved)
            c.check("alarm saves: source channel, threshold and detection range survive",restored.channel=="saved alarm"&&restored.threshold==75.0&&restored.range==8,"")
        }
    }
}
