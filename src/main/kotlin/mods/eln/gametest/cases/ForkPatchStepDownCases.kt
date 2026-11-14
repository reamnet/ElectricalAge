package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gl.GlFrame
import mods.eln.gridnode.GridLink
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.mna.component.*
import mods.eln.transparentnode.utilitylight.*
import mods.eln.utility.*
import net.minecraft.nbt.CompoundTag
import java.io.*
import kotlin.math.abs

object ForkPatchStepDownCases {
    @JvmStatic fun cases(): List<TestCase> = listOf(Supply(120),Supply(240),Twin(),Recovery(false),Recovery(true),SettingsOnly(),LegacyCore())
    private class Supply(val voltage: Int): TestCase {
        lateinit var e: StepDownUnitElement;lateinit var load: Resistor;val parts=ArrayList<Component>()
        override fun id()="forkpatch_mains_stepdown_${voltage}v"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun ticks()=80
        override fun build(c: TestContext) {
            e=c.placeTransparent(StepDownUnitDescriptor.ID,c.at(4,1,4)) as StepDownUnitElement;e.unit.configure(voltage,25000,true)
            load=Resistor(e.unit.secondary,null).setR(voltage.toDouble()*voltage/1000)
            parts.add(VoltageSource("standalone supply",e.unit.primary,null).setU(3200.0));parts.add(load);parts.forEach(Eln.simulator::addElectricalComponent)
        }
        override fun measure(c: TestContext) {
            try {
                val u=e.unit;c.check("step-down: registered active converter supplies real regulated load",abs(u.secondary.u-voltage)<.2 && load.p>990 && u.input.p< -1000,"")
                c.check("step-down: converter dissipates electronics and winding losses",u.thermal.Tc>0 && -u.input.p-u.output.p>45,"")
                for(front in listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP)) {
                    e.front=front;c.check("step-down: native input/output terminals rotate consistently",e.getElectricalLoad(front.left(),LRDU.Down)===u.primary && e.getElectricalLoad(front.right(),LRDU.Down)===u.secondary && e.getElectricalLoad(front,LRDU.Down)==null,front.toString())
                }
                val bytes=ByteArrayOutputStream();e.networkSerialize(DataOutputStream(bytes));val r=StepDownUnitRender(c.level().getBlockEntity(e.node.coordonate.pos()) as TransparentNodeEntity,e.descriptor);r.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray())))
                c.check("step-down: live readings and selected capacity reach native display",r.display.ratedWatts==25000 && r.display.voltage==voltage && abs(r.display.output-u.secondary.u)<1e-8,"")
                GlFrame.beginRenderFrame();val frame=GlFrame.open();try { r.draw();c.check("step-down: enclosure renders through the native recording facade",frame.batches().values.sumOf { it.vertices() }>100,"") }finally { GlFrame.close();r.destructor() }
                c.check("step-down: native menu has the configured control panel",mods.eln.gui.MenuSpecs.of(e.descriptor).width()==368 && e.hasGui(),"")
                parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear()
                val pos=e.node.coordonate.pos();Reload.node(c.level(),pos);val saved=(mods.eln.node.NodeManager.instance.getNodeFromCoordonate(Coordonate(pos,c.level())) as TransparentNode).element as StepDownUnitElement
                c.check("step-down: placed reload preserves rating, winding and enable state",saved.unit.ratedWatts==25000 && saved.unit.outputVoltage==voltage && saved.unit.enabled,"")
            }finally { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        }
    }
    private class Twin: TestCase {
        lateinit var e: DistributionPoleElement;val parts=ArrayList<Component>()
        override fun id()="forkpatch_twin_led_selected_service_and_both_heads"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun ticks()=100
        override fun build(c: TestContext) {
            e=c.placeTransparent(7949,c.at(6,1,6)) as DistributionPoleElement;e.unit.lamp!!.chooseSupply(240)
            parts.add(VoltageSource("twin service240",e.unit.services[2],null).setU(240.0));parts.forEach(Eln.simulator::addElectricalComponent)
        }
        override fun measure(c: TestContext) {
            try {
                val l=e.unit.lamp!!;c.check("twin: both LED heads share one actual selected 120 W service load",l.selected==240 && abs(l.watts-120)<1e-6 && l.light>=14 && e.unit.transformer==null,"")
                for(head in e.kind.lampHeads) {
                    val v=StreetLightMount.adjust(head,e.front,l.heightStep,l.yaw,l.pitch);val p=e.node.coordonate.pos().offset(kotlin.math.floor(v.x+.5).toInt(),kotlin.math.floor(v.y+.5).toInt(),kotlin.math.floor(v.z+.5).toInt())
                    c.check("twin: each rendered head has its own temporary world light",c.level().getBlockState(p).`is`(mods.eln.registry.ElnDeviceRegistry.lightBlock),p.toString())
                }
                c.check("twin: legacy HV terminals cannot be restored as service feeds",(0..5).none(e::acceptsSavedGridTerminal) && (32..38).none(e::acceptsSavedGridTerminal) && listOf(40,41,42,50).all(e::acceptsSavedGridTerminal),"")
                c.check("twin: recovery slots reject manual and automated insertion",e.hardware.containerSize==2 && !e.hardware.canInsertItem(0,Eln.findItemStack(PoleHardware.transformerNames[0],1),0) && mods.eln.gui.MenuSpecs.of(e.descriptor).slots().all { !it.filter().test(Eln.findItemStack(PoleHardware.transformerNames[0],1)) },"")
                GlFrame.beginRenderFrame();val frame=GlFrame.open();try { (e.descriptor as DistributionPoleDescriptor).drawEquipment(e.front,DistributionDisplay().apply { capture(e) });c.check("twin: actual two-head source fixture is loaded and rendered",frame.batches().values.sumOf { it.vertices() }>200,"") }finally { GlFrame.close() }
            }finally { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        }
    }
    private class Recovery(val blown: Boolean): TestCase {
        override fun id()="forkpatch_twin_recovery_"+if(blown)"spent_cartridge"else"paid_core_and_cartridge"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {
            val e=c.placeTransparent(7949,c.at(6,1,6)) as DistributionPoleElement
            e.hardware.setItem(0,Eln.findItemStack("Mains Step-down Unit",1))
            val old=CutoutFuse();old.removeCartridge();old.insertCartridge(Eln.findItemStack(PoleHardware.fuseNames[5],1));if(blown)old.step(1.0,1000.0,10.0)
            val tag=CompoundTag();e.writeToNBT(tag);tag.remove("utilityLamp");tag.putInt("mainsOutputVoltage",240);tag.putInt("mainsRatedWatts",50000);tag.putInt("poleSelectedTerminal",32);tag.putBoolean("mainsLampAutomatic",true);old.write(tag,"poleFuse")
            val copy=DistributionPoleElement(TransparentNode(),e.descriptor);copy.readFromNBT(tag)
            c.check("twin: actual old hardware becomes one recoverable paid core",PoleHardware.transformerWatts(copy.hardware.getItem(0))==50000 && copy.unit.transformer==null,"")
            c.check("twin: actual former cartridge is recovered with spent state",copy.hardware.getItem(1).count==1 && (if(blown)PoleHardware.fuse(copy.hardware.getItem(1))?.spent==true else PoleHardware.fuse(copy.hardware.getItem(1))?.amps==10.0),"")
            c.check("twin: old lamp controls migrate while HV selection falls back to LV bundle",copy.unit.lamp!!.selected==240 && copy.unit.lamp!!.automatic && copy.terminal==50,"")
            copy.hardware.removeItemNoUpdate(0);copy.hardware.removeItemNoUpdate(1);copy.readItemStackNBT(tag);copy.readItemStackNBT(tag)
            c.check("twin: repeated carried reads cannot repeat hardware recovery",copy.hardware.isEmpty,"")
            val carry=copy.itemStackNBT;c.check("twin: new carried settings close the legacy gate without copying recovery inventory",carry.contains("utilityLamp") && !TwinPoleMigration.legacy(e.kind,carry) && !carry.contains("inv"),"")
        }
    }
    private class SettingsOnly: TestCase {
        override fun id()="forkpatch_twin_settings_cannot_mint_hardware"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {
            val e=c.placeTransparent(7949,c.at(6,1,6)) as DistributionPoleElement
            val tag=CompoundTag().apply { putInt("mainsOutputVoltage",240);putInt("mainsRatedWatts",100000) }
            e.readItemStackNBT(tag);c.check("twin: settings-only old tags do not create a core or cartridge",e.hardware.isEmpty,"")
            val original=Eln.findItemStack("Mains Step-down Unit",1);e.hardware.setItem(0,original);c.check("twin: unsupported old ratings cannot index a paid core",!PoleHardware.migrate(e.hardware,123456) && e.hardware.getItem(0)===original,"")
        }
    }
    private class LegacyCore: TestCase {
        override fun id()="forkpatch_legacy_stepdown_item_in_actual_transformer_slot"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {
            val e=c.placeTransparent(7972,c.at(6,1,6)) as DistributionPoleElement;e.hardware.setItem(0,Eln.findItemStack("Mains Step-down Unit",1))
            c.check("legacy core: actual standalone hardware retains the 10 kW fallback",e.unit.transformer!!.ratedWatts==10000 && mods.eln.gui.MenuSpecs.of(e.descriptor).slots()[0].filter().test(e.hardware.getItem(0)),"")
            val tag=CompoundTag();e.writeToNBT(tag);tag.putInt("mainsRatedWatts",75000)
            val copy=DistributionPoleElement(TransparentNode(),e.descriptor);copy.readFromNBT(tag)
            c.check("legacy core: placed legacy item converts once to its saved supported paid rating",copy.unit.transformer!!.ratedWatts==75000 && PoleHardware.transformerWatts(copy.hardware.getItem(0))==75000,"")
            c.check("legacy core: supported rating alone cannot manufacture installed hardware",!PoleHardware.migrate(net.minecraft.world.SimpleContainer(1),100000),"")
        }
    }
}
