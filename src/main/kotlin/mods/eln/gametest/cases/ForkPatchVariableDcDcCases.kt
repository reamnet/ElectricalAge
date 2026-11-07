package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.ItemNbt
import mods.eln.gl.GlFrame
import mods.eln.gui.MenuSpecs
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodeMenus
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.sixnode.electricalcable.*
import mods.eln.transparentnode.converter.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.io.*
import kotlin.math.abs

/** Paid windings and the actual accepted-step solver, container, publication and owner save path. */
object ForkPatchVariableDcDcCases {
    @JvmStatic fun cases():List<TestCase> = listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP).map { Circuit(it,false) }+
        Circuit(Direction.XN,true)+ControlSequence()+InventoryAndHeat()+Legacy()+Melt(false)+Melt(true)+WindingGuards()
    private fun wire()=UtilityCables.all.first { it.material==UtilityCableMaterial.COPPER && it.sizeLabel=="12 AWG" && !it.insulated && !it.melted }
    private fun spool(metres:Double)=wire().newItemStack().also { wire().setRemainingLengthMeters(it,metres) }
    private fun core()=requireNotNull(Eln.findItemStack("Cheap Ferromagnetic Core",1))
    private fun packet(id:Int,write:(DataOutputStream)->Unit={}):DataInputStream {
        val bytes=ByteArrayOutputStream();val out=DataOutputStream(bytes);out.writeByte(id);write(out);return DataInputStream(ByteArrayInputStream(bytes.toByteArray()))
    }
    open class Base:TestCase {
        override fun id()="forkpatch_variable_dcdc_base"
        override fun build(c:TestContext){}
        override fun measure(c:TestContext){}
        fun place(c:TestContext,front:Direction=Direction.XN,filled:Boolean=true,id:Int=VariableDcDcDescriptor.ID):VariableDcDcElement {
            val player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());val yaw=player.yRot;val pitch=player.xRot
            val element=try {
                player.xRot=0f;player.yRot=when(front) { Direction.XN->270f;Direction.XP->90f;Direction.ZN->0f;else->180f }
                c.placeTransparent(id,c.at(4,1,1)) as VariableDcDcElement
            }finally { player.yRot=yaw;player.xRot=pitch }
            if(filled) {
                element.inventory.setItem(0,spool(3.25));element.inventory.setItem(1,spool(7.75));element.inventory.setItem(2,core());element.inventory.setChanged()
            }
            return element
        }
        fun published(c:TestContext,e:VariableDcDcElement) {
            mods.eln.client.ClientRenders.registerAll()
            val pos=e.node.coordonate.pos();val client=TransparentNodeEntity(pos,c.level().getBlockState(pos));client.setLevel(c.level())
            val input=DataInputStream(ByteArrayInputStream(e.node.publishPacket.toByteArray()))
            try {
                input.readByte();repeat(4) { input.readInt() };input.readUTF();client.serverPublishUnserialize(input)
                val r=client.elementRender as? VariableDcDcRender
                c.check("variable DC/DC: actual native publication selects its registered client factory",r!=null,"")
                requireNotNull(r)
                c.check("variable DC/DC: complete native publication includes settings, paid coils, temperatures and voltmeters",input.available()==0 && r.settings.version==e.settings.version && r.settings.mode==e.settings.mode && r.settings.enabled==e.settings.enabled && r.primaryCelsius==e.primaryThermal.Tc+20 && r.secondaryCelsius==e.secondaryThermal.Tc+20 && r.primaryVolts==e.primary.u && r.secondaryVolts==e.secondary.u && r.stacks[3].count==e.inventory.getItem(3).count,"")
                c.check("variable DC/DC: renderer uses actual coil and signal cable types",r.getCableRender(e.front.left(),LRDU.Down)===DcDcWinding.of(e.inventory.getItem(0))?.descriptor?.render && r.getCableRender(e.front,LRDU.Down)===(if((e.descriptor as VariableDcDcDescriptor).variable)mods.eln.registry.ElnDeviceRegistry.signalCableDescriptor.render else null),"")
                GlFrame.beginRenderFrame();val frame=GlFrame.open()
                try { r.draw();c.check("variable DC/DC: original coil, core and housing mesh renders natively",frame.batches().values.sumOf { it.vertices() }>100,"") }finally { GlFrame.close() }
            }finally { client.clientDestroy() }
        }
    }
    private class Circuit(val face:Direction,val reverse:Boolean):Base() {
        lateinit var e:VariableDcDcElement;lateinit var feed:VoltageSource;lateinit var load:Resistor
        override fun id()="forkpatch_variable_dcdc_loaded_"+face.name.lowercase()+if(reverse)"_reverse" else "_forward"
        override fun ticks()=60
        override fun build(c:TestContext) {
            e=place(c,face);e.settings.mode="RATIO";e.settings.value=.5;e.updateControl()
            feed=VoltageSource("converter fixture",if(reverse)e.secondary else e.primary,null).setU(if(reverse)60.0 else 120.0)
            load=Resistor(if(reverse)e.primary else e.secondary,null).setR(100.0)
            listOf(feed,load).forEach(Eln.simulator::addElectricalComponent)
        }
        override fun measure(c:TestContext) {
            try {
                c.check("variable DC/DC: actual placement follows every horizontal front",e.front==face,"${e.front}, $face")
                c.check("variable DC/DC: independent paid spool lengths populate V2",e.populated && DcDcWinding.of(e.inventory.getItem(0))!!.amount==3.25 && DcDcWinding.of(e.inventory.getItem(1))!!.amount==7.75,e.construction)
                c.check("variable DC/DC: native coupled solver transfers reversible loaded power",e.converter.status=="TRANSFERRING" && load.p>20 && abs(e.secondaryInternal.u-e.primaryInternal.u*.5)<1e-6,"${e.converter.status}, load ${load.p}")
                val systems=listOfNotNull(e.primaryInternal.subSystem,e.secondaryInternal.subSystem).distinct()
                val versions=systems.map { it.matrixVersion }
                repeat(1000) { e.updateControl();e.windings.forEach { it.update() } }
                c.check("variable DC/DC: stable control and cached winding properties retain native LU factors",systems.map { it.matrixVersion }==versions && systems.all { Batch1Cases.field<Boolean>(it,"matrixValid") },"")
                val input=e.primarySource.p;val output=e.secondarySource.p
                c.check("variable DC/DC: accepted winding ports conserve power",abs(input+output)<1e-6*maxOf(1.0,abs(input)),"$input / $output")
                c.check("variable DC/DC: paid copper resistance produces real thermal loss",e.primaryResistance.r>0 && e.secondaryResistance.r>e.primaryResistance.r && e.windings.sumOf { it.heating.sampledJoules }>0,"")
                c.check("variable DC/DC: directional terminal and signal masks match source",e.getElectricalLoad(face.left(),LRDU.Down)===e.primary && e.getElectricalLoad(face.right(),LRDU.Down)===e.secondary && e.getElectricalLoad(face,LRDU.Down)===e.control && e.getElectricalLoad(face.inverse,LRDU.Down)===e.control && e.getElectricalLoad(face,LRDU.Up)==null && e.getConnectionMask(face,LRDU.Down)==NodeBase.maskElectricalInputGate,"")
                published(c,e)
                c.check("variable DC/DC: historical native converter identity remains unchanged",Eln.transparentNodeItem.getDescriptor(128)!!.javaClass===mods.eln.transparentnode.transformer.TransformerDescriptor::class.java,"")
            }finally { listOf(feed,load).forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() } }
        }
    }
    private class ControlSequence:Base() {
        lateinit var e:VariableDcDcElement;lateinit var feed:VoltageSource;lateinit var load:Resistor;lateinit var clock:IProcess
        var elapsed=0.0;var phase=0
        override fun id()="forkpatch_variable_dcdc_voltage_zero_disable_recovery_and_packet_guards"
        override fun ticks()=140
        override fun build(c:TestContext) {
            e=place(c);e.settings.mode="VOLTAGE";e.settings.value=48.0;e.updateControl()
            feed=VoltageSource("converter target fixture",e.primary,null).setU(120.0);load=Resistor(e.secondary,null).setR(100.0)
            listOf(feed,load).forEach(Eln.simulator::addElectricalComponent)
            clock=IProcess { dt->elapsed+=dt
                when {
                    phase==0 && elapsed>=1.0 -> { c.check("variable DC/DC: voltage target drives loaded winding",load.p>20 && e.secondary.u in 47.0..48.01,"${e.secondary.u}");e.networkUnserialize(packet(41){it.writeDouble(0.0)});phase++ }
                    phase==1 && elapsed>=2.0 -> { c.check("variable DC/DC: target zero opens both internal ports",!e.primarySource.enabled && !e.secondarySource.enabled && load.p<1e-10,e.converter.status);e.networkUnserialize(packet(41){it.writeDouble(48.0)});phase++ }
                    phase==2 && elapsed>=3.0 -> { c.check("variable DC/DC: zero target recovers without inventory reset",load.p>20,e.converter.status);e.networkUnserialize(packet(42){it.writeBoolean(false)});phase++ }
                    phase==3 && elapsed>=4.0 -> { c.check("variable DC/DC: explicit disable stops actual power",load.p<1e-10,e.converter.status);e.networkUnserialize(packet(42){it.writeBoolean(true)});phase++ }
                }
            };Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c:TestContext) {
            try {
                c.check("variable DC/DC: re-enable restores target after actual zero and disabled steps",phase==4 && load.p>20,e.converter.status)
                for(value in listOf(Double.NaN,Double.POSITIVE_INFINITY,-1.0,120001.0))e.networkUnserialize(packet(41){it.writeDouble(value)})
                e.networkUnserialize(packet(41){it.writeInt(30)});e.networkUnserialize(packet(40){it.writeInt(9)})
                c.check("variable DC/DC: invalid packets leave entire accepted command intact",e.settings.enabled && e.settings.mode=="VOLTAGE" && e.settings.value==48.0 && e.converter.voltageTarget==48.0,"")
                published(c,e)
            }finally { Eln.simulator.removeElectricalProcess(clock);listOf(feed,load).forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() } }
        }
    }
    private class InventoryAndHeat:Base() {
        lateinit var e:VariableDcDcElement
        override fun id()="forkpatch_variable_dcdc_hot_paid_winding_removal_menu_and_owner_reload"
        override fun build(c:TestContext) { e=place(c);e.inventory.setItem(3,Eln.findItemStack("Casing",1));e.inventory.setChanged() }
        override fun measure(c:TestContext) {
            val spec=MenuSpecs.of(e.descriptor);val player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
            val menu=NodeMenus.menu(1,player,e.node,-1)
            c.check("variable DC/DC: actual native server menu binds four installed hardware slots",spec.slots().size==4 && spec.slots()[0].limit()==16 && spec.slots()[1].limit()==16 && menu!=null && menu.deviceSlots==4 && menu.deviceInventory===e.inventory,"")
            c.check("variable DC/DC: native slot filters reject signal, iron and wrong hardware",spec.slots()[0].filter().test(spool(.75)) && !spec.slots()[0].filter().test(ElnDeviceSignal()) && !spec.slots()[2].filter().test(ItemStack(Items.IRON_INGOT)) && spec.slots()[2].filter().test(core()) && !spec.slots()[3].filter().test(core()),"")
            e.primaryThermal.Tc=100.0;val savedLength=wire().getRemainingLengthMeters(e.inventory.getItem(0));val oldPosition=e.node.coordonate.pos()
            e= (Reload.node(c.level(),oldPosition).fresh as TransparentNode).element as VariableDcDcElement
            c.check("variable DC/DC: real owner reload retains installed fractional metres and heat",e.populated && e.inventory.getItem(3).count==1 && abs(e.primaryThermal.Tc-100.0)<1e-4 && wire().getRemainingLengthMeters(e.inventory.getItem(0))==savedLength,"${e.primaryThermal.Tc}")
            val removed=e.inventory.removeItem(0,1);e.inventory.setChanged()
            c.check("variable DC/DC: extracting paid winding carries its heat and opens converter",!e.populated && abs(ItemNbt.get(removed)!!.getDoubleOr("windingTemperatureCelsius",0.0)-120.0)<1e-4 && wire().getRemainingLengthMeters(removed)==savedLength,"")
            e.inventory.setItem(0,removed);e.inventory.setChanged()
            c.check("variable DC/DC: reinserting hot winding cannot cool it for free",e.populated && abs(e.primaryThermal.Tc-100.0)<1e-4,"")
            // Historical device saves may have only the thermal-load key, without item temperature metadata.
            val oldSave=CompoundTag();e.writeToNBT(oldSave)
            val oldCopy=VariableDcDcElement(TransparentNode(),e.descriptor);oldCopy.front=e.front;oldCopy.readFromNBT(oldSave)
            for(i in 0..1)ItemNbt.get(oldCopy.inventory.getItem(i))?.let { tag->tag.remove("windingTemperatureCelsius");ItemNbt.set(oldCopy.inventory.getItem(i),tag) }
            oldCopy.configure()
            c.check("variable DC/DC: first configuration retains historical load heat without item metadata",abs(oldCopy.primaryThermal.Tc-100.0)<1e-4,"")
            val carried=VariableDcDcElement(TransparentNode(),e.descriptor);carried.front=e.front;carried.readItemStackNBT(e.itemStackNBT)
            c.check("variable DC/DC: carried control settings cannot duplicate paid windings or stored heat",carried.inventory.isEmpty && !carried.populated && carried.primaryThermal.Tc==0.0,"")
            published(c,e);menu?.removed(player)
        }
        fun ElnDeviceSignal()=mods.eln.registry.ElnDeviceRegistry.signalCableDescriptor.newItemStack()
    }
    private class Legacy:Base() {
        lateinit var e:VariableDcDcElement
        override fun id()="forkpatch_variable_dcdc_legacy_matched_windings_and_disabled_upgrade"
        override fun build(c:TestContext) { e=place(c);e.settings.load(CompoundTag());e.configure();e.updateControl() }
        override fun measure(c:TestContext) {
            c.check("variable DC/DC: missing saved version retains V1 strict winding equality",e.settings.version==1 && !e.populated,"")
            e.inventory.setItem(1,spool(3.25));e.inventory.setChanged()
            c.check("variable DC/DC: physically matching paid windings satisfy historical construction",e.populated,"")
            e.networkUnserialize(packet(40){it.writeInt(2)})
            c.check("variable DC/DC: V1 client cannot silently select voltage regulation",e.settings.mode=="SIGNAL","")
            e.networkUnserialize(packet(43));e.inventory.setItem(1,spool(9.5));e.inventory.setChanged()
            c.check("variable DC/DC: explicit V2 upgrade allows mixed lengths but stays disabled",e.settings.version==2 && e.settings.mode=="RATIO" && e.settings.value==1.0 && !e.settings.enabled && e.populated && !e.converter.enabled,"")
        }
    }
    private class Melt(val bare:Boolean):Base() {
        lateinit var e:VariableDcDcElement;var initial=0.0
        override fun id()="forkpatch_variable_dcdc_hot_"+if(bare)"bare_winding_becomes_paid_scrap" else "jacket_spool_melts_without_refilling"
        override fun build(c:TestContext) {
            e=place(c)
            if(!bare) { val d=UtilityCables.all.first { it.material==UtilityCableMaterial.COPPER && it.sizeLabel=="12 AWG" && it.insulated && !it.melted };e.inventory.setItem(0,d.newItemStack().also { d.setRemainingLengthMeters(it,3.25) });e.inventory.setChanged() }
            initial=e.windings[0].wire!!.amount;e.primaryThermal.Tc=wire().material.meltingPointCelsius+300.0
        }
        override fun measure(c:TestContext) {
            val stack=e.inventory.getItem(0);val melted=mods.eln.generic.GenericItemBlockUsingDamageDescriptor.getDescriptor(stack) as? UtilityCableDescriptor
            val tag=ItemNbt.get(stack)
            val failed=if(bare)stack.item===UtilityCables.scraps.parentItem else melted?.melted==true
            val metres=if(bare)tag?.getDoubleOr("wireScrapMeters",0.0) ?: 0.0 else melted?.getRemainingLengthMeters(stack) ?: 0.0
            c.check("variable DC/DC: thermal failure opens converter and leaves physical damaged stock",failed && !e.populated && !e.primarySource.enabled && !e.secondarySource.enabled,e.construction)
            c.check("variable DC/DC: deferred melting preserves paid metres and hot item data",abs(metres-initial)<1e-12 && (tag?.getDoubleOr("windingTemperatureCelsius",0.0) ?: 0.0)>1000,"")
            if(bare)c.check("variable DC/DC: melted bare copper retains its paid scrap mass",abs(tag!!.getDoubleOr("wireScrapMassKg",0.0)-WirePhysics.massKg(wire().material,wire().totalConductorAreaMm2,initial))<1e-12,"")
        }
    }
    private class WindingGuards:Base() {
        lateinit var e:VariableDcDcElement
        override fun id()="forkpatch_variable_dcdc_winding_metres_material_limits_and_invalid_stock"
        override fun build(c:TestContext) { e=place(c,filled=false) }
        override fun measure(c:TestContext) {
            val partial=spool(.375);val legacy=mods.eln.registry.ElnDeviceRegistry.lowVoltageCableDescriptor.newItemStack(7)
            c.check("variable DC/DC: spool metres and legacy cable count remain distinct",DcDcWinding.of(partial)!!.amount==.375 && DcDcWinding.of(legacy)!!.amount==7.0,"")
            val invalid=spool(1.0);invalid.count=2
            c.check("variable DC/DC: empty, nonwire, signal, stacked spools and melted metal are not windings",DcDcWinding.of(ItemStack.EMPTY)==null && DcDcWinding.of(ItemStack(Items.IRON_INGOT))==null && DcDcWinding.of(mods.eln.registry.ElnDeviceRegistry.signalCableDescriptor.newItemStack())==null && DcDcWinding.of(invalid)==null && DcDcWinding.of(UtilityCables.all.first { it.melted }.newItemStack())==null,"")
            e.inventory.setItem(0,legacy);e.inventory.setItem(1,partial);e.inventory.setItem(2,core());e.inventory.setChanged()
            c.check("variable DC/DC: different physical conductor types share native V2 core",e.populated && e.windings[0].wire!!.utility==null && e.windings[1].wire!!.utility!=null,"")
            e.primaryThermal.Tc=70.0;val split=e.inventory.removeItem(0,2);e.inventory.setChanged()
            c.check("variable DC/DC: splitting hot legacy cable retains temperature on both amounts",split.count==2 && e.inventory.getItem(0).count==5 && ItemNbt.get(split)!!.getDoubleOr("windingTemperatureCelsius",0.0)==90.0 && e.primaryThermal.Tc==70.0,"")
        }
    }
}
