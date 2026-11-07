package mods.eln.transparentnode.wiremachine

import mods.eln.Eln
import mods.eln.generic.ItemNbt
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.*
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sixnode.electricalcable.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.io.*
import kotlin.math.*

/** Source manufacturing with native accepted-step work and owned material buffers. */
class WireMachineElement(node:TransparentNode,d:TransparentNodeDescriptor):TransparentNodeElement(node,d) {
    val machine=d as WireMachineDescriptor
    val kind=machine.kind
    val power=NbtElectricalLoad("wireMachinePower")
    val resistor=Resistor(power,null)
    var selectedOption=0;private set
    var targetLengthMeters=32;private set
    var progressMeters=0.0;private set
    var progressTargetMeters=0.0;private set
    var loadedMaterial:UtilityCableMaterial?=null;private set
    var loadedMassKg=0.0;private set
    var insulationMetersBuffer=0.0;private set
    var running=false;private set
    var acceptedJoules=0.0;private set
    var usefulJoules=0.0;private set
    var completedJoules=0.0;private set
    private var active:UtilityCableDescriptor?=null
    private var layerCost=1.0
    private var internalChange=false
    private var optionsDirty=true
    private var optionsCache=emptyList<UtilityCableDescriptor>()
    private var itemBuffersRead=false
    private var breakHandled=false
    private var paidDrop:ItemStack?=null
    private val items=object:TransparentNodeElementInventory(kind.slotCount,64,this) {
        override fun getAccessibleSlotsFromSide(side:Int)=IntArray(kind.slotCount) { it }
        override fun canInsertItem(slot:Int,stack:ItemStack,side:Int)=WireProduction.accepts(kind,slot,stack)
        override fun canExtractItem(slot:Int,stack:ItemStack,side:Int)=slot==kind.outputSlot
    }
    init {
        electricalLoadList.add(power);electricalComponentList.add(resistor);power.rs=1e-6
        electricalProcessList.add(IProcess { dt->
            val watts=resistor.p
            if(running && active!=null && dt.isFinite() && dt>0 && watts.isFinite() && watts>0) {
                val joules=min(watts,kind.watts)*dt
                acceptedJoules+=watts*dt;usefulJoules+=joules
                progressMeters+=joules*kind.metresPerSecond/kind.watts/layerCost
            }
        })
        slowProcessList.add(IProcess { prepare() });slowProcessList.add(NodePeriodicPublishProcess(node,.5,0.0))
    }
    override fun getInventory():TransparentNodeElementInventory=items
    override fun hasGui()=true
    override fun onBlockActivated(p:Player,side:Direction,x:Float,y:Float,z:Float)=false
    override fun getElectricalLoad(side:Direction,lrdu:LRDU)=if(lrdu==LRDU.Down)power else null
    override fun getThermalLoad(side:Direction,lrdu:LRDU):ThermalLoad?=null
    override fun getConnectionMask(side:Direction,lrdu:LRDU)=if(lrdu==LRDU.Down)NodeBase.maskElectricalPower else 0
    override fun multiMeterString(side:Direction)=Utils.plotUIP(power.u,resistor.current)
    override fun thermoMeterString(side:Direction)=""
    override fun initialize() { itemBuffersRead=true;prepare();connect() }
    override fun inventoryChange(inventory:Container?) {
        optionsDirty=true
        if(!internalChange) { progressMeters=0.0;progressTargetMeters=target();running=false;active=null;resistor.highImpedance() }
        needPublish()
    }
    fun options():List<UtilityCableDescriptor> {
        if(optionsDirty) {
            optionsCache=when(kind) {
                WireMachineKind.ROLLER->WireProduction.rollerOptions(loadedMaterial ?: WireProduction.material(items.getItem(0)))
                WireMachineKind.INSULATOR->WireProduction.insulatorOptions(items.getItem(0))
                WireMachineKind.COMBINER->WireProduction.combinerOptions(inputs())
            };optionsDirty=false
        }
        return optionsCache
    }
    private fun inputs()=WireProduction.combinerInputs.map { items.getItem(it) }.filterNot { it.isEmpty }
    private fun target():Double=when(kind) {
        WireMachineKind.ROLLER->targetLengthMeters.toDouble()
        WireMachineKind.INSULATOR->items.getItem(0).let { WireProduction.cable(it)?.getRemainingLengthMeters(it) ?: if(WireProduction.bundle.valid(it))WireProduction.bundle.getLengthMeters(it) else 0.0 }
        WireMachineKind.COMBINER->inputs().takeIf { it.size>=2 }?.minOf { WireProduction.cable(it)?.getRemainingLengthMeters(it) ?: 0.0 } ?: 0.0
    }
    private inline fun mutate(block:()->Unit) { internalChange=true;try { block() }finally { internalChange=false } }
    /** Only readiness/material mutation runs here; physical work advances after accepted electrical steps. */
    fun prepare() {
        val choices=options();selectedOption=selectedOption.coerceIn(0,max(0,choices.lastIndex))
        val selected=choices.getOrNull(selectedOption);val length=target()
        if(progressTargetMeters!=0.0 && progressTargetMeters!=length)progressMeters=0.0
        progressTargetMeters=length
        var ready=selected!=null && length.isFinite() && length>0 && items.getItem(kind.outputSlot).isEmpty
        if(ready && selected!=null) {
            mutate {
                when(kind) {
                    WireMachineKind.ROLLER->{
                        ready=WireProduction.wheel(items.getItem(1)) && WireProduction.wheel(items.getItem(2))
                        if(ready) {
                            val input=items.getItem(0);val material=WireProduction.material(input)
                            if(material!=null && (loadedMaterial==null || loadedMaterial==material)) {
                                loadedMaterial=material;loadedMassKg+=input.count;items.setItem(0,ItemStack.EMPTY);optionsDirty=true;needPublish()
                            }
                            ready=loadedMaterial==selected.material && loadedMassKg>=requiredKg(selected,length)
                        }
                    }
                    WireMachineKind.INSULATOR->{
                        val rubber=items.getItem(1)
                        if(WireProduction.rubber(rubber)) { insulationMetersBuffer+=rubber.count*WireProduction.RUBBER_METERS;items.setItem(1,ItemStack.EMPTY);needPublish() }
                        ready=insulationMetersBuffer>=length*WireProduction.insulationCostFactor(selected)
                    }
                    WireMachineKind.COMBINER->{ ready=inputs().size==selected.conductorCount }
                }
            }
        }
        running=ready;active=if(ready)selected else null
        layerCost=if(kind==WireMachineKind.INSULATOR && selected!=null)WireProduction.insulationCostFactor(selected) else 1.0
        resistor.r=if(ready)200.0*200/kind.watts else 1e12
        if(!ready) { progressMeters=0.0;return }
        if(progressMeters+1e-9>=length && selected!=null) {
            mutate { finish(selected,length);items.setChanged() }
            completedJoules+=length/kind.metresPerSecond*kind.watts*layerCost
            progressMeters=0.0;running=false;active=null;resistor.highImpedance();needPublish()
        }
    }
    private fun requiredKg(d:UtilityCableDescriptor,length:Double)=WirePhysics.massKg(d.material,d.totalConductorAreaMm2,length)
    private fun finish(d:UtilityCableDescriptor,length:Double) {
        when(kind) {
            WireMachineKind.ROLLER->{
                val out=d.newItemStack();d.setRemainingLengthMeters(out,length);items.setItem(3,out)
                loadedMassKg=(loadedMassKg-requiredKg(d,length)).coerceAtLeast(0.0);if(loadedMassKg==0.0)loadedMaterial=null
            }
            WireMachineKind.INSULATOR->{
                val input=items.getItem(0);val wire=WireProduction.cable(input)
                val out=d.newItemStack();d.setRemainingLengthMeters(out,length)
                ItemNbt.get(input)?.getDoubleOr("windingTemperatureCelsius",Double.NaN)?.takeIf { it.isFinite() }?.let { t->ItemNbt.edit(out) { it.putDouble("windingTemperatureCelsius",t) } }
                items.setItem(2,out);insulationMetersBuffer=(insulationMetersBuffer-length*WireProduction.insulationCostFactor(d)).coerceAtLeast(0.0)
                if(wire!=null) {
                    val left=(wire.getRemainingLengthMeters(input)-length).coerceAtLeast(0.0)
                    if(left==0.0)items.setItem(0,ItemStack.EMPTY) else wire.setRemainingLengthMeters(input,left)
                }else {
                    val left=(WireProduction.bundle.getLengthMeters(input)-length).coerceAtLeast(0.0)
                    if(left==0.0)items.setItem(0,ItemStack.EMPTY) else WireProduction.bundle.setLengthMeters(input,left)
                }
            }
            WireMachineKind.COMBINER->{
                val physics=WireThermalPhysics(d.material,d.conductorAreaMm2,length)
                var enthalpy=0.0
                for(slot in WireProduction.combinerInputs) {
                    val stack=items.getItem(slot);val cable=WireProduction.cable(stack) ?: continue
                    val temperature=ItemNbt.get(stack)?.getDoubleOr("windingTemperatureCelsius",20.0)?.takeIf { it.isFinite() && it>=-273.15 } ?: 20.0
                    enthalpy+=physics.sensibleEnthalpy(temperature)
                    val left=(cable.getRemainingLengthMeters(stack)-length).coerceAtLeast(0.0)
                    if(left==0.0)items.setItem(slot,ItemStack.EMPTY) else cable.setRemainingLengthMeters(stack,left)
                }
                val out=WireProduction.bundle.createBundleStack(d.sizeLabel,d.conductorAreaMm2.toString(),d.material,d.conductorCount,d.conductorAreaMm2,length)
                val temperature=physics.temperature(enthalpy/d.conductorCount)
                ItemNbt.edit(out) { it.putDouble("windingTemperatureCelsius",temperature) }
                items.setItem(5,out)
            }
        }
    }
    override fun getItemStackNBT()=CompoundTag().apply { putInt("selectedOption",selectedOption);putInt("targetLengthMeters",targetLengthMeters) }
    override fun readItemStackNBT(tag:CompoundTag?) {
        if(tag==null || itemBuffersRead)return
        selectedOption=tag.getIntOr("selectedOption",0).coerceAtLeast(0);targetLengthMeters=tag.getIntOr("targetLengthMeters",32).coerceIn(1,WireProduction.MAX_LENGTH)
        if(tag.contains("wireBuffers"))readBuffers(tag.getCompoundOrEmpty("wireBuffers"))
        itemBuffersRead=true
        progressMeters=0.0;progressTargetMeters=0.0;optionsDirty=true
    }
    override fun getDropItemStack():ItemStack=paidDrop?.also { paidDrop=null } ?: super.getDropItemStack()
    override fun onBreakElement() {
        if(breakHandled)return
        breakHandled=true
        val out=super.getDropItemStack();val tag=ItemNbt.get(out) ?: CompoundTag()
        tag.put("wireBuffers",CompoundTag().also(::writeBuffers));ItemNbt.set(out,tag);paidDrop=out
        loadedMassKg=0.0;loadedMaterial=null;insulationMetersBuffer=0.0
        try { super.onBreakElement() }finally { paidDrop=null }
    }
    private fun readBuffers(tag:CompoundTag) {
        loadedMaterial=UtilityCableMaterial.entries.getOrNull(tag.getIntOr("loadedMaterial",-1))
        loadedMassKg=tag.getDoubleOr("loadedMassKg",0.0).takeIf { kind==WireMachineKind.ROLLER && loadedMaterial!=null && it.isFinite() && it>=0 } ?: 0.0
        if(loadedMassKg==0.0)loadedMaterial=null
        insulationMetersBuffer=tag.getDoubleOr("insulationMetersBuffer",0.0).takeIf { kind==WireMachineKind.INSULATOR && it.isFinite() && it>=0 } ?: 0.0
    }
    private fun writeBuffers(tag:CompoundTag) {
        tag.putInt("loadedMaterial",loadedMaterial?.ordinal ?: -1);tag.putDouble("loadedMassKg",loadedMassKg);tag.putDouble("insulationMetersBuffer",insulationMetersBuffer)
    }
    override fun readFromNBT(tag:CompoundTag) {
        super.readFromNBT(tag);itemBuffersRead=true;selectedOption=tag.getIntOr("selectedOption",0).coerceAtLeast(0);targetLengthMeters=tag.getIntOr("targetLengthMeters",32).coerceIn(1,WireProduction.MAX_LENGTH)
        progressMeters=positive(tag,"progressMeters");progressTargetMeters=positive(tag,"progressTargetMeters");readBuffers(tag)
        acceptedJoules=positive(tag,"wireMachineAcceptedJoules");usefulJoules=positive(tag,"wireMachineUsefulJoules");completedJoules=positive(tag,"wireMachineCompletedJoules");optionsDirty=true
    }
    override fun writeToNBT(tag:CompoundTag) {
        super.writeToNBT(tag);tag.putInt("selectedOption",selectedOption);tag.putInt("targetLengthMeters",targetLengthMeters)
        tag.putDouble("progressMeters",progressMeters);tag.putDouble("progressTargetMeters",target());writeBuffers(tag)
        tag.putDouble("wireMachineAcceptedJoules",acceptedJoules);tag.putDouble("wireMachineUsefulJoules",usefulJoules);tag.putDouble("wireMachineCompletedJoules",completedJoules)
    }
    private fun positive(tag:CompoundTag,key:String)=tag.getDoubleOr(key,0.0).takeIf { it.isFinite() && it>=0 } ?: 0.0
    override fun networkUnserialize(stream:DataInputStream):Byte {
        val id=super.networkUnserialize(stream)
        if(id.toInt() !in 1..2)return id
        try {
            val value=stream.readInt()
            if(id.toInt()==1 && value in options().indices && value!=selectedOption) { selectedOption=value;inventoryChange(items) }
            if(id.toInt()==2 && kind==WireMachineKind.ROLLER && value in 1..WireProduction.MAX_LENGTH && value!=targetLengthMeters) { targetLengthMeters=value;inventoryChange(items) }
        }catch(_:IOException){}
        return unserializeNulldId
    }
    fun writeConfigTool(tag:CompoundTag,player:Player) { tag.merge(itemStackNBT) }
    fun readConfigTool(tag:CompoundTag,player:Player) {
        if(!tag.contains("targetLengthMeters"))return
        val value=tag.getIntOr("targetLengthMeters",32);if(value !in 1..WireProduction.MAX_LENGTH)return
        selectedOption=tag.getIntOr("selectedOption",0).coerceAtLeast(0);targetLengthMeters=value;inventoryChange(items)
    }
    override fun networkSerialize(stream:DataOutputStream) {
        super.networkSerialize(stream);stream.writeInt(selectedOption);stream.writeInt(targetLengthMeters)
        stream.writeDouble(progressMeters);stream.writeDouble(progressTargetMeters);stream.writeBoolean(running)
        stream.writeInt(loadedMaterial?.ordinal ?: -1);stream.writeDouble(loadedMassKg);stream.writeDouble(insulationMetersBuffer)
        stream.writeDouble(power.u);stream.writeDouble(resistor.p)
        repeat(kind.slotCount) { Utils.serialiseItemStack(stream,items.getItem(it)) }
    }
    override fun getWaila()=linkedMapOf(tr("Mode") to (options().getOrNull(selectedOption)?.name ?: tr("None")),tr("Progress") to tr("%1$ / %2$ m",progressMeters,progressTargetMeters),tr("Metal Buffer") to tr("%1$ kg",loadedMassKg),tr("Insulation Buffer") to tr("%1$ m",insulationMetersBuffer))
}
