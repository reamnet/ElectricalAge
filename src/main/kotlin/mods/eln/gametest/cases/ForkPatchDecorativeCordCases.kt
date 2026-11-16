package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.misc.Direction
import mods.eln.node.transparent.*
import mods.eln.node.six.SixNode
import mods.eln.sixnode.lampsocket.*
import mods.eln.sixnode.lampsupply.LampSupplyElement
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.mna.component.*
import mods.eln.transparentnode.cord.*
import mods.eln.transparentnode.festive.StringLightsElement
import net.minecraft.nbt.CompoundTag
import java.util.UUID
import kotlin.math.abs

/** Real paid native outlets/cords; shared wireless channels must not double-bill physical fixtures. */
object ForkPatchDecorativeCordCases {
    @JvmStatic fun cases(): List<TestCase> = listOf(StringRating(120),StringRating(240),PhysicalFixtures())
    private open class Base:TestCase {
        val parts=mutableListOf<Component>()
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {}
        fun feed(input: ElectricalLoad,volts: Int)=VoltageSource("fixture cord paid source",input,null).setU(volts.toDouble()).also {parts+=it;Eln.simulator.addElectricalComponent(it)}
        fun clean() {parts.forEach {Eln.simulator.removeElectricalComponent(it);it.breakConnection()};parts.clear()}
        fun cord(owner: Any)=requireNotNull(CordSupport.device(owner)).cord
        fun outlet(c: TestContext,volts: Int): OutletElement {
            val pos=c.at(4,1,4);c.floor(pos.north())
            val d=requireNotNull(Eln.transparentNodeItem.getDescriptor(if(volts==120)7968 else 7969))
            return requireNotNull((d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level()),c.level(),pos,Direction.ZN)).element as OutletElement
        }
        fun strings(c: TestContext,x: Int=8,z: Int=8): StringLightsElement {
            val pos=c.at(x,2,z);c.floor(pos.north());val d=requireNotNull(Eln.transparentNodeItem.getDescriptor(4418))
            return requireNotNull((d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level()),c.level(),pos,Direction.ZN)).element as StringLightsElement
        }
    }
    private class StringRating(val volts: Int):Base() {
        private lateinit var lamp: StringLightsElement
        private lateinit var extra: StringLightsElement
        private lateinit var outlet: OutletElement
        private lateinit var source: VoltageSource
        override fun id()="forkpatch_string_lights_${volts}v_cord_auto_rating_payment_and_ownership"
        override fun build(c: TestContext) {
            outlet=outlet(c,volts);lamp=strings(c);extra=strings(c,8,6)
            lamp.networkUnserialize(c.stream {it.writeByte(80);it.writeInt(if(volts==120)240 else 120)})
            val lead=cord(lamp)
            c.check("String cord accepts opposite selected household source rating",lead.attach(outlet.anchor(0),true) && lead.connected && lead.paid,lead.state)
            c.check("String cord source rating configures actual15W resistance on attachment",lamp.nominalVolts==volts && abs(lamp.loadResistor.r-volts.toDouble()*volts/15)<1e-9,"${lamp.nominalVolts},R=${lamp.loadResistor.r}")
            lamp.networkUnserialize(c.stream {it.writeByte(80);it.writeInt(if(volts==120)240 else 120)})
            c.check("String cord attached voltage control cannot override paid source rating",lamp.nominalVolts==volts,"")
            c.check("String cord cannot steal occupied socket or acquire unpaid duplicate",!cord(extra).attach(outlet.anchor(0),true) && !cord(extra).paid && outlet.occupied(0),"")
            source=feed(outlet.input,volts)
        }
        override fun measure(c: TestContext) {try {
            val lead=cord(lamp);val losses=outlet.contact.p+lead.conductor.p
            c.check("String cord real native source pays lamp and contact/lead losses",lamp.loadResistor.p>14.9 && lamp.loadResistor.p<=15.00001 && abs(abs(source.u*source.current)-lamp.loadResistor.p-losses)<1e-4,"source=${source.u*source.current},lamp=${lamp.loadResistor.p},loss=$losses")
            c.check("String physical cord produces actual light without wireless link",lamp.lightValue>=14 && !lamp.wirelessConnected,"")
            val saved=CompoundTag();lamp.writeToNBT(saved)
            val copyNode=TransparentNode().apply {coordonate=mods.eln.misc.Coordonate(lamp.node.coordonate)}
            val copy=StringLightsElement(copyNode,lamp.descriptor);copy.readFromNBT(saved)
            val restored=cord(copy)
            c.check("String world reload preserves exact paid anchor and reservation token",restored.anchor==lead.anchor && restored.token==lead.token && restored.paid && !restored.connected,"${restored.state}")
            restored.poll()
            c.check("String detached saved owner cannot steal live cord connection",!restored.connected && outlet.occupied(0) && lead.connected,"")
            val cordItem=Eln.findItemStack(outlet.rating.cordName,1).item
            fun refunded()=c.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java,net.minecraft.world.phys.AABB(lamp.node.coordonate.pos()).inflate(3.0)).filter {it.item.item===cordItem}.sumOf {it.item.count}
            val refundsBefore=refunded()
            lead.detach()
            c.check("String paid cord detachment refunds exactly one physical cord",refunded()==refundsBefore+1,"before=$refundsBefore,after=${refunded()}")
            c.check("String cord detach releases socket and pays no duplicate token",!lead.plugged && !lead.connected && !lead.paid && !outlet.occupied(0),"")
            lead.detach()
            c.check("String repeated detach keeps paid ownership cleared and cannot mint another cord",!lead.paid && !outlet.occupied(0) && refunded()==refundsBefore+1,"")
        } finally {clean()} }
    }
    private class PhysicalFixtures:Base() {
        private lateinit var fan: DecorativeLampElement
        private lateinit var table: DecorativeLampElement
        private lateinit var outlet: OutletElement
        private lateinit var source: VoltageSource
        private lateinit var wirelessSource: VoltageSource
        override fun id()="forkpatch_decorative_fan_table_cords_pay_motor_bulbs_without_wireless_double_billing"
        override fun ticks()=120
        override fun build(c: TestContext) {
            outlet=outlet(c,120)
            val pos=c.at(8,6,6);c.floor(pos.above())
            fan=c.placeSix(DecorativeLampStyle.FAN_CLASSIC.lookup,pos,Direction.YP) as DecorativeLampElement
            table=c.placeSix(DecorativeLampStyle.TABLE_DRUM.lookup,c.at(8,1,8)) as DecorativeLampElement
            val channel="corded fixtures ${UUID.randomUUID()}"
            for(e in listOf(fan,table)) {
                e.inventory.setItem(0,Eln.findItemStack("120V LED Light Bulb",1));e.inventoryChange(e.inventory)
                e.networkUnserialize(c.stream {it.writeByte(4);it.writeUTF(channel)})
            }
            c.check("fan unconnected wireless mode has no physical motor lead",fan.isWirelessPower && !fan.hasPowerLead && fan.fanMotor.r.isInfinite(),"")
            c.check("fan real paid cord attaches and configures physical motor",cord(fan).attach(outlet.anchor(0),true) && fan.hasPowerLead && !fan.isWirelessPower && abs(fan.fanMotor.r-120.0*120/55)<1e-9,"")
            c.check("table real paid cord independently reserves second socket",cord(table).attach(outlet.anchor(1),true) && cord(table).connected,"")
            val supply=c.placeSix(4162,c.at(10,1,6)) as LampSupplyElement
            supply.networkUnserialize(c.stream {it.writeByte(LampSupplyElement.setPowerName.toInt());it.writeByte(0);it.writeUTF(channel)})
            source=feed(outlet.input,120);wirelessSource=feed(supply.powerLoad,120)
        }
        override fun measure(c: TestContext) {try {
            val leads=listOf(cord(fan),cord(table))
            val lamps=fan.lampResistor.p+table.lampResistor.p
            val motor=fan.fanMotor.p
            val losses=outlet.contact.p+leads.sumOf {it.conductor.p}
            c.check("physical decorative source pays both lamps motor and all real connector losses",lamps>29.8 && motor>54.5 && abs(abs(source.u*source.current)-lamps-motor-losses)<1e-4,"source=${source.u*source.current},lamps=$lamps,motor=$motor,losses=$losses")
            c.check("physical decorative cords suppress same-channel wireless billing",abs(wirelessSource.u*wirelessSource.current)<1e-6,"wireless=${wirelessSource.u*wirelessSource.current}")
            c.check("physical decorative fan genuinely spins while table emits light",fan.fanPower>.9 && table.lightValue>=14,"")
            val saved=CompoundTag();fan.writeToNBT(saved)
            val node=SixNode().apply {coordonate=mods.eln.misc.Coordonate(fan.sixNode.coordonate)}
            val copy=DecorativeLampElement(node,fan.side,fan.sixNodeElementDescriptor);copy.readFromNBT(saved)
            c.check("decorative saved cord retains paid token without creating a duplicate live lead",cord(copy).anchor==cord(fan).anchor && cord(copy).token==cord(fan).token && cord(copy).paid && !cord(copy).connected,"")
            cord(fan).detach()
            c.check("fan cord removal immediately returns motor to wireless-disabled demand",fan.isWirelessPower && !fan.hasPowerLead && fan.fanMotor.r.isInfinite() && fan.fanMotor.aPin==null && !outlet.occupied(0),"")
        } finally {clean()} }
    }
}
