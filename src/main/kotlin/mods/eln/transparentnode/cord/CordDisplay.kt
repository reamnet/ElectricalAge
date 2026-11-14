package mods.eln.transparentnode.cord

import mods.eln.gl.GL11
import mods.eln.misc.Direction
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.phys.*
import java.io.*

/** Cached terrain-following mesh; drawing never advances simulation or paid ownership. */
class CordDisplay {
    var endpoint: BlockPos?=null;private set
    var front=Direction.ZP;private set
    var rating=CordRating.V120;private set
    var socket=0;private set
    var horizontal=false;private set
    var strip=false;private set
    var recessed=false;private set
    var entry=Vec3.ZERO;private set
    var state="UNPLUGGED";private set
    private var points=emptyList<Vec3>()
    private var mesh=DoubleArray(0)
    private var next=0L
    @JvmField var rebuilds=0
    fun active()=endpoint!=null
    fun clear() { endpoint=null;points=emptyList();mesh=DoubleArray(0);next=0 }
    fun read(s: DataInputStream) {
        val p=BlockPos.of(s.readLong());val f=Direction.fromInt(s.readUnsignedByte()) ?: throw IOException("Invalid cord facing")
        val r=CordRating.entries.getOrNull(s.readUnsignedByte()) ?: throw IOException("Invalid cord rating")
        val index=s.readUnsignedByte();val h=s.readBoolean();val st=s.readBoolean();val rc=s.readBoolean();val e=Vec3(s.readDouble(),s.readDouble(),s.readDouble());val status=s.readUTF()
        if(st && rc || f.isY || index !in 0 until (if(st)6 else 2) || !e.x.isFinite() || !e.y.isFinite() || !e.z.isFinite() || e.lengthSqr()>4)throw IOException("Invalid cord endpoint")
        if(endpoint!=p || front!=f || rating!=r || socket!=index || horizontal!=h || strip!=st || recessed!=rc || entry!=e) { points=emptyList();mesh=DoubleArray(0);next=0 }
        endpoint=p;front=f;rating=r;socket=index;horizontal=h;strip=st;recessed=rc;entry=e;state=status
    }
    fun bounds(pos: BlockPos,base: AABB)=endpoint?.let { base.minmax(AABB(it).inflate(1.0)) } ?: base
    fun draw(world: Level?,pos: BlockPos) {
        val target=endpoint ?: return;if(world==null)return
        val origin=Vec3.atCenterOf(pos);val start=Vec3.atCenterOf(target).add(CordGeometry.world(front,CordGeometry.tip(socket,horizontal,strip,recessed)));val end=origin.add(entry)
        if(start.distanceTo(end)>10)return
        if(points.isEmpty() || world.gameTime>=next) {
            next=world.gameTime+20
            val sampled=CordPath.samples(start,end) { x,z,y->CordPath.floor(world,x,z,y,pos) }
            if(sampled!=points) {
                points=sampled;rebuilds++
                val rings=CordPath.rings(sampled,.018);val vertices=ArrayList<Double>()
                for(i in 1 until rings.size)for(j in 0..3) {
                    val k=(j+1)%4;val a=rings[i-1][j];val b=rings[i-1][k];val c=rings[i][k];val d=rings[i][j];val normal=b.subtract(a).cross(c.subtract(a)).normalize()
                    for(v in listOf(a,b,c,d)) { vertices.add(v.x-origin.x);vertices.add(v.y-origin.y);vertices.add(v.z-origin.z);vertices.add(normal.x);vertices.add(normal.y);vertices.add(normal.z) }
                }
                mesh=vertices.toDoubleArray()
            }
        }
        GL11.glPushAttrib(0xFFFF)
        try { GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glColor3f(.035f,.04f,.045f);GL11.glBegin(GL11.GL_QUADS)
            var i=0;while(i<mesh.size) { GL11.glNormal3f(mesh[i+3].toFloat(),mesh[i+4].toFloat(),mesh[i+5].toFloat());GL11.glVertex3d(mesh[i],mesh[i+1],mesh[i+2]);i+=6 };GL11.glEnd()
        }finally { GL11.glPopAttrib() }
    }
}
