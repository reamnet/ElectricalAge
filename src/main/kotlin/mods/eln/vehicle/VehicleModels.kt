package mods.eln.vehicle
import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.client.power.PowerPanelDisplay
private inline fun preserveMatrix(block:()->Unit) {GL11.glPushMatrix();try{block()}finally{GL11.glPopMatrix()}}
object VehicleModels {
    fun draw(kind: RoadVehicleKind,charge: Double=0.0,plugged: Boolean=false,spin: Float=0f)=preserveMatrix {
        val model=requireNotNull(Eln.obj.getObj(if(kind==RoadVehicleKind.CART)"electric_golf_cart" else "arc_runner_coupe"))
        GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun p(name: String,r: Float,g: Float,b: Float) { GL11.glColor3f(r,g,b);model.getPart(name)?.drawNoBind() }
            p("Body",if(kind==RoadVehicleKind.CART).08f else .12f,if(kind==RoadVehicleKind.CART).52f else .26f,if(kind==RoadVehicleKind.CART).33f else .56f)
            p("Trim",.09f,.11f,.14f);p("Seats",.22f,.24f,.27f);p("Frame",.56f,.6f,.64f);p("Roof",.78f,.8f,.82f);p("Headlights",.92f,.96f,1f);p("TailLights",.9f,.08f,.04f)
            for(i in 0..3)preserveMatrix {
                val x=(if(i%2==0)1 else -1)*(if(kind==RoadVehicleKind.CART).77 else .86);val z=if(kind==RoadVehicleKind.CART) { if(i<2).9 else -.9 } else { if(i<2)1.25 else -1.23 }
                GL11.glTranslated(x,.29,z);GL11.glRotatef(spin,1f,0f,0f);p("Wheel$i",.07f,.08f,.09f);p("Rim$i",.64f,.68f,.73f)
            }
            PowerPanelDisplay.lamp(model,"ChargeLamp",plugged,.16f,.9f,.45f)
            PowerPanelDisplay.equipmentNumber(model,charge*100,true,"BatteryDisplay")
            // Draw the glazing after the cabin, with a light tint and no opaque depth mask.
            GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA)
            GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDepthMask(false)
            GL11.glColor4f(.62f,.82f,.9f,.16f);model.getPart("Glass")?.drawNoBind()
            Unit
        } finally { GL11.glPopAttrib() }
    }
}
