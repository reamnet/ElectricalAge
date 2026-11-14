package mods.eln.utility

import mods.eln.Eln
import mods.eln.gridnode.GridDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.registry.ElnDeviceRegistry
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

/** Wall service geometry with native grid terminals; floor power cabinets remain separate. */
open class ServiceWallDescriptor(name: String,model: String,element: Class<*>,render: Class<*>): GridDescriptor(name,requireNotNull(Eln.obj.getObj(model)),element,render,"textures/wire.png",ElnDeviceRegistry.veryHighVoltageCableDescriptor) {
    override fun blockProperties(p: net.minecraft.world.level.block.state.BlockBehaviour.Properties)=super.blockProperties(p).dynamicShape()
    override fun getFrontType()=TransparentNode.FrontType.BlockSideInv
    override fun mustHaveFloor()=false
    override fun mustHaveWallFrontInverse()=true
    override fun rotationIsFixed()=true
    override fun checkCanPlace(c: Coordonate?,front: Direction)=if(front.isY)tr("Place this cabinet against a vertical wall") else super.checkCanPlace(c,front)
    fun acceptsCable(front: Direction,side: Direction,lrdu: LRDU)=side.getLRDUGoingTo(front.inverse)==lrdu
    fun checkWallSupport(e: TransparentNodeElement) {
        val c=e.node.coordonate;val p=c.pos().relative(e.front.inverse.toEnumFacing());val w=c.world()
        if(w is net.minecraft.server.level.ServerLevel && w.chunkSource.getChunkNow(p.x shr 4,p.z shr 4)==null)return
        if(!w.getBlockState(p).isSolidRender())e.selfDestroy()
    }
    open fun wallBox(front: Direction)=when(front) {
        Direction.XN->AABB(.67,0.0,.08,1.0,1.0,.92);Direction.XP->AABB(0.0,0.0,.08,.33,1.0,.92)
        Direction.ZN->AABB(.08,0.0,.67,.92,1.0,1.0);else->AABB(.08,0.0,0.0,.92,1.0,.33)
    }
    private val shapes=java.util.concurrent.ConcurrentHashMap<Direction,VoxelShape>()
    override fun hasOrientedShape()=true
    override fun orientedShape(front: Direction): VoxelShape=shapes.computeIfAbsent(front) { Shapes.create(wallBox(it)) }
    override fun addCollisionBoxesToList(query: AABB,boxes: MutableList<Any?>,world: Level?,x: Int,y: Int,z: Int) {
        val front=(world?.getBlockEntity(BlockPos(x,y,z)) as? TransparentNodeEntity)?.front() ?: return
        val box=wallBox(front).move(x.toDouble(),y.toDouble(),z.toDouble());if(query.intersects(box))boxes.add(box)
    }
}
