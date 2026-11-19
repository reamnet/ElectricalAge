package mods.eln.transparentnode.solarpanel

import mods.eln.Eln
import mods.eln.compat.BlockMeta
import mods.eln.ghost.GhostBlock
import mods.eln.ghost.GhostGroup
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.node.NodeManager
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel

/** New placements carry their shape to clients; old native cubes are upgraded only after their owner loads. */
object SolarGhostShapes {
    @JvmStatic fun flatFootprint(original:GhostGroup?,front:Direction):GhostGroup? {
        if(original==null)return null
        val group=GhostGroup()
        for(cell in original.elementList)group.addElement(cell.x,cell.y,cell.z,cell.block,
            if((cell.block==null || cell.block===Eln.ghostBlock) && cell.meta==GhostBlock.tCube)GhostBlock.tFlatSolar else cell.meta)
        return group.newRotate(front)
    }

    /** Return false only for a deferred resident-chunk probe. No simulator callback or chunk request is involved. */
    @JvmStatic fun repair(world:ServerLevel,pos:BlockPos):Boolean {
        check(world.server.isSameThread) { "Solar ghost repair requires the server thread" }
        if(world.chunkSource.getChunkNow(pos.x shr 4,pos.z shr 4)==null)return false
        val owner=Coordonate(pos,world)
        val panel=NodeManager.instance?.getTransparentNodeFromCoordinate(owner) as? SolarPanelElement ?: return true
        val descriptor=panel.transparentNodeDescriptor as? SolarPanelDescriptor ?: return true
        if(descriptor.canRotate || !panel.roofMountKnown || panel.roofMount.pitched)return true
        val cells=descriptor.getGhostGroupAt(owner,panel.front) ?: return true
        var complete=true
        for(cell in cells.elementList) {
            if(cell.meta!=GhostBlock.tFlatSolar)continue
            val at=pos.offset(cell.x,cell.y,cell.z)
            val chunk=world.chunkSource.getChunkNow(at.x shr 4,at.z shr 4)
            if(chunk==null) { complete=false;continue }
            val ghost=Eln.ghostManager.getGhost(Coordonate(at,world)) ?: continue
            if(ghost.observatorCoordonate!=owner || ghost.uuid!=descriptor.ghostGroupUuid)continue
            val state=chunk.getBlockState(at)
            if(state.block===Eln.ghostBlock && state.getValue(BlockMeta.META)==GhostBlock.tCube)
                world.setBlock(at,state.setValue(BlockMeta.META,GhostBlock.tFlatSolar),2)
        }
        return complete
    }
}
