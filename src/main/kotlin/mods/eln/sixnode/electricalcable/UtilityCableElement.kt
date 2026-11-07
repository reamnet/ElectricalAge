package mods.eln.sixnode.electricalcable

import mods.eln.Eln
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import mods.eln.sim.*
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.power.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import java.io.DataOutputStream

class UtilityCableElement(node: SixNode,side: Direction,d: SixNodeDescriptor): SixNodeElement(node,side,d) {
    val descriptor=d as UtilityCableDescriptor
    val conductorLoads=Array(descriptor.conductorCount) { UtilityConductorLoad(this,it) }
    val electricalLoad get()=conductorLoads[CableConnections.defaultCore(activeColors())]
    var paletteIndex=0;private set
    fun activeColors()=descriptor.colorPalettes[paletteIndex].conductorColors.copyOf()
    fun cyclePalette() { if(descriptor.colorPalettes.size>1) { paletteIndex=(paletteIndex+1)%descriptor.colorPalettes.size;reconnect();needPublish() } }
    val thermalLoad=WireThermalLoad("thermalLoad",WireThermalPhysics(descriptor.material,descriptor.totalConductorAreaMm2))
    private val voltages=DoubleArray(descriptor.conductorCount)
    private var damage=InsulationDamage()
    private var fault: Resistor?=null
    private var queued=false
    private var published=Double.NaN
    private var color=0;private var care=1
    val insulationFailed get()=damage.fault!=null
    private val resistance=mods.eln.gridnode.SpanResistanceRefresh()
    init {
        conductorLoads.forEach { it.setCanBeSimplifiedByLine(true);electricalLoadList.add(it) }
        thermalLoad.setAsSlow();thermalLoadList.add(thermalLoad)
        electricalProcessList.add(IProcess { dt->if(dt.isFinite() && dt>0)thermalLoad.moveEnergyTo(dt*(conductorLoads.sumOf { it.serialPower }+(fault?.p ?: 0.0))) })
        thermalSlowProcessList.add(IProcess { refresh() })
        slowProcessList.add(IProcess { dt->
            if(queued)return@IProcess
            if(thermalLoad.failed) { replace(UtilityCables.piles.getValue(descriptor.material));return@IProcess }
            if(descriptor.insulated && thermalLoad.absoluteCelsius>=descriptor.meltTemperatureCelsius) { descriptor.meltedDescriptor?.let(::replace);return@IProcess }
            if(descriptor.insulated && !descriptor.melted && dt.isFinite() && dt>0) {
                val was=insulationFailed
                for(i in conductorLoads.indices)voltages[i]=conductorLoads[i].u
                damage.commit(worstInsulationStress(voltages,InsulationClass("utility",descriptor.insulationVoltageRating,descriptor.insulationVoltageRating)),dt)
                if(!was && insulationFailed)later { installFault();sixNode.reconnect();needPublish() }
            }
            if(!published.isFinite() || kotlin.math.abs(thermalLoad.absoluteCelsius-published)>=10) { published=thermalLoad.absoluteCelsius;needPublish() }
        })
    }
    private fun refresh() {
        val air=Ambient.relative(sixNode.coordonate)+20.0
        thermalLoad.updateProperties(air,air,descriptor.insulated && !insulationFailed)
        resistance.update(descriptor.resistanceOhms(celsius=thermalLoad.absoluteCelsius)/2) { r->conductorLoads.forEach { it.rs=r } }
    }
    override fun initialize() { refresh();installFault() }
    private fun installFault() { if(insulationFailed && fault==null && descriptor.insulated)Resistor(conductorLoads[damage.fault!!.fromCore],damage.fault!!.toCore?.let { conductorLoads[it] }).setR(100.0).also { fault=it;electricalComponentList.add(it) } }
    private fun later(action: ()->Unit) {
        val world=sixNode.coordonate.world() as? net.minecraft.server.level.ServerLevel ?: return
        queued=true;world.server.schedule(net.minecraft.server.TickTask(world.server.tickCount) {
            if(!sixNode.isDestructing && sixNode.sideElementList[slot]===this)action()
            queued=false
        })
    }
    private fun replace(d: SixNodeDescriptor)=later {
        sixNode.disconnect();thermalLoad.flushPendingEnergy()
        val fresh=d.ElementClass.getConstructor(SixNode::class.java,Direction::class.java,SixNodeDescriptor::class.java).newInstance(sixNode,side,d) as SixNodeElement
        fresh.slot=slot;fresh.ox=ox;fresh.oy=oy;fresh.oz=oz;fresh.front=front
        if(fresh is UtilityCableElement) { fresh.color=color;fresh.care=care;fresh.paletteIndex=paletteIndex.coerceAtMost(fresh.descriptor.colorPalettes.lastIndex);fresh.thermalLoad.inheritHeat(thermalLoad) }
        if(fresh is MoltenMetalPileElement)fresh.inheritHeat(thermalLoad)
        sixNode.sideElementList[slot]=fresh;sixNode.sideElementIdList[slot]=d.parentItemDamage
        fresh.initialize();sixNode.connect();needPublish()
    }
    override fun disconnectJob() { thermalLoad.flushPendingEnergy();super.disconnectJob() }
    override fun getElectricalLoad(lrdu: LRDU)=electricalLoad
    override fun getThermalLoad(lrdu: LRDU)=thermalLoad
    override fun getConnectionMask(lrdu: LRDU)=descriptor.nodeMask+if(descriptor.insulated && descriptor.actsAsSingleConductor)(color shl NodeBase.maskColorShift)+(care shl NodeBase.maskColorCareShift) else 0
    override fun getDropItemStack()=UtilityCables.scrap(descriptor,1.0)
    override fun multiMeterString()=conductorLoads.mapIndexed { i,l->tr("Core %1$: %2$",i+1,Utils.plotUIP(l.u,l.i)) }.joinToString("\n")
    override fun thermoMeterString()=Utils.plotCelsius("T",thermalLoad.Tc)
    override fun getWaila(): LinkedHashMap<String,String> {
        val info=linkedMapOf(tr("Voltage to ground") to Utils.plotVolt("",electricalLoad.u),tr("Current") to Utils.plotAmpere("",electricalLoad.i),tr("Temperature") to Utils.plotCelsius("",thermalLoad.Tc),tr("Insulation condition") to when { descriptor.melted->tr("Damaged - exposed conductor");insulationFailed->tr("Insulation failed - repair required");descriptor.insulated->tr("Intact");else->tr("Bare conductor") })
        if(!descriptor.actsAsSingleConductor) {
            info[tr("Conductor colors")]=tr(descriptor.colorPalettes[paletteIndex].displayName)
            conductorLoads.forEachIndexed { i,l->info[tr("Core %1$",i+1)]=Utils.plotUIP(l.u,l.i) }
        }
        return info
    }
    override fun networkSerialize(out: DataOutputStream) { super.networkSerialize(out);out.writeByte(color shl 4);out.writeFloat(thermalLoad.absoluteCelsius.toFloat());out.writeByte(paletteIndex) }
    override fun readFromNBT(t: CompoundTag) {
        super.readFromNBT(t);val b=t.getByteOr("color",16).toInt();color=b and 15;care=(b shr 4) and 1
        paletteIndex=t.getByteOr("palette",0).toInt().coerceIn(0,descriptor.colorPalettes.lastIndex)
        val from=t.getIntOr("insulationFaultFrom",0);val to=t.getIntOr("insulationFaultTo",-1)
        val exposure=t.getDoubleOr("insulationExposure",0.0).takeIf { it.isFinite() && it>=0 } ?: 0.0
        damage=InsulationDamage(exposure,if(t.getBooleanOr("insulationFailed",false) && descriptor.insulated && from in conductorLoads.indices && (to==-1 || to in conductorLoads.indices && to!=from))InsulationStress(from,to.takeIf { it>=0 },descriptor.insulationVoltageRating,descriptor.insulationVoltageRating) else null)
    }
    override fun writeToNBT(t: CompoundTag) {
        thermalLoad.flushPendingEnergy();super.writeToNBT(t);t.putByte("color",(color+(care shl 4)).toByte());t.putBoolean("insulationFailed",insulationFailed);t.putDouble("insulationExposure",damage.exposure);t.putByte("palette",paletteIndex.toByte());damage.fault?.let { t.putInt("insulationFaultFrom",it.fromCore);t.putInt("insulationFaultTo",it.toCore ?: -1) }
    }
    override fun onBlockActivated(p: net.minecraft.world.entity.player.Player,side: Direction,x: Float,y: Float,z: Float): Boolean {
        if(Utils.isPlayerUsingWrench(p) && !descriptor.actsAsSingleConductor) { cyclePalette();Utils.sendMessage(p,tr("Conductor colors: %1$",descriptor.colorPalettes[paletteIndex].displayName));return true }
        if(Utils.isPlayerUsingWrench(p)) { care=care xor 1;Utils.sendMessage(p,tr("Wire color care %1$",care));reconnect();return true }
        val brush=mods.eln.generic.GenericItemUsingDamageDescriptor.getDescriptor(p.mainHandItem) as? mods.eln.item.BrushDescriptor
        if(descriptor.insulated && descriptor.actsAsSingleConductor && brush!=null) { val next=brush.getColor(p.mainHandItem);if(next!=color && brush.use(p.mainHandItem,p)) { color=next;reconnect() };return true }
        return false
    }
}
