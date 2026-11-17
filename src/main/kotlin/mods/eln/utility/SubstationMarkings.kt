package mods.eln.utility

import mods.eln.gl.GL11
import mods.eln.misc.UtilsClient

internal object SubstationMarkings {
    fun draw(kind: SubstationKind) {
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        UtilsClient.bindTexture(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln","model/transmissionmarkings/labels.png"))
        GL11.glEnable(GL11.GL_CULL_FACE);GL11.glColor3f(1f,1f,1f)
        fun plate(center: net.minecraft.world.phys.Vec3,right: net.minecraft.world.phys.Vec3,width: Double,height: Double,row: Int) {
            val up=net.minecraft.world.phys.Vec3(0.0,height/2,0.0);val half=right.scale(width/2)
            val vertices=listOf(center.subtract(half).subtract(up),center.add(half).subtract(up),center.add(half).add(up),center.subtract(half).add(up))
            val normal=right.cross(net.minecraft.world.phys.Vec3(0.0,1.0,0.0))
            // Give each side its own correctly oriented lettering. Merely
            // disabling culling would mirror the text when viewed from behind.
            for(back in listOf(false,true)) {
                val n=if(back)normal.scale(-1.0) else normal
                GL11.glNormal3f(n.x.toFloat(),n.y.toFloat(),n.z.toFloat());GL11.glBegin(GL11.GL_QUADS)
                val order=if(back)listOf(1,0,3,2) else listOf(0,1,2,3)
                for((i,index)in order.withIndex()) {
                    GL11.glTexCoord2f(if(i==0 || i==3)0f else 1f,(row+if(i<2)1f else 0f)/8f)
                    val v=vertices[index];GL11.glVertex3d(v.x,v.y,v.z)
                }
                GL11.glEnd()
            }
        }
        for(side in listOf(-1,1)) {
            val row=if(side<0) { if(kind.inputVolts>100_000)0 else 2 } else when { kind.outputVolts>100_000->7;kind.outputVolts>20_000->1;else->3 }
            plate(net.minecraft.world.phys.Vec3(side*2.62,kind.height-1.55,0.0),net.minecraft.world.phys.Vec3(0.0,0.0,-side.toDouble()),2.4,.44,row)
            for(end in listOf(-1,1))plate(net.minecraft.world.phys.Vec3(side*2.0,kind.height-1.8,end*(kind.spread+.52)),net.minecraft.world.phys.Vec3(end.toDouble(),0.0,0.0),1.65,.4,row)
            repeat(3) { phase->plate(net.minecraft.world.phys.Vec3(side*2.65,kind.height-.95,(phase-1)*kind.spread),net.minecraft.world.phys.Vec3(0.0,0.0,-side.toDouble()),.28,.28,4+phase) }
        }
    }
}
