package mods.eln.sixnode.lampsocket

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3

/** Bounded loaded-chunk queries; each refresh owns only short-lived native light leases. */
object DecorativeLampLighting {
    fun cells(style: DecorativeLampStyle, count: Int, origin: Vec3,
              side: mods.eln.misc.Direction, front: mods.eln.misc.LRDU,
              loaded: (BlockPos)->Boolean, open: (BlockPos)->Boolean, aim: LightAim = style.newAim(), drop: Double = 1.0): Set<BlockPos> {
        val cells = linkedSetOf<BlockPos>()
        for((index,head) in style.heads.take(count.coerceIn(0,style.bulbs)).withIndex()) {
            val aiming=style.aimHeadCount>0
            val h=style.aimHead(index)
            val point=if(aiming)aim.point(head,style.aimPivot(h),h) else head
            val source = style.worldPoint(origin,side,front,point.add(if(style.hangingWarehouse)drop else 0.0,0.0,0.0))
            val base=if(aiming)aim.rotate(if(aim.axialPan)Vec3(-1.0,0.0,0.0) else Vec3(1.0,0.0,0.0),h) else style.beam.normalize()
            val directions=mutableListOf(base)
            if(style==DecorativeLampStyle.SCONCE_CYLINDER)directions+=Vec3(.15,-1.0,0.0).normalize()
            if(style.wideBeam || style==DecorativeLampStyle.WAREHOUSE) {
                val u=base.cross(if(kotlin.math.abs(base.y)<.9)Vec3(0.0,1.0,0.0) else Vec3(1.0,0.0,0.0)).normalize()
                val v=base.cross(u).normalize()
                directions+=listOf(base.add(u.scale(.55)),base.add(u.scale(-.55)),base.add(v.scale(.40)),base.add(v.scale(-.40))).map { it.normalize() }
            }
            val beams=directions.map { style.worldDirection(side,front,it) }
            val cell = BlockPos.containing(source)
            if(!loaded(cell)) continue
            cells.add(cell)
            for(direction in beams) {
                var last: BlockPos? = null
                for(step in 1..style.range) {
                    val at = BlockPos.containing(source.add(direction.scale(step.toDouble())))
                    if(!loaded(at) || !open(at)) break
                    last = at
                    if(step%4==0) cells.add(at)
                }
                last?.let(cells::add)
            }
        }
        return cells
    }
    fun update(element: DecorativeLampElement, light: Int) {
        val coord = element.coordonate
        val world = coord.world() as? ServerLevel ?: return
        val loaded = HashMap<Long,Boolean>()
        val open = HashMap<BlockPos,Boolean>()
        val cells = cells(element.style,element.installedBulbs,Vec3(coord.x+.5,coord.y+.5,coord.z+.5),element.side,element.front,
            { pos -> loaded.getOrPut(((pos.x shr 4).toLong() shl 32) xor ((pos.z shr 4).toLong() and 0xffffffffL)) {
                world.chunkSource.getChunkNow(pos.x shr 4,pos.z shr 4)!=null } },
            { pos -> open.getOrPut(pos) { !world.getBlockState(pos).isSolidRender } },element.aim,element.suspension.drop)
        for(cell in cells) LightBlockEntity.addLight(world,cell.x,cell.y,cell.z,light,5)
    }
}
