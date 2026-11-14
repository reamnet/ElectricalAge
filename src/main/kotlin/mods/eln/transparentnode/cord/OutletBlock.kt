package mods.eln.transparentnode.cord

import mods.eln.node.transparent.*
import mods.eln.misc.*
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.*
import net.minecraft.world.phys.shapes.*

/** State-only source outlet geometry avoids resident block-entity queries in collision workers. */
class OutletBlock(p: BlockBehaviour.Properties,val outlet: OutletDescriptor): TransparentNodeBlock(p,outlet) {
    init { registerDefaultState(defaultBlockState().setValue(RECESSED,false).setValue(HORIZONTAL,false).setValue(FACING,net.minecraft.core.Direction.NORTH)) }
    override fun createBlockStateDefinition(b: StateDefinition.Builder<Block,BlockState>) { super.createBlockStateDefinition(b);b.add(HORIZONTAL,FACING,RECESSED) }
    override fun stateForFront(front: Direction)=defaultBlockState().setValue(FACING,front.toEnumFacing())
    override fun placedState(front: Direction,world: Level,pos: BlockPos): BlockState {
        val owner=(mods.eln.node.NodeManager.instance.getNodeFromCoordonate(Coordonate(pos,world)) as? TransparentNode)?.element as? OutletElement
        return super.placedState(front,world,pos).setValue(HORIZONTAL,owner?.horizontal ?: false).setValue(RECESSED,owner?.recessed ?: false)
    }
    private val shapes=java.util.concurrent.ConcurrentHashMap<Int,VoxelShape>()
    private fun shape(state: BlockState): VoxelShape {
        val front=Direction.fromIntMinecraftSide(state.getValue(FACING).get3DDataValue());val h=state.getValue(HORIZONTAL)
        val recessed=state.getValue(RECESSED)
        return shapes.getOrPut(front.int*4+(if(h)1 else 0)+(if(recessed)2 else 0)) {
            val plate=Shapes.create(box(front,h,outlet.strip).move(if(recessed)front.toEnumFacing().stepX.toDouble() else 0.0,0.0,if(recessed)front.toEnumFacing().stepZ.toDouble() else 0.0))
            if(recessed)Shapes.or(Shapes.block(),plate) else plate
        }
    }
    override fun getShape(state: BlockState,world: net.minecraft.world.level.BlockGetter,pos: BlockPos,context: CollisionContext)=shape(state)
    override fun getCollisionShape(state: BlockState,world: net.minecraft.world.level.BlockGetter,pos: BlockPos,context: CollisionContext)=shape(state)
    override fun getOcclusionShape(state: BlockState)=if(state.getValue(RECESSED))Shapes.block() else Shapes.empty()
    override fun getLightDampening(state: BlockState)=if(state.getValue(RECESSED))15 else 0
    override fun propagatesSkylightDown(state: BlockState)=!state.getValue(RECESSED)
    override fun onBlockExploded(state: BlockState,world: net.minecraft.server.level.ServerLevel,pos: BlockPos,explosion: net.minecraft.world.level.Explosion) {
        val n=mods.eln.node.NodeManager.instance.getNodeFromCoordonate(Coordonate(pos,world)) as? TransparentNode
        if(n==null || !OutletWall.restore(n,false))super.onBlockExploded(state,world,pos,explosion)
    }
    override fun onDestroyedByPlayer(state: BlockState,world: Level,pos: BlockPos,player: net.minecraft.world.entity.player.Player,tool: net.minecraft.world.item.ItemStack,willHarvest: Boolean,fluid: net.minecraft.world.level.material.FluidState): Boolean {
        if(state.getValue(RECESSED)) {
            if(!world.isClientSide) {
                val n=mods.eln.node.NodeManager.instance.getNodeFromCoordonate(Coordonate(pos,world)) as? TransparentNode
                if(n!=null) { n.removedByPlayer=player as? net.minecraft.server.level.ServerPlayer;OutletWall.restore(n,true) }
            }
            return false
        }
        return super.onDestroyedByPlayer(state,world,pos,player,tool,willHarvest,fluid)
    }
    companion object {
        @JvmField val RECESSED=BooleanProperty.create("recessed")
        @JvmField val HORIZONTAL=BooleanProperty.create("horizontal")
        @JvmField val FACING=BlockStateProperties.HORIZONTAL_FACING
        fun box(front: Direction,horizontal: Boolean,strip: Boolean): AABB {
            val width=if(strip).40 else .17;val height=if(strip).22 else .31
            val w=if(horizontal)height else width;val h=if(horizontal)width else height
            val p=CordGeometry.world(front,Vec3(-w,-h,-.5)).add(.5,.5,.5);val q=CordGeometry.world(front,Vec3(w,h,-.36)).add(.5,.5,.5)
            return AABB(p,q)
        }
    }
}
