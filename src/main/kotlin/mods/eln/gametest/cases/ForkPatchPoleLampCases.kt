package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gl.GlFrame
import mods.eln.gridnode.*
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.registry.ElnDeviceRegistry
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.utility.*
import mods.eln.sixnode.electricalcable.*
import mods.eln.transparentnode.utilitylight.StreetLightMount
import net.minecraft.nbt.CompoundTag
import java.io.*
import kotlin.math.abs

object ForkPatchPoleLampCases {
    @JvmStatic fun cases(): List<TestCase> = DistributionPoles.supported.filter { it.light }.flatMap { listOf(Lamp(it,false),Lamp(it,true)) }+listOf(Controls(),Feeder())
    private open class Base: TestCase {
        val parts=ArrayList<Component>()
        override fun id()="forkpatch_pole_lamp_base"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {}
        fun pole(c: TestContext,k: DistributionKind,x: Int=6)=c.placeTransparent(DistributionPoles.id(k),c.at(x,1,6)) as DistributionPoleElement
        fun add(p: Component) { parts.add(p);Eln.simulator.addElectricalComponent(p) }
        fun clean() { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
    }
    private class Lamp(val kind: DistributionKind,val turnOff: Boolean): Base() {
        lateinit var e: DistributionPoleElement;var clock: IProcess?=null;var elapsed=0.0;var switched=false;var lit=false
        override fun id()="forkpatch_pole_lamp_${kind.name.lowercase()}_"+if(turnOff)"switch_off" else "powered"
        override fun ticks()=120
        override fun build(c: TestContext) {
            e=pole(c,kind)
            if(kind.transformer) {
                e.hardware.setItem(0,Eln.findItemStack(PoleHardware.transformerNames[0],1));add(VoltageSource("lamp primary",e.unit.lines[0],null).setU(12470.0))
            } else {
                val source=pole(c,DistributionKind.TRANSFORMER_SINGLE,14);source.hardware.setItem(0,Eln.findItemStack(PoleHardware.transformerNames[0],1))
                add(VoltageSource("lamp service source",source.unit.lines[0],null).setU(12470.0))
                val wire=UtilityCables.all.first { it.material==UtilityCableMaterial.COPPER && it.sizeLabel=="2 AWG" && it.insulated && !it.melted }
                if(kind.underground)add(mods.eln.sim.ElectricalConnection(source.unit.services[0],e.unit.services[0])) else check(GridLink.addLink(source,e,40,40,wire,8))
            }
            clock=IProcess { dt->elapsed+=dt;val l=e.unit.lamp!!;if(l.light>=14 && l.watts>kind.lampWatts*.98)lit=true
                if(turnOff && !switched && elapsed>=1.5) { c.check("pole lamp: real service powers light before switch-off",lit,"${l.watts}W ${l.light}");l.configure(false,false);switched=true } }
            Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c: TestContext) {
            try {
                val l=e.unit.lamp!!;val v=StreetLightMount.adjust(kind.lampHeads[0],e.front,l.heightStep,l.yaw,l.pitch)
                val pos=e.node.coordonate.pos().offset(kotlin.math.floor(v.x+.5).toInt(),kotlin.math.floor(v.y+.5).toInt(),kotlin.math.floor(v.z+.5).toInt())
                if(turnOff)c.check("pole lamp: no power or lingering world light after switch-off",switched && l.light==0 && l.watts<1e-6 && !c.level().getBlockState(pos).`is`(ElnDeviceRegistry.lightBlock),"${l.watts}W ${c.level().getBlockState(pos)}")
                else {
                    c.check("pole lamp: actual load and brightness come from supplied service",l.watts in kind.lampWatts*.98..kind.lampWatts*1.001 && l.light>=14,"${l.watts}W ${l.light}")
                    c.check("pole lamp: native temporary light block appears at the rendered head",c.level().getBlockState(pos).`is`(ElnDeviceRegistry.lightBlock),pos.toString())
                }
                val bytes=ByteArrayOutputStream();e.networkSerialize(DataOutputStream(bytes));val render=DistributionPoleRender(c.level().getBlockEntity(e.node.coordonate.pos()) as TransparentNodeEntity,e.descriptor)
                render.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray())))
                c.check("pole lamp: power, brightness and controls reach live display",render.display.light==l.light && render.display.lampOn==l.enabled && abs(render.display.lampWatts-l.watts)<1e-6,"")
                render.destructor()
            }finally { clock?.let(Eln.simulator::removeElectricalProcess);clean() }
        }
    }
    private class Controls: Base() {
        override fun id()="forkpatch_pole_lamp_controls_geometry_and_persistence"
        override fun measure(c: TestContext) {
            val e=pole(c,DistributionKind.STREET_THREE);val l=e.unit.lamp!!
            fun send(id: Int,value: Int?=null,b: Boolean?=null) { val bytes=ByteArrayOutputStream();val s=DataOutputStream(bytes);s.writeByte(id);value?.let(s::writeInt);b?.let(s::writeBoolean);e.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray()))) }
            send(102,b=false);send(103,b=true);send(104,-1);send(105,-5);send(106,5)
            c.check("pole lamp: native packets validate aim, height and dusk controls",!l.enabled && l.automatic && l.heightStep==-1 && l.yaw==355 && l.pitch==5,"")
            send(104,999);send(105,999);send(106,999);c.check("pole lamp: malformed aim packets do not alter physical fixture",l.heightStep==-1 && l.yaw==355 && l.pitch==5,"")
            val n=CompoundTag();e.writeToNBT(n);val copy=DistributionPoleElement(TransparentNode(),e.descriptor);copy.front=e.front;copy.readFromNBT(n)
            c.check("pole lamp: native NBT retains actual lamp controls",copy.unit.lamp!!.heightStep==-1 && copy.unit.lamp!!.yaw==355 && copy.unit.lamp!!.pitch==5 && !copy.unit.lamp!!.enabled && copy.unit.lamp!!.automatic,"")
            val d=e.descriptor as DistributionPoleDescriptor
            fun draw(display: DistributionDisplay): Int { GlFrame.beginRenderFrame();val f=GlFrame.open();try { d.drawEquipment(Direction.XN,display);return f.batches().values.fold(0) { a,b->31*a+b.data.copyOf(b.floats).contentHashCode() } }finally { GlFrame.close() } }
            c.check("pole lamp: fixture aim and illuminated lens affect recorded native geometry",draw(DistributionDisplay())!=draw(DistributionDisplay().apply { light=15;heightStep=-1;yaw=355;pitch=5 }),"")
            c.check("pole lamp: transformer hardware and controls fit native menu",mods.eln.gui.MenuSpecs.of(d).height()==442 && mods.eln.gui.MenuSpecs.of(d).slots().size==1,"")
        }
    }
    private class Feeder: Base() {
        lateinit var e: DistributionPoleElement;lateinit var load: Resistor
        override fun id()="forkpatch_pole_single_feeder_primary_and_secondary_ports"
        override fun build(c: TestContext) { e=pole(c,DistributionKind.FEEDER_SINGLE);add(VoltageSource("riser primary",e.unit.lines[0],null).setU(12470.0));load=Resistor(e.unit.outs[0],null).setR(1000.0);add(load);add(VoltageSource("independent LV",e.unit.services[2],null).setU(240.0)) }
        override fun measure(c: TestContext) {
            try {
                c.check("single feeder: fused underground primary carries actual power",load.p>150000 && !e.unit.fuses[0].blown,"")
                for(front in listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP)) { e.front=front
                    c.check("single feeder: source base and bottom primary ports share fused output",e.getElectricalLoad(Direction.YN,LRDU.Up)===e.unit.outs[0] && e.getElectricalLoad(front.left(),LRDU.Down)===e.unit.outs[0],front.toString())
                    c.check("single feeder: secondary remains separate from the 12.47 kV riser",e.getElectricalLoad(Direction.YN,LRDU.Down)===e.unit.services[2] && e.getElectricalLoad(front.right(),LRDU.Down)===e.unit.services[2],front.toString())
                }
                e.unit.operateFuse(0);c.check("single feeder: opening the riser does not disconnect an independent service",!e.unit.contacts[0].closed && e.unit.services[2].u>239,"")
            }finally { clean() }
        }
    }
}
