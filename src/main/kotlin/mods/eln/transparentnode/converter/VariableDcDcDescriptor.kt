package mods.eln.transparentnode.converter

import mods.eln.gl.GL11
import mods.eln.misc.Obj3D
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.wiki.Data
import net.minecraft.world.item.Item

class VariableDcDcDescriptor(name:String,model:Obj3D?,coreModel:Obj3D?,caseModel:Obj3D?):TransparentNodeDescriptor(name,VariableDcDcElement::class.java,VariableDcDcRender::class.java) {
    companion object {
        const val ID=129
        const val MIN_LOAD_HUM=.5
        const val COIL_BASE_HEIGHT=.031f
        const val COIL_OUTER_HALF_WIDTH=.0868f
        const val COIL_CORE_HALF_WIDTH=.0625f
        const val COIL_CENTER_Y=.0155f
        const val COIL_CENTER_Z=-.3125f
        const val COIL_STACK_MIN_Y=-.1575f
        const val COIL_STACK_MAX_Y=.25f
        const val COIL_STACK_CENTER_Y=(COIL_STACK_MIN_Y+COIL_STACK_MAX_Y)*.5f
        const val COIL_STACK_HEIGHT=COIL_STACK_MAX_Y-COIL_STACK_MIN_Y
    }
    private val main=requireNotNull(model?.getPart("main")) { "Converter base mesh must be indexed" }
    private val coil=requireNotNull(model?.getPart("sbire")) { "Converter winding mesh must be indexed" }
    private val core=coreModel?.getPart("fero")
    private val casing=caseModel?.getPart("Case")
    private val casingLeftDoor=caseModel?.getPart("DoorL")
    private val casingRightDoor=caseModel?.getPart("DoorR")
    init { voltageLevelColor=VoltageLevelColor.Neutral }
    override fun setParent(item:Item,damage:Int) { super.setParent(item,damage);Data.addWiring(newItemStack()) }
    override fun hasItemDraw()=true
    override fun drawItem(entity:Boolean)=draw(core,1,4,1f,1f,false,0f)
    internal fun draw(
        core: Obj3D.Obj3DPart?,
        priCableNbr: Int,
        secCableNbr: Int,
        primaryThickness: Float,
        secondaryThickness: Float,
        hasCasing: Boolean,
        doorOpen: Float
    ) {
        main?.draw()
        core?.draw()
        if (core != null) {
            drawCoils(priCableNbr, false, primaryThickness)
            drawCoils(secCableNbr, true, secondaryThickness)
        }

        if (hasCasing) {
            casing?.draw()
            casingLeftDoor?.draw(-doorOpen * 90, 0f, 1f, 0f)
            casingRightDoor?.draw(doorOpen * 90, 0f, 1f, 0f)
        }
    }

    private fun drawCoils(count: Int, secondary: Boolean, thickness: Float) {
        if (count == 0) return
        val wireScale = thickness.coerceIn(0.05f, 1.85f)
        val wireHeight = COIL_BASE_HEIGHT * wireScale
        val gap = wireHeight * 0.5f
        val pitch = wireHeight + gap
        val displayedCount = kotlin.math.min(count, ((COIL_STACK_HEIGHT + gap) / pitch).toInt().coerceAtLeast(1))
        val firstCenter = -pitch * (displayedCount - 1) / 2f
        val radialScale = (COIL_CORE_HALF_WIDTH + wireHeight) / COIL_OUTER_HALF_WIDTH
        GL11.glPushMatrix()
        if (secondary) GL11.glRotatef(180f, 0f, 1f, 0f)
        for (idx in 0 until displayedCount) {
            GL11.glPushMatrix()
            GL11.glTranslatef(0f, COIL_STACK_CENTER_Y + firstCenter + pitch * idx, COIL_CENTER_Z)
            GL11.glScalef(radialScale, wireScale, radialScale)
            GL11.glTranslatef(0f, -COIL_CENTER_Y, -COIL_CENTER_Z)
            coil?.draw()
            GL11.glPopMatrix()
        }
        GL11.glPopMatrix()
    }
}
