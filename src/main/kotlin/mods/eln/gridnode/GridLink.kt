package mods.eln.gridnode

import mods.eln.Eln
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeManager
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.ElectricalLoad
import mods.eln.sixnode.electricalcable.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

/** One paid span record shared by its loaded endpoints. No per-tick endpoint/world searches. */
class GridLink : INBTTReady {
    @JvmField var a=Coordonate()
    @JvmField var b=Coordonate()
    private var from=0;private var to=0
    private var cable=ItemStack.EMPTY
    private var rs=0.0
    private val connections=ArrayList<GridSpanConnection>()
    private val thermals=ArrayList<WireSpanThermal>()
    private var savedThermals: CompoundTag?=null
    private var burnQueued=false
    private var broken=false
    fun thermalLoads()=thermals.map { it.load }
    fun insulationDamaged()=thermals.any { it.insulationDamaged }
    val connected get()=connections.isNotEmpty()
    constructor(a: Coordonate,b: Coordonate,sideA: Direction,sideB: Direction,cable: ItemStack,rs: Double):this(a,b,sideA.int,sideB.int,cable,rs)
    constructor(a: Coordonate,b: Coordonate,from: Int,to: Int,cable: ItemStack,rs: Double) {
        this.a=a;this.b=b;this.from=from;this.to=to;this.cable=cable.copy();this.rs=rs
    }
    constructor(nbt: CompoundTag,str: String) { readFromNBT(nbt,str) }
    fun elementA(): GridElement?=getElementFromCoordinate(a)
    fun elementB(): GridElement?=getElementFromCoordinate(b)
    fun getTerminal(e: GridElement)=if(e.coordonate()==a)from else to
    fun getSide(e: GridElement): Direction=Direction.fromInt(getTerminal(e)) ?: Direction.XN
    fun getOtherElement(e: GridElement): GridElement?=getElementFromCoordinate(if(e.coordonate()==a)b else a)
    fun links(left: GridElement,right: GridElement)=a==left.coordonate() && b==right.coordonate() || a==right.coordonate() && b==left.coordonate()
    fun sameTerminals(other: GridLink)=a==other.a && b==other.b && from==other.from && to==other.to || a==other.b && b==other.a && from==other.to && to==other.from
    fun connect(): Boolean {
        if(broken || connected)return false
        val left=elementA();val right=elementB()
        // Retain the saved record even when its remote chunk is not loaded yet.
        val canonical=(left?.gridLinkList.orEmpty()+right?.gridLinkList.orEmpty()).firstOrNull { it!==this && sameTerminals(it) }
        if(canonical!=null) {
            left?.gridLinkList?.remove(this);right?.gridLinkList?.remove(this)
            left?.gridLinkList?.add(canonical);right?.gridLinkList?.add(canonical)
            canonical.connect();return false
        }
        left?.gridLinkList?.add(this);right?.gridLinkList?.add(this)
        if(left==null || right==null || !left.gridReady() || !right.gridReady())return false
        if(!left.acceptsSavedGridTerminal(from) || !right.acceptsSavedGridTerminal(to))return false
        val x=left.gridLoads(from);val y=right.gridLoads(to)
        if(x.isEmpty() || x.size!=y.size || !rs.isFinite() || rs<0)return false
        val utility=mods.eln.generic.GenericItemBlockUsingDamageDescriptor.getDescriptor(cable) as? UtilityCableDescriptor
        val meters=utility?.getRemainingLengthMeters(cable)?.div(x.size)
        if(utility!=null && (meters==null || !meters.isFinite() || meters<=0))return false
        for(i in x.indices) {
            val c=GridSpanConnection(x[i],y[i],rs);connections.add(c);Eln.simulator.addElectricalComponent(c)
            if(utility!=null) {
                val heat=thermals.getOrNull(i) ?: WireSpanThermal(utility,meters!!).also {
                    savedThermals?.let { tag->it.read(if(i==0)tag else tag.getCompoundOrEmpty("spanThermal$i")) };thermals.add(it)
                }
                heat.connect(c,{ (left.spanAmbientCelsius()+right.spanAmbientCelsius())*.5 }) { queueBurn() }
            }
        }
        savedThermals=null
        publish(left);publish(right);return true
    }
    fun disconnect() {
        if(!connected)return
        thermals.forEach { it.disconnect() }
        for(c in connections) { Eln.simulator.removeElectricalComponent(c);c.breakConnection() }
        connections.clear();elementA()?.let(::publish);elementB()?.let(::publish)
    }
    fun selfDestroy() { onBreakElement() }
    fun onBreakElement(): ItemStack {
        if(broken)return ItemStack.EMPTY
        broken=true;disconnect()
        elementA()?.let { it.gridLinkList.remove(this);publish(it) }
        elementB()?.let { it.gridLinkList.remove(this);publish(it) }
        val utility=mods.eln.generic.GenericItemBlockUsingDamageDescriptor.getDescriptor(cable) as? UtilityCableDescriptor
        val refund=if(utility!=null && insulationDamaged())utility.meltedDescriptor?.let { cable.transmuteCopy(it.parentItem,1) }?.also { utility.meltedDescriptor!!.setRemainingLengthMeters(it,utility.getRemainingLengthMeters(cable)) } ?: cable.copy() else cable.copy()
        cable=ItemStack.EMPTY;return refund
    }
    private fun queueBurn() {
        if(broken || burnQueued)return
        val world=(elementA()?.coordonate()?.world() ?: elementB()?.coordonate()?.world()) as? net.minecraft.server.level.ServerLevel ?: return
        burnQueued=true;world.server.schedule(net.minecraft.server.TickTask(world.server.tickCount) { if(!broken)onBreakElement();burnQueued=false })
    }
    override fun readFromNBT(nbt: CompoundTag,str: String) {
        a.readFromNBT(nbt,str+"a");b.readFromNBT(nbt,str+"b")
        from=nbt.getIntOr(str+"as",0);to=nbt.getIntOr(str+"bs",0);rs=nbt.getDoubleOr(str+"rs",0.0)
        cable=nbt.get(str+"cable")?.let(Utils::loadItem) ?: ItemStack.EMPTY
        savedThermals=nbt.copy()
    }
    override fun writeToNBT(nbt: CompoundTag,str: String) {
        a.writeToNBT(nbt,str+"a");b.writeToNBT(nbt,str+"b");nbt.putInt(str+"as",from);nbt.putInt(str+"bs",to);nbt.putDouble(str+"rs",rs)
        Utils.saveItem(cable)?.let { nbt.put(str+"cable",it) }
        if(thermals.isNotEmpty())thermals.forEachIndexed { i,t->if(i==0)t.write(nbt) else nbt.put("spanThermal$i",CompoundTag().also(t::write)) }
        else savedThermals?.let { tag->for(key in tag.keySet())if(key.startsWith("span"))tag.get(key)?.let { nbt.put(key,it.copy()) } }
    }
    companion object {
        private fun publish(e: GridElement) {
            e.updateIdealRenderAngle();e.needPublish()
            e.gridLinkList.forEach { it.getOtherElement(e)?.needPublish() }
        }
        @JvmStatic fun getElementFromCoordinate(c: Coordonate?): GridElement?=if(c==null)null else NodeManager.instance?.getTransparentNodeFromCoordinate(c) as? GridElement
        @JvmStatic fun addLink(a: GridElement,b: GridElement,sideA: Direction,sideB: Direction,cable: ElectricalCableDescriptor,length: Int)=addLink(a,b,sideA.int,sideB.int,cable,length)
        @JvmStatic fun addLink(a: GridElement,b: GridElement,from: Int,to: Int,cable: ElectricalCableDescriptor,length: Int): Boolean = addLink(a,b,from,to,cable,length,null)
        @JvmStatic fun addLink(a: GridElement,b: GridElement,from: Int,to: Int,cable: ElectricalCableDescriptor,length: Int,paid: ItemStack?): Boolean {
            require(a!==b && a.coordonate().dimention==b.coordonate().dimention) { tr("Invalid grid cable span") }
            require(a.allowsNewGridLinks() && b.allowsNewGridLinks()) { tr("Terminate overhead wires at a Service Weatherhead; use placed wall cables for meters and panels") }
            require(a.acceptsGridCable(cable) && b.acceptsGridCable(cable) && GridCablePolicy.acceptsTerminal(cable,from) && GridCablePolicy.acceptsTerminal(cable,to)) { tr("Grid links require an intact power cable") }
            val x=a.gridLoads(from);val y=b.gridLoads(to)
            require(x.isNotEmpty() && x.size==y.size) { tr("Select matching grid terminal circuits") }
            val ka=a.gridCircuitKey(from);val kb=b.gridCircuitKey(to)
            require(ka==kb || ka=="LEGACY" || kb=="LEGACY") { tr("Grid phases or service voltages do not match") }
            val distance=a.coordonate().trueDistanceTo(b.coordonate())
            require(distance<=minOf(a.connectRange,b.connectRange)) { tr("Grid cable span exceeds the endpoint range") }
            require(length>=kotlin.math.ceil(distance).toInt()*x.size && length>0 && (cable is UtilityCableDescriptor || length<=cable.newItemStack().maxStackSize)) { tr("Grid cable item does not match the paid length") }
            val old=a.gridLinkList+b.gridLinkList
            if(old.any { link->
                if(!link.links(a,b))return@any false
                val ox=a.gridLoads(link.getTerminal(a));val oy=b.gridLoads(link.getTerminal(b))
                ox.zip(oy).any { (u,v)->x.indices.any { u===x[it] && v===y[it] } }
            })return false
            val stack=paid?.copy() ?: if(cable is UtilityCableDescriptor)cable.newItemStack().also { cable.setRemainingLengthMeters(it,length.toDouble()) } else cable.newItemStack(length)
            val correct=if(cable is UtilityCableDescriptor)stack.count==1 && kotlin.math.abs(cable.getRemainingLengthMeters(stack)-length)<=UtilityCableDescriptor.LENGTH_METERS_EPSILON else stack.count==length
            require(correct && mods.eln.generic.GenericItemBlockUsingDamageDescriptor.getDescriptor(stack)===cable) { tr("Grid cable item does not match the paid length") }
            val ohms=if(cable is UtilityCableDescriptor)cable.resistanceOhms(length.toDouble()/x.size) else cable.electricalRs*length/x.size
            val link=GridLink(a.coordonate(),b.coordonate(),from,to,stack,ohms)
            if(!link.connect()) { link.onBreakElement();throw IllegalArgumentException(tr("Grid endpoint is no longer available")) }
            return true
        }
    }
}

/** Cable resistance is additional to the two terminal contacts, including after temperature changes. */
class GridSpanConnection(private val from: ElectricalLoad,private val to: ElectricalLoad,spanOhms: Double): ElectricalConnection(from,to) {
    var spanOhms=spanOhms
        set(value) { require(value.isFinite() && value>=0);if(field!=value) { field=value;notifyRsChange() } }
    init { require(spanOhms.isFinite() && spanOhms>=0) }
    override fun notifyRsChange() { r=spanOhms+from.rs+to.rs }
}
