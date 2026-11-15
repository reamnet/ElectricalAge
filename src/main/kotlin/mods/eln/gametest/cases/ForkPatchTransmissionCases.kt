package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gl.GlFrame
import mods.eln.gridnode.*
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.mna.component.*
import mods.eln.sixnode.electricalcable.*
import mods.eln.utility.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.Blocks
import java.io.*
import kotlin.math.abs

object ForkPatchTransmissionCases {
    @JvmStatic fun cases(): List<TestCase> = TransmissionKind.entries.flatMap { kind ->
        listOf(Power(kind,0),Power(kind,1),Power(kind,2),PortsAndGraphics(kind),PaidSpan(kind),Resume(kind),PlayerSpan(kind))
    } + listOf(Clearance(),PhaseGuard(),Recipes(),DistributionLink(),Alignment(0),Alignment(1),Alignment(2)) + listOf(TransmissionKind.RISER_69,TransmissionKind.RISER_230).flatMap { kind ->
        listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP).map { PlacedRiser(kind,it) }
    }
    private fun wire(service: Boolean = false) = UtilityCables.all.first { it.material == UtilityCableMaterial.COPPER && it.sizeLabel == "2 AWG" && it.insulated == service && !it.melted }
    private open class Base : TestCase {
        val parts = ArrayList<Component>()
        override fun id() = "forkpatch_transmission_base"
        override fun width() = 32
        override fun height() = 28
        override fun depth() = 16
        override fun floorDepth() = 16
        override fun ticks() = 20
        override fun build(c: TestContext) { }
        override fun measure(c: TestContext) { clean() }
        fun pole(c: TestContext,kind: TransmissionKind,x: Int = 6) = c.placeTransparent(kind.id,c.at(x,1,7)) as TransmissionElement
        fun add(vararg components: Component) { parts.addAll(components); components.forEach(Eln.simulator::addElectricalComponent) }
        fun clean() { parts.forEach { Eln.simulator.removeElectricalComponent(it); it.breakConnection() }; parts.clear() }
    }
    private class Power(val kind: TransmissionKind,val mode: Int) : Base() {
        lateinit var a: TransmissionElement; lateinit var b: TransmissionElement
        val loads = ArrayList<Resistor>()
        override fun id() = "forkpatch_transmission_${kind.name.lowercase()}_"+listOf("forward","reverse","independent_service")[mode]
        override fun build(c: TestContext) {
            a = pole(c,kind); b = pole(c,kind,20)
            val service = mode == 2
            check(GridLink.addLink(a,b,if (service) 50 else 52,if (service) 50 else 52,wire(service),42))
            val source = if (mode == 1) b else a
            val sink = if (mode == 1) a else b
            for (i in 0..2) {
                val volts = if (service) listOf(120.0,120.0,240.0)[i] else kind.volts*(.8+i*.1)
                val incoming = if (service) source.services[i] else source.inputs[i]
                val outgoing = if (service) sink.services[i] else sink.inputs[i]
                val load = Resistor(outgoing,null).setR(volts/2.0)
                loads.add(load); add(VoltageSource("transmission$i",incoming,null).setU(volts),load)
            }
        }
        override fun measure(c: TestContext) {
            try {
                val sink = if (mode == 1) a else b
                for (i in 0..2) {
                    val expected = if (mode == 2) listOf(120.0,120.0,240.0)[i] else kind.volts*(.8+i*.1)
                    c.check("transmission: paid span carries each actual circuit independently",abs(loads[i].aPin!!.state-expected)<.2 && loads[i].p>expected*1.99,"${kind.name} mode=$mode phase=$i U=${loads[i].aPin!!.state}")
                }
                val unused = if (mode == 2) a.inputs+b.inputs else a.services+b.services
                c.check("transmission: independent rack cannot invent or cross-feed power",unused.all { abs(it.u)<1e-8 },"${kind.name} mode=$mode")
                val source = if (mode == 1) b else a
                val sourcePorts = if (mode == 2) source.services else source.inputs
                val supplied = sourcePorts.sumOf { it.u*abs(it.current) }
                val consumed = loads.sumOf { it.p }
                c.check("transmission: real source pays load and passive wire losses",supplied>=consumed && supplied-consumed<5.0,"source=$supplied load=$consumed")
                c.check("transmission: no new solver or per-tick network traversal is installed",a.electricalComponentList.size==2 && a.electricalComponentList.all { it is Resistor && it.r>=1e19 } && a.electricalProcessList.isEmpty(),kind.name)
                c.check("transmission: sink remains the correct source identity",Eln.transparentNodeItem.getDescriptor(kind.id) === sink.descriptor,kind.id.toString())
            } finally { clean() }
        }
    }
    private class PortsAndGraphics(val kind: TransmissionKind) : Base() {
        override fun id() = "forkpatch_transmission_${kind.name.lowercase()}_ports_packets_graphics"
        override fun measure(c: TestContext) {
            val e = pole(c,kind)
            val savedFront = e.front
            for (front in listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP)) {
                e.front = front; e.updateIdealRenderAngle()
                c.check("transmission: riser base exposes phase A/B/C only at Down",e.getElectricalLoad(front.left(),LRDU.Down) === if (kind.riser) e.inputs[0] else null,"$kind $front A")
                c.check("transmission: riser right and rear preserve phase ordering",e.getElectricalLoad(front.right(),LRDU.Down) === if (kind.riser) e.inputs[1] else null,"$kind $front B")
                c.check("transmission: rear riser does not short to service",e.getElectricalLoad(front.inverse,LRDU.Down) === if (kind.riser) e.inputs[2] else null,"$kind $front C")
                c.check("transmission: primary entry rejects upper cables",e.getElectricalLoad(front.left(),LRDU.Up) == null && e.getElectricalLoad(Direction.YP,LRDU.Down) == null,"")
                c.check("transmission: bottom service remains independent",e.getElectricalLoad(Direction.YN,LRDU.Left) === e.services[0] && e.getElectricalLoad(Direction.YN,LRDU.Right) === e.services[1] && e.getElectricalLoad(Direction.YN,LRDU.Down) === e.services[2],"")
                // Offset 5 is a packed ARGB integer stored in float bits, not a numeric coordinate.
                GlFrame.beginRenderFrame(); val frame = GlFrame.open()
                try { (e.descriptor as TransmissionDescriptor).drawEquipment(front); c.check("transmission: complete source steel/porcelain/bus mesh emits finite native geometry",frame.batches().values.sumOf { it.vertices() }>500 && frame.batches().values.all { b -> (0 until b.floats).filter { it % GlFrame.STRIDE != 5 }.all { b.data[it].isFinite() } },"$kind $front") } finally { GlFrame.close() }
            }
            e.front = savedFront; e.updateIdealRenderAngle()
            for (port in kind.terminals) { e.networkUnserialize(c.stream { it.writeByte(82);it.writeInt(port) }); c.check("transmission: native menu selects every explicit source terminal",e.selected==port,port.toString()) }
            for (bad in listOf(-1,0,5,35,36,53,999,Int.MAX_VALUE)) { e.networkUnserialize(c.stream { it.writeByte(82);it.writeInt(bad) }); c.check("transmission: malformed terminal cannot select aliases or absent circuits",e.selected==42,bad.toString()) }
            e.networkUnserialize(c.stream { it.writeByte(82) })
            c.check("transmission: truncated control packet leaves state intact",e.selected==42,"")
            val tag = CompoundTag(); e.writeToNBT(tag)
            val copy = TransmissionElement(TransparentNode(),e.descriptor); copy.readFromNBT(tag)
            val carried = TransmissionElement(TransparentNode(),e.descriptor); carried.readItemStackNBT(e.itemStackNBT)
            c.check("transmission: owner save and carried settings preserve selection",copy.selected==42 && carried.selected==42,"")
            carried.readItemStackNBT(CompoundTag().apply { putInt("transmissionTerminal",999) })
            c.check("transmission: invalid saved selection returns to three-phase line",carried.selected==52,"")
            val menu = mods.eln.gui.MenuSpecs.of(e.descriptor)
            c.check("transmission: source-sized native player inventory has no phantom device slots",e.hasGui() && menu.width()==240 && menu.height()==230 && menu.playerInventory() && menu.slots().isEmpty(),"")
            mods.eln.client.ClientRenders.registerAll()
            val pos=e.node.coordonate.pos()
            val client=TransparentNodeEntity(pos,c.level().getBlockState(pos)); client.setLevel(c.level())
            try {
                val stream=DataInputStream(ByteArrayInputStream(e.node.publishPacket.toByteArray()))
                stream.readByte();repeat(4) { stream.readInt() };stream.readUTF()
                client.serverPublishUnserialize(stream)
                val render=client.elementRender as? TransmissionRender
                c.check("transmission: registered native publication factory constructs the real renderer",render!=null,kind.name)
                if (render==null) return
                c.check("transmission: native publication consumes complete display and terminal state",stream.available()==0 && render.selected==42,"")
                c.check("transmission: renderer retains a loaded pole beyond vanilla's 64-block owner cutoff",render.visibleFrom(net.minecraft.world.phys.Vec3(pos.x+100.5,pos.y+20.5,pos.z+.5)),kind.name)
                c.check("transmission: extended view distance remains bounded",!render.visibleFrom(net.minecraft.world.phys.Vec3(pos.x+512.5,pos.y+.5,pos.z+.5)),kind.name)
                val bounds = render.renderBounds()
                c.check("transmission: tall structure and whole paid-span envelope cannot be culled at the base",bounds.maxY>=e.node.coordonate.y+kind.height && bounds.minX<=e.node.coordonate.x-kind.range && bounds.maxZ>=e.node.coordonate.z+kind.range,"")
                for (phase in 0..2) c.check("transmission: primary attachment matches source height and crossarm spread",e.gridCablePoint(32+phase,0)==net.minecraft.world.phys.Vec3(0.0,kind.height-.75,(phase-1)*kind.spread),phase.toString())
            } finally { client.clientDestroy() }
        }
    }
    private class PaidSpan(val kind: TransmissionKind) : Base() {
        override fun id() = "forkpatch_transmission_${kind.name.lowercase()}_paid_span_and_saved_mesh"
        override fun measure(c: TestContext) {
            val a = pole(c,kind); val b = pole(c,kind,20)
            check(GridLink.addLink(a,b,52,52,wire(),42))
            val link = a.gridLinkList.single(); val saved = CompoundTag(); link.writeToNBT(saved,"")
            val restored = GridLink(saved,"")
            c.check("transmission: paid native record retains exact endpoints and phase bundles",restored.sameTerminals(link),"")
            c.check("transmission: bundle cannot double-spend an individual phase span",!GridLink.addLink(a,b,32,32,wire(),14),"")
            val bytes = ByteArrayOutputStream(); a.networkSerialize(DataOutputStream(bytes))
            val render = TransmissionRender(c.level().getBlockEntity(a.node.coordonate.pos()) as TransparentNodeEntity,a.descriptor)
            try {
                render.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray()))); val revision = render.spanGeometryRevision
                render.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray())))
                c.check("transmission: repeated status updates reuse cached native catenaries",revision>0 && render.spanGeometryRevision==revision,"")
                GlFrame.beginRenderFrame(); val frame = GlFrame.open()
                try { render.drawCables(); c.check("transmission: bundled primary emits all three paid wire meshes",frame.batches().values.sumOf { it.vertices() }>500,"") } finally { GlFrame.close() }
            } finally { render.destructor() }
            val refund = link.onBreakElement()
            c.check("transmission: breaking span returns exactly the paid core metres",wire().getRemainingLengthMeters(refund)==42.0 && link.onBreakElement().isEmpty && a.gridLinkList.isEmpty() && b.gridLinkList.isEmpty(),refund.toString())
        }
    }
    private class Alignment(val mode: Int) : Base() {
        override fun id()="forkpatch_transmission_primary_alignment_"+listOf("north_south","diagonal","service_does_not_turn_primary")[mode]
        override fun depth()=32;override fun floorDepth()=32
        override fun measure(c: TestContext) {
            val a=c.placeTransparent(TransmissionKind.POLE_230.id,c.at(8,1,8)) as TransmissionElement
            val b=c.placeTransparent(TransmissionKind.RISER_230.id,c.at(if(mode==1)22 else 8,1,22)) as TransmissionElement
            a.front=Direction.XN;b.front=Direction.XP;a.updateIdealRenderAngle();b.updateIdealRenderAngle()
            val beforeA=a.renderedGridCablePoint(40,0);val beforeB=b.renderedGridCablePoint(40,0)
            check(GridLink.addLink(a,b,if(mode==2)50 else 52,if(mode==2)50 else 52,wire(mode==2),if(mode==1)60 else 42))
            if(mode==2)c.check("transmission alignment: secondary branch preserves independently placed primary orientation",a.primaryAngle==0f && b.primaryAngle==180f,"")
            else {
                val expected=if(mode==0)-90f else -45f
                c.check("transmission alignment: both crossarms follow the actual primary run in matching phase order",abs(a.primaryAngle-expected)<.01 && abs(b.primaryAngle-expected)<.01,"${a.primaryAngle} ${b.primaryAngle}")
                for(i in 0..2)c.check("transmission alignment: rendered cable endpoints follow the same primary transform",a.renderedGridCablePoint(32+i,0)==a.gridCablePoint(32+i,0).yRot(Math.toRadians(a.primaryAngle.toDouble()).toFloat()),i.toString())
            }
            c.check("transmission alignment: primary spans cannot rotate the secondary suspension",a.renderedGridCablePoint(40,0)==beforeA && b.renderedGridCablePoint(40,0)==beforeB,"")
            c.check("transmission alignment: rotating crossarms have a pre-reserved swept clearance",!c.level().getBlockState(a.node.coordonate.pos().offset(3,23,3)).isAir,"")
            mods.eln.client.ClientRenders.registerAll()
            val pos=a.node.coordonate.pos();val client=TransparentNodeEntity(pos,c.level().getBlockState(pos));client.setLevel(c.level())
            try {
                val input=DataInputStream(ByteArrayInputStream(a.node.publishPacket.toByteArray()));input.readByte();repeat(4) { input.readInt() };input.readUTF();client.serverPublishUnserialize(input)
                val render=client.elementRender as? TransmissionRender
                c.check("transmission alignment: actual publication retains separate primary and service frames",render!=null && input.available()==0 && render.primaryAngle==a.primaryAngle && render.front==a.front,"")
            } finally { client.clientDestroy() }
        }
    }
    private class PlayerSpan(val kind: TransmissionKind) : Base() {
        override fun id()="forkpatch_transmission_${kind.name.lowercase()}_survival_link_payment"
        override fun measure(c: TestContext) {
            val a=pole(c,kind);val b=pole(c,kind,20)
            val player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(id().toByteArray()),"steelSpan"))
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL)
            val stock=wire().newItemStack();wire().setRemainingLengthMeters(stock,200.0)
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stock)
            fun click(owner: TransmissionElement) { owner.onBlockActivated(player,Direction.XN,.5f,.5f,.5f) }
            click(a);click(b)
            c.check("transmission: actual survival clicks spend three core lengths for a primary bundle",a.gridLinkList.size==1 && wire().getRemainingLengthMeters(stock)==158.0,kind.name)
            click(a);click(b)
            c.check("transmission: duplicate player link cannot consume cable twice",a.gridLinkList.size==1 && wire().getRemainingLengthMeters(stock)==158.0,kind.name)
            val paid=a.gridLinkList.single().onBreakElement()
            c.check("transmission: real survival span returns exactly its consumed stock",wire().getRemainingLengthMeters(paid)==42.0 && mods.eln.generic.GenericItemBlockUsingDamageDescriptor.getDescriptor(paid)===wire(),kind.name)
            a.selectTerminal(32);b.selectTerminal(33);click(a);click(b)
            c.check("transmission: rejected player cross-phase link preserves remaining paid cable",a.gridLinkList.isEmpty() && b.gridLinkList.isEmpty() && wire().getRemainingLengthMeters(stock)==158.0,kind.name)
        }
    }
    private class DistributionLink : Base() {
        lateinit var steel: TransmissionElement;lateinit var wood: DistributionPoleElement
        val loads=ArrayList<Resistor>()
        override fun id()="forkpatch_transmission_interoperates_with_existing_wooden_distribution"
        override fun build(c: TestContext) {
            steel=pole(c,TransmissionKind.POLE_69)
            wood=c.placeTransparent(DistributionPoles.id(DistributionKind.THREE),c.at(20,1,7)) as DistributionPoleElement
            check(GridLink.addLink(steel,wood,52,52,wire(),42))
            for(i in 0..2) { val load=Resistor(wood.unit.lines[i],null).setR(12000.0);loads.add(load);add(VoltageSource("steel to wooden phase$i",steel.inputs[i],null).setU(12000.0+i*100),load) }
        }
        override fun measure(c: TestContext) {
            try {
                for(i in 0..2)c.check("transmission: native wooden distribution accepts each independently supplied primary phase",abs(wood.unit.lines[i].u-(12000.0+i*100))<.2 && loads[i].p>11990,"phase=$i U=${wood.unit.lines[i].u}")
                c.check("transmission: mixed-height span preserves independent service racks",(steel.services+wood.unit.services).all { abs(it.u)<1e-8 },"")
                val bytes=ByteArrayOutputStream();steel.networkSerialize(DataOutputStream(bytes))
                val render=TransmissionRender(c.level().getBlockEntity(steel.node.coordonate.pos()) as TransparentNodeEntity,steel.descriptor)
                try {
                    render.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray())))
                    GlFrame.beginRenderFrame();val frame=GlFrame.open()
                    try { render.drawCables();c.check("transmission: mixed-height primary span has finite recorded positions",frame.batches().values.sumOf { it.vertices() }>500 && frame.batches().values.all { b -> (0 until b.floats).filter { it % GlFrame.STRIDE != 5 }.all { b.data[it].isFinite() } },"") } finally { GlFrame.close() }
                } finally { render.destructor() }
            } finally { clean() }
        }
    }
    private class Resume(val kind: TransmissionKind) : Base() {
        lateinit var a: TransmissionElement; lateinit var b: TransmissionElement
        lateinit var load: Resistor; lateinit var clock: mods.eln.sim.IProcess
        var elapsed=0.0; var queued=false; var reloaded=false
        override fun id()="forkpatch_transmission_${kind.name.lowercase()}_live_owner_reload_resumes_power"
        override fun ticks()=100
        private fun feed() {
            load=Resistor(b.inputs[0],null).setR(kind.volts)
            add(VoltageSource("transmission reload",a.inputs[0],null).setU(kind.volts),load)
        }
        override fun build(c: TestContext) {
            a=pole(c,kind);b=pole(c,kind,20);a.selectTerminal(34);b.selectTerminal(50)
            check(GridLink.addLink(a,b,52,52,wire(),42));feed()
            clock=mods.eln.sim.IProcess { dt ->
                elapsed+=dt
                if (!queued && elapsed>=1.0) {
                    queued=true
                    c.level().server.schedule(net.minecraft.server.TickTask(c.level().server.tickCount) {
                        c.check("transmission reload: real load is powered before unload",load.p>kind.volts*.999,kind.name)
                        val ap=a.node.coordonate.pos();val bp=b.node.coordonate.pos();clean()
                        Reload.node(c.level(),ap);Reload.node(c.level(),bp)
                        a=GridLink.getElementFromCoordinate(Coordonate(ap,c.level())) as TransmissionElement
                        b=GridLink.getElementFromCoordinate(Coordonate(bp,c.level())) as TransmissionElement
                        feed();reloaded=true
                    })
                }
            }
            Eln.simulator.addSlowProcess(clock)
        }
        override fun measure(c: TestContext) {
            try {
                c.check("transmission reload: real native circuit resumes after both owners are reconstructed",reloaded && load.p>kind.volts*.999,kind.name)
                c.check("transmission reload: saved selections survive live owner replacement",a.selected==34 && b.selected==50,kind.name)
                c.check("transmission reload: both owners share one canonical paid span",a.gridLinkList.size==1 && b.gridLinkList.size==1 && a.gridLinkList.single()===b.gridLinkList.single() && a.gridLinkList.single().connected,kind.name)
                val link=a.gridLinkList.single();val returned=link.onBreakElement()
                c.check("transmission reload: restored span refunds its paid length once",wire().getRemainingLengthMeters(returned)==42.0 && link.onBreakElement().isEmpty,kind.name)
            } finally { Eln.simulator.removeSlowProcess(clock);clean() }
        }
    }
    private class Recipes : Base() {
        override fun id()="forkpatch_transmission_source_recipes_require_paid_poles_and_three_cables"
        private fun craft(c: TestContext,rows: List<String>,key: Map<Char,net.minecraft.world.item.ItemStack>): net.minecraft.world.item.ItemStack {
            val input=net.minecraft.world.item.crafting.CraftingInput.of(rows[0].length,rows.size,rows.flatMap { row -> row.map { key[it]?.copy() ?: net.minecraft.world.item.ItemStack.EMPTY } })
            return c.level().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,c.level()).map { it.value().assemble(input) }.orElse(net.minecraft.world.item.ItemStack.EMPTY)
        }
        override fun measure(c: TestContext) {
            fun item(name: String)=Eln.findItemStack(name,1)
            val pole=craft(c,listOf("I I","IPI","I I"),mapOf('I' to net.minecraft.world.item.ItemStack(net.minecraft.world.level.block.Blocks.IRON_BARS),'P' to item(DistributionKind.THREE.title)))
            c.check("transmission recipe: source steel pole consumes the wooden three-phase pole and six iron bars",pole.item===item(TransmissionKind.POLE_69.title).item && pole.count==1,pole.toString())
            val tower=craft(c,listOf("IPI","PBP","I I"),mapOf('I' to net.minecraft.world.item.ItemStack(net.minecraft.world.level.block.Blocks.IRON_BLOCK),'P' to pole,'B' to net.minecraft.world.item.ItemStack(net.minecraft.world.level.block.Blocks.IRON_BARS)))
            c.check("transmission recipe: source tower consumes three steel poles and four iron blocks",tower.item===item(TransmissionKind.POLE_230.title).item && tower.count==1,tower.toString())
            for ((base,riser) in listOf(pole to TransmissionKind.RISER_69,tower to TransmissionKind.RISER_230)) {
                val keys=mapOf('P' to base,'C' to item("Copper Cable"))
                val result=craft(c,listOf("PCC","C  "),keys)
                c.check("transmission recipe: each riser pays its matching pole and all three base conductors",result.item===item(riser.title).item && result.count==1,result.toString())
                val short=craft(c,listOf("PCC"),keys)
                c.check("transmission recipe: missing a conductor cannot craft the three-phase riser",short.item!==item(riser.title).item,short.toString())
            }
        }
    }
    private class PlacedRiser(val kind: TransmissionKind,val orientation: Direction) : Base() {
        lateinit var a: TransmissionElement; lateinit var b: TransmissionElement
        val loads = ArrayList<Resistor>()
        override fun id() = "forkpatch_transmission_placed_${kind.name.lowercase()}_${orientation.name.lowercase()}"
        override fun build(c: TestContext) {
            val player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
            val yaw = player.yRot; val pitch = player.xRot
            try {
                player.xRot = 0f
                player.yRot = when (orientation) { Direction.XN -> 270f; Direction.XP -> 90f; Direction.ZN -> 0f; else -> 180f }
                a = pole(c,kind); b = pole(c,kind,20)
            } finally { player.yRot = yaw; player.xRot = pitch }
            check(GridLink.addLink(a,b,52,52,wire(),42))
            for (i in 0..2) {
                fun cable(owner: TransmissionElement): UtilityCableElement {
                    val side = listOf(owner.front.left(),owner.front.right(),owner.front.inverse)[i]
                    return c.placeSix(wire().parentItemDamage,owner.node.coordonate.pos().relative(side.toEnumFacing())) as UtilityCableElement
                }
                val from = cable(a); val to = cable(b)
                val load = Resistor(to.electricalLoad,null).setR(kind.volts*(.8+i*.1))
                loads.add(load)
                add(VoltageSource("placed riser phase$i",from.electricalLoad,null).setU(kind.volts*(.8+i*.1)),load)
            }
        }
        override fun measure(c: TestContext) {
            try {
                c.check("transmission: real player facing rotates riser ports and clearance together",a.front==orientation && b.front==orientation,"$kind $orientation")
                for (i in 0..2) {
                    val volts = kind.volts*(.8+i*.1)
                    c.check("transmission: actual placed cables route each primary phase through both risers and a paid span",abs(loads[i].aPin!!.state-volts)<.2 && loads[i].p>volts*.999,"$kind $orientation phase=$i U=${loads[i].aPin!!.state}")
                }
                c.check("transmission: base cable connections cannot energize unrelated service rack",(a.services+b.services).all { abs(it.u)<1e-8 },"")
            } finally { clean() }
        }
    }
    private class Clearance : Base() {
        override fun id() = "forkpatch_transmission_tower_clearance_transaction"
        override fun measure(c: TestContext) {
            val kind = TransmissionKind.POLE_230; val p = c.at(6,1,7)
            c.level().setBlock(p.above(23),Blocks.STONE.defaultBlockState(),3)
            val descriptor = requireNotNull(Eln.transparentNodeItem.getDescriptor(kind.id))
            val player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
            val blocked = (descriptor.parentItem as TransparentNodeItem).placeAt(descriptor.newItemStack(),player,c.level(),p,Direction.YN)
            c.check("transmission: high obstruction rejects entire placement transaction",blocked==null && c.level().getBlockState(p).isAir && c.level().getBlockState(p.above()).isAir,"")
            c.level().setBlock(p.above(23),Blocks.AIR.defaultBlockState(),3)
            val swept=p.offset(3,23,3);c.level().setBlock(swept,Blocks.STONE.defaultBlockState(),3)
            val sweptBlocked=(descriptor.parentItem as TransparentNodeItem).placeAt(descriptor.newItemStack(),player,c.level(),p,Direction.YN)
            c.check("transmission: an obstruction in the rotating crossarm envelope rejects placement without overwriting it",sweptBlocked==null && c.level().getBlockState(p).isAir && c.level().getBlockState(swept).`is`(Blocks.STONE),"")
            c.level().setBlock(swept,Blocks.AIR.defaultBlockState(),3)
            val e = pole(c,kind)
            c.check("transmission: successful tower reserves full height and both crossarm ends",!c.level().getBlockState(p.above(24)).isAir && (!c.level().getBlockState(p.offset(0,23,5)).isAir || !c.level().getBlockState(p.offset(5,23,0)).isAir),"")
            c.check("transmission: reserved structure has unique coordinates",e.descriptor.getGhostGroupAt(e.node.coordonate,e.front).elementList.map { Triple(it.x,it.y,it.z) }.distinct().size==e.descriptor.getGhostGroupAt(e.node.coordonate,e.front).elementList.size,"")
        }
    }
    private class PhaseGuard : Base() {
        override fun id() = "forkpatch_transmission_cross_phase_and_service_rejection"
        override fun measure(c: TestContext) {
            val a = pole(c,TransmissionKind.POLE_69); val b = pole(c,TransmissionKind.RISER_69,20)
            for ((from,to) in listOf(32 to 33,32 to 40,52 to 50,50 to 52)) {
                var rejected = false; try { GridLink.addLink(a,b,from,to,wire(),42) } catch (_: IllegalArgumentException) { rejected=true }
                c.check("transmission: primary phases and service voltages cannot cross-wire",rejected && a.gridLinkList.isEmpty() && b.gridLinkList.isEmpty(),"$from->$to")
            }
        }
    }
}
