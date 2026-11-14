package mods.eln.transparentnode.cord

import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.server.level.ServerPlayer
import java.io.*
import java.util.*

/** Passive contacts and durable socket reservations; protection remains upstream. */
class OutletElement(node: TransparentNode,d: TransparentNodeDescriptor): TransparentNodeElement(node,d),CordDevice {
    val desc=d as OutletDescriptor
    val rating get()=desc.rating
    val capacity get()=desc.sockets
    val input=NbtElectricalLoad("outletInput").apply { rs=.002 }
    val output=NbtElectricalLoad("outletOutput").apply { rs=.002 }
    val contact=Resistor(input,output).apply { r=.005 }
    val wirelessPower=WirelessOutletPower(this)
    override val cord=ApplianceCord(this,this,input)
    var wallState=net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();private set
    val recessed get()=!wallState.isAir
    private var suppressDeviceDrop=false
    fun installWall(state: net.minecraft.world.level.block.state.BlockState) {
        require(!desc.strip && OutletWall.eligible(state))
        wallState=state
        node.setLightValue(state.lightEmission)
    }
    fun placementPending(pending: Boolean) { suppressDeviceDrop=pending }
    fun removeRecessed(dropDevice: Boolean) { suppressDeviceDrop=!dropDevice;node.onBreakBlock() }
    override fun getDropItemStack()=if(suppressDeviceDrop)net.minecraft.world.item.ItemStack.EMPTY else super.getDropItemStack()
    override fun checkCanStay(onCreate: Boolean) { if(!recessed)super.checkCanStay(onCreate) }
    var id=UUID.randomUUID();private set
    var wireless=false;private set
    var channel="Default channel";private set
    var horizontal=false;private set
    private val reservations=arrayOfNulls<UUID>(capacity)
    private val occupants=arrayOfNulls<ApplianceCord>(capacity)
    private val known=arrayOfNulls<java.lang.ref.WeakReference<ApplianceCord>>(capacity)
    private var broken=false
    private var timer=0.0
    private val items=TransparentNodeElementInventory(0,1,this)
    init {
        electricalLoadList.add(input);electricalLoadList.add(output);electricalComponentList.add(contact)
        slowProcessList.add(IProcess { dt->if(dt.isFinite() && dt>0) { timer-=dt;if(timer<=0) { timer=.5;wirelessPower.refresh() } } })
        slowProcessList.add(NodePeriodicPublishProcess(node,.5,0.0))
    }
    override fun getInventory()=items
    override fun initialize() { connect() }
    override fun acceptsCord(rating: CordRating)=desc.strip && rating==this.rating
    fun occupied(slot: Int)=slot in reservations.indices && reservations[slot]!=null
    fun freeSocket()=reservations.indices.firstOrNull { !occupied(it) }
    private fun otherOwner(c: ApplianceCord,slot: Int)=known[slot]?.get()?.let { it!==c && CordSupport.resident(it.owner) } ?: false
    fun canClaim(c: ApplianceCord,slot: Int)=slot in reservations.indices && reservations[slot]==c.token && !otherOwner(c,slot) && (occupants[slot]==null || occupants[slot]===c)
    fun duplicateClaim(c: ApplianceCord,slot: Int)=slot in reservations.indices && reservations[slot]==c.token && otherOwner(c,slot)
    fun reserve(c: ApplianceCord,slot: Int): Boolean {
        if(slot !in reservations.indices || reservations[slot]!=null && reservations[slot]!=c.token || occupants[slot]!=null && occupants[slot]!==c)return false
        if(otherOwner(c,slot) || reservations.indices.any { it!=slot && reservations[it]==c.token })return false
        reservations[slot]=c.token;known[slot]=java.lang.ref.WeakReference(c);needPublish();return true
    }
    fun claim(c: ApplianceCord,slot: Int): Boolean { if(!canClaim(c,slot))return false;occupants[slot]=c;known[slot]=java.lang.ref.WeakReference(c);return true }
    fun release(c: ApplianceCord,forget: Boolean) {
        var changed=false
        for(i in reservations.indices) {
            if(occupants[i]===c)occupants[i]=null
            if(forget && reservations[i]==c.token && !otherOwner(c,i) && (occupants[i]==null || occupants[i]===c)) { reservations[i]=null;known[i]=null;changed=true }
        }
        if(changed)needPublish()
    }
    fun disconnectSocket(slot: Int,player: ServerPlayer?=null) {
        if(slot !in reservations.indices)return
        known[slot]?.get()?.takeIf { CordSupport.resident(it.owner) }?.detach(player)
        occupants[slot]=null;known[slot]=null;reservations[slot]=null;needPublish()
    }
    fun anchor(slot: Int)=CordAnchor(node.coordonate.dimention,node.coordonate.pos(),id,front,rating,slot,horizontal,desc.strip,recessed)
    override fun disconnectJob() {
        occupants.filterNotNull().toList().forEach { it.disconnect() }
        wirelessPower.disconnect();super.disconnectJob()
    }
    override fun onBreakElement() {
        if(broken)return;broken=true
        node.setLightValue(0)
        known.mapNotNull { it?.get() }.filter { CordSupport.resident(it.owner) }.toList().forEach { it.detach() }
        super.onBreakElement()
    }
    override fun getElectricalLoad(side: Direction,edge: LRDU)=if(!wireless && !cord.plugged && (if(recessed)side==front && OutletWiring.endpoint(this,edge)!=null else desc.acceptsCable(front,side,edge)))input else null
    override fun getConnectionMask(side: Direction,edge: LRDU)=if(getElectricalLoad(side,edge)!=null)NodeBase.maskElectricalPower else 0
    override fun getThermalLoad(side: Direction,edge: LRDU): mods.eln.sim.ThermalLoad?=null
    override fun hasGui()=true
    override fun onBlockActivated(player: Player,side: Direction,x: Float,y: Float,z: Float)=false
    override fun onNeighborBlockChange() { super.onNeighborBlockChange();if(!recessed)desc.checkWallSupport(this) }
    override fun multiMeterString(side: Direction)=Utils.plotUIP(input.u,contact.current)
    override fun thermoMeterString(side: Direction)=""
    override fun networkUnserialize(s: DataInputStream): Byte {
        val b=super.networkUnserialize(s)
        try { when(b.toInt()) {
            81 -> { reservations.indices.forEach { disconnectSocket(it) };id=UUID.randomUUID();needPublish() }
            82 -> { wireless=!wireless;wirelessPower.disconnect();reconnect();timer=0.0;needPublish() }
            83 -> { val next=s.readUTF().take(128);if(next!=channel) { channel=next;wirelessPower.disconnect();timer=0.0;needPublish() } }
            84 -> { horizontal=!horizontal;val world=node.coordonate.world();val pos=node.coordonate.pos();val state=world.getBlockState(pos);if(state.block is OutletBlock)world.setBlock(pos,state.setValue(OutletBlock.HORIZONTAL,horizontal),3);known.mapNotNull { it?.get() }.filter { CordSupport.active(it.owner) }.forEach { it.poll() };needPublish() }
            85 -> disconnectSocket(s.readInt())
        } }catch(_: IOException) {}
        return b
    }
    override fun readFromNBT(t: CompoundTag) {
        super.readFromNBT(t)
        if(t.contains("outletWall")) {
            val wall=net.minecraft.nbt.NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK,t.getCompoundOrEmpty("outletWall"))
            require(!desc.strip && OutletWall.eligible(wall)) { "Invalid saved recessed outlet wall" }
            wallState=wall
        }
        id=CordNbt.uuid(t,"outletId") ?: id
        wireless=t.getBooleanOr("outletWireless",false);channel=t.getStringOr("outletChannel","Default channel").take(128);horizontal=t.getBooleanOr("outletHorizontal",false)
        for(i in reservations.indices)reservations[i]=CordNbt.uuid(t,if(i==0)"outletReservation" else "outletReservation$i")
    }
    override fun writeToNBT(t: CompoundTag) {
        super.writeToNBT(t);if(recessed)t.put("outletWall",net.minecraft.nbt.NbtUtils.writeBlockState(wallState));CordNbt.putUuid(t,"outletId",id);t.putBoolean("outletWireless",wireless);t.putString("outletChannel",channel);t.putBoolean("outletHorizontal",horizontal)
        reservations.forEachIndexed { i,value->if(value!=null)CordNbt.putUuid(t,if(i==0)"outletReservation" else "outletReservation$i",value) }
    }
    override fun getItemStackNBT()=CompoundTag().apply { putBoolean("outletHorizontal",horizontal) }
    override fun readItemStackNBT(t: CompoundTag?) { horizontal=t?.getBooleanOr("outletHorizontal",false) ?: false }
    override fun networkSerialize(s: DataOutputStream) {
        super.networkSerialize(s);s.writeLong(id.mostSignificantBits);s.writeLong(id.leastSignificantBits);s.writeBoolean(wireless);s.writeUTF(channel);s.writeBoolean(wireless && kotlin.math.abs(input.u)>1.0)
        s.writeBoolean(horizontal);s.writeInt(reservations.indices.fold(0) { bits,i->if(occupied(i))bits or (1 shl i) else bits });s.writeDouble(input.u);s.writeDouble(contact.current);s.writeInt(net.minecraft.world.level.block.Block.getId(wallState))
    }
    override fun getWaila()=linkedMapOf(tr("Sockets") to tr("%1$ / %2$ occupied",reservations.count { it!=null },capacity),tr("Voltage") to Utils.plotVolt("",input.u))
}
