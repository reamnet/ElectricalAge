package mods.eln.node.transparent

import mods.eln.i18n.I18N.tr
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB

/** Separate wall variants preserve the floor equipment's identities and wiring. */
open class PowerCabinetDescriptor(name: String, element: Class<*>, render: Class<*>, val wallMounted: Boolean) :
    TransparentNodeDescriptor(name, element, render) {
    override fun getFrontType() = if (wallMounted) TransparentNode.FrontType.BlockSideInv else super.getFrontType()
    override fun mustHaveFloor() = !wallMounted
    override fun mustHaveWallFrontInverse() = wallMounted
    override fun checkCanPlace(coord: Coordonate?, front: Direction): String? {
        if (wallMounted && front.isY) return tr("Place this cabinet against a vertical wall")
        return super.checkCanPlace(coord, front)
    }
    fun connectionPlane(front: Direction) = if (wallMounted) front.inverse else Direction.YN
    fun acceptsCable(front: Direction, side: Direction, lrdu: LRDU) = side.getLRDUGoingTo(connectionPlane(front)) == lrdu
    fun checkWallSupport(element: TransparentNodeElement) {
        if (!wallMounted) return
        val coord = element.node?.coordonate ?: return
        val support = coord.pos().relative(element.front.inverse.toEnumFacing())
        val world = coord.world()
        if (world is net.minecraft.server.level.ServerLevel && world.chunkSource.getChunkNow(support.x shr 4, support.z shr 4) == null) return
        if (!holds(coord, element.front.inverse)) element.selfDestroy()
    }
    fun wallBox(front: Direction): AABB = when (front) {
        Direction.XN -> AABB(.67, .04, .04, 1.0, .96, .96)
        Direction.XP -> AABB(0.0, .04, .04, .33, .96, .96)
        Direction.ZN -> AABB(.04, .04, .67, .96, .96, 1.0)
        Direction.ZP -> AABB(.04, .04, 0.0, .96, .96, .33)
        else -> error("Wall cabinets need a horizontal front")
    }
    override fun addCollisionBoxesToList(query: AABB, boxes: MutableList<Any?>, world: Level?, x: Int, y: Int, z: Int) {
        if (!wallMounted) { super.addCollisionBoxesToList(query, boxes, world, x, y, z); return }
        val tile = world?.getBlockEntity(BlockPos(x, y, z)) as? TransparentNodeEntity
        val front = tile?.elementRender?.front ?: (tile?.node as? TransparentNode)?.element?.front ?: return
        val box = wallBox(front).move(x.toDouble(), y.toDouble(), z.toDouble())
        if (query.intersects(box)) boxes.add(box)
    }
}
