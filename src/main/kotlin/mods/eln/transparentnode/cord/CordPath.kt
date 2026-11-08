package mods.eln.transparentnode.cord

import mods.eln.gl.GL11
import mods.eln.misc.*
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.io.DataInputStream
import kotlin.math.*

/** A loose, terrain-following visual; no blocks are placed for the cord itself. */
object CordPath {
    fun samples(start: Vec3,end: Vec3,ground: (Double,Double,Double)->Double): List<Vec3> {
        val dx=end.x-start.x;val dz=end.z-start.z;val length=hypot(dx,dz)
        if(!length.isFinite() || !start.y.isFinite() || !end.y.isFinite())return emptyList()
        val n=(ceil(length*6).toInt()).coerceIn(8,64)
        if(length<1e-6)return (0..n).map { start.lerp(end,it.toDouble()/n) }
        val path=(0..n).map { i ->
            if(i==0)return@map start
            if(i==n)return@map end
            val t=i.toDouble()/n
            val bow=sin(PI*t)*min(.35,length*.06)
            val x=start.x+dx*t+if(length>1e-6)-dz/length*bow else 0.0
            val z=start.z+dz*t+if(length>1e-6)dx/length*bow else 0.0
            Vec3(x,start.y+(end.y-start.y)*t-sin(PI*t)*1.5,z)
        }
        val support=path.mapIndexed { i,p ->
            if(i==0 || i==n)p.y else {
                val floor=ground(p.x,p.z,max(start.y,end.y))+.025
                if(floor.isFinite())max(floor,p.y) else p.y
            }
        }
        // An upper parabolic envelope anticipates steps and rounds the climb/drop.
        // Unlike averaging or a free spline it never dips below sampled obstacles.
        return path.mapIndexed { i,p ->
            if(i==0)start else if(i==n)end else {
                val y=support.indices.maxOf { j ->
                    val distance=(i-j)*length/n
                    support[j]-distance*distance/(2*.45)
                }
                Vec3(p.x,y,p.z)
            }
        }
    }
    fun floor(world: Level,x: Double,z: Double,ceiling: Double,ignore: BlockPos?=null): Double {
        if(!x.isFinite() || !z.isFinite() || !ceiling.isFinite())return Double.NEGATIVE_INFINITY
        val bx=kotlin.math.floor(x).toInt();val bz=kotlin.math.floor(z).toInt()
        if(world is net.minecraft.server.level.ServerLevel) { if(world.chunkSource.getChunkNow(bx shr 4,bz shr 4)==null)return Double.NEGATIVE_INFINITY } else if(!world.hasChunkAt(BlockPos(bx,0,bz)))return Double.NEGATIVE_INFINITY
        val start=floor(ceiling).toInt()+1
        for(y in start downTo start-5) {
            val p=BlockPos.containing(x,y.toDouble(),z)
            if(p==ignore)continue
            val boxes=world.getBlockState(p).getCollisionShape(world,p).toAabbs()
            val top=boxes.filter { x-p.x>=it.minX && x-p.x<=it.maxX && z-p.z>=it.minZ && z-p.z<=it.maxZ }.maxOfOrNull { it.maxY }
            if(top!=null && y+top<=ceiling+.6)return y+top
        }
        return Double.NEGATIVE_INFINITY
    }
    fun draw(points: List<Vec3>,origin: Vec3,color: FloatArray?=null,radius: Double=.018) {
        if(points.size<2)return
        GL11.glPushAttrib(0xFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glColor3f(color?.get(0) ?: .035f,color?.get(1) ?: .04f,color?.get(2) ?: .045f)
            val rings=rings(points,radius)
            GL11.glBegin(GL11.GL_QUADS)
            for(i in 1 until rings.size) {
                for(j in 0..3) {
                    val next=(j+1)%4
                    val a=rings[i-1][j];val b=rings[i-1][next];val c=rings[i][next];val d=rings[i][j]
                    val normal=b.subtract(a).cross(c.subtract(a)).normalize()
                    GL11.glNormal3f(normal.x.toFloat(),normal.y.toFloat(),normal.z.toFloat())
                    for(k in listOf(a,b,c,d))GL11.glVertex3d(k.x-origin.x,k.y-origin.y,k.z-origin.z)
                }
            }
            GL11.glEnd()
        } finally { GL11.glPopAttrib() }
    }
    /** Shared rings and transported frames avoid disconnected elbows and vertical-frame flips. */
    fun rings(points: List<Vec3>,radius: Double): List<List<Vec3>> {
        var previous=Vec3.ZERO
        return points.mapIndexed { i,p ->
            val delta=points[min(i+1,points.lastIndex)].subtract(points[max(0,i-1)])
            val tangent=if(delta.lengthSqr()>1e-12)delta.normalize() else Vec3(0.0,1.0,0.0)
            var u=previous.subtract(tangent.scale(previous.dot(tangent)))
            if(u.lengthSqr()<1e-12)u=tangent.cross(if(abs(tangent.y)>.9)Vec3(1.0,0.0,0.0) else Vec3(0.0,1.0,0.0))
            if(u.lengthSqr()<1e-12)u=Vec3(1.0,0.0,0.0)
            u=u.normalize();previous=u
            val v=tangent.cross(u).normalize()
            listOf(p.add(u.scale(radius)),p.add(v.scale(radius)),p.subtract(u.scale(radius)),p.subtract(v.scale(radius)))
        }
    }
}
