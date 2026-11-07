package mods.eln.transparentnode.converter

import mods.eln.Eln
import mods.eln.generic.ItemNbt
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.*
import mods.eln.node.transparent.*
import mods.eln.sim.*
import mods.eln.sim.mna.component.*
import mods.eln.sim.nbt.*
import mods.eln.sim.power.*
import mods.eln.sim.process.destruct.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.io.*

/** Source variable converter on the destination's conservative coupled solver and fixed-reference thermal loads. */
open class VariableDcDcElement(node:TransparentNode,d:TransparentNodeDescriptor):TransparentNodeElement(node,d) {
    val primary=NbtElectricalLoad("primaryLoad");val secondary=NbtElectricalLoad("secondaryLoad")
    val primaryInternal=NbtElectricalLoad("primaryInternal");val secondaryInternal=NbtElectricalLoad("secondaryInternal")
    val control=NbtElectricalGateInput("control")
    val primarySource=SwitchableVoltageSource("primaryVoltageSource");val secondarySource=SwitchableVoltageSource("secondaryVoltageSource")
    val primaryResistance=Resistor(primary,primaryInternal);val secondaryResistance=Resistor(secondaryInternal,secondary)
    val settings=DcDcControl()
    val hardwareDescriptor=d as VariableDcDcDescriptor
    val oneWayDescriptor get()=hardwareDescriptor as OneWayDcDcDescriptor
    val inputSink get()=primarySource
    val outputSource get()=secondarySource
    val primaryMeltCurrent get()=windings[0].wire?.maximumCurrent ?: 5.0
    val secondaryMeltCurrent get()=windings[1].wire?.maximumCurrent ?: 5.0
    var activeRatio=1.0
    var movedPower=0.0
    val transferReady get()=populated && !queuedMelt
    var populated=false;private set
    var construction=tr("Needs Core");private set
    private var queuedMelt=false
    val converter=SafeTransformerProcess(primaryInternal,secondaryInternal,primarySource,secondarySource) { populated && !queuedMelt }
    val primaryThermal=WindingThermalLoad("primaryThermalLoad");val secondaryThermal=WindingThermalLoad("secondaryThermalLoad")
    val windings=arrayOf(DcDcWindingThermal(primaryThermal,primaryResistance,{primarySource.current},{primary.serialPower}),
        DcDcWindingThermal(secondaryThermal,secondaryResistance,{secondarySource.current},{secondary.serialPower}))
    private var inventoryReady=false
    private val items=object:TransparentNodeElementInventory(4,64,this) {
        override fun getStackInSlot(slot:Int):ItemStack {
            val stack=super.getStackInSlot(slot)
            if(inventoryReady && slot in 0..1)windings[slot].stamp(stack)
            return stack
        }
    }
    val directional=if(hardwareDescriptor.oneWay)OneWayDcDcProcess(this) else null
    val transfer:ResettablePowerProcess get()=directional ?: converter
    val transferRatio get()=if(directional!=null)activeRatio else converter.ratio
    init {
        electricalLoadList.addAll(listOf(primary,secondary,primaryInternal,secondaryInternal))
        if(hardwareDescriptor.variable)electricalLoadList.add(control)
        electricalComponentList.addAll(listOf(primaryResistance,secondaryResistance,primarySource,secondarySource))
        thermalLoadList.addAll(listOf(primaryThermal,secondaryThermal));primaryThermal.setAsSlow();secondaryThermal.setAsSlow()
        windings.forEach { electricalProcessList.add(it.heating.sample) }
        val failure=Failure(this).transformer()
        slowProcessList.add(VoltageStateWatchDog().set(primary).setUNominal(120000.0).set(failure))
        slowProcessList.add(VoltageStateWatchDog().set(secondary).setUNominal(120000.0).set(failure))
        slowProcessList.add(IProcess { updateControl();windings.forEachIndexed { index,w->w.update();w.melting()?.let { queueMelt(index,it) } } })
        slowProcessList.add(NodePeriodicPublishProcess(node,.5,0.0));inventoryReady=true
    }
    override fun getInventory():Container=items
    override fun hasGui()=true
    override fun getLightOpacity()=1f
    override fun onBlockActivated(p:Player,side:Direction,x:Float,y:Float,z:Float)=false
    override fun getElectricalLoad(side:Direction,lrdu:LRDU):ElectricalLoad? = if(lrdu!=LRDU.Down)null else when(side) { front.left()->primary;front.right()->secondary;front,front.inverse->if(hardwareDescriptor.variable)control else null;else->null }
    override fun getConnectionMask(side:Direction,lrdu:LRDU)=when(getElectricalLoad(side,lrdu)) { null->0;control->NodeBase.maskElectricalInputGate;else->NodeBase.maskElectricalPower }
    override fun getThermalLoad(side:Direction,lrdu:LRDU):ThermalLoad? = if(lrdu!=LRDU.Down)null else when(side) { front.left()->primaryThermal;front.right()->secondaryThermal;else->null }
    override fun initialize() {
        primarySource.connectTo(primaryInternal,null);secondarySource.connectTo(secondaryInternal,null)
        configure();updateControl();connect()
    }
    override fun connectJob() { Eln.simulator.mna.addProcess(transfer);windings.forEach { Eln.simulator.addThermalSlowProcess(it.heating.deliver) };super.connectJob() }
    override fun disconnectJob() { windings.forEach { it.heating.flushIntoLoad();Eln.simulator.removeThermalSlowProcess(it.heating.deliver) };super.disconnectJob();Eln.simulator.mna.removeProcess(transfer) }
    fun configure() {
        construction=DcDcWinding.construction(items.getItem(2),items.getItem(0),items.getItem(1),if(hardwareDescriptor.fixedRatio)2 else settings.version)
        populated=construction==tr("Operational")
        windings.forEachIndexed { index,w->w.configure(items.getItem(index)) }
        primary.rs=if(populated)1e-6 else 1e12;secondary.rs=if(populated)1e-6 else 1e12
        converter.maximumPrimaryVoltage=windings[0].wire?.maximumVoltage ?: 0.0
        converter.maximumSecondaryVoltage=windings[1].wire?.maximumVoltage ?: 0.0
    }
    fun computeRatio():Double {
        if(!populated)return 1.0
        if(hardwareDescriptor.fixedRatio)return ((windings[1].wire?.amount ?: 1.0)/(windings[0].wire?.amount ?: 1.0)).coerceIn(1.0/256,256.0)
        return settings.ratio(hardwareDescriptor.kind,control.normalized)
    }
    fun updateControl() {
        if(directional!=null)return
        converter.enabled=(hardwareDescriptor.fixedRatio || settings.enabled) && !queuedMelt
        converter.voltageTarget=if(!hardwareDescriptor.fixedRatio && settings.mode=="VOLTAGE")settings.value else null
        try { converter.ratio=computeRatio() }
        catch(_:IllegalArgumentException) { converter.enabled=false }
    }
    override fun inventoryChange(inventory:Container?) {
        disconnect();configure();transfer.resetFault();updateControl();connect();needPublish()
    }
    private fun queueMelt(slot:Int,d:mods.eln.sixnode.electricalcable.UtilityCableDescriptor) {
        if(queuedMelt)return
        val world=node.coordonate.world() as? ServerLevel ?: return
        val expected=items.getItem(slot);queuedMelt=true;converter.enabled=false
        world.server.schedule(net.minecraft.server.TickTask(world.server.tickCount) {
            try {
                if(!node.isDestructing && NodeManager.instance.getNodeFromCoordonate(node.coordonate)===node && items.getItem(slot)===expected) {
                    windings[slot].heating.flushIntoLoad();windings[slot].stamp(expected)
                    val metres=d.getRemainingLengthMeters(expected)
                    val damaged=d.meltedDescriptor
                    val replacement=if(damaged!=null)damaged.newItemStack(1).also {
                        damaged.setRemainingLengthMeters(it,metres)
                        ItemNbt.get(expected)?.let { saved->ItemNbt.set(it,saved) }
                    }else mods.eln.sixnode.electricalcable.UtilityCables.scrap(d,metres).also {
                        ItemNbt.edit(it) { tag->tag.putDouble("windingTemperatureCelsius",windings[slot].absoluteCelsius) }
                    }
                    items.setItem(slot,replacement);items.setChanged()
                }
            }finally { queuedMelt=false;updateControl();needPublish() }
        })
    }
    override fun readFromNBT(t:CompoundTag) { super.readFromNBT(t);settings.load(t) }
    override fun writeToNBT(t:CompoundTag) { windings.forEach { it.heating.flushIntoLoad() };super.writeToNBT(t);settings.save(t) }
    override fun getItemStackNBT()=CompoundTag().also(settings::save)
    override fun readItemStackNBT(t:CompoundTag?) { if(t!=null)settings.load(t) }
    fun writeConfigTool(t:CompoundTag,p:Player)=settings.save(t)
    fun readConfigTool(t:CompoundTag,p:Player) { if(t.contains("converterControlVersion")) { settings.load(t);configure();transfer.resetFault();updateControl();needPublish() } }
    override fun networkUnserialize(s:DataInputStream):Byte {
        val id=super.networkUnserialize(s)
        if(hardwareDescriptor.fixedRatio && !hardwareDescriptor.oneWay && id.toInt() in 40..43)return unserializeNulldId
        val before=listOf(settings.version,settings.mode,settings.value,settings.enabled)
        if(!settings.handle(id,s))return id
        if(before!=listOf(settings.version,settings.mode,settings.value,settings.enabled)) { configure();transfer.resetFault();updateControl();needPublish() }
        return unserializeNulldId
    }
    override fun networkSerialize(s:DataOutputStream) {
        super.networkSerialize(s);settings.write(s)
        for(i in 0..3)Utils.serialiseItemStack(s,items.getItem(i))
        s.writeDouble(transferRatio);s.writeUTF(transfer.status);s.writeUTF(construction)
        s.writeDouble(primary.u);s.writeDouble(secondary.u);s.writeDouble(primarySource.current);s.writeDouble(secondarySource.current)
        s.writeDouble(primaryThermal.Tc+20);s.writeDouble(secondaryThermal.Tc+20)
        node.lrduCubeMask.getTranslate(front.down()).serialize(s)
    }
    override fun multiMeterString(side:Direction)=tr("Primary: %1$; secondary: %2$",Utils.plotUIP(primary.u,primarySource.current),Utils.plotUIP(secondary.u,secondarySource.current))
    override fun thermoMeterString(side:Direction)=tr("Primary %1$ C; secondary %2$ C",Utils.plotValue(primaryThermal.Tc+20),Utils.plotValue(secondaryThermal.Tc+20))
    override fun getWaila()=linkedMapOf(tr("Construction") to construction,tr("Converter state") to transfer.status,tr("Ratio") to Utils.plotValue(transferRatio),tr("Control mode") to tr("%1$ (version %2$)",settings.mode,settings.version))
}
