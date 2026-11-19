package mods.eln.transparentnode.offgrid
import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.client.power.PowerPanelDisplay
import mods.eln.ghost.GhostBlock
import mods.eln.ghost.GhostGroup
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.transparent.*
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB

class OffGridDescriptor(val type: OffGridType): PowerCabinetDescriptor(type.title,OffGridElement::class.java,OffGridRender::class.java,type.wall) {
    private val model=requireNotNull(Eln.obj.getObj(type.model))
    init { if(type.bank)ghostGroup=GhostGroup().apply { addElement(0,1,0) } }
    override fun getGhostGroup(front: Direction): GhostGroup? = if(!type.bank)null else GhostGroup().apply {
        addElement(0,1,0,Eln.ghostBlock,if(type.wall)GhostBlock.wallServiceMeta(front) else GhostBlock.tBatteryFloor)
    }
    override fun checkCanPlace(coord: Coordonate?,front: Direction): String? {
        val error=super.checkCanPlace(coord,front);if(error!=null)return error
        if(type.wall && coord!=null && !holds(coord.newWithOffset(0,1,0),front.inverse))return tr("Both battery cabinet blocks need a supporting wall")
        return null
    }
    fun checkSupport(e: OffGridElement) {
        checkWallSupport(e)
        if(!type.wall)return
        val at=e.node?.coordonate ?: return
        val world=at.world() as? ServerLevel ?: return
        val top=at.newWithOffset(0,1,0)
        val support=top.pos().relative(e.front.inverse.toEnumFacing())
        if(world.chunkSource.getChunkNow(support.x shr 4,support.z shr 4)!=null && !holds(top,e.front.inverse))e.selfDestroy()
    }
    override fun addCollisionBoxesToList(query: AABB,list: MutableList<Any?>,world: Level?,x: Int,y: Int,z: Int) {
        val tile=world?.getBlockEntity(BlockPos(x,y,z)) as? TransparentNodeEntity
        val front=tile?.elementRender?.front ?: (tile?.node as? TransparentNode)?.element?.front ?: Direction.ZP
        val box=if(type.wall)wallBox(front).let { AABB(it.minX,0.0,it.minZ,it.maxX,2.0,it.maxZ) }
            else AABB(.06,0.0,.08,.94,if(type.bank)2.0 else .96,.92)
        val placed=box.move(x.toDouble(),y.toDouble(),z.toDouble());if(query.intersects(placed))list.add(placed)
    }
    override fun hasItemDraw()=true
    override fun drawItem(entity: Boolean) {
        GL11.glPushMatrix()
        try { if(type.bank) { GL11.glScalef(.48f,.48f,.48f);GL11.glTranslatef(0f,-.5f,0f) };draw(Direction.ZP) }
        finally { GL11.glPopMatrix() }
    }
    fun draw(front: Direction,d: OffGridDisplay?=null)=run {
        GL11.glPushMatrix()
        front.inverse.glRotateZnRef();GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun part(name: String,r: Float,g: Float,b: Float) { GL11.glColor3f(r,g,b);model.getPart(name)?.drawNoBind() }
            part("Case",.82f,.85f,.86f);part("Trim",.09f,.13f,.16f);part("Vents",.035f,.045f,.05f)
            part("Solar",.1f,.5f,.9f);part("Grid",.1f,.28f,.75f);part("Positive",.85f,.15f,.08f);part("Negative",.08f,.09f,.1f);part("Output",.95f,.44f,.07f)
            val lit=d?.enabled==true
            val fault=d?.status in setOf("OVERVOLTAGE","REVERSE","TEMPERATURE","OVERLOAD","BACKFEED","FUSE","PROTECTED")
            PowerPanelDisplay.lamp(model,"StatusLed",lit && !fault && d?.status !in setOf("EMPTY","TRANSFER","WAIT_GRID","NO_POWER"),.1f,.8f,.3f)
            PowerPanelDisplay.lamp(model,"ChargeLed",d?.chargeWatts?.let { it>1 }==true || d?.watts?.let { it< -1 }==true,.1f,.5f,1f)
            PowerPanelDisplay.lamp(model,"FaultLed",fault,1f,.06f,.025f)
            PowerPanelDisplay.equipmentNumber(model,d?.dc ?: 0.0,lit,"Voltage")
            PowerPanelDisplay.equipmentNumber(model,if(type.bank && d!=null && d.capacity>0)d.energy/d.capacity*100 else d?.chargeWatts ?: 0.0,lit,"Charge")
            PowerPanelDisplay.equipmentNumber(model,d?.watts ?: 0.0,lit,"Power")
            if(type.bank)PowerPanelDisplay.equipmentNumber(model,type.kwh.toDouble(),true,"Capacity")
        } finally { GL11.glPopAttrib();GL11.glPopMatrix() }
    }
    override fun addInformation(stack: ItemStack?,player: Player?,list: MutableList<String>,advanced: Boolean) {
        if(type.bank) {
            list.add(tr("%1$ kWh LiFePO4; 51.2 V nominal DC; %2$ A BMS",type.kwh,type.kwh/16*100))
            list.add(tr("Two blocks tall; integrated BMS and DC disconnect; crafted empty"))
            list.add(tr("DC storage only: a separate inverter is required for 120 or 240 V loads"))
            list.add(tr("Left red: battery positive; right black: battery negative"))
            if(type.wall)list.add(tr("Mount against a two-block-tall wall; the cabinet projects 0.33 blocks"))
        } else when(type) {
            OffGridType.INVERTER->{ list.add(tr("120 or 240 V isolated backup output; 12000 W maximum"));list.add(tr("120/240 V grid or generator input; 56.8 V battery charging; no grid export"));list.add(tr("Left: DC+; rear: DC-; front blue: AC input; right orange: backed-up loads")) }
            OffGridType.AC_CHARGER->{ list.add(tr("120/240 V AC input to 56.8 V DC; adjustable 0 to 200 A charging"));list.add(tr("Left: battery DC+; rear: DC-; front blue: grid or generator input")) }
            OffGridType.MPPT->{ list.add(tr("10000 W solar battery charger; 56.8 V output; 200 A maximum"));list.add(tr("Left blue: PV+; rear black: PV-; right red: battery+; front black: battery-")) }
            OffGridType.COMBINER->{ list.add(tr("Two PV string inputs; included 30 A fuses; 1000 V DC maximum"));list.add(tr("Left and front: PV positives; right: combined positive; rear: common return"));list.add(tr("The handle opens both positives; replace blown fuses with 30 A or smaller cartridges")) }
            else->{ list.add(tr("Manual 400 A battery DC breaker; left input and right output"));list.add(tr("Trips on overcurrent; clear the fault before resetting")) }
        }
    }
}
