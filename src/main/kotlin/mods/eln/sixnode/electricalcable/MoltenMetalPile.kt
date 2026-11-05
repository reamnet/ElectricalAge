package mods.eln.sixnode.electricalcable
import mods.eln.cable.*
import mods.eln.misc.*
import mods.eln.node.six.*
import mods.eln.sim.*
import mods.eln.i18n.I18N.tr
import mods.eln.gl.GL11
import net.minecraft.nbt.CompoundTag
import java.io.*

class MoltenMetalPileDescriptor(name: String,val material: UtilityCableMaterial,val render: CableRenderDescriptor): SixNodeDescriptor(name,MoltenMetalPileElement::class.java,MoltenMetalPileRender::class.java) {
    init { setDefaultIcon("arc_metal_block") }
}
class MoltenMetalPileElement(node: SixNode,side: Direction,d: SixNodeDescriptor): SixNodeElement(node,side,d) {
    val descriptor=d as MoltenMetalPileDescriptor
    var thermal=WireThermalLoad("scrap",WireThermalPhysics(descriptor.material,1.0));private set
    private var published=Double.NaN
    init {
        thermal.setAsSlow();thermalLoadList.add(thermal)
        thermalSlowProcessList.add(IProcess { val air=Ambient.relative(sixNode.coordonate)+20;thermal.updateProperties(air,air,false) })
        slowProcessList.add(IProcess { if(!published.isFinite() || kotlin.math.abs(thermal.absoluteCelsius-published)>10) { published=thermal.absoluteCelsius;needPublish() } })
    }
    private fun geometry(area: Double) { thermalLoadList.remove(thermal);thermal=WireThermalLoad("scrap",WireThermalPhysics(descriptor.material,area));thermal.setAsSlow();thermalLoadList.add(thermal) }
    fun inheritHeat(from: WireThermalLoad) { geometry(from.physics.totalAreaMm2);thermal.inheritHeat(from) }
    override fun initialize() { val air=Ambient.relative(sixNode.coordonate)+20;thermal.updateProperties(air,air,false) }
    override fun readFromNBT(t: CompoundTag) { geometry(t.getDoubleOr("scrapAreaMm2",1.0).takeIf { it.isFinite() && it>0 } ?: 1.0);super.readFromNBT(t) }
    override fun writeToNBT(t: CompoundTag) { super.writeToNBT(t);t.putDouble("scrapAreaMm2",thermal.physics.totalAreaMm2) }
    override fun disconnectJob() { thermal.flushPendingEnergy();super.disconnectJob() }
    override fun getElectricalLoad(l: LRDU)=null
    override fun getThermalLoad(l: LRDU)=null
    override fun getConnectionMask(l: LRDU)=0
    override fun multiMeterString()=""
    override fun thermoMeterString()=Utils.plotCelsius("T",thermal.absoluteCelsius)
    override fun getDropItemStack()=UtilityCables.scrap(descriptor.material,thermal.physics.totalAreaMm2,1.0,descriptor.name)
    override fun networkSerialize(out: DataOutputStream) { super.networkSerialize(out);out.writeFloat(thermal.absoluteCelsius.toFloat()) }
}
class MoltenMetalPileRender(e: SixNodeEntity,side: Direction,d: SixNodeDescriptor): SixNodeElementRender(e,side,d) {
    private val desc=d as MoltenMetalPileDescriptor
    private var temperature=20f
    override fun publishUnserialize(s: DataInputStream) { super.publishUnserialize(s);temperature=s.readFloat() }
    override fun drawCableAuto()=false
    override fun glListDraw() { CableRender.drawNode(desc.render,connectedSide,CableRender.connectionType(this,side)) }
    override fun getCableRender(l: LRDU)=desc.render
    override fun draw() {
        GL11.glPushAttrib(0xFFFFF)
        try {
            if(desc.material==UtilityCableMaterial.COPPER)GL11.glColor3f(.92f,.38f,.10f) else GL11.glColor3f(.83f,.85f,.88f)
            UtilsClient.bindTexture(desc.render.cableTexture);glListCall()
            if(temperature>550) { UtilsClient.disableLight();UtilsClient.enableBlend();GL11.glColor4f(1f,.55f,.12f,.8f);glListCall() }
        }finally { GL11.glPopAttrib() }
    }
}
