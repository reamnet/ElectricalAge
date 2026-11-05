package mods.eln.utility

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/** A passive secondary splice enclosure; protection remains upstream. No artificial source or regulator. */
class UndergroundHandholeElement(node: TransparentNode,descriptor: TransparentNodeDescriptor): TransparentNodeElement(node,descriptor) {
    val service=NbtElectricalLoad("buriedServiceJunction").apply { rs=.00005 }
    init { electricalLoadList.add(service) }
    override fun initialize() { connect() }
    override fun getThermalLoad(side: Direction,lrdu: LRDU)=null
    override fun thermoMeterString(side: Direction): String?=null
    override fun onBlockActivated(player: Player,side: Direction,vx: Float,vy: Float,vz: Float)=false
    override fun getElectricalLoad(side: Direction,lrdu: LRDU)=if(side!=Direction.YP)service else null
    override fun getConnectionMask(side: Direction,lrdu: LRDU)=if(getElectricalLoad(side,lrdu)!=null)NodeBase.maskElectricalPower else 0
    override fun multiMeterString(side: Direction)=Utils.plotUIP(service.u,service.i)
}
class UndergroundHandholeDescriptor: TransparentNodeDescriptor(I18N.TR_NAME(I18N.Type.NONE,"120/240V Underground Service Handhole"),UndergroundHandholeElement::class.java,UndergroundHandholeRender::class.java) {
    private val model by lazy { Eln.obj.getObj("undergroundservicehandhole") }
    fun draw()=serviceMatrix {
        GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun p(name: String,r: Float,g: Float,b: Float) { GL11.glColor3f(r,g,b);model.getPart(name)?.drawNoBind() }
            p("Case",.42f,.44f,.41f);p("Lid",.18f,.30f,.20f);p("Trim",.13f,.15f,.13f);p("Lugs",.66f,.48f,.27f);p("Lettering",.65f,.69f,.62f)
        } finally { GL11.glPopAttrib() }
    }
    override fun hasItemDraw()=true
    override fun drawItem(entity: Boolean)=draw()
    override fun addInformation(itemStack: ItemStack?,entityPlayer: Player?,list: MutableList<String>,par4: Boolean) {
        list.add(tr("Bury flush with grade; four side cable entries and one bottom entry"))
        list.add(tr("Splices one 240 V service equivalent; the downstream socket/panel derives proportional 120 V legs"))
        list.add(tr("Use insulated service cables and upstream overcurrent protection"))
    }
}
class UndergroundHandholeRender(entity: TransparentNodeEntity,descriptor: TransparentNodeDescriptor): TransparentNodeElementRender(entity,descriptor) {
    override fun draw()=(transparentNodedescriptor as UndergroundHandholeDescriptor).draw()
}
