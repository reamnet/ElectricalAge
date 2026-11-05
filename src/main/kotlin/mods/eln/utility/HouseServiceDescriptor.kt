package mods.eln.utility

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.ghost.*
import net.minecraft.world.phys.AABB
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.core.BlockPos

class HouseServiceDescriptor(val meter: Boolean,val disconnect: Boolean,val wide: Boolean): PowerCabinetDescriptor(
    if(disconnect)I18N.TR_NAME(I18N.Type.NONE,"Red Handle Service Disconnect") else if(wide)I18N.TR_NAME(I18N.Type.NONE,"Wide Underground Meter Socket") else if(meter)I18N.TR_NAME(I18N.Type.NONE,"North American Meter Base") else I18N.TR_NAME(I18N.Type.NONE,"Underground Service Entrance Riser"),HouseServiceElement::class.java,HouseServiceRender::class.java,true) {
    private val model=requireNotNull(Eln.obj.getObj(if(disconnect)"redhandleservicedisconnect" else if(wide)"wideundergroundmetersocket" else if(meter)"northamericanmeterbase" else "undergroundserviceentrance"))
    val serviceWidth get()=if(wide)2 else 1
    init { require(!wide || meter);if(wide)ghostGroup=GhostGroup() }
    override fun getGhostGroup(front: Direction): GhostGroup?=if(!wide)null else GhostGroup().apply {
        val right=front.right().toEnumFacing();addElement(right.stepX,0,right.stepZ,Eln.ghostBlock,GhostBlock.wallServiceMeta(front))
    }
    override fun checkCanPlace(c: Coordonate?,front: Direction): String? {
        val reason=super.checkCanPlace(c,front);if(reason!=null)return reason
        if(wide && c!=null) {
            val p=c.pos().relative(front.right().toEnumFacing()).relative(front.inverse.toEnumFacing())
            if(!c.world().getBlockState(p).isSolidRender())return tr("The wide socket needs a solid wall behind both halves")
        }
        return null
    }
    fun checkSupport(e: HouseServiceElement) {
        checkWallSupport(e)
        if(!wide || e.node.isDestructing)return
        val c=e.node.coordonate;val p=c.pos().relative(e.front.right().toEnumFacing()).relative(e.front.inverse.toEnumFacing());val world=c.world()
        if(world is net.minecraft.server.level.ServerLevel && world.chunkSource.getChunkNow(p.x shr 4,p.z shr 4)==null)return
        if(!world.getBlockState(p).isSolidRender())e.selfDestroy()
    }
    override fun wallBox(front: Direction)=serviceBox(front)
    fun serviceBox(front: Direction): AABB {
        val b=when(front) { Direction.XN->AABB(.67,0.0,.08,1.0,1.0,.92);Direction.XP->AABB(0.0,0.0,.08,.33,1.0,.92);Direction.ZN->AABB(.08,0.0,.67,.92,1.0,1.0);Direction.ZP->AABB(.08,0.0,0.0,.92,1.0,.33);else->error("Horizontal wall front required") }
        val d=front.right().toEnumFacing();return b.expandTowards(d.stepX*(serviceWidth-1).toDouble(),0.0,d.stepZ*(serviceWidth-1).toDouble())
    }
    override fun addCollisionBoxesToList(query: AABB,list: MutableList<Any?>,world: Level?,x: Int,y: Int,z: Int) {
        val tile=world?.getBlockEntity(BlockPos(x,y,z)) as? TransparentNodeEntity ?: return
        val front=tile.elementRender?.front ?: (tile.node as? TransparentNode)?.element?.front ?: return
        val box=serviceBox(front).move(x.toDouble(),y.toDouble(),z.toDouble());if(query.intersects(box))list.add(box)
    }
    override fun hasItemDraw()=true
    override fun drawItem(entity: Boolean) { serviceMatrix { if(wide) { GL11.glScaled(.5,.5,.5);GL11.glTranslated(-.5,0.0,0.0) };drawService(Direction.ZP) } }
    fun drawService(front: Direction,d: HouseServiceDisplay?=null,disk: Double=0.0,page: Int=0)=serviceMatrix {
        if(disconnect) { ServiceDisconnectVisual.draw(front,d?.disconnected==true,model);return@serviceMatrix }
        front.inverse.glRotateZnRef();GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun p(n: String,r: Float,g: Float,b: Float) { GL11.glColor3f(r,g,b);model.getPart(n)?.drawNoBind() }
            p("Case",.55f,.57f,.57f)
            if(!meter) { p("Pipe",.49f,.51f,.52f);p("Hood",.40f,.43f,.45f);repeat(3) { p("Terminal$it",.61f,.64f,.58f) };return@serviceMatrix }
            p("Socket",.10f,.12f,.13f);p("GutterCover",.55f,.57f,.57f);p("EntryLine",.13f,.27f,.48f);p("EntryLoad",.55f,.32f,.12f);p("Bond",.48f,.58f,.47f);p("Screws",.72f,.74f,.73f)
            repeat(3) { p("Terminal$it",.62f,.48f,.26f);p("TopTerminal$it",.62f,.48f,.26f) }
            if(d==null || d.type==0) { p("JawInsulator",.12f,.14f,.15f);p("Jaws",.75f,.52f,.29f) }
            else ServiceMeterVisual.draw(model,d.type,d.imported,d.exported,d.watts,d.lineVoltage,d.disconnected,d.powered,disk,page)
        } finally { GL11.glPopAttrib() }
    }
    override fun addInformation(stack: ItemStack?,player: Player?,list: MutableList<String>,advanced: Boolean) {
        if(disconnect) { list.add(tr("Place between the meter and panel; top is input, bottom is output"));list.add(tr("Red handle manually opens all service circuits; use placed wall cables"));return }
        list.add(tr("Three-wire service bundle: L1 120 V, L2 120 V and 240 V circuit equivalents"))
        if(wide) { list.add(tr("Two blocks wide; downward input under the left half, downward output under the right"));list.add(tr("Bottom entries only: utility line on the left, customer load on the right")) }
        if(meter) { list.add(tr("Insert an Analog Glass Service Meter or Smart Digital Service Meter"));list.add(tr("Smart meter has a manual disconnect and a future remote-control hook")) }
        else list.add(tr("Buried service enters below; continue upward to the meter with placed cables"))
        list.add(tr("Top and bottom cable terminals use the circuit selected in the menu"))
    }
}
