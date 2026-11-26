package mods.eln.utility.insulator

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.gridnode.*
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.registry.ElnDeviceRegistry
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.io.DataInputStream

/** ForkPatch single-circuit mounts, using the native grid connection and model rendering paths. */
class LineInsulatorDescriptor(val voltage: InsulatorVoltage,val feed: Boolean): GridDescriptor(voltage.title(feed),requireNotNull(Eln.obj.getObj(voltage.model)),
    LineInsulatorElement::class.java,LineInsulatorRender::class.java,"textures/wire.png",ElnDeviceRegistry.veryHighVoltageCableDescriptor) {
    private val model=requireNotNull(Eln.obj.getObj(voltage.model))
    init {
        if(voltage.length>1)ghostGroup=mods.eln.ghost.GhostGroup().apply {
            for(x in 1 until kotlin.math.ceil(voltage.length+.03).toInt())addElement(x,0,0,Eln.ghostBlock,mods.eln.ghost.GhostBlock.tSelectableClearance)
        }
    }
    override fun getFrontType()=TransparentNode.FrontType.BlockSide
    override fun mustHaveFloor()=false
    override fun rotationIsFixed()=true
    fun supported(world: Level,pos: BlockPos,front: Direction): Boolean {
        val support=pos.relative(front.toEnumFacing())
        return world.getBlockState(support).isFaceSturdy(world,support,front.inverse.toEnumFacing())
    }
    override fun checkCanPlace(coord: Coordonate?,front: Direction): String? {
        if(coord!=null && !supported(coord.world(),coord.pos(),front))return tr("Insulators need a solid mounting face")
        return super.checkCanPlace(coord,front)
    }
    fun drawInsulator(front: Direction) {
        GL11.glPushMatrix();GL11.glPushAttrib(0xFFFFF)
        try {
            front.glRotateXnRef();GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glColor3f(.42f,.45f,.47f);model.getPart("main")?.drawNoBind()
            GL11.glColor3f(.57f,.66f,.68f);model.getPart("Porcelain")?.drawNoBind()
            GL11.glColor3f(.60f,.54f,.35f);model.getPart("Clamp")?.drawNoBind()
            if(feed) { GL11.glColor3f(.25f,.28f,.30f);model.getPart("CableFeed")?.drawNoBind() }
        } finally { GL11.glPopAttrib();GL11.glPopMatrix() }
    }
    override fun shapeAt(entity: TransparentNodeEntity,collision: Boolean): net.minecraft.world.phys.shapes.VoxelShape? {
        if(voltage.length<=1)return null
        // Preserve the source long-insulator envelope; ordinary mounts use native model hitboxes.
        val front=entity.elementRender?.front ?: (entity.node as? TransparentNode)?.element?.front ?: Direction.XN
        val a=front.rotateFromXN(net.minecraft.world.phys.Vec3(-.5,-voltage.radius,-voltage.radius))
        val b=front.rotateFromXN(net.minecraft.world.phys.Vec3(-.5+voltage.length,voltage.radius,voltage.radius))
        return net.minecraft.world.phys.shapes.Shapes.create(net.minecraft.world.phys.AABB(
            minOf(a.x,b.x)+.5,minOf(a.y,b.y)+.5,minOf(a.z,b.z)+.5,
            maxOf(a.x,b.x)+.5,maxOf(a.y,b.y)+.5,maxOf(a.z,b.z)+.5))
    }
    override fun addInformation(stack: net.minecraft.world.item.ItemStack,player: net.minecraft.world.entity.player.Player?,list: MutableList<String>,advanced: Boolean) {
        list.add(tr("Single conductor; mount on floors, walls or ceilings"))
        list.add(tr("Rated insulation: %1$ V; span limit: %2$ m",voltage.volts,voltage.range))
        if(feed)list.add(tr("Base cable terminals connect directly to the same conductor"))
        if(voltage==InsulatorVoltage.LOW)list.add(tr("Select a service circuit before connecting spans"))
    }
    override fun drawItem(entity: Boolean) {
        GL11.glPushMatrix()
        try { val scale=.85/maxOf(.5,voltage.length);GL11.glScaled(scale,scale,scale);GL11.glTranslated(.5-voltage.length/2,0.0,0.0);drawInsulator(Direction.XN) }
        finally { GL11.glPopMatrix() }
    }
}
class LineInsulatorRender(entity: TransparentNodeEntity,descriptor: TransparentNodeDescriptor): GridRender(entity,descriptor) {
    private val insulator=descriptor as LineInsulatorDescriptor
    var terminal=32;var volts=0.0;var amps=0.0;var connected=false
    override fun draw() { drawCables();insulator.drawInsulator(front) }
    override fun networkUnserialize(input: DataInputStream) { super.networkUnserialize(input);terminal=input.readUnsignedByte();volts=input.readDouble();amps=input.readDouble();connected=input.readBoolean() }
}
