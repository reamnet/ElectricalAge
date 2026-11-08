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
    init { registerDefaultState(defaultBlockState().setValue(HORIZONTAL,false).setValue(FACING,net.minecraft.core.Direction.NORTH)) }
    override fun createBlockStateDefinition(b: StateDefinition.Builder<Block,BlockState>) { super.createBlockStateDefinition(b);b.add(HORIZONTAL,FACING) }
    override fun stateForFront(front: Direction)=defaultBlockState().setValue(FACING,front.toEnumFacing())
    override fun placedState(front: Direction,world: Level,pos: BlockPos): BlockState {
        val owner=(mods.eln.node.NodeManager.instance.getNodeFromCoordonate(Coordonate(pos,world)) as? TransparentNode)?.element as? OutletElement
        return super.placedState(front,world,pos).setValue(HORIZONTAL,owner?.horizontal ?: false)
    }
    private val shapes=java.util.concurrent.ConcurrentHashMap<Int,VoxelShape>()
    private fun shape(state: BlockState): VoxelShape {
        val front=Direction.fromIntMinecraftSide(state.getValue(FACING).get3DDataValue());val h=state.getValue(HORIZONTAL)
        return shapes.getOrPut(front.int*2+if(h)1 else 0) { Shapes.create(box(front,h,outlet.strip)) }
    }
    override fun getShape(state: BlockState,world: net.minecraft.world.level.BlockGetter,pos: BlockPos,context: CollisionContext)=shape(state)
    override fun getCollisionShape(state: BlockState,world: net.minecraft.world.level.BlockGetter,pos: BlockPos,context: CollisionContext)=shape(state)
    companion object {
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
