package mods.eln.transparentnode.floodlight

import mods.eln.i18n.I18N
import mods.eln.misc.*
import mods.eln.misc.Utils.entityLivingHorizontalViewDirection
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.wiki.Data
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import mods.eln.gl.GL11
import java.util.*

class FloodlightDescriptor(val itemName: String, val obj: Obj3D, val motorized: Boolean) :
    TransparentNodeDescriptor(itemName, FloodlightElement::class.java, FloodlightRender::class.java) {

    private val base: Obj3D.Obj3DPart = obj.getPart("Lamp_Base_Cube.008")
    private val swivel: Obj3D.Obj3DPart = obj.getPart("Lamp_Swivel_Cube.014")
    private val head: Obj3D.Obj3DPart = obj.getPart("Lamp_Head_Cylinder.004")
    private val bulb1off: Obj3D.Obj3DPart = obj.getPart("Lamp1_OFF_Cylinder.003")
    private val bulb2off: Obj3D.Obj3DPart = obj.getPart("Lamp2_OFF_Cylinder.002")
    private val bulb1on: Obj3D.Obj3DPart = obj.getPart("Lamp1_ON_Cylinder.000")
    private val bulb2on: Obj3D.Obj3DPart = obj.getPart("Lamp2_ON_Cylinder.001")

    fun accepts(stack:ItemStack):Boolean {
        val d=Utils.getItemObject(stack)as? mods.eln.item.LampDescriptor?:return false
        return d.type==mods.eln.item.LampDescriptor.Type.LED||mods.eln.sixnode.lampsocket.SourceLampLife.technology(d)==5
    }
    override fun addInformation(itemStack: ItemStack, entityPlayer: Player?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        Collections.addAll(list, *I18N.tr(
            "A powerful lamp that specializes in\n" +
                    "the illumination of large spaces.\n" +
                    "Uses inserted light bulbs."
        )!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())

        if (motorized) list.add(I18N.tr("Intended for 240V bulb families."))
        else list.add(I18N.tr("Intended for 120V bulb families."))
        list.add(I18N.tr("Accepted lamp types: LED / halogen"))
    }

    override fun hasItemDraw()=true
    override fun drawItem(entity:Boolean)=preserveMatrix{drawPreview()}
    private fun drawPreview() {
        GL11.glRotated(90.0, 0.0, 1.0, 0.0)
        GL11.glTranslated(-0.5, -0.5, 0.5)
        base.draw()
        GL11.glTranslated(0.5, 0.5, -0.5)
        GL11.glRotated(90.0, 0.0, 1.0, 0.0)
        GL11.glTranslated(-0.5, -0.5, 0.5)
        swivel.draw()
        head.draw()
        bulb1off.draw()
        bulb2off.draw()
    }

    fun draw(swivelAngle: Double, headAngle: Double, bulb1: ItemStack?, bulb2: ItemStack?, powered: Boolean) {
        GL11.glTranslated(-0.5, -0.5, 0.5)
        base.draw()
        GL11.glTranslated(0.5, 0.5, -0.5)
        GL11.glRotated(swivelAngle + 90.0, 0.0, 1.0, 0.0)
        GL11.glTranslated(-0.5, -0.5, 0.5)
        swivel.draw()
        GL11.glTranslated(0.5, 0.5, -0.5)
        GL11.glRotated(-headAngle, 1.0, 0.0, 0.0)
        GL11.glTranslated(-0.5, -0.5, 0.5)
        head.draw()

        if (bulb1 != null && !bulb1.isEmpty) {
            if (powered) bulb1on.draw()
            else bulb1off.draw()
        }
        if (bulb2 != null && !bulb2.isEmpty) {
            if (powered) bulb2on.draw()
            else bulb2off.draw()
        }
    }

    override fun mustHaveFloor(): Boolean {
        return false
    }

    override fun getFrontFromPlace(side:Direction,entity:LivingEntity?)=side.inverse
    fun placement(stack:ItemStack,side:Direction,player:Player?):ItemStack=stack.copy().also{copy->
        val axis=side.inverse
        val facing=if(axis.isY){if(player!=null)entityLivingHorizontalViewDirection(player).inverse else Direction.XP}else Direction.YN
        mods.eln.generic.ItemNbt.edit(copy){it.putBoolean("initialPlacement",false);it.putInt("rotationAxis",axis.int);it.putInt("blockFacing",facing.int)}
    }
}
