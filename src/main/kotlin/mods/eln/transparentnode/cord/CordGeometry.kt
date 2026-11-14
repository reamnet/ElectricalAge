package mods.eln.transparentnode.cord

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.misc.Direction
import mods.eln.misc.preserveMatrix
import net.minecraft.world.phys.Vec3

object CordGeometry {
    fun world(front: Direction,v: Vec3): Vec3 { val r=DoubleArray(3);front.right().applyTo(r,v.x);front.applyTo(r,v.z);return Vec3(r[0],v.y,r[2]) }
    fun offset(socket: Int,strip: Boolean)=if(strip)Vec3((socket%3-1)*.25,if(socket<3).02 else .22,0.0) else Vec3(0.0,socket*.26,0.0)
    fun tip(socket: Int,horizontal: Boolean,strip: Boolean,recessed: Boolean=false,cabinet: Boolean=false): Vec3 {
        if(cabinet)return CompactSocketGeometry.tip(socket)
        val v=Vec3(0.0,-.1,-.224).add(offset(socket,strip))
        val rotated=if(horizontal)Vec3(-v.y,v.x,v.z) else v
        return if(recessed)rotated.add(0.0,0.0,1.0) else rotated
    }
    fun plug(front: Direction,rating: CordRating,socket: Int,horizontal: Boolean,strip: Boolean)=preserveMatrix {
        front.inverse.glRotateZnRef();if(horizontal)GL11.glRotatef(90f,0f,0f,1f)
        val v=offset(socket,strip);GL11.glTranslated(v.x,v.y,v.z);plug(rating)
    }
    fun plug(rating: CordRating)=preserveMatrix {
        val model=requireNotNull(Eln.obj.getObj(when { rating==CordRating.V240_30->"nema1030plug";rating.industrial->"industrialmachineplug";rating==CordRating.V120->"nema520plug";else->"nema620plug" }))
        GL11.glPushAttrib(0xFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glColor3f(.075f,.080f,.085f);requireNotNull(model.getPart("Case")).drawNoBind()
            GL11.glColor3f(.14f,.15f,.16f);requireNotNull(model.getPart("Grip")).drawNoBind()
            GL11.glColor3f(.73f,.61f,.31f);requireNotNull(model.getPart("Blades")).drawNoBind()
            val c=rating.color;GL11.glColor3f(c[0],c[1],c[2]);requireNotNull(model.getPart("Badge")).drawNoBind()
        }finally { GL11.glPopAttrib() }
    }
}
