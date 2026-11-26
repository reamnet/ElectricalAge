package mods.eln.gridnode

import mods.eln.Eln
import mods.eln.compat.BlockMeta
import mods.eln.ghost.GhostBlock
import mods.eln.misc.Coordonate
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block

/** Upgrade only this pole's reservations; never load chunks or overwrite neighbouring builds. */
object GridGhosts {
    /** Deferred repair is node-owned and stops once all affected chunks have been inspected. */
    class Repair(private val pole: GridElement): mods.eln.sim.IProcess {
        private var pending=true
        private var wait=0.0
        private var queued=false
        fun initialize() { pending=!repair(pole);wait=1.0 }
        override fun process(deltaT: Double) {
            if(!pending || queued)return
            wait-=deltaT
            if(wait>0)return
            val world=pole.coordonate().world() as? ServerLevel ?: return
            queued=true
            world.server.schedule(net.minecraft.server.TickTask(world.server.tickCount) {
                queued=false
                if(!pole.node.isDestructing && mods.eln.node.NodeManager.instance.getNodeFromCoordonate(pole.coordonate())===pole.node)
                    initialize()
                else pending=false
            })
        }
    }

    fun repair(pole: GridElement): Boolean {
        val owner=pole.coordonate()
        val world=owner.world() as? ServerLevel ?: return true
        check(world.server.isSameThread) { "Grid ghost repair requires the server thread" }
        val descriptor=pole.transparentNodeDescriptor
        val cells=descriptor.getGhostGroupAt(owner,pole.front) ?: return true
        var complete=true
        for(cell in cells.elementList) {
            if(cell.meta!=GhostBlock.tSelectableClearance)continue
            val pos=owner.pos().offset(cell.x,cell.y,cell.z)
            val chunk=world.chunkSource.getChunkNow(pos.x shr 4,pos.z shr 4)
            if(chunk==null) { complete=false;continue }
            val at=Coordonate(pos,world)
            val state=chunk.getBlockState(pos)
            val ghost=Eln.ghostManager.getGhost(at)
            if(ghost?.observatorCoordonate==owner) {
                if(state.block===Eln.ghostBlock && state.getValue(BlockMeta.META)==GhostBlock.tClearance)
                    world.setBlock(pos,state.setValue(BlockMeta.META,cell.meta),Block.UPDATE_CLIENTS)
            }else if(ghost==null && state.isAir) {
                Eln.ghostManager.createGhost(at,owner,descriptor.ghostGroupUuid,Eln.ghostBlock,cell.meta)
            }
        }
        return complete
    }
}
