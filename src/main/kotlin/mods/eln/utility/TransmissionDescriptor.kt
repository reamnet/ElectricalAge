package mods.eln.utility

import mods.eln.Eln
import mods.eln.ghost.*
import mods.eln.gl.GL11
import mods.eln.gridnode.GridDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.registry.ElnDeviceRegistry
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class TransmissionDescriptor(val kind: TransmissionKind) : GridDescriptor(kind.title,
    requireNotNull(Eln.obj.getObj(kind.model)),TransmissionElement::class.java,TransmissionRender::class.java,
    "textures/wire.png",ElnDeviceRegistry.veryHighVoltageCableDescriptor) {
    private val model = requireNotNull(Eln.obj.getObj(kind.model))
    init {
        ghostGroup = GhostGroup().apply {
            for (y in 1..kind.height) addElement(0,y,0,Eln.ghostBlock,GhostBlock.tSelectableClearance)
            val arm = kotlin.math.ceil(kind.spread).toInt()
            // A primary span can turn the crossarm independently of the service front.
            // Reserve its complete swept envelope at placement; linking never overwrites blocks.
            for (x in -arm..arm) for (z in -arm..arm) for (y in kind.height-2..kind.height)
                if (x != 0 || z != 0) addElement(x,y,z,Eln.ghostBlock,GhostBlock.tSelectableClearance)
            // Native mounting rotates this rack cell; leave the lamp-head cells free.
            addElement(0,5,-1,Eln.ghostBlock,GhostBlock.tSelectableClearance)
        }
    }
    override fun rotationIsFixed() = true
    fun drawEquipment(front: Direction,lamp: TransmissionLampDisplay? = null,primaryAngle: Float = TransmissionKind.facingAngle(front)) {
        GL11.glPushMatrix(); GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glPushMatrix()
            try {
                GL11.glRotatef(primaryAngle,0f,1f,0f)
                fun part(name: String,r: Float,g: Float,b: Float) { GL11.glColor3f(r,g,b); model.getPart(name)?.drawNoBind() }
                part("main",.48f,.51f,.54f)
                part("Porcelain",.68f,.74f,.72f)
                part("Bus",.19f,.22f,.24f)
            } finally { GL11.glPopMatrix() }
            front.glRotateXnRef()
            TransmissionPoleVisual.draw(lamp)
        } finally { GL11.glPopAttrib(); GL11.glPopMatrix() }
    }
    override fun drawItem(entity: Boolean) {
        GL11.glPushMatrix()
        try {
            val scale = 1.6/kind.height
            GL11.glScaled(scale,scale,scale); GL11.glTranslated(0.0,-kind.height*.45,0.0)
            GL11.glRotatef(-35f,0f,1f,0f); drawEquipment(Direction.XN)
        } finally { GL11.glPopMatrix() }
    }
    override fun addInformation(stack: ItemStack?,player: Player?,list: MutableList<String>,advanced: Boolean) {
        list.add(tr("Three independent phase circuits; nominal input: %1$ kV",kind.volts/1000))
        list.add(tr("Steel structure: %1$ blocks tall",kind.height))
        list.add(tr("Upper crossarm clearance is reserved for automatic span alignment"))
        if (kind.riser) list.add(tr("Base cables: left A, right B, rear C; overhead links connect all three phases"))
        list.add(tr("Secondary service rack is independent; this pole does not transform voltage"))
        list.add(tr("Accepts a removable Pole Streetlight Attachment powered only by secondary service"))
        list.add(tr("Use bare overhead conductors or cables rated for the applied voltage"))
    }
}
