package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.TestCase
import mods.eln.gametest.TestContext
import mods.eln.gridnode.GridElement
import mods.eln.gridnode.GridLink
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sixnode.electricalcable.UtilityCableMaterial
import mods.eln.sixnode.electricalcable.UtilityCables
import mods.eln.utility.modular.BusbarElement
import mods.eln.utility.modular.BusbarSize
import mods.eln.utility.modular.Busbars
import mods.eln.utility.modular.ModularTransformerElement
import mods.eln.utility.modular.ModularTransformerKind
import net.minecraft.core.BlockPos
import net.minecraft.server.level.TicketType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.TicketStorage
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB
import kotlin.math.abs
import kotlin.math.ceil

/** EA's resident network keeps supplying a load while Minecraft evicts one bay's block entities. */
class ModularChunkCase : TestCase {
    private val inputPos = BlockPos(1_021_004, 100, 1_021_000)
    // Keep the bay outside the incoming chunk ticket's generation halo.
    private val transformerPos = inputPos.east(190)
    private val outputPos = transformerPos.south(7)
    private val inputChunk = chunk(inputPos)
    private val bayChunk = chunk(transformerPos)
    private lateinit var incoming: BusbarElement
    private lateinit var transformer: ModularTransformerElement
    private lateinit var outgoing: BusbarElement
    private lateinit var source: VoltageSource
    private lateinit var load: Resistor
    private lateinit var inputLink: GridLink
    private lateinit var outputLink: GridLink
    private var oldInputEntity: BlockEntity? = null
    private var oldBayEntity: BlockEntity? = null

    override fun id() = "modular_powered_bay_real_chunk_eviction"
    override fun ticks() = 480

    override fun build(c: TestContext) {
        ticket(c, inputChunk, true)
        ticket(c, bayChunk, true)
        for (pos in listOf(inputPos, transformerPos, outputPos)) c.floor(pos.below())
        val mediumCopper = Busbars.FIRST_ID + BusbarSize.MEDIUM.ordinal * UtilityCableMaterial.entries.size
        incoming = c.placeTransparent(mediumCopper, inputPos) as BusbarElement
        transformer = c.placeTransparent(ModularTransformerKind.DISTRIBUTION.id, transformerPos) as ModularTransformerElement
        outgoing = c.placeTransparent(Busbars.FIRST_ID, outputPos) as BusbarElement
        check(transformer.configure(3200, 230, 3, 1, 75, 110))
        inputLink = span(incoming, transformer, 32, 32)
        outputLink = span(transformer, outgoing, 36, 32)
        source = VoltageSource("modular chunk test source", incoming.load, null).apply { u = 3200.0 }
        load = Resistor(outgoing.load, null).apply { r = 1000.0 }
        Eln.simulator.addElectricalComponent(source)
        Eln.simulator.addElectricalComponent(load)

        c.after(30) {
            checkPowered(c)
            oldInputEntity = c.level().getBlockEntity(inputPos)
            oldBayEntity = c.level().getBlockEntity(transformerPos)
            c.check("both initially loaded columns have native block entities",
                oldInputEntity != null && oldBayEntity != null,
                "input=$oldInputEntity, bay=$oldBayEntity at ${c.level().getBlockState(transformerPos)}")
            ticket(c, bayChunk, false)
        }
        c.after(390) {
            val chunks = c.level().chunkSource
            val bayEvicted = chunks.getChunkNow(bayChunk.x(), bayChunk.z()) == null
            val oldEntityRemoved = oldBayEntity?.isRemoved == true
            val inputRetained = oldInputEntity != null && c.level().getBlockEntity(inputPos) === oldInputEntity
            val bayTickets = chunks.dataStorage.computeIfAbsent(TicketStorage.TYPE).getTickets(bayChunk.pack())
            c.check("Minecraft evicts the transformer bay while the incoming bar stays loaded",
                bayEvicted && oldEntityRemoved && inputRetained,
                "bay evicted=$bayEvicted, old entity removed=$oldEntityRemoved, incoming retained=$inputRetained, tickets=$bayTickets")
            checkPowered(c)
            ticket(c, bayChunk, true)
            c.level().getChunk(bayChunk.x(), bayChunk.z())
        }
    }

    override fun measure(c: TestContext) {
        try {
            val freshEntity = c.level().getBlockEntity(transformerPos)
            c.check("reload creates a fresh Minecraft block entity for the same configured transformer",
                freshEntity != null && freshEntity !== oldBayEntity && !freshEntity.isRemoved &&
                    c.nodeAt(transformerPos) === transformer.node &&
                    transformer.primary == 3200 && transformer.secondary == 230 && transformer.tap == 1 &&
                    transformer.ratingPercent == 75,
                "old=$oldBayEntity, fresh=$freshEntity, live=${c.nodeAt(transformerPos) === transformer.node}")
            checkPowered(c)
            c.check("the paid input and output still have one link and one thermal conductor each",
                incoming.gridLinkList.singleOrNull() === inputLink &&
                    outgoing.gridLinkList.singleOrNull() === outputLink &&
                    transformer.gridLinkList == setOf(inputLink, outputLink) &&
                    inputLink.thermalLoads().size == 1 && outputLink.thermalLoads().size == 1, "")
            c.level().destroyBlock(transformerPos, false)
            c.check("removing the reloaded transformer releases both links from both bars",
                incoming.gridLinkList.isEmpty() && outgoing.gridLinkList.isEmpty() &&
                    !inputLink.connected && !outputLink.connected && c.nodeAt(transformerPos) == null, "")
        } finally {
            for (part in listOf(load, source)) {
                Eln.simulator.removeElectricalComponent(part)
                part.breakConnection()
            }
            for (pos in listOf(inputPos, transformerPos, outputPos)) {
                c.level().removeBlock(pos, false)
                c.level().removeBlock(pos.below(), false)
            }
            c.level().getEntitiesOfClass(ItemEntity::class.java, AABB(transformerPos).inflate(10.0)).forEach { it.discard() }
            ticket(c, inputChunk, false)
            ticket(c, bayChunk, false)
        }
    }

    private fun checkPowered(c: TestContext) {
        c.checkValue("the configured transformer still supplies the downstream load", 230.0 * 1.0125 / 1000.0, load.i)
        c.check("phase B and phase C stay isolated during the lifecycle",
            transformer.outputs.drop(1).all { abs(it.u) < 1e-7 }, "")
    }

    private fun span(a: GridElement, b: GridElement, from: Int, to: Int): GridLink {
        val wire = UtilityCables.all.first { it.insulated && it.sizeLabel == "2 AWG" &&
            it.material == UtilityCableMaterial.COPPER && it.insulationVoltageRating == 20000.0 }
        val meters = ceil(a.coordonate().trueDistanceTo(b.coordonate())).toInt()
        val spool = wire.newItemStack().also { wire.setRemainingLengthMeters(it, meters.toDouble()) }
        return savedModularSpan(a, b, from, to, spool, wire.resistanceOhms(meters.toDouble()))
    }

    private fun chunk(pos: BlockPos) = ChunkPos(pos.x shr 4, pos.z shr 4)

    private fun ticket(c: TestContext, chunk: ChunkPos, add: Boolean) {
        if (add) c.level().chunkSource.addTicketWithRadius(TicketType.FORCED, chunk, 0)
        else c.level().chunkSource.removeTicketWithRadius(TicketType.FORCED, chunk, 0)
    }
}
