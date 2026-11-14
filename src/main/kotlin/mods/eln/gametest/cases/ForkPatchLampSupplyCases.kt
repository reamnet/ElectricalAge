package mods.eln.gametest.cases

import com.mojang.authlib.GameProfile
import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.ItemNbt
import mods.eln.gui.MenuSpecs
import mods.eln.misc.*
import mods.eln.node.*
import mods.eln.node.six.*
import mods.eln.node.transparent.TransparentNodeItem
import mods.eln.node.voxel.CableConverter
import mods.eln.registry.ElnDeviceRegistry
import mods.eln.sixnode.electricalcable.ElectricalCableElement
import mods.eln.sixnode.lampsupply.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.mna.component.*
import mods.eln.transparentnode.cord.*
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.*
import java.util.UUID
import kotlin.math.abs

/** Rated source supplies retain the native three-channel owner and its passive electrical paths. */
object ForkPatchLampSupplyCases {
    private val ratings=listOf(CordRating.V120,CordRating.V240)
    @JvmStatic fun cases(): List<TestCase> = listOf(Identity(),Recipes()) + listOf(4160,4161,4162).map { Controls(it) } + ratings.flatMap { rating ->
        listOf(Inlet(rating,false),Inlet(rating,true),SavedOwner(rating),PaidUse(rating)) + (0..2).map { Wireless(rating,it) }
    }
    private open class Base: TestCase {
        val parts=mutableListOf<Component>()
        override fun id()="forkpatch_lamp_supply_base"
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {}
        fun item(name: String,n: Int=1)=Eln.findItemStack(name,n)
        fun supply(c: TestContext,rating: CordRating): LampSupplyElement = c.placeSix(
            if(rating==CordRating.V120)LampSupplyDescriptor.RATED_120_ID else LampSupplyDescriptor.RATED_240_ID,c.at(4,1,1)) as LampSupplyElement
        fun outlet(c: TestContext,rating: CordRating,x: Int=1,z: Int=1): OutletElement {
            val pos=c.at(x,1,z);c.floor(pos.north())
            val d=requireNotNull(Eln.transparentNodeItem.getDescriptor(if(rating==CordRating.V120)7968 else 7969))
            return requireNotNull((d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),
                net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level()),c.level(),pos,Direction.ZN)).element as OutletElement
        }
        fun cord(e: LampSupplyElement)=requireNotNull(CordSupport.device(e)).cord
        fun feed(load: ElectricalLoad,volts: Double)=VoltageSource("rated lamp supply source",load,null).setU(volts).also { parts.add(it);Eln.simulator.addElectricalComponent(it) }
        fun load(load: ElectricalLoad,ohms: Double)=Resistor(load,null).setR(ohms).also { parts.add(it);Eln.simulator.addElectricalComponent(it) }
        fun clean() { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        fun powerChannel(c: TestContext,e: LampSupplyElement,index: Int,name: String) { e.networkUnserialize(c.stream { it.writeByte(LampSupplyElement.setPowerName.toInt());it.writeByte(index);it.writeUTF(name) }) }
        fun player(c: TestContext)=TransportTestPlayer(c.level(),GameProfile(UUID.nameUUIDFromBytes(id().toByteArray()),"ratedSupply")).also { it.setGameMode(GameType.SURVIVAL);it.setPos(c.at(3,1,1).x+.5,c.at(3,1,1).y.toDouble(),c.at(3,1,1).z+.5) }
    }
    private class Identity: Base() {
        override fun id()="forkpatch_lamp_supply_native_identity_and_source_conflict"
        override fun measure(c: TestContext) {
            val legacy=c.placeSix(4160,c.at(1,1,1)) as LampSupplyElement
            c.check("rated supply: established native identity remains generic and has no cord input",legacy.descriptor.name=="Lamp Supply" && legacy.descriptor.nominalVoltage==0.0 && CordSupport.device(legacy)==null && item("Lamp Supply").item===legacy.descriptor.parentItem,"")
            c.check("rated supply: source lookup helper makes the 120 V conflict explicit",LampSupplyDescriptor.destinationIdForSource(4160)==4162 && LampSupplyDescriptor.destinationIdForSource(4161)==4161 && LampSupplyDescriptor.destinationIdForSource(0)==null,"")
            for(rating in ratings) {
                val d=Eln.sixNodeItem.getDescriptor(if(rating==CordRating.V120)4162 else 4161) as LampSupplyDescriptor
                c.check("rated supply: distinct items retain source voltage and native menu inventory",d.nominalVoltage==rating.volts && d.channelCount==3 && d.range==32 && MenuSpecs.of(d).slots().size==1 && item("${rating.volts.toInt()}V Lamp Supply").item===d.parentItem && d.parentItem!==legacy.descriptor.parentItem,"")
            }
        }
    }
    private class Recipes: Base() {
        override fun id()="forkpatch_lamp_supply_paid_recipes_preserve_native_generic_output"
        fun craft(c: TestContext,rows: List<String>,key: Map<Char,ItemStack>): ItemStack {
            val input=CraftingInput.of(rows[0].length,rows.size,rows.flatMap { row->row.map { key[it]?.copy() ?: ItemStack.EMPTY } })
            return c.level().recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,c.level()).map { it.value().assemble(input) }.orElse(ItemStack.EMPTY)
        }
        override fun measure(c: TestContext) {
            val metals=mapOf('I' to ItemStack(Items.IRON_INGOT),'C' to ItemStack(Items.COPPER_INGOT))
            val generic=craft(c,listOf(" I ","ICI"," I "),metals)
            val rated=craft(c,listOf("S"),mapOf('S' to generic))
            val high=craft(c,listOf(" I ","ACA"," I "),metals+('A' to item("Advanced Machine Block")))
            c.check("rated supply recipe: original paid native pattern still crafts the original identity",generic.item===item("Lamp Supply").item && generic.count==1,generic.toString())
            c.check("rated supply recipe: 120 V conversion consumes the already paid native enclosure",rated.item===item("120V Lamp Supply").item && rated.count==1,rated.toString())
            c.check("rated supply recipe: 240 V retains both source advanced machine blocks",high.item===item("240V Lamp Supply").item && high.count==1,high.toString())
            val wrong=craft(c,listOf(" I ","ACA"," I "),metals+('A' to item("Machine Block")))
            c.check("rated supply recipe: basic blocks cannot substitute for advanced hardware",wrong.item!==item("240V Lamp Supply").item,wrong.toString())
        }
    }
    private class Controls(val lookup: Int): Base() {
        override fun id()="forkpatch_lamp_supply_${lookup}_bounded_controls_and_saved_channels"
        override fun measure(c: TestContext) {
            val e=c.placeSix(lookup,c.at(4,1,1)) as LampSupplyElement
            val channel="bounded ${UUID.randomUUID()}";powerChannel(c,e,0,channel)
            fun save()=CompoundTag().also(e::writeToNBT)
            val before=save()
            val wireless="signal ${UUID.randomUUID()}"
            e.networkUnserialize(c.stream { it.writeByte(LampSupplyElement.setWirelessName.toInt());it.writeByte(0);it.writeUTF(wireless) })
            c.check("rated supply decoder: wireless controls never register an extra power channel",LampSupplyElement.channelMap[wireless]?.none { it.element===e }!=false && LampSupplyElement.channelMap[channel]?.count { it.element===e && it.id==0 }==1,"")
            e.readFromNBT(before.copy())
            // Clean up an inherited failure so this reproduction cannot influence another case.
            LampSupplyElement.channelMap[wireless]?.removeIf { it.element===e }
            for(command in listOf(LampSupplyElement.setPowerName,LampSupplyElement.setWirelessName,LampSupplyElement.setSelectedAggregator)) {
                for(index in listOf(-128,-1,3,127)) {
                    var threw=false
                    try { e.networkUnserialize(c.stream { it.writeByte(command.toInt());it.writeByte(index);if(command==LampSupplyElement.setSelectedAggregator)it.writeByte(1) else it.writeUTF("rejected") }) }catch(_: RuntimeException) { threw=true }
                    c.check("rated supply decoder: invalid channel is rejected without mutation or exception",!threw && save()==before,"command=$command index=$index")
                    e.readFromNBT(before.copy())
                }
            }
            for(mode in listOf(-128,-1,3,127)) {
                e.networkUnserialize(c.stream { it.writeByte(LampSupplyElement.setSelectedAggregator.toInt());it.writeByte(0);it.writeByte(mode) })
                c.check("rated supply decoder: invalid aggregator is rejected without changing native state",save()==before,"mode=$mode")
                e.readFromNBT(before.copy())
            }
            for(command in listOf(LampSupplyElement.setPowerName,LampSupplyElement.setWirelessName,LampSupplyElement.setSelectedAggregator)) {
                for(bytes in listOf(byteArrayOf(command),byteArrayOf(command,0))) {
                    e.networkUnserialize(java.io.DataInputStream(java.io.ByteArrayInputStream(bytes)))
                    c.check("rated supply decoder: truncated controls cannot partially change native state",save()==before,"command=$command bytes=${bytes.size}")
                }
            }
            val bad=before.copy().apply { putString("entry_p3","unowned fourth channel");putInt("selectedAggregator0",127) }
            var threw=false
            try { e.readFromNBT(bad) }catch(_: RuntimeException) { threw=true }
            val restored=save()
            c.check("rated supply decoder: extra saved channels are ignored and invalid aggregators recover safely",!threw && restored.getIntOr("selectedAggregator0",-1)==2 && !restored.contains("entry_p3") && (0..2).all { restored.getIntOr("selectedAggregator$it",-1) in 0..2 },"")
            e.readFromNBT(before.copy())
            c.check("rated supply decoder: repeated reads leave exactly one native channel registration",LampSupplyElement.channelMap[channel]?.count { it.element===e && it.id==0 }==1,"")
            for(index in 0..2) {
                val name="valid $index ${UUID.randomUUID()}";powerChannel(c,e,index,name)
                for(mode in 0..2) {
                    e.networkUnserialize(c.stream { it.writeByte(LampSupplyElement.setSelectedAggregator.toInt());it.writeByte(index);it.writeByte(mode) })
                    val valid=save();e.readFromNBT(valid.copy())
                    c.check("rated supply decoder: every valid aggregator and channel survives an exact native round trip",save()==valid && LampSupplyElement.channelMap[name]?.count { it.element===e && it.id==index }==1,"channel=$index mode=$mode")
                }
            }
            val sparse=before.copy().apply { remove("entry_p0");remove("entry_p1");putString("entry_p2","sparse $channel") }
            e.readFromNBT(sparse)
            c.check("rated supply decoder: sparse native saves restore their actual indexed channel",save().getStringOr("entry_p2","")=="sparse $channel" && save().getStringOr("entry_p0","").isEmpty(),"")
            val legacy=CompoundTag().apply { putString("channel","legacy $channel") };e.readFromNBT(legacy)
            val old=save()
            c.check("rated supply decoder: legacy one-channel saves clear stale later channels",old.getStringOr("entry_p0","")=="legacy $channel" && (1..2).all { old.getStringOr("entry_p$it","").isEmpty() } && LampSupplyElement.channelMap.values.flatten().none { it.element===e && it.id!=0 },"")
            e.readFromNBT(before.copy())
        }
    }
    private class Inlet(val rating: CordRating,val voxel: Boolean): Base() {
        lateinit var e: LampSupplyElement;lateinit var o: OutletElement;lateinit var source: VoltageSource;lateinit var resistor: Resistor
        override fun id()="forkpatch_lamp_supply_${rating.volts.toInt()}v_${if(voxel)"voxel" else "legacy"}_inlet_isolation"
        override fun build(c: TestContext) {
            o=outlet(c,rating);e=supply(c,rating)
            if(voxel) { val pos=e.sixNode.coordonate.pos();check(CableConverter.convert(e.sixNode)==null);e=(NodeManager.instance.getNodeFromCoordonate(Coordonate(pos,c.level())) as SixNode).getElement(Direction.YN) as LampSupplyElement }
            val device=requireNotNull(CordSupport.device(e))
            c.check("rated supply: source nominal acceptance rejects every wrong voltage",CordRating.entries.all { device.acceptsCord(it)==it.accepts(rating.volts) },"")
            check(cord(e).attach(o.anchor(0),true));source=feed(o.input,rating.volts);resistor=load(e.powerLoad,rating.volts*rating.volts/100)
            val pos=e.sixNode.coordonate.pos().relative(e.side.applyLRDU(e.front).toEnumFacing());c.floor(pos.below())
            val cable=c.placeSix(ElnDeviceRegistry.meduimVoltageCableDescriptor.parentItemDamage,pos) as ElectricalCableElement
            feed(cable.electricalLoad,250.0)
        }
        override fun measure(c: TestContext) { try {
            val link=cord(e)
            c.check("rated supply: adjacent 250 V cable cannot bypass its paid rated inlet",e.powerLoad.u>rating.volts*.99 && e.powerLoad.u<rating.volts && resistor.p>99 && resistor.p<100 && link.connected,"${e.powerLoad.u} / ${resistor.p}")
            c.check("rated supply: actual source pays load and both contact losses",abs(abs(source.u*source.current)-resistor.p-link.conductor.p-o.contact.p-e.loadResistor.p)<1e-4,"")
            c.check("rated supply: plugged native load and every power mask are isolated",LRDU.values().all { e.connectionElectricalLoad(it)==null && e.connectionMask(it) and NodeBase.maskElectricalPower==0 },"")
            val version=e.powerLoad.subSystem!!.matrixVersion;repeat(2000) { link.poll() }
            c.check("rated supply: stable owner polls preserve cached LU factors",e.powerLoad.subSystem!!.matrixVersion==version,"")
            link.detach()
            c.check("rated supply: unplug restores its original front inlet only",LRDU.values().all { e.connectionElectricalLoad(it)==(if(it==e.front)e.powerLoad else null) && e.connectionMask(it)==(if(it==e.front)NodeBase.maskElectricalPower else 0) },"")
        }finally { clean() } }
    }
    private class Wireless(val rating: CordRating,val channel: Int): Base() {
        lateinit var e: LampSupplyElement;lateinit var upstream: OutletElement;lateinit var downstream: OutletElement;lateinit var source: VoltageSource;lateinit var resistor: Resistor
        override fun id()="forkpatch_lamp_supply_${rating.volts.toInt()}v_channel_${channel}_real_passive_power"
        override fun build(c: TestContext) {
            upstream=outlet(c,rating);e=supply(c,rating);check(cord(e).attach(upstream.anchor(0),true))
            downstream=outlet(c,rating,6);val name="rated ${UUID.randomUUID()}";powerChannel(c,e,channel,name)
            downstream.networkUnserialize(c.stream { it.writeByte(83);it.writeUTF(name) });downstream.networkUnserialize(c.stream { it.writeByte(82) })
            source=feed(upstream.input,rating.volts);resistor=load(downstream.output,rating.volts*rating.volts/100)
        }
        override fun measure(c: TestContext) { try {
            val loss=cord(e).conductor.p+upstream.contact.p+downstream.wirelessPower.conductor.p+downstream.contact.p
            c.check("rated supply: actual selected native channel feeds passive downstream work",e.getChannelState(channel) && downstream.wirelessPower.supply===e && resistor.p>99 && resistor.p<100,"${resistor.p}")
            c.check("rated supply: upstream source pays every bridge and contact exactly once",e.loadResistor.p<1e-5 && abs(abs(source.u*source.current)-resistor.p-loss)<1e-4,"source=${source.u*source.current} load=${resistor.p} losses=$loss")
            val version=e.powerLoad.subSystem!!.matrixVersion;repeat(2000) { downstream.wirelessPower.refresh();cord(e).poll() }
            c.check("rated supply: stable channel and input polls retain cached LU factors",e.powerLoad.subSystem!!.matrixVersion==version,"")
            e.networkUnserialize(c.stream { it.writeByte(LampSupplyElement.setWirelessName.toInt());it.writeByte(channel);it.writeUTF("false") });e.lampSupplySlowProcess.process(.1);downstream.wirelessPower.refresh()
            c.check("rated supply: native per-channel control removes its passive bridge",!e.getChannelState(channel) && !downstream.wirelessPower.connected,"")
            e.networkUnserialize(c.stream { it.writeByte(LampSupplyElement.setWirelessName.toInt());it.writeByte(channel);it.writeUTF("true") });e.lampSupplySlowProcess.process(.1);downstream.wirelessPower.refresh()
            c.check("rated supply: reenabled native channel restores the same real source",downstream.wirelessPower.supply===e,"")
            e.sixNode.unload();downstream.wirelessPower.refresh()
            c.check("rated supply: source unload removes downstream bridge and its input conductor",!downstream.wirelessPower.connected && !cord(e).connected && cord(e).plugged && upstream.occupied(0),"")
        }finally { clean() } }
    }
    private class SavedOwner(val rating: CordRating): Base() {
        override fun id()="forkpatch_lamp_supply_${rating.volts.toInt()}v_real_owner_reload"
        override fun measure(c: TestContext) {
            val o=outlet(c,rating);var e=supply(c,rating);powerChannel(c,e,2,"saved ${UUID.randomUUID()}")
            val cable=item("Medium Voltage Cable",7);e.inventory.setItem(0,cable);e.inventory.setChanged()
            val old=cord(e);check(old.attach(o.anchor(0),true));val token=old.token;val pos=e.sixNode.coordonate.pos()
            val reload=Reload.node(c.level(),pos);e=(reload.fresh as SixNode).getElement(Direction.YN) as LampSupplyElement
            val fresh=cord(e);fresh.poll()
            c.check("rated supply: actual reload retains its paid token, hardware and increased range",fresh!==old && fresh.token==token && fresh.paid && fresh.connected && e.inventory.getItem(0).count==7 && e.range==39,"")
            c.check("rated supply: native channel registration moves to the fresh owner",LampSupplyElement.channelMap.values.flatten().none { it.element===old.owner } && LampSupplyElement.channelMap.values.flatten().any { it.element===e && it.id==2 },"")
            old.onBroken();fresh.poll()
            c.check("rated supply: stale unloaded owner cannot detach restored input",fresh.connected && fresh.paid && o.occupied(0),"")
        }
    }
    private class PaidUse(val rating: CordRating): Base() {
        override fun id()="forkpatch_lamp_supply_${rating.volts.toInt()}v_survival_cord_transaction"
        override fun measure(c: TestContext) {
            val o=outlet(c,rating);val e=supply(c,rating);val p=player(c)
            p.setItemInHand(InteractionHand.MAIN_HAND,item(rating.cordName,3));ItemNbt.edit(p.mainHandItem) { it.putString("retained","value") }
            fun use(pos: BlockPos) { c.level().getBlockState(pos).useItemOn(p.mainHandItem,c.level(),p,InteractionHand.MAIN_HAND,BlockHitResult(Vec3.atLowerCornerOf(pos).add(.5,.05,.5),net.minecraft.core.Direction.UP,pos,false)) }
            fun count()=(0 until p.inventory.containerSize).sumOf { p.inventory.getItem(it).let { s->if(s.item===item(rating.cordName).item)s.count else 0 } }
            try {
                use(o.node.coordonate.pos());use(e.sixNode.coordonate.pos())
                c.check("rated supply: actual survival use pays once and preserves unrelated held settings",count()==2 && cord(e).connected && cord(e).paid && ItemNbt.get(p.mainHandItem)?.getStringOr("retained","")=="value","")
                p.setShiftKeyDown(true);use(e.sixNode.coordonate.pos());use(e.sixNode.coordonate.pos())
                c.check("rated supply: repeated actual unplug refunds exactly one physical cord",count()==3 && !cord(e).plugged && !o.occupied(0),"")
            }finally { p.setShiftKeyDown(false);p.inventory.clearContent() }
        }
    }
}
