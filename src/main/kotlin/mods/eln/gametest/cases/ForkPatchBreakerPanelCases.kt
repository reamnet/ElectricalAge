package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.TestCase
import mods.eln.gametest.TestContext
import mods.eln.gridnode.GridElement
import mods.eln.misc.Direction
import mods.eln.node.transparent.TransparentNodeItem
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.utility.BreakerPanelDescriptor
import mods.eln.utility.BreakerPanelElement
import mods.eln.utility.PanelBreaker
import net.minecraft.world.level.block.Blocks
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

object ForkPatchBreakerPanelCases {
    @JvmStatic fun cases():List<TestCase> = listOf(RatedHardware(),PhysicalBranches(),ThermalLatch(),WirelessConsumer(),PanelPublication(),RatedRelayControl(),WiredBranch(false),WiredBranch(true),NativeTrip())
    private val amps=intArrayOf(5,10,15,20,30,40,50,60,100,125,200,400)
    private val ids=intArrayOf(6288,6273,6274,6275,6279,6280,6281,6282,6283,6285,6286,6287)
    private open class Base:TestCase {
        override fun id()="forkpatch_panel_base"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c:TestContext) {}
        override fun measure(c:TestContext) {}
        fun placed(c:TestContext):BreakerPanelElement {
            val at=c.at(6,2,6);c.level().setBlockAndUpdate(at.relative(net.minecraft.core.Direction.EAST),Blocks.STONE.defaultBlockState())
            val d=requireNotNull(Eln.transparentNodeItem.getDescriptor((124 shl 6)+52)) as BreakerPanelDescriptor
            val player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
            return ((d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),player,c.level(),at,Direction.XP)?:error("panel placement failed")).element as BreakerPanelElement
        }
    }
    private class RatedHardware:Base() {
        override fun id()="forkpatch_panel_source_rated_breaker_items"
        override fun build(c:TestContext) {}
        override fun measure(c:TestContext) {
            val generic=Eln.sixNodeItem.getDescriptor(6272) as mods.eln.sixnode.electricalbreaker.ElectricalBreakerDescriptor
            c.check("panel: legacy generic breaker remains infinite rated",generic.currentLimit==Double.POSITIVE_INFINITY,"")
            c.check("panel: twelve source breaker current ratings and IDs are preserved",ids.indices.all { i->(Eln.sixNodeItem.getDescriptor(ids[i]) as? mods.eln.sixnode.electricalbreaker.ElectricalBreakerDescriptor)?.currentLimit==amps[i].toDouble() },"")
        }
    }
    private class PhysicalBranches:Base() {
        lateinit var panel:BreakerPanelElement
        private val components=ArrayList<Component>()
        override fun id()="forkpatch_panel_real_120v_and_240v_branches"
        override fun ticks()=60
        override fun build(c:TestContext) {
            panel=placed(c)
            panel.getInventory()!!.setItem(0,Eln.findItemStack("10A Electrical Breaker",1))
            panel.getInventory()!!.setItem(1,Eln.findItemStack("20A Electrical Breaker",1))
            components+=VoltageSource("panel L1 feed",panel.buses[0],null).setU(120.0)
            components+=VoltageSource("panel L2 feed",panel.buses[1],null).setU(120.0)
            components+=VoltageSource("panel 240V feed",panel.buses[2],null).setU(240.0)
            components+=Resistor(panel.branches[0].load,null).setR(120.0)
            components+=Resistor(panel.branches[1].load,null).setR(120.0)
            components.forEach(Eln.simulator::addElectricalComponent)
        }
        override fun measure(c:TestContext) {
            try {
                c.check("panel: all twelve independent branch contacts exist",panel.branches.size==12 && panel.branches.all { it.contacts.size==3 },"")
                val conductance=1.0/120.0+1e-10+1e-19
                val expectedVoltage=120.0/(1.0+.002*conductance)
                val expectedCurrent=expectedVoltage*conductance
                c.check("panel: installed breaker gates actual L1 load including contact loss",panel.branches[0].contact.closed && kotlin.math.abs(panel.branches[0].load.u-expectedVoltage)<1e-8 && kotlin.math.abs(panel.branches[0].contact.current-expectedCurrent)<1e-8,"U=${panel.branches[0].load.u}, I=${panel.branches[0].contact.current}")
                val packet=ByteArrayOutputStream();DataOutputStream(packet).use { it.writeByte(84);it.writeInt(0) };panel.networkUnserialize(DataInputStream(packet.toByteArray().inputStream()))
                c.check("panel: selecting 240 V routes the same protected branch onto the service bus",panel.branches[0].bus==2 && panel.branches[0].contact.closed,"bus=${panel.branches[0].bus}")
                c.check("panel: empty branches remain isolated",panel.branches.drop(2).all { it.contacts.none { c->c.closed } },"")
            } finally { components.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() } }
        }
    }
    private class ThermalLatch:Base() {
        override fun id()="forkpatch_panel_thermal_trip_latch_and_save"
        override fun build(c:TestContext) {}
        override fun measure(c:TestContext) {
            val b=PanelBreaker();var tripped=false;repeat(70){tripped=b.step(1.0,40.0,20.0)||tripped}
            c.check("panel: sustained physical overcurrent trips and latches",tripped && b.tripped && b.enabled,"")
            b.toggle();c.check("panel: manual operation resets a trip explicitly",!b.tripped && b.enabled,"")
            val saved=PanelBreaker();repeat(70){saved.step(1.0,40.0,20.0)}
            val tag=net.minecraft.nbt.CompoundTag();saved.write(tag,"panel0");val restored=PanelBreaker();restored.read(tag,"panel0")
            c.check("panel: source trip and heat fields survive save",restored.tripped && restored.enabled && restored.heat==0.0,"")
        }
    }
    private class WirelessConsumer:Base() {
        lateinit var panel:BreakerPanelElement
        lateinit var lamp:mods.eln.sixnode.lampsocket.DecorativeLampElement
        lateinit var source:VoltageSource
        val channel="panel wireless "+java.util.UUID.randomUUID()
        override fun id()="forkpatch_panel_wireless_native_lamp_demand_and_disconnect"
        override fun ticks()=100
        override fun build(c:TestContext) {
            panel=placed(c);panel.getInventory().setItem(0,Eln.findItemStack("10A Electrical Breaker",1))
            panel.networkUnserialize(c.stream {it.writeByte(81);it.writeUTF("0\n$channel")})
            val pos=c.at(10,1,6);c.floor(pos.below())
            lamp=c.placeSix(4118,pos) as mods.eln.sixnode.lampsocket.DecorativeLampElement
            lamp.getInventory().setItem(0,Eln.findItemStack("120V LED Light Bulb",1));lamp.inventoryChange(lamp.getInventory())
            lamp.networkUnserialize(c.stream {it.writeByte(4);it.writeUTF(channel)})
            source=VoltageSource("panel wireless paid source",panel.buses[0],null).setU(120.0);Eln.simulator.addElectricalComponent(source)
        }
        override fun measure(c:TestContext) {try {
            val branch=panel.branches[0]
            val input=kotlin.math.abs(source.u*source.current)
            c.check("panel wireless: native fixture receives selected branch voltage",lamp.isWirelessPower && lamp.positiveLoad.u>119.9 && kotlin.math.abs(lamp.positiveLoad.u-branch.load.u)<1e-8,"lamp=${lamp.positiveLoad.u},bus=${branch.load.u}")
            c.check("panel wireless: one real upstream source pays the LED demand once",input>14.9 && input<15.01,"$input W")
            val resistance=lamp.wirelessResistance
            repeat(100) { mods.eln.utility.WirelessLoadDemand.report(lamp,branch.handle,resistance) }
            branch.updateWirelessLoad()
            c.check("panel wireless: repeated reports replace one identity demand",kotlin.math.abs(branch.wirelessDemand.r-resistance)<1e-9,"${branch.wirelessDemand.r} / $resistance")
            lamp.sixNode.disconnect();branch.updateWirelessLoad()
            c.check("panel wireless: consumer suspension removes persistent demand immediately",branch.wirelessDemand.r==1e19,"${branch.wirelessDemand.r}")
            lamp.sixNode.connect();panel.node.disconnect()
            c.check("panel wireless: supply suspension unregisters its handles immediately",mods.eln.utility.WirelessPowerChannels.nearest(channel,lamp.sixNode.coordonate)==null,"")
        }finally {Eln.simulator.removeElectricalComponent(source);source.breakConnection()} }
    }
    private class PanelPublication:Base() {
        override fun id()="forkpatch_panel_publication_slots_and_source_settings"
        override fun build(c:TestContext) {
            val p=placed(c)
            repeat(12) {i->p.getInventory().setItem(i,Eln.findItemStack("${amps[i]}A Electrical Breaker",1))}
            p.networkUnserialize(c.stream {it.writeByte(86);it.writeInt(512)})
            p.networkUnserialize(c.stream {it.writeByte(80);it.writeInt(11)})
            p.networkUnserialize(c.stream {it.writeByte(84);it.writeInt(11)})
            val spec=mods.eln.gui.MenuSpecs.of(p.descriptor)
            c.check("panel menu: all twelve source slots remain accessible beside controls",spec.slots().size==12 && spec.slots().withIndex().all {(i,slot)->slot.index()==i && slot.x()==8+(i%2)*152 && slot.y()==34+(i/2)*20},"${spec.slots()}")
            c.check("panel menu: source panel and inventory dimensions match",spec.width()==312 && spec.height()==386 && spec.inventoryX()==8 && spec.inventoryY()==304,"")
            mods.eln.client.ClientRenders.registerAll()
            val at=p.node.coordonate.pos();val tile=mods.eln.node.transparent.TransparentNodeEntity(at,c.level().getBlockState(at));tile.setLevel(c.level())
            try {
                val data=DataInputStream(p.node.publishPacket.toByteArray().inputStream());data.readByte();repeat(4){data.readInt()};data.readUTF();tile.serverPublishUnserialize(data)
                val render=tile.elementRender as? mods.eln.utility.BreakerPanelRender
                c.check("panel publication: actual native factory consumes controls, meters and cable mask",render!=null && data.available()==0 && render.selected()==11 && render.range==512 && render.branches[11].use240 && render.branches.map{it.rating}==amps.map{it.toDouble()},"remaining=${data.available()}")
            }finally{tile.clientDestroy()}
            val settings=p.itemStackNBT
            c.check("panel save: source branch keys and selected range survive",settings.getBooleanOr("panel11240",false) && settings.getIntOr("panelRange",0)==512 && settings.contains("panel11Enabled"),"$settings")
            val restored=BreakerPanelElement(mods.eln.node.transparent.TransparentNode(),p.descriptor)
            restored.readItemStackNBT(settings)
            c.check("panel settings-only items never duplicate paid breaker inventory",restored.getInventory().isEmpty && restored.selected==11 && restored.transmissionRange==512,"")
        }
    }

    private class RatedRelayControl:Base() {
        private val relays=ArrayList<mods.eln.sixnode.electricalrelay.ElectricalRelayElement>()
        private val parts=ArrayList<Component>()
        override fun id()="forkpatch_panel_rated_recipe_relays_real_control_voltage_and_contact_loss"
        override fun build(c:TestContext) {
            repeat(4) {i->
                val at=c.at(3+i*4,1,3);c.floor(at.below())
                val relay=c.placeSix(6021+i,at) as mods.eln.sixnode.electricalrelay.ElectricalRelayElement
                relays+=relay
                parts+=VoltageSource("rated relay contact input",relay.aLoad,null).setU(120.0)
                parts+=VoltageSource("rated relay coil input",relay.gate,null).setU(if(i==0)5.0 else 12.0)
                parts+=Resistor(relay.bLoad,null).setR(120.0)
            }
            parts.forEach(Eln.simulator::addElectricalComponent)
        }
        override fun measure(c:TestContext) {try {
            relays.forEachIndexed {i,r->
                val contact=listOf(.04,.01,.005,.0025)[i]
                val expected=120.0*120/(120+contact)
                c.check("rated relay $i: actual source coil voltage closes a paid lossy contact",r.switchState && kotlin.math.abs(r.bLoad.u-expected)<1e-7,"U=${r.bLoad.u},expected=$expected")
                c.check("rated relay $i: source contact and coil ratings remain distinct",r.descriptor.currentRating==listOf(10.0,20.0,50.0,100.0)[i] && r.descriptor.contactNominalVoltage==(if(i==0)120.0 else 600.0) && r.descriptor.controlVoltage==(if(i==0)5.0 else 12.0),"")
                r.gate.state=r.descriptor.controlVoltage*.2;r.gateProcess.process(.05)
                c.check("rated relay $i: source low threshold opens the contact",!r.switchState && r.switchResistor.r>1e10,"")
            }
        }finally{parts.forEach{Eln.simulator.removeElectricalComponent(it);it.breakConnection()}}}
    }

    private class WiredBranch(val use240:Boolean):Base() {
        lateinit var panel:BreakerPanelElement
        lateinit var source:VoltageSource
        lateinit var load:Resistor
        override fun id()="forkpatch_panel_placed_240v_feeder_to_${if(use240)240 else 120}v_branch"
        override fun ticks()=80
        override fun build(c:TestContext) {
            panel=placed(c);panel.getInventory().setItem(0,Eln.findItemStack("10A Electrical Breaker",1))
            if(use240)panel.networkUnserialize(c.stream{it.writeByte(84);it.writeInt(0)})
            val at=panel.node.coordonate.pos();val mount=panel.front.inverse
            fun wire(pos:net.minecraft.core.BlockPos):mods.eln.sim.ElectricalLoad {
                c.floor(pos.relative(mount.toEnumFacing()))
                return c.placeSix(mods.eln.registry.ElnDeviceRegistry.highVoltageCableDescriptor.parentItemDamage,pos,mount).getElectricalLoad(mods.eln.misc.LRDU.Up)
            }
            val input=wire(at.above());val output=wire(at.below())
            source=VoltageSource("placed panel feeder",input,null).setU(240.0)
            load=Resistor(output,null).setR(if(use240)240.0 else 120.0)
            Eln.simulator.addElectricalComponent(source);Eln.simulator.addElectricalComponent(load)
        }
        override fun measure(c:TestContext) {try{
            val volts=if(use240)240.0 else 120.0
            c.check("panel wall feeder: real240V cable supplies the selected protected branch",load.u>volts*.99 && load.u<=volts && panel.branches[0].contact.closed,"U=${load.u},bus=${panel.buses.map{it.u}}")
            val paid=kotlin.math.abs(source.u*source.current)
            c.check("panel wall feeder: upstream source pays delivered load and passive losses",load.p>volts*.98 && paid>=load.p-1e-7 && paid<load.p+2.0,"input=$paid,load=${load.p}")
        }finally{Eln.simulator.removeElectricalComponent(source);source.breakConnection();Eln.simulator.removeElectricalComponent(load);load.breakConnection()}}
    }
    private class NativeTrip:Base() {
        lateinit var panel:BreakerPanelElement
        lateinit var source:VoltageSource
        lateinit var load:Resistor
        override fun id()="forkpatch_panel_native_overload_opens_and_persists_latch"
        override fun ticks()=80
        override fun build(c:TestContext) {
            panel=placed(c);panel.getInventory().setItem(0,Eln.findItemStack("10A Electrical Breaker",1))
            source=VoltageSource("panel physical overload",panel.buses[0],null).setU(120.0)
            load=Resistor(panel.branches[0].load,null).setR(.1)
            Eln.simulator.addElectricalComponent(source);Eln.simulator.addElectricalComponent(load)
        }
        override fun measure(c:TestContext) {try{
            val branch=panel.branches[0]
            c.check("panel overload: real accepted current opens the branch and clears load voltage",branch.breaker.tripped && !branch.contact.closed && kotlin.math.abs(load.u)<1e-6,"trip=${branch.breaker.tripped},U=${load.u}")
            c.check("panel overload: SCADA cannot clear the trip latch",!panel.remoteCircuit(0,true) && branch.breaker.tripped,"")
            val tag=net.minecraft.nbt.CompoundTag();panel.writeToNBT(tag)
            val saved=BreakerPanelElement(mods.eln.node.transparent.TransparentNode(),panel.descriptor);saved.readFromNBT(tag)
            c.check("panel overload: paid hardware and real trip survive full save",saved.branches[0].breaker.tripped && !saved.branches[0].contact.closed && saved.branches[0].rating==10.0,"")
        }finally{Eln.simulator.removeElectricalComponent(source);source.breakConnection();Eln.simulator.removeElectricalComponent(load);load.breakConnection()}}
    }

}
