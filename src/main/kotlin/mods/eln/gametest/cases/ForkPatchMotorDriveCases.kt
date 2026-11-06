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

object ForkPatchMotorDriveCases {
    @JvmStatic fun cases():List<TestCase> = listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP).map(::Supply)+Thermal()+Controls()+Packets()
    private open class Base:TestCase {
        lateinit var e:MotorDriveElement;val parts=ArrayList<Component>();lateinit var load:Resistor
        override fun id()="forkpatch_motor_drive_base"
        override fun build(c:TestContext) {}
        override fun measure(c:TestContext) {}
        fun place(c:TestContext) { e=c.placeTransparent(MotorDriveDescriptor.ID,c.at(3,1,1)) as MotorDriveElement }
        fun feed(r:Double=24.0) {
            load=Resistor(e.unit.output,null).setR(r);parts.add(load);parts.add(VoltageSource("drive supply",e.unit.input,null).setU(480.0));parts.forEach(Eln.simulator::addElectricalComponent)
        }
        fun cleanup() { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        fun panel(c:TestContext) {
            mods.eln.client.ClientRenders.registerAll();val pos=e.node.coordonate.pos();val client=TransparentNodeEntity(pos,c.level().getBlockState(pos));client.setLevel(c.level())
            try {
                val s=DataInputStream(ByteArrayInputStream(e.node.publishPacket.toByteArray()));s.readByte();repeat(4) { s.readInt() };s.readUTF();client.serverPublishUnserialize(s)
                val r=client.elementRender as? MotorDriveRender
                c.check("drive: actual native factory consumes publication",r!=null && s.available()==0,"")
                if(r!=null) {
                    c.check("drive: live frequency, output, temperature and latch reach panel",r.display.hertz==e.unit.hertz && r.display.tripped==e.unit.tripped && abs(r.display.output-e.unit.output.u)<1e-6 && abs(r.display.temperature-e.unit.temperature)<1e-6,"")
                    GlFrame.beginRenderFrame();val f=GlFrame.open();try { r.draw();c.check("drive: source enclosure and live input display render",f.batches().values.sumOf { it.vertices() }>100,"") }finally { GlFrame.close() }
                }
            }finally { client.clientDestroy() }
            val spec=mods.eln.gui.MenuSpecs.of(e.descriptor);c.check("drive: native settings panel has no phantom inventory",spec.width()==368 && spec.height()==292 && !spec.playerInventory() && spec.slots().isEmpty(),"")
        }
    }
    private class Supply(val front:Direction):Base() {
        override fun id()="forkpatch_motor_drive_${front.name.lowercase()}"
        override fun ticks()=120
        override fun build(c:TestContext) { place(c);e.front=front;e.unit.applySettings(doubleArrayOf(480.0,480.0,30.0,5.0,100.0));feed() }
        override fun measure(c:TestContext) {
            try {
                val u=e.unit;c.check("drive: registered 30Hz scalar output supplies real load at 200V after impedance drop",abs(u.hertz-30)<1e-6 && abs(u.output.u-200)<1e-6 && load.p>1666,"")
                c.check("drive: accepted input equals load plus physical winding losses",abs(u.inputWatts-u.outputWatts-u.lossWatts)<1e-6 && u.temperature>20,"")
                c.check("drive: all native power and signal terminals follow placement",e.getElectricalLoad(front.left(),LRDU.Down)===u.input && e.getElectricalLoad(Direction.YN,LRDU.Down)===u.input && e.getElectricalLoad(front.right(),LRDU.Down)===u.output && e.getElectricalLoad(front,LRDU.Down)===e.signal && e.getElectricalLoad(front,LRDU.Up)===e.tripSignal,front.name)
                panel(c);val temp=u.temperature;cleanup();val pos=e.node.coordonate.pos();Reload.node(c.level(),pos)
                val copy=(NodeManager.instance.getNodeFromCoordonate(Coordonate(pos,c.level())) as TransparentNode).element as MotorDriveElement
                c.check("drive: real placed reload retains settings and heat but restarts safely at zero",copy.unit.settings[2]==30.0 && abs(copy.unit.temperature-temp)<1e-9 && copy.unit.hertz==0.0 && !copy.unit.tap.enabled && copy.front==front,"")
            }finally { cleanup() }
        }
    }
    private class Thermal:Base() {
        override fun id()="forkpatch_motor_drive_thermal_latch_and_safe_reload"
        override fun ticks()=160
        override fun build(c:TestContext) {
            place(c);val t=CompoundTag().also(e.unit::write);t.putDouble("industrialTemperature",119.0);e.unit.read(t);feed(.001)
        }
        override fun measure(c:TestContext) {
            try {
                val u=e.unit;c.check("drive: winding overload trips a hot case and opens the physical tap",u.tripped && !u.tap.enabled && u.temperature>80 && load.p<1e-8,"")
                c.check("drive: actual trip signal is powered",e.tripSignal.u>Eln.SVU*.9,"")
                u.setEnabled(false);u.setEnabled(true);c.check("drive: enable cannot reset hot latch",u.tripped && !u.resetTrip(),"")
                panel(c);cleanup();val pos=e.node.coordonate.pos();Reload.node(c.level(),pos)
                val copy=(NodeManager.instance.getNodeFromCoordonate(Coordonate(pos,c.level())) as TransparentNode).element as MotorDriveElement
                c.check("drive: actual reload retains hot latched fault",copy.unit.tripped && !copy.unit.tap.enabled && copy.unit.temperature>80,"")
                val tag=CompoundTag().also(copy.unit::write);tag.putDouble("industrialTemperature",70.0);copy.unit.read(tag)
                c.check("drive: cool explicit reset clears latch but does not jump to running frequency",copy.unit.resetTrip() && !copy.unit.tripped && !copy.unit.tap.enabled && copy.unit.hertz==0.0,"")
            }finally { cleanup() }
        }
    }
    private class Controls:Base() {
        lateinit var clock:IProcess;lateinit var signal:VoltageSource;var elapsed=0.0;var phase=0
        override fun id()="forkpatch_motor_drive_enable_and_wired_inhibit_ramps"
        override fun ticks()=240
        override fun build(c:TestContext) {
            place(c);e.unit.applySettings(doubleArrayOf(480.0,480.0,30.0,5.0,100.0));signal=VoltageSource("drive inhibit",e.signal,null).setU(0.0);parts.add(signal);feed()
            clock=IProcess { dt->elapsed+=dt
                if(phase==0 && elapsed>=3.2) { c.check("drive: reaches programmed frequency",abs(e.unit.hertz-30)<1e-6,"");e.unit.setEnabled(false);phase=1 }
                else if(phase==1 && elapsed>=4.2) { c.check("drive: stop ramps down while remaining physically connected",e.unit.hertz in 17.0..19.0 && e.unit.tap.enabled,"");phase=2 }
                else if(phase==2 && elapsed>=6.0) { c.check("drive: completed stop opens tap",e.unit.hertz==0.0 && !e.unit.tap.enabled,"");e.unit.setEnabled(true);phase=3 }
                else if(phase==3 && elapsed>=9.0) { c.check("drive: restarted output reaches target",abs(e.unit.hertz-30)<1e-6,"");signal.u=Eln.SVU;phase=4 }
            };Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c:TestContext) {
            try { c.check("drive: physical wired inhibit ramps to zero and opens",phase==4 && e.unit.hertz==0.0 && !e.unit.tap.enabled && load.p<1e-8,"") }
            finally { Eln.simulator.removeElectricalProcess(clock);cleanup() }
        }
    }
    private class Packets:Base() {
        override fun id()="forkpatch_motor_drive_atomic_packets_and_settings_tool"
        override fun build(c:TestContext) { place(c);feed() }
        override fun measure(c:TestContext) {
            try {
                fun send(p:DoubleArray) { val b=ByteArrayOutputStream();DataOutputStream(b).use { s->s.writeByte(80);p.forEach(s::writeDouble) };e.networkUnserialize(DataInputStream(ByteArrayInputStream(b.toByteArray()))) }
                val old=e.unit.settings.copyOf();send(doubleArrayOf(480.0,240.0));c.check("drive: truncated native settings packet is atomic",e.unit.settings.contentEquals(old),"")
                send(doubleArrayOf(480.0,240.0,20.0,2.0,Double.NaN));c.check("drive: nonfinite native settings packet is atomic",e.unit.settings.contentEquals(old),"")
                send(doubleArrayOf(480.0,240.0,20.0,2.0,50.0));c.check("drive: valid settings apply through native packet",e.unit.settings[1]==240.0 && e.unit.settings[4]==50.0,"")
                val temp=e.unit.temperature;val energy=e.unit.lifetimeJoules;val player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
                e.readConfigTool(CompoundTag().apply { for(i in 0..4)putDouble("industrialSetting$i",IndustrialKind.DRIVE.defaults[i]);putDouble("industrialTemperature",20.0);putDouble("industrialLifetime",1e18);putBoolean("industrialTrip",false) },player)
                c.check("drive: settings tool cannot cool case or clone energy history",e.unit.settings.contentEquals(IndustrialKind.DRIVE.defaults) && e.unit.temperature==temp && e.unit.lifetimeJoules==energy,"")
            }finally { cleanup() }
        }
    }
}
