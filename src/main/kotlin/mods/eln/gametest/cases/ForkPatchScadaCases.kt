package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gl.GlFrame
import mods.eln.misc.*
import mods.eln.node.NodeManager
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.transparentnode.industrial.*
import mods.eln.transparentnode.switchgear.*
import mods.eln.utility.*
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.Blocks
import java.io.*
import kotlin.math.abs

object ForkPatchScadaCases {
    @JvmStatic fun cases():List<TestCase> = (0..9).map(::Endpoint)+Addresses()+Packets()+Power()
    private open class Base:TestCase {
        lateinit var e:ScadaElement;val parts=ArrayList<Component>();lateinit var supply:VoltageSource
        override fun id()="forkpatch_scada_base"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c:TestContext) { e=c.placeTransparent(ScadaDescriptor.ID,c.at(4,1,4)) as ScadaElement;supply=VoltageSource("SCADA supply",e.unit.input,null).setU(120.0);parts.add(supply);Eln.simulator.addElectricalComponent(supply) }
        override fun measure(c:TestContext) {}
        fun add(p:Component) { parts.add(p);Eln.simulator.addElectricalComponent(p) }
        fun cleanup() { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        fun bind(at:BlockPos,slot:Int=0,circuit:Int=1):Boolean { e.unit.select(slot);return e.unit.bind(doubleArrayOf(at.x.toDouble(),at.y.toDouble(),at.z.toDouble(),circuit.toDouble())) }
        fun panel(c:TestContext) {
            mods.eln.client.ClientRenders.registerAll();val pos=e.node.coordonate.pos();val client=TransparentNodeEntity(pos,c.level().getBlockState(pos));client.setLevel(c.level())
            try {
                val s=DataInputStream(ByteArrayInputStream(e.node.publishPacket.toByteArray()));s.readByte();repeat(4) { s.readInt() };s.readUTF();client.serverPublishUnserialize(s)
                val r=client.elementRender as? ScadaRender
                c.check("SCADA: actual native factory consumes full publication",r!=null && s.available()==0,"")
                if(r!=null) {
                    c.check("SCADA: selected address, circuit draft, power and telemetry reach display",r.display.selected==e.unit.selected && r.display.address==e.unit.targets[e.unit.selected] && r.display.settings.contentEquals(e.unit.settings) && r.display.telemetry==e.unit.telemetry && abs(r.display.watts-e.unit.resistor.p)<1e-6,"")
                    GlFrame.beginRenderFrame();val f=GlFrame.open();try { r.draw();c.check("SCADA: source console and live voltage display render",f.batches().values.sumOf { it.vertices() }>100,"") }finally { GlFrame.close() }
                }
            }finally { client.clientDestroy() }
            val spec=mods.eln.gui.MenuSpecs.of(e.descriptor);c.check("SCADA: eight-target native control panel has no phantom inventory",spec.width()==368 && spec.height()==292 && !spec.playerInventory() && spec.slots().isEmpty(),"")
        }
    }
    private class Endpoint(val type:Int):Base() {
        lateinit var target:TransparentNodeElement;var load:Resistor?=null;lateinit var clock:IProcess;var elapsed=0.0;var phase=0
        override fun id()="forkpatch_scada_"+listOf("load_bank","switchgear","service_disconnect","latched_relay","locked_recloser","air_break","motor_drive","latched_pad","pack_charger","welder")[type]
        override fun ticks()=80
        fun active():Boolean=when(val t=target) {
            is LoadBankElement->t.unit.enabled;is SwitchgearElement->t.unit.enabled;is HouseServiceElement->!t.unit.disconnected
            is IndustrialElement->t.unit.enabled;is DistributionPoleElement->if(type==4)t.unit.recloser.closed else t.unit.gangClosed
            is MotorDriveElement->t.unit.enabled;is PadTransformerElement->t.unit.enabled;is ChargerElement->t.unit.enabled;is WelderElement->t.unit.enabled;else->false
        }
        override fun build(c:TestContext) {
            super.build(c)
            val id=listOf(8064,8037,7992,8066,7977,7983,8069,8068,8071,8072)[type]
            val pos=c.at(12,1,12)
            target=if(type==2) {
                c.level().setBlockAndUpdate(pos.south(),Blocks.STONE.defaultBlockState());c.level().setBlockAndUpdate(pos.south().above(),Blocks.STONE.defaultBlockState())
                val d=requireNotNull(Eln.transparentNodeItem.getDescriptor(id));val player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
                requireNotNull((d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),player,c.level(),pos,Direction.ZP)).element
            }else c.placeTransparent(id,pos)
            when(val t=target) {
                is LoadBankElement->add(VoltageSource("remote bank",t.unit.input,null).setU(480.0))
                is SwitchgearElement->{add(VoltageSource("remote contactor",t.unit.commons[0],null).setU(120.0));add(VoltageSource("contactor gate",t.unit.gate,null).setU(Eln.SVU));load=Resistor(t.unit.normallyOpen[0],null).setR(12.0)}
                is HouseServiceElement->{add(VoltageSource("remote disconnect",t.unit.line[2],null).setU(240.0));load=Resistor(t.unit.load[2],null).setR(57.6)}
                is IndustrialElement->{t.unit.applySettings(doubleArrayOf(480.0,10.0,.8,1.2,.05,0.0));add(VoltageSource("relay overload",t.unit.input,null).setU(480.0));load=Resistor(t.unit.outputs[0],null).setR(24.0)}
                is DistributionPoleElement->if(type==4) {t.unit.recloser.read(CompoundTag().apply {putBoolean("recloserLocked",true);putBoolean("recloserClosed",false)});t.unit.applyContacts()} else {add(VoltageSource("gang switch",t.unit.lines[0],null).setU(3200.0));load=Resistor(t.unit.outs[0],null).setR(10000.0)}
                is MotorDriveElement->{add(VoltageSource("remote drive",t.unit.input,null).setU(480.0));load=Resistor(t.unit.output,null).setR(24.0)}
                is ChargerElement->{t.getInventory().setItem(0,Eln.findItemStack(WorkshopParts.PACK,1));t.unit.prepare();add(VoltageSource("remote pack charger",t.unit.input,null).setU(240.0))}
                is WelderElement->{t.getInventory().setItem(0,net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,3));t.getInventory().setItem(1,net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,3));t.unit.prepare();add(VoltageSource("remote welder",t.unit.input,null).setU(240.0))}
                is PadTransformerElement->{t.unit.protection.tripped=true;t.unit.protection.reason=7;t.unit.configure()}
            }
            load?.let(::add);check(bind(target.node.coordonate.pos()))
            clock=IProcess { dt->elapsed+=dt
                if(phase==0 && elapsed>=.6) {
                    c.check("SCADA: powered console opens supported endpoint",e.unit.command(false),target.descriptor.name);phase=1
                }else if(phase==1 && elapsed>=1.6) {
                    c.check("SCADA: remote open reaches actual device state",!active(),target.descriptor.name)
                    if(type in listOf(1,2,5,6))c.check("SCADA: remote open removes actual load after physical stop boundary",load!!.p<1e-6,target.descriptor.name)
                    val accepted=e.unit.command(true);c.check("SCADA: close delegates without resetting protection",accepted==(type!=4),target.descriptor.name);phase=2
                }
            };Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c:TestContext) {
            try {
                c.check("SCADA: endpoint command sequence completed",phase==2,"")
                if(type==4) {val t=target as DistributionPoleElement;c.check("SCADA: remote close cannot reset locked recloser",t.unit.recloser.locked && !t.unit.recloser.closed,"")}
                else c.check("SCADA: enabled target preserves its own controls",active(),target.descriptor.name)
                if(type in listOf(1,2,5,6))c.check("SCADA: restored endpoint supplies actual load",load!!.p>1,target.descriptor.name)
                if(type==0)c.check("SCADA: enabled bank resumes real consumption",(target as LoadBankElement).unit.watts>49000,"")
                if(type==8) {val t=target as ChargerElement;c.check("SCADA: remote charger resumes actual pack charging",t.unit.watts>7100 && ChargerInventory.battery(t.getInventory().getItem(0))!!.getEnergy(t.getInventory().getItem(0))>0,"")}
                if(type==9) {val t=target as WelderElement;c.check("SCADA: remote welder resumes paid work on its actual inventory",t.unit.watts>5900 && t.unit.workJoules>0 && t.getInventory().getItem(0).count==3,"")}
                if(type==3) {val t=target as IndustrialElement;c.check("SCADA: enabling never clears actual overload trip",t.unit.protection.tripped && !t.unit.contacts[0].closed && load!!.p<1e-6,"")}
                if(type==7) {val t=target as PadTransformerElement;c.check("SCADA: enabling cannot clear core trip or mint paid hardware",t.unit.protection.tripped && !t.unit.tap.enabled && t.inventory.isEmpty,"")}
                e.unit.poll();c.check("SCADA: telemetry names the actual selected device",e.unit.telemetry.contains(target.descriptor.name),"");panel(c)
            }finally {Eln.simulator.removeElectricalProcess(clock);cleanup()}
        }
    }
    private class Addresses:Base() {
        override fun id()="forkpatch_scada_range_unloaded_target_and_real_saved_addresses"
        override fun measure(c:TestContext) {
            try {
                for(i in 0..7)c.check("SCADA: eight address slots bind independently",bind(c.at(5+i,1,6),i,i+1),"")
                val addresses=e.unit.targets.copyOf();val circuits=e.unit.circuits.copyOf();val here=e.node.coordonate.pos()
                c.check("SCADA: out-of-range bind cannot overwrite a saved address",!e.unit.bind(doubleArrayOf(here.x+257.0,here.y.toDouble(),here.z.toDouble(),1.0)) && e.unit.targets.contentEquals(addresses),"")
                val before=c.level().chunkSource.loadedChunksCount
                c.check("SCADA: out-of-range lookup cannot request terrain",ScadaLink.target(e,here.offset(257,0,0).asLong())==null && c.level().chunkSource.loadedChunksCount==before,"")
                val missing=listOf(here.offset(0,0,-255),here.offset(-255,0,0),here.offset(0,0,255),here.offset(255,0,0)).firstOrNull { c.level().chunkSource.getChunkNow(it.x shr 4,it.z shr 4)==null }
                if(missing!=null) {
                    val count=c.level().chunkSource.loadedChunksCount
                    c.check("SCADA: unloaded in-range target stays unloaded",ScadaLink.target(e,missing.asLong())==null && c.level().chunkSource.getChunkNow(missing.x shr 4,missing.z shr 4)==null && c.level().chunkSource.loadedChunksCount==count,"")
                }else c.line("INFO SCADA unloaded-target branch unavailable: surrounding chunks already loaded")
                for(front in listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP)) {
                    e.front=front;c.check("SCADA: supply and signal ports follow native orientation",e.getElectricalLoad(front.left(),LRDU.Down)===e.unit.input && e.getElectricalLoad(Direction.YN,LRDU.Down)===e.unit.input && e.getElectricalLoad(front,LRDU.Down)===e.signal && e.getElectricalLoad(front,LRDU.Up)===e.tripSignal && e.getElectricalLoad(front.right(),LRDU.Down)==null,"")
                }
                panel(c);cleanup();val pos=e.node.coordonate.pos();Reload.node(c.level(),pos)
                val saved=(NodeManager.instance.getNodeFromCoordonate(Coordonate(pos,c.level())) as TransparentNode).element as ScadaElement
                c.check("SCADA: actual placed reload retains all eight world-local addresses",saved.unit.selected==7 && saved.unit.targets.contentEquals(addresses) && saved.unit.circuits.contentEquals(circuits),"")
            }finally {cleanup()}
        }
    }
    private class Packets:Base() {
        override fun id()="forkpatch_scada_atomic_bind_and_native_control_packets"
        override fun measure(c:TestContext) {
            try {
                fun send(write:(DataOutputStream)->Unit) {val b=ByteArrayOutputStream();DataOutputStream(b).use(write);e.networkUnserialize(DataInputStream(ByteArrayInputStream(b.toByteArray())))}
                val here=e.node.coordonate.pos();val at=here.offset(1,0,0);val old=e.unit.settings.copyOf()
                send {it.writeByte(87);it.writeDouble(at.x.toDouble());it.writeDouble(at.y.toDouble())}
                c.check("SCADA: truncated bind leaves both address and draft unchanged",e.unit.targets[0]==Long.MIN_VALUE && e.unit.settings.contentEquals(old),"")
                send {s->s.writeByte(87);doubleArrayOf(at.x.toDouble(),at.y.toDouble(),at.z.toDouble(),3.0).forEach(s::writeDouble)}
                c.check("SCADA: atomic native bind records address and circuit together",e.unit.targets[0]==at.asLong() && e.unit.circuits[0]==2,"")
                send {it.writeByte(84);it.writeInt(99)};c.check("SCADA: invalid native slot cannot select another address",e.unit.selected==0,"")
                send {it.writeByte(85);it.writeBoolean(true)};c.check("SCADA: missing target is reported as rejected",e.unit.telemetry.contains("rejected"),"")
                send {it.writeByte(81);it.writeBoolean(false)};c.check("SCADA: native disable blocks commands immediately",!e.unit.enabled && !e.unit.canCommand,"")
            }finally {cleanup()}
        }
    }
    private class Power:Base() {
        lateinit var target:LoadBankElement;lateinit var signal:VoltageSource;lateinit var clock:IProcess;var elapsed=0.0;var phase=0
        override fun id()="forkpatch_scada_real_power_inhibit_and_unsupported_console_guard"
        override fun ticks()=80
        override fun build(c:TestContext) {
            super.build(c);target=c.placeTransparent(LoadBankDescriptor.ID,c.at(12,1,12)) as LoadBankElement;check(bind(target.node.coordonate.pos()));signal=VoltageSource("console inhibit",e.signal,null).setU(Eln.SVU);add(signal)
            clock=IProcess {dt->elapsed+=dt
                if(phase==0 && elapsed>=.5) {c.check("SCADA: actual inhibit blocks remote commands and opens 20W draw",!e.unit.command(false) && target.unit.enabled && e.unit.resistor.p<1e-8,"");signal.u=0.0;supply.u=60.0;phase=1}
                else if(phase==1 && elapsed>=1.0) {c.check("SCADA: sagged console cannot command",!e.unit.command(false) && target.unit.enabled,"");supply.u=180.0;phase=2}
                else if(phase==2 && elapsed>=1.5) {c.check("SCADA: overvoltage console cannot command or draw",!e.unit.command(false) && e.unit.resistor.p<1e-8,"");supply.u=120.0;phase=3}
                else if(phase==3 && elapsed>=2.0) {c.check("SCADA: healthy supply restores real 20W draw and remote control",abs(e.unit.resistor.p-20)<1e-6 && e.unit.command(false) && !target.unit.enabled,"");phase=4}
            };Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c:TestContext) {
            try {c.check("SCADA: physical supply sequence completed",phase==4,"");bind(e.node.coordonate.pos());c.check("SCADA: consoles cannot recursively command consoles",!e.unit.command(false) && e.unit.enabled,"")}
            finally {Eln.simulator.removeElectricalProcess(clock);cleanup()}
        }
    }
}
