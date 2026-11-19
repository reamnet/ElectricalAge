package mods.eln.utility
import mods.eln.Eln
import mods.eln.gl.GL11
import net.minecraft.world.phys.Vec3

internal object PoleTransformerVisual {
    private val model by lazy { requireNotNull(Eln.obj.getObj("distributionpole")) }
    fun draw(watts: Int) {
        val shape=PoleTransformerShape.of(watts)
        GL11.glPushMatrix()
        try { GL11.glTranslated(PoleTransformerShape.BACK,PoleTransformerShape.TOP,0.0);GL11.glScaled(shape.diameter,shape.height,shape.diameter);GL11.glTranslated(.015,-PoleTransformerShape.TOP,0.0);GL11.glColor3f(.53f,.55f,.54f);model.getPart("Transformer")?.drawNoBind() }finally { GL11.glPopMatrix() }
        GL11.glPushMatrix()
        try { GL11.glTranslated(shape.bushing.x+.33,0.0,0.0);GL11.glColor3f(.32f,.43f,.44f);model.getPart("TransformerPin")?.drawNoBind() }finally { GL11.glPopMatrix() }
        PoleLeadVisual.draw(Vec3(-.68,5.85,0.0),shape.bushing)
        // Route out past the can before reaching the rack on the opposite side of the pole.
        for(i in 0..2) {
            val from=shape.point(Vec3(-.60,5.15+i*.05,.20));val escape=Vec3(-.24-.64*shape.diameter,from.y,.38*shape.diameter)
            val corner=Vec3(escape.x,4.45+i*.12,-.78);val target=mods.eln.gridnode.ServiceCableMount.point(40+i,0)
            listOf(from,escape,corner,Vec3(0.0,corner.y,-.95),target).zipWithNext().forEach { (a,b)->PoleLeadVisual.draw(a,b) }
        }
    }
}
