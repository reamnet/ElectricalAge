package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gl.GlFrame
import mods.eln.gui.MenuSpecs
import mods.eln.misc.*
import mods.eln.node.NodeManager
import mods.eln.node.NodeMenus
import mods.eln.node.six.*
import mods.eln.sim.mna.component.*
import mods.eln.sixnode.variableinductor.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import java.io.*
import kotlin.math.abs

object ForkPatchVariableInductorCases {
    @JvmStatic fun cases():List<TestCase> = Direction.values().mapIndexed { i,side->Circuit(side,i/5.0) }+FloatingAndInvalid()+CoreAndReload()
    private open class Base:TestCase {
        override fun id()="forkpatch_variable_inductor_base"
        override fun build(c:TestContext) {}
        override fun measure(c:TestContext) {}
        fun place(c:TestContext,side:Direction=Direction.YN,core:Boolean=true):VariableInductorElement {
            val p=c.at(4,2,1);c.level().setBlock(p.relative(side.toEnumFacing()),Blocks.STONE.defaultBlockState(),2)
            return (c.placeSix(VariableInductorDescriptor.ID,p,side) as VariableInductorElement).apply {
                if(core) { inventory.setItem(0,requireNotNull(Eln.sharedItem.getDescriptor(450)).newItemStack());inventory.setChanged() }
            }
        }
        fun published(c:TestContext,e:VariableInductorElement) {
            mods.eln.client.ClientRenders.registerAll();val p=e.sixNode.coordonate.pos();val client=SixNodeEntity(p,c.level().getBlockState(p));client.setLevel(c.level())
            try {
                val input=DataInputStream(ByteArrayInputStream(e.sixNode.publishPacket.toByteArray()));input.readByte();repeat(4) { input.readInt() };input.readUTF();client.serverPublishUnserialize(input)
                val r=client.elementRenderList[e.slot] as? VariableInductorRender
                c.check("variable inductor: real publication uses its own renderer and reads all bytes",r!=null && input.available()==0,"")
                if(r!=null) {
                    c.check("variable inductor: live control and field reach the client",abs(r.inductance-e.inductor.l)<1e-12 && abs(r.normalizedControl-e.normalizedControl)<1e-12,"")
                    GlFrame.beginRenderFrame();val frame=GlFrame.open();try { r.draw();c.check("variable inductor: native source winding mesh renders",frame.batches().values.sumOf { it.vertices() }>50,"") }finally { GlFrame.close() }
                    c.check("variable inductor: signal and power stubs keep their different cable types",r.getCableRender(r.front)===mods.eln.registry.ElnDeviceRegistry.signalCableDescriptor.render && r.getCableRender(r.front.left())===mods.eln.registry.ElnDeviceRegistry.lowVoltageCableDescriptor.render,"")
                }
            }finally { client.clientDestroy() }
        }
    }
    private class Circuit(val face:Direction,val signal:Double):Base() {
        lateinit var e:VariableInductorElement;lateinit var command:VoltageSource;lateinit var source:VoltageSource;lateinit var load:Resistor
        override fun id()="forkpatch_variable_inductor_circuit_"+face.name.lowercase()
        override fun ticks()=40
        override fun build(c:TestContext) {
            e=place(c,face);command=VoltageSource("testControl",e.control,null).setU(signal*Eln.SVU)
            source=VoltageSource("testPower",e.positive,null).setU(10.0);load=Resistor(e.negative,null).setR(10.0)
            for(component in listOf(command,source,load)) { e.electricalComponentList.add(component);Eln.simulator.addElectricalComponent(component) }
        }
        override fun measure(c:TestContext) {
            c.check("variable inductor: actual signal circuit selects its source E6 value",abs(e.inductor.l-e.descriptor.inductanceFromControl(signal))<1e-10,"${e.inductor.l}, signal $signal")
            c.check("variable inductor: native solver carries a finite real RL current",e.inductor.current in .001..1.000001 && abs(e.inductor.current-load.current)<1e-7 && load.p>0,"${e.inductor.current}, ${load.current}")
            c.check("variable inductor: every face keeps two power ports and a separate signal input",e.getElectricalLoad(e.front.left())===e.negative && e.getElectricalLoad(e.front.right())===e.positive && e.getElectricalLoad(e.front)===e.control && e.getConnectionMask(e.front)==mods.eln.node.NodeBase.maskElectricalInputGate && e.getConnectionMask(e.front.inverse())==0,"")
            val system=requireNotNull(e.inductor.subSystem);val version=system.matrixVersion
            repeat(1000) { e.updateControl() }
            val matrixValid:Boolean=Batch1Cases.field(system,"matrixValid")
            c.check("variable inductor: stable quantized input preserves native LU factors",matrixValid && system.matrixVersion==version,"")
            published(c,e)
            // Snapshot a live controlled field without resetting its accepted current during restore.
            val tag=CompoundTag();e.writeToNBT(tag);val copy=VariableInductorElement(SixNode(),face,e.descriptor);copy.readFromNBT(tag);copy.initialize()
            c.check("variable inductor: saved current and inductance restore before signal reconnection",abs(copy.inductor.current-e.inductor.current)<1e-12 && copy.inductor.l==e.inductor.l,"")
            // Test-only sources are not saved devices. Remove them before the harness's owner round trip.
            for(component in listOf(command,source,load)) { Eln.simulator.removeElectricalComponent(component);component.breakConnection();e.electricalComponentList.remove(component) }
            e.updateControl()
        }
    }
    private class FloatingAndInvalid:Base() {
        lateinit var e:VariableInductorElement
        override fun id()="forkpatch_variable_inductor_floating_and_nonfinite_signal"
        override fun build(c:TestContext) { e=place(c) }
        override fun measure(c:TestContext) {
            c.check("variable inductor: unconnected input selects source full-scale inductance",e.inductor.l==100.0 && e.normalizedControl==1.0,"")
            val source=VoltageSource("testInvalid",e.control,null)
            for(value in listOf(Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY)) {
                e.control.state=value;e.updateControl();c.check("variable inductor: nonfinite control cannot crash or poison its inductance",e.inductor.l==100.0,"")
            }
            source.breakConnection();e.control.state=0.0;e.updateControl()
            c.check("variable inductor: fixed inductor retains its old identity",Eln.sixNodeItem.getDescriptor(6178)?.javaClass===mods.eln.sixnode.powerinductorsix.PowerInductorSixDescriptor::class.java,"")
        }
    }
    private class CoreAndReload:Base() {
        lateinit var e:VariableInductorElement
        override fun id()="forkpatch_variable_inductor_core_menu_and_real_owner_reload"
        override fun build(c:TestContext) { e=place(c,core=false) }
        override fun measure(c:TestContext) {
            c.check("variable inductor: absent core leaves high-impedance terminals",e.positive.rs>=1e8 && e.negative.rs>=1e8,"")
            val spec=MenuSpecs.of(e.descriptor);val core=requireNotNull(Eln.sharedItem.getDescriptor(450)).newItemStack()
            c.check("variable inductor: native menu has exactly one real core slot",spec.slots().size==1 && spec.slots()[0].limit()==1 && spec.slots()[0].filter().test(core) && !spec.slots()[0].filter().test(ItemStack(Items.IRON_INGOT)),"")
            val player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());val menu=NodeMenus.menu(1,player,e.sixNode,e.slot)
            c.check("variable inductor: server menu uses the actual device inventory",menu!=null && menu.deviceInventory===e.inventory && menu.deviceSlots==1,"")
            e.inventory.setItem(0,ItemStack(Items.IRON_INGOT));e.inventory.setChanged()
            c.check("variable inductor: invalid saved core is safely nonconducting",e.positive.rs>=1e8,"")
            requireNotNull(menu).slots[0].setByPlayer(core)
            c.check("variable inductor: core insertion applies real winding resistance",e.positive.rs>0 && e.positive.rs<1 && e.negative.rs==e.positive.rs,"")
            e.inductor.currentState.state=.75
            val p=e.sixNode.coordonate.pos();val slot=e.slot;val restored=Reload.node(c.level(),p).fresh as SixNode;e=restored.getElement(slot) as VariableInductorElement
            c.check("variable inductor: real owner reload preserves core and magnetic current",e.inventory.getItem(0).count==1 && e.inductor.current==.75 && e.inductor.l==100.0,"")
            published(c,e)
            requireNotNull(menu).removed(player)
            e.inventory.setItem(0,ItemStack.EMPTY);e.inventory.setChanged()
            c.check("variable inductor: removing the core opens terminals and clears the field once",e.positive.rs>=1e8 && e.inductor.current==0.0,"")
        }
    }
}
