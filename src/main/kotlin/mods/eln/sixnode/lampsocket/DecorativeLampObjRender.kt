package mods.eln.sixnode.lampsocket

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.item.LampDescriptor
import mods.eln.misc.UtilsClient
import kotlin.math.ceil

/** Original mesh parts, palette, lens states and item poses, recorded through the native draw shim. */
class DecorativeLampObjRender(val style: DecorativeLampStyle) : LampSocketObjRender {
    private val model = Eln.obj.getObj(style.model)
    override fun reach() = ceil(style.depth).toInt()
    private inline fun matrix(draw: ()->Unit) {
        GL11.glPushMatrix()
        try { draw() } finally { GL11.glPopMatrix() }
    }
    override fun drawItem(descriptor: LampSocketDescriptor) = matrix {
        if(!style.ceiling && !style.floor) GL11.glRotatef(-70f,0f,1f,0f)
        else GL11.glRotatef(if(style.ceiling)-90f else 90f,0f,0f,1f)
        val scale=1.35/style.depth
        GL11.glScaled(scale,scale,scale)
        GL11.glTranslated(-style.depth*.5,0.0,0.0)
        drawModel(0,style.bulbs,null)
    }
    override fun draw(render: LampSocketRender, distanceToPlayer: Double) = matrix {
        render.front.glRotateOnX()
        GL11.glRotatef(-90f,1f,0f,0f)
        drawModel(render.light.toInt(),(render as DecorativeLampRender).bulbCount,render.lampDescriptor)
    }
    private fun drawModel(light: Int, count: Int, bulb: LampDescriptor?) {
        GL11.glPushAttrib(-1)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            when(style) {
                DecorativeLampStyle.PORCH -> GL11.glColor3f(.20f,.14f,.09f)
                DecorativeLampStyle.HOLDER -> GL11.glColor3f(.88f,.86f,.80f)
                else -> GL11.glColor3f(.20f,.23f,.26f)
            }
            model.getPart("Base")?.drawNoBind()
            when(style) {
                DecorativeLampStyle.PORCH -> GL11.glColor3f(.28f,.20f,.12f)
                DecorativeLampStyle.CANDLE,DecorativeLampStyle.CRYSTAL,DecorativeLampStyle.SPUTNIK -> GL11.glColor3f(.66f,.45f,.16f)
                DecorativeLampStyle.WAGON,DecorativeLampStyle.LANTERN -> GL11.glColor3f(.12f,.13f,.14f)
                DecorativeLampStyle.SHOP,DecorativeLampStyle.FACTORY -> GL11.glColor3f(.75f,.77f,.78f)
                in DecorativeLampStyle.LUXURY_RINGS..DecorativeLampStyle.LUXURY_EMPIRE -> GL11.glColor3f(.77f,.58f,.28f)
                else -> GL11.glColor3f(.26f,.29f,.32f)
            }
            if(style == DecorativeLampStyle.RIG_CAGE || style == DecorativeLampStyle.RIG_LINEAR) GL11.glColor3f(.82f,.64f,.12f)
            model.getPart("Metal")?.drawNoBind()
            GL11.glColor3f(.65f,.68f,.71f)
            model.getPart("Trim")?.drawNoBind()
            if(style == DecorativeLampStyle.HALLOWEEN_GHOST) GL11.glColor3f(.93f,.94f,.96f)
            else GL11.glColor3f(.88f,.79f,.62f)
            GL11.glPushAttrib(-1)
            try { GL11.glDisable(GL11.GL_CULL_FACE); model.getPart("Shade")?.drawNoBind() }
            finally { GL11.glPopAttrib() }
            GL11.glColor3f(.92f,.35f,.035f)
            model.getPart("Accent")?.drawNoBind()
            for(i in 0 until style.bulbs) {
                val installed=i<count
                if(!installed && style.exposedBulbs) continue
                if(installed && light>=9) {
                    UtilsClient.disableLight()
                    when {
                        bulb==null || bulb.type==LampDescriptor.Type.LED || bulb.type==LampDescriptor.Type.eco -> GL11.glColor3f(.90f,.96f,1f)
                        bulb.vegetableGrowRate>0 -> GL11.glColor3f(1f,.78f,.92f)
                        else -> GL11.glColor3f(1f,.90f,.65f)
                    }
                    when(style) {
                        DecorativeLampStyle.CHRISTMAS_FAIRY -> GL11.glColor3f(1f,.76f,.35f)
                        DecorativeLampStyle.CHRISTMAS_COLOR -> when(i%4) {
                            0->GL11.glColor3f(1f,.10f,.05f);1->GL11.glColor3f(.12f,1f,.20f);2->GL11.glColor3f(.15f,.35f,1f);else->GL11.glColor3f(1f,.72f,.08f)
                        }
                        DecorativeLampStyle.CHRISTMAS_ICICLE,DecorativeLampStyle.HALLOWEEN_GHOST -> GL11.glColor3f(.65f,.80f,1f)
                        DecorativeLampStyle.CHRISTMAS_STAR -> GL11.glColor3f(1f,.80f,.20f)
                        DecorativeLampStyle.HALLOWEEN_PUMPKIN,DecorativeLampStyle.HALLOWEEN_JACK -> GL11.glColor3f(1f,.40f,.035f)
                        DecorativeLampStyle.HALLOWEEN_BAT -> GL11.glColor3f(.68f,.15f,1f)
                        else -> Unit
                    }
                    model.getPart("Light$i")?.drawNoBind()
                    UtilsClient.enableLight()
                } else {
                    GL11.glColor3f(.18f,.21f,.24f)
                    model.getPart("Light$i")?.drawNoBind()
                }
            }
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA)
            GL11.glDepthMask(false)
            GL11.glColor4f(.70f,.84f,.91f,.16f)
            model.getPart("Glass")?.drawNoBind()
        } finally { GL11.glPopAttrib() }
    }
}
