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
import net.minecraft.nbt.CompoundTag
import java.io.*
import kotlin.math.abs

object ForkPatchLoadBankCases {
    @JvmStatic fun cases():List<TestCase> = listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP).map(::Supply)+Controls()+Packets()
    private open class Base:TestCase {
        lateinit var e:LoadBankElement
        val parts=ArrayList<Component>()
        override fun id()="forkpatch_load_bank_base"
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {}
        fun place(c: TestContext) { e=c.placeTransparent(LoadBankDescriptor.ID,c.at(3,1,1)) as LoadBankElement }
        fun feed(volts:Double=480.0) { parts.add(VoltageSource("load bank feed",e.unit.input,null).setU(volts));parts.forEach(Eln.simulator::addElectricalComponent) }
        fun cleanup() { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        fun publish(c:TestContext) {
            mods.eln.client.ClientRenders.registerAll()
            val pos=e.node.coordonate.pos();val client=TransparentNodeEntity(pos,c.level().getBlockState(pos));client.setLevel(c.level())
            try {
                val input=DataInputStream(ByteArrayInputStream(e.node.publishPacket.toByteArray()))
                input.readByte();repeat(4) { input.readInt() };input.readUTF();client.serverPublishUnserialize(input)
                val r=client.elementRender as? LoadBankRender
                c.check("load bank: real client factory consumes complete publication",r!=null && input.available()==0,"")
                if(r!=null) {
                    c.check("load bank: accepted power, heat and meter reach display",abs(r.display.watts-e.unit.watts)<1e-6 && abs(r.display.temperature-e.unit.temperature)<1e-6 && abs(r.display.joules-e.unit.lifetimeJoules)<1e-6,"")
                    c.check("load bank: registered native fan loop follows accepted power",mods.eln.sound.ElnSounds.names().any { it.toString()=="eln:appliance_industrial_fan" } && r.loopedSounds.size==1 && abs(r.loopedSounds[0].getVolume()-(if(e.unit.watts>50).25f else 0f))<1e-6 && r.loopedSounds[0].getPitch()==.8f,"")
                    GlFrame.beginRenderFrame();val frame=GlFrame.open()
                    try { r.draw();c.check("load bank: source enclosure emits native geometry",frame.batches().values.sumOf { it.vertices() }>100,"") }finally { GlFrame.close() }
                }
            }finally { client.clientDestroy() }
            val spec=mods.eln.gui.MenuSpecs.of(e.descriptor)
            c.check("load bank: native settings panel has no phantom slots",spec.width()==368 && spec.height()==224 && !spec.playerInventory() && spec.slots().isEmpty(),"")
        }
    }
    private class Supply(val front:Direction):Base() {
        override fun id()="forkpatch_load_bank_${front.name.lowercase()}"
        override fun build(c: TestContext) { place(c);e.front=front;feed(240.0) }
        override fun measure(c: TestContext) {
            try {
                c.check("load bank: real 240V supply draws one quarter of 480V programmed power",abs(e.unit.watts-12500)<1e-6 && abs(e.unit.amps-12500.0/240)<1e-6,"")
                c.check("load bank: physical intervals accumulate consumed energy and heat",e.unit.lifetimeJoules>1000 && e.unit.temperature>20,"")
                c.check("load bank: input and signal rotate without inventing power output terminals",e.getElectricalLoad(front.left(),LRDU.Down)===e.unit.input && e.getElectricalLoad(Direction.YN,LRDU.Down)===e.unit.input && e.getElectricalLoad(front,LRDU.Down)===e.signal && e.getElectricalLoad(front.right(),LRDU.Down)==null && e.getElectricalLoad(front,LRDU.Up)===e.tripSignal && abs(e.tripSignal.u)<1e-9,front.name)
                publish(c);val heat=e.unit.temperature;val joules=e.unit.lifetimeJoules;cleanup()
                val pos=e.node.coordonate.pos();Reload.node(c.level(),pos)
                val restored=(NodeManager.instance.getNodeFromCoordonate(Coordonate(pos,c.level())) as TransparentNode).element as LoadBankElement
                c.check("load bank: real placed reload preserves heat, meter and controls",abs(restored.unit.temperature-heat)<1e-9 && abs(restored.unit.lifetimeJoules-joules)<1e-9 && restored.unit.nominal==480.0 && restored.front==front,"")
            }finally { cleanup() }
        }
    }
    private class Controls:Base() {
        lateinit var clock:IProcess;lateinit var inhibit:VoltageSource;var elapsed=0.0;var phase=0
        override fun id()="forkpatch_load_bank_signal_disable_and_thermal_recovery"
        override fun ticks()=160
        override fun build(c: TestContext) {
            place(c);e.unit.setEnabled(false);inhibit=VoltageSource("bank inhibit",e.signal,null).setU(0.0);parts.add(inhibit);feed()
            clock=IProcess { dt->elapsed+=dt
                if(phase==0 && elapsed>=.4) { c.check("load bank: disabled physical resistor is open",e.unit.watts<1e-8,"");e.unit.setEnabled(true);phase=1 }
                else if(phase==1 && elapsed>=.8) { c.check("load bank: enable restores real load",abs(e.unit.watts-50000)<1e-6,"");inhibit.u=Eln.SVU;phase=2 }
                else if(phase==2 && elapsed>=1.2) { c.check("load bank: wired inhibit opens physical resistor",e.unit.watts<1e-8,"");inhibit.u=0.0;phase=3 }
                else if(phase==3 && elapsed>=1.6) { c.check("load bank: wired release restores real load",abs(e.unit.watts-50000)<1e-6,"");val t=CompoundTag().also(e.unit::write);t.putDouble("industrialTemperature",130.0);e.unit.read(t);phase=4 }
                else if(phase==4 && elapsed>=1.8) { c.check("load bank: hot saved case disconnects while cooling",e.unit.watts<1e-8 && e.unit.temperature>120,"");phase=5 }
            };Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c: TestContext) {
            try { c.check("load bank: cooling restores nominal physical load without reset",phase==5 && e.unit.temperature<120 && abs(e.unit.watts-50000)<1e-6,"");publish(c) }
            finally { Eln.simulator.removeElectricalProcess(clock);cleanup() }
        }
    }
    private class Packets:Base() {
        override fun id()="forkpatch_load_bank_transactional_packets_and_config_tool"
        override fun build(c: TestContext) { place(c);feed() }
        override fun measure(c: TestContext) {
            try {
                fun send(writer:(DataOutputStream)->Unit) {
                    val b=ByteArrayOutputStream();DataOutputStream(b).use(writer)
                    e.networkUnserialize(DataInputStream(ByteArrayInputStream(b.toByteArray())))
                }
                send { it.writeByte(80);it.writeDouble(120.0) }
                c.check("load bank: truncated native settings packet cannot partially apply",e.unit.nominal==480.0 && e.unit.demand==50000.0,"")
                send { it.writeByte(80);it.writeDouble(120.0);it.writeDouble(Double.NaN) }
                c.check("load bank: nonfinite packet is rejected",e.unit.nominal==480.0 && e.unit.demand==50000.0,"")
                send { it.writeByte(80);it.writeDouble(240.0);it.writeDouble(1000.0) }
                c.check("load bank: complete native settings packet applies atomically",e.unit.nominal==240.0 && e.unit.demand==1000.0,"")
                val heat=e.unit.temperature;val joules=e.unit.lifetimeJoules
                val player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
                e.readConfigTool(CompoundTag().apply { putDouble("industrialSetting0",480.0);putDouble("industrialSetting1",2000.0);putDouble("industrialTemperature",20.0);putDouble("industrialLifetime",1e18) },player)
                c.check("load bank: settings tool transfers controls without copying heat or history",e.unit.demand==2000.0 && e.unit.temperature==heat && e.unit.lifetimeJoules==joules,"")
                c.check("load bank: original source identity is preserved",e.descriptor.parentItemDamage==8064 && Eln.findItemStack("Programmable Load Bank",1).count==1,"")
            }finally { cleanup() }
        }
    }
}
