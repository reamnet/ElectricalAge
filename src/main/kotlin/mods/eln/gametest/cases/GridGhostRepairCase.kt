package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.compat.BlockMeta
import mods.eln.gametest.*
import mods.eln.ghost.GhostBlock
import mods.eln.gridnode.GridGhosts
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.node.transparent.TransparentNode
import mods.eln.utility.*
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

/** Real loaded/unloaded chunk boundary; the repair itself must not wake the neighbouring chunk. */
class GridGhostRepairCase: TestCase {
    override fun id()="grid_ghost_repair_preserves_unloaded_neighbor"
    override fun build(c: TestContext) {
        val world=c.level()
        val seedX=(c.at(0,0,0).x shr 4)+4096
        val seedZ=(c.at(0,0,0).z shr 4)+4096
        world.getChunk(seedX,seedZ)
        // Generation dependencies may also be resident. Find their loaded boundary without more chunk requests.
        var edge: Pair<Int,Int>?=null
        for(x in seedX-8..seedX+8)for(z in seedZ-8..seedZ+8)
            if(world.chunkSource.getChunkNow(x,z)!=null && world.chunkSource.getChunkNow(x,z-1)==null)edge=x to z
        check(edge!=null) { "No unloaded-neighbour fixture boundary" }
        val (cx,cz)=edge!!
        val owner=Coordonate(BlockPos(cx*16+8,200,cz*16+1),world)
        val descriptor=Eln.transparentNodeItem.getDescriptor(DistributionPoles.id(DistributionKind.SECONDARY)) as DistributionPoleDescriptor
        val node=TransparentNode().apply { coordonate=owner }
        val pole=DistributionPoleElement(node,descriptor).apply { front=Direction.XN }
        val cells=descriptor.getGhostGroupAt(owner,pole.front)!!.elementList
        val rack=owner.pos().offset(0,5,-1)
        val quiet=Block.UPDATE_CLIENTS or Block.UPDATE_KNOWN_SHAPE
        var orphanInserted=false
        val target=Coordonate(rack,world)
        try {
            c.check("chunk fixture begins with a resident owner and absent neighbour",world.chunkSource.getChunkNow(cx,cz)!=null && world.chunkSource.getChunkNow(cx,cz-1)==null,"")
            c.check("missing owned rack is restored",GridGhosts.repair(pole) && Eln.ghostManager.getGhost(Coordonate(rack,world))?.observatorCoordonate==owner,"")
            c.check("repairing an air reservation does not load its neighbour",world.chunkSource.getChunkNow(cx,cz-1)==null,"")
            world.setBlock(rack,Eln.ghostBlock.defaultBlockState().setValue(BlockMeta.META,GhostBlock.tClearance),quiet)
            c.check("old owned clearance upgrades without loading terrain",GridGhosts.repair(pole) && world.getBlockState(rack).getValue(BlockMeta.META)==GhostBlock.tSelectableClearance && world.chunkSource.getChunkNow(cx,cz-1)==null,"")
            Eln.ghostManager.removeGhost(Coordonate(rack,world));world.setBlock(rack,Blocks.STONE.defaultBlockState(),quiet)
            GridGhosts.repair(pole)
            c.check("foreign blocks and absent neighbours remain untouched",world.getBlockState(rack).block===Blocks.STONE && world.chunkSource.getChunkNow(cx,cz-1)==null,"")
            world.setBlock(rack,Blocks.AIR.defaultBlockState(),quiet)
            check(!mods.eln.node.NodeManager.instance.hasOrphanAt(target))
            val tag=net.minecraft.nbt.CompoundTag().apply {
                putString("tag","eln:test_missing_grid_repair_node")
                put("node",net.minecraft.nbt.CompoundTag().also { target.writeToNBT(it,"c") })
                putString("sentinel","preserve inactive orphan")
            }
            val original=tag.copy()
            mods.eln.node.NodeManager.instance.loadFromNbt(net.minecraft.nbt.CompoundTag().apply { put("n0",tag) },target.dimention)
            orphanInserted=true
            GridGhosts.repair(pole)
            c.check("air-looking inactive orphans keep their saved state and ownership",
                world.getBlockState(rack).isAir && Eln.ghostManager.getGhost(target)==null &&
                mods.eln.node.NodeManager.instance.hasOrphanAt(target) &&
                mods.eln.node.NodeManager.instance.getOrphans(target.dimention).any { it==original },"")
        }finally {
            if(orphanInserted)mods.eln.node.NodeManager.instance.dropOrphans(target,"grid repair test cleanup")
            Eln.ghostManager.removeObserver(owner)
            for(cell in cells) {
                val pos=owner.pos().offset(cell.x,cell.y,cell.z)
                Eln.ghostManager.removeGhost(Coordonate(pos,world))
                world.setBlock(pos,Blocks.AIR.defaultBlockState(),quiet)
            }
        }
    }
    override fun measure(c: TestContext) { }
}
