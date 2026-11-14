package mods.eln.sixnode.electricalcable
import mods.eln.node.six.*
import mods.eln.misc.*
import mods.eln.gl.GL11
import java.io.DataInputStream

class UtilityCableRender(e: SixNodeEntity,side: Direction,private val d: SixNodeDescriptor): ElectricalCableRender(e,side,d) {
    var temperature=20f;private set
    var paletteIndex=0;private set
    override fun publishUnserialize(s: DataInputStream) { super.publishUnserialize(s);temperature=s.readFloat();paletteIndex=s.readUnsignedByte().coerceAtMost((d as UtilityCableDescriptor).colorPalettes.lastIndex) }
    override fun glListDraw() {
        val desc=d as UtilityCableDescriptor
        if(desc.flatStyle && !desc.actsAsSingleConductor) {
            val jacket=when(desc.sizeLabel) {
                "14/2"->floatArrayOf(.90f,.89f,.84f);"14/3"->floatArrayOf(.62f,.80f,.93f)
                "12/2"->floatArrayOf(.93f,.82f,.19f);"12/3"->floatArrayOf(.77f,.33f,.74f)
                "10/2"->floatArrayOf(.92f,.49f,.16f);"10/3"->floatArrayOf(.95f,.63f,.78f)
                else->floatArrayOf(.12f,.12f,.12f)
            }
            GL11.glColor3f(jacket[0],jacket[1],jacket[2])
        }
        super.glListDraw()
        if(!desc.flatStyle || desc.actsAsSingleConductor)return
        val colors=desc.colorPalettes[paletteIndex].conductorColors
        val band=desc.render.width*.82f/colors.size
        val core=mods.eln.cable.CableRenderDescriptor("eln","sprites/cable.png",band*.7f*16f,.15f)
        val ct=mods.eln.cable.CableRender.connectionType(this,side)
        for(i in colors.indices) {
            GL11.glPushMatrix()
            try {
                GL11.glTranslatef(desc.render.height,0f,-desc.render.width*.41f+band*(i+.5f))
                Utils.setGlColorFromDye(colors[i],1f)
                mods.eln.cable.CableRender.drawCable(core,connectedSide,ct)
                // The native junction cap resets its tint. Put the core-color mark above that cap,
                // rather than hiding it inside the white node or drawing oversized white core caps.
                val mask=connectedSide.mask
                val capped=mask==0 || Integer.bitCount(mask)==1 || ((mask and 3)!=0 && (mask and 12)!=0)
                val x=(if(capped)1f/16f else 0f)+.002f
                GL11.glBegin(GL11.GL_QUADS);GL11.glNormal3f(1f,0f,0f)
                GL11.glVertex3f(x,-.04f,-band*.35f);GL11.glVertex3f(x,.04f,-band*.35f)
                GL11.glVertex3f(x,.04f,band*.35f);GL11.glVertex3f(x,-.04f,band*.35f)
                GL11.glEnd()
            }finally { GL11.glPopMatrix() }
        }
        GL11.glColor3f(1f,1f,1f)
    }
    override fun draw() {
        super.draw()
        if(temperature>550) {
            GL11.glPushAttrib(0xFFFFF)
            try { UtilsClient.disableLight();UtilsClient.enableBlend();GL11.glColor4f(1f,.55f,.12f,.8f);glListCall() }finally { GL11.glPopAttrib() }
        }
    }
}
