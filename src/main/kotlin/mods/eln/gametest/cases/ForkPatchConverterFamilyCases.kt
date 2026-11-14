package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.ItemNbt
import mods.eln.gui.MenuSpecs
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.sixnode.electricalcable.*
import mods.eln.transparentnode.converter.*
import net.minecraft.nbt.CompoundTag
import java.io.*
import kotlin.math.abs

object ForkPatchConverterFamilyCases {
    @JvmStatic fun cases():List<TestCase> = listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP).flatMap { listOf(Fixed(it,false),Fixed(it,true)) }+
        OneWayDcDcMode.entries.flatMap { listOf(Directional(it,false),Directional(it,true),Protection(it)) }+
        OneWayDcDcMode.entries.filter { it!=OneWayDcDcMode.FIXED }.map(::Legacy)+NamesAndClamps()
    private fun cable(label:String="12 AWG")=UtilityCables.all.first { it.sizeLabel==label && it.material==UtilityCableMaterial.COPPER && !it.insulated && !it.melted }
    private fun spool(metres:Double,label:String="12 AWG")=cable(label).newItemStack().also { cable(label).setRemainingLengthMeters(it,metres) }
    private fun packet(id:Int,write:(DataOutputStream)->Unit={}):DataInputStream {
        val b=ByteArrayOutputStream();val s=DataOutputStream(b);s.writeByte(id);write(s);return DataInputStream(ByteArrayInputStream(b.toByteArray()))
    }
    private open class Base:ForkPatchVariableDcDcCases.Base() {
        fun installed(c:TestContext,id:Int,face:Direction=Direction.XN):VariableDcDcElement = place(c,face,id=id).also {
            it.inventory.setItem(0,spool(8.0));it.inventory.setItem(1,spool(4.0));it.inventory.setChanged()
        }
        fun remove(vararg components:Component)=components.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() }
        fun parts(vararg components:Component)=components.forEach(Eln.simulator::addElectricalComponent)
        fun policy(e:VariableDcDcElement,mode:OneWayDcDcMode) {
            e.settings.mode="RATIO";e.settings.value=if(mode==OneWayDcDcMode.BOOST)2.0 else .5;e.updateControl()
        }
    }
    private class Fixed(val face:Direction,val reverse:Boolean):Base() {
        lateinit var e:VariableDcDcElement;lateinit var feed:VoltageSource;lateinit var load:Resistor
        override fun id()="forkpatch_fixed_dcdc_"+face.name.lowercase()+if(reverse)"_reverse" else "_forward"
        override fun ticks()=60
        override fun build(c:TestContext) {
            e=installed(c,130,face)
            feed=VoltageSource("fixed fixture",if(reverse)e.secondary else e.primary,null).setU(if(reverse)60.0 else 120.0)
            load=Resistor(if(reverse)e.primary else e.secondary,null).setR(100.0);parts(feed,load)
        }
        override fun measure(c:TestContext) {
            try {
                c.check("fixed DC/DC: registered winding converter follows real front and paid turns",e is FixedDcDcElement && e.front==face && e.populated && e.transferRatio==.5,"")
                c.check("fixed DC/DC: installed metre ratio transfers reversible conservative power",load.p>20 && abs(e.secondaryInternal.u-.5*e.primaryInternal.u)<1e-6 && abs(e.primarySource.p+e.secondarySource.p)<1e-6,e.transfer.status)
                c.check("fixed DC/DC: fixed unit has two power entries and no signal terminals",e.getElectricalLoad(face.left(),LRDU.Down)===e.primary && e.getElectricalLoad(face.right(),LRDU.Down)===e.secondary && e.getElectricalLoad(face,LRDU.Down)==null && e.getElectricalLoad(face.inverse,LRDU.Down)==null,"")
                e.networkUnserialize(packet(40){it.writeInt(2)});e.networkUnserialize(packet(41){it.writeDouble(0.0)});e.networkUnserialize(packet(42){it.writeBoolean(false)})
                c.check("fixed DC/DC: variable-control packets cannot override physical winding ratio",e.transferRatio==.5 && e.converter.enabled && e.converter.voltageTarget==null,"")
                published(c,e)
            }finally { remove(feed,load) }
        }
    }
    private class Directional(val mode:OneWayDcDcMode,val reverse:Boolean):Base() {
        lateinit var e:VariableDcDcElement;lateinit var feed:VoltageSource;lateinit var load:Resistor
        override fun id()="forkpatch_one_way_dcdc_"+mode.name.lowercase()+if(reverse)"_reverse_block" else "_loaded"
        override fun ticks()=60
        override fun build(c:TestContext) {
            e=installed(c,131+mode.ordinal);policy(e,mode)
            feed=VoltageSource("one-way fixture",if(reverse)e.secondary else e.primary,null).setU(120.0)
            load=Resistor(if(reverse)e.primary else e.secondary,null).setR(500.0);parts(feed,load)
        }
        override fun measure(c:TestContext) {
            try {
                c.check("one-way DC/DC: correct registered mode owns paid winding inventory",e is OneWayDcDcElement && e.oneWayDescriptor.mode==mode && e.populated,"")
                if(reverse)c.check("one-way DC/DC: energizing output cannot backfeed the input",load.p<1e-10 && !e.primarySource.enabled && !e.secondarySource.enabled,e.transfer.status)
                else {
                    val expected=if(mode==OneWayDcDcMode.BOOST)240.0 else 60.0
                    c.check("one-way DC/DC: real loaded output follows source mode",e.secondary.u in expected*.98..expected*1.001 && load.p>1,"${e.secondary.u}, ${e.transfer.status}")
                    val efficiency=if(mode==OneWayDcDcMode.FIXED)1.0 else .97
                    c.check("one-way DC/DC: accepted ports conserve electronics and winding losses",e.primarySource.p<0 && e.secondarySource.p>0 && abs(-e.primarySource.p*efficiency-e.secondarySource.p)<1e-6*maxOf(1.0,abs(e.primarySource.p)),"${e.primarySource.p}, ${e.secondarySource.p}")
                }
                c.check("one-way DC/DC: signal entries exist only on variable modes",(e.getElectricalLoad(e.front,LRDU.Down)===e.control)==(mode!=OneWayDcDcMode.FIXED),"")
                published(c,e)
            }finally { remove(feed,load) }
        }
    }
    private class Protection(val mode:OneWayDcDcMode):Base() {
        lateinit var e:VariableDcDcElement;lateinit var feed:VoltageSource;lateinit var load:Resistor;lateinit var clock:IProcess
        var elapsed=0.0;var phase=0
        override fun id()="forkpatch_one_way_dcdc_"+mode.name.lowercase()+"_short_limit_and_recovery"
        override fun ticks()=160
        override fun build(c:TestContext) {
            e=installed(c,131+mode.ordinal);e.inventory.setItem(1,spool(4.0,"26 AWG"));e.inventory.setChanged();policy(e,mode)
            feed=VoltageSource("one-way fault fixture",e.primary,null).setU(120.0);load=Resistor(e.secondary,null).setR(1.0);parts(feed,load)
            clock=IProcess { dt->elapsed+=dt
                if(phase==0 && elapsed>=2.0) {
                    val limit=e.secondaryMeltCurrent
                    if(mode==OneWayDcDcMode.FIXED)c.check("one-way fixed: overload opens source without changing physical ratio",load.p<1e-8 && !e.primarySource.enabled && !e.secondarySource.enabled,e.transfer.status)
                    else if(mode==OneWayDcDcMode.BOOST)c.check("one-way boost: impossible short gain opens both ports",load.p<1e-8 && !e.primarySource.enabled && !e.secondarySource.enabled,e.transfer.status)
                    else c.check("one-way variable: actual short respects installed conductor current",load.current in .001..limit*1.00001 && e.populated,"${load.current} / $limit, ${e.transfer.status}")
                    load.r=if(mode==OneWayDcDcMode.BOOST)150.0 else 500.0;phase=1
                }
                if(mode==OneWayDcDcMode.BOOST && phase==1 && elapsed>=4.0) {
                    c.check("one-way boost: feasible overload respects installed conductor current",abs(load.current-e.secondaryMeltCurrent)<1e-4 && e.populated,"${load.current}, ${e.transfer.status}")
                    load.r=500.0;phase=2
                }
            };Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c:TestContext) {
            try {
                val expected=if(mode==OneWayDcDcMode.BOOST)240.0 else 60.0
                c.check("one-way DC/DC: real fault removal restores output without replacing healthy wire",phase==(if(mode==OneWayDcDcMode.BOOST)2 else 1) && e.secondary.u in expected*.97..expected*1.001 && e.populated && load.p>1,"${e.secondary.u}, ${e.transfer.status}")
                c.check("one-way DC/DC: accepted fault heat is counted once",e.windings.all { abs(it.heating.sampledJoules-it.heating.deliveredJoules-it.heating.pendingJoules)<1e-7 && it.heating.sampledJoules>=0 },"")
            }finally { Eln.simulator.removeElectricalProcess(clock);remove(feed,load) }
        }
    }
    private class Legacy(val mode:OneWayDcDcMode):Base() {
        lateinit var e:VariableDcDcElement;lateinit var feed:VoltageSource;lateinit var load:Resistor
        override fun id()="forkpatch_one_way_dcdc_"+mode.name.lowercase()+"_v1_save_and_explicit_v2_upgrade"
        override fun build(c:TestContext) {
            e=installed(c,131+mode.ordinal);e.settings.load(CompoundTag());e.inventory.setItem(1,spool(8.0));e.inventory.setChanged();policy(e,mode)
            feed=VoltageSource("one-way V1 fixture",e.primary,null).setU(120.0);load=Resistor(e.secondary,null).setR(500.0);parts(feed,load)
        }
        override fun measure(c:TestContext) {
            try {
                c.check("one-way DC/DC: historical V1 transfers with original efficiency policy",e.settings.version==1 && e.populated && load.p>1 && abs(e.primarySource.p+e.secondarySource.p)<1e-5,e.transfer.status)
                e.networkUnserialize(packet(43));e.inventory.setItem(1,spool(5.75));e.inventory.setChanged()
                c.check("one-way DC/DC: V2 upgrade is explicit, disabled and allows independent paid lengths",e.settings.version==2 && !e.settings.enabled && e.settings.mode=="RATIO" && e.settings.value==1.0 && e.populated,"")
                published(c,e)
            }finally { remove(feed,load) }
        }
    }
    private class NamesAndClamps:Base() {
        lateinit var e:VariableDcDcElement
        override fun id()="forkpatch_fixed_dcdc_native_name_and_bounded_paid_winding_ratio"
        override fun build(c:TestContext) { e=installed(c,130) }
        override fun measure(c:TestContext) {
            c.check("fixed DC/DC: distinct native130 name preserves native128 identity and source mapping",Eln.findItemStack("DC-DC Converter",1).item===Eln.transparentNodeItem.getDescriptor(128)!!.parentItem && Eln.findItemStack("Winding DC-DC Converter",1).item===e.descriptor.parentItem && FixedDcDcDescriptor.nativeRegistryNameForSource(130)=="winding_dc_dc_converter" && FixedDcDcDescriptor.nativeRegistryNameForSource(128)==null,"")
            for(reverse in listOf(false,true)) {
                e.inventory.setItem(0,spool(if(reverse)25600.0 else .00001));e.inventory.setItem(1,spool(if(reverse).00001 else 25600.0));e.inventory.setChanged()
                c.check("fixed DC/DC: extreme real winding lengths retain finite source ratio bounds",e.transferRatio==if(reverse)1.0/256 else 256.0,"")
            }
            val menu=MenuSpecs.of(e.descriptor)
            c.check("fixed DC/DC: shared menu retains four real filtered hardware slots",menu.slots().size==4 && menu.slots()[0].limit()==16 && menu.slots()[2].limit()==1 && menu.slots()[3].limit()==1,"")
            published(c,e)
        }
    }
}
