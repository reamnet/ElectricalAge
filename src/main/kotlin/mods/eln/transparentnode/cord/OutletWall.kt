package mods.eln.transparentnode.cord

import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.node.NodeManager
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeItem
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

/** Trusted world placement only: item settings can never supply a replacement wall. */
object OutletWall {
    @JvmStatic fun eligible(state: BlockState)=state.isSolidRender && !state.hasBlockEntity() && state.fluidState.isEmpty

    /** Null means ordinary surface placement; a rejected recessed transaction must not fall through. */
    @JvmStatic fun use(item: TransparentNodeItem,context: UseOnContext): InteractionResult? {
        val desc=item.descriptor as? OutletDescriptor ?: return null
        val world=context.level;val at=context.clickedPos;val wall=world.getBlockState(at)
        if(desc.strip || context.clickedFace.axis==net.minecraft.core.Direction.Axis.Y || !eligible(wall))return null
        val player=context.player ?: return InteractionResult.FAIL
        val held=context.itemInHand
        if(!world.mayInteract(player,at) || !player.mayUseItemAt(at,context.clickedFace,held))return InteractionResult.FAIL
        if(world.isClientSide)return InteractionResult.SUCCESS
        if(!place(item,world,at,Direction.fromEnumFacing(context.clickedFace),player,held,wall))return InteractionResult.FAIL
        if(!player.isCreative)held.shrink(1)
        return InteractionResult.SUCCESS
    }

    private fun place(item: TransparentNodeItem,world: Level,at: BlockPos,front: Direction,player: net.minecraft.world.entity.player.Player,held: net.minecraft.world.item.ItemStack,wall: BlockState): Boolean {
        val coord=Coordonate(at,world)
        if(front.isY || !eligible(wall) || world.getBlockState(at)!=wall || NodeManager.instance.getNodeFromCoordonate(coord)!=null)return false
        val node=TransparentNode()
        try {
            node.onBlockPlacedBy(coord,front,player,held)
            val owner=node.element as? OutletElement ?: error("Recessed placement requires an outlet")
            owner.installWall(wall)
            owner.placementPending(true)
            val snapshot=net.neoforged.neoforge.common.util.BlockSnapshot.create(world.dimension(),world,at)
            val state=item.outletPlacedState(world,at,front)
            if(!world.setBlock(at,state,2) || world.getBlockState(at)!=state || net.neoforged.neoforge.event.EventHooks.onBlockPlace(player,snapshot,front.toEnumFacing()) || NodeManager.instance.getNodeFromCoordonate(coord)!==node) {
                val resident=NodeManager.instance.getNodeFromCoordonate(coord)===node
                node.unload();if(resident)NodeManager.instance.removeNode(node)
                if(resident && world.getBlockState(at)==state)world.setBlock(at,wall,3)
                return false
            }
            owner.placementPending(false)
            world.updateNeighborsAt(at,state.block)
            node.reconnect();owner.needPublish()
            return true
        } catch(e: RuntimeException) {
            val resident=NodeManager.instance.getNodeFromCoordonate(coord)===node
            node.unload();if(resident)NodeManager.instance.removeNode(node)
            if(resident && (world.getBlockState(at).block as? OutletBlock)?.outlet===item.descriptor)world.setBlock(at,wall,3)
            throw e
        }
    }

    /** An opaque wall has no skylight inside its cell; its visible plate uses the exposed face. */
    @JvmStatic fun faceLight(world: net.minecraft.world.level.BlockAndLightGetter,pos: BlockPos,front: Direction,base: Int)=
        net.minecraft.util.LightCoordsUtil.max(base,net.minecraft.util.LightCoordsUtil.getLightCoords(world,pos.relative(front.toEnumFacing())))

    /** Removal is explicit; arbitrary block replacement is never permission to resurrect this wall. */
    @JvmStatic fun restore(node: TransparentNode,dropDevice: Boolean): Boolean {
        val owner=node.element as? OutletElement ?: return false
        if(!owner.recessed || node.isDestructing)return false
        val world=node.coordonate.world();val at=node.coordonate.pos()
        if((NodeManager.instance.getNodeFromCoordonate(node.coordonate) as? TransparentNode)!==node || (world.getBlockState(at).block as? OutletBlock)?.outlet!==owner.desc || !world.getBlockState(at).getValue(OutletBlock.RECESSED))return false
        val wall=owner.wallState;val before=world.getBlockState(at)
        owner.removeRecessed(dropDevice)
        if(world.getBlockState(at)==before && NodeManager.instance.getNodeFromCoordonate(node.coordonate)==null)world.setBlock(at,wall,3)
        return true
    }
}
