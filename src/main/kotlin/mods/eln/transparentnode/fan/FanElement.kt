package mods.eln.transparentnode.fan

import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.transparentnode.cord.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.*
import java.io.*
import java.util.UUID
import kotlin.math.*

class FanElement(node: TransparentNode,descriptor: TransparentNodeDescriptor): TransparentNodeElement(node,descriptor),CordDevice {
    val type=(descriptor as FanDescriptor).type
    private val items=TransparentNodeElementInventory(0,1,this)
    override fun getInventory()=items
    val load=NbtElectricalLoad("fanInput").apply { rs=.002 }
    val motor=Resistor(load,null)
    val rotor=FanDynamics(type.start,type.coast)
    var enabled=true
    var setting=1.0
        private set
    override val cord=ApplianceCord(this,this,load)
    override fun acceptsCord(rating: CordRating)=rating.accepts(type.volts)
    override val cordPortable get()=!type.industrial
    private val hitAfter=mutableMapOf<UUID,Long>()
    private var publishedSpeed=0.0
    init {
        electricalLoadList.add(load);electricalComponentList.add(motor)
        slowProcessList.add(IProcess(::tick));slowProcessList.add(NodePeriodicPublishProcess(node,.5,0.0))
    }
    override fun initialize() { configure();connect() }
    private fun configure() {
        motor.r=if(enabled && abs(load.u)<=type.volts*1.2)type.volts*type.volts/(type.watts*setting) else 1e19
    }
    private fun tick(dt: Double) {
        configure()
        val target=if(enabled && abs(load.u) in type.volts*.8..type.volts*1.2)setting*(abs(load.u)/type.volts).coerceAtMost(1.0) else 0.0
        rotor.step(dt,target)
        if(abs(rotor.speed-publishedSpeed)>.009 || rotor.speed==0.0 && publishedSpeed!=0.0) { publishedSpeed=rotor.speed;needPublish() }
        if(type.industrial)hazard()
    }
    private fun hazard() {
        val world=world() as? ServerLevel ?: return
        if(rotor.speed<.1) { hitAfter.clear();return }
        val c=coordonate();val center=Vec3(c.x+.5,c.y+.5+type.centerY,c.z+.5)
        val entities=world.getEntitiesOfClass(LivingEntity::class.java,AABB(center,center).inflate(2.7))
        val touching=mutableSetOf<UUID>()
        for(e in entities) {
            if(!e.isAlive || e.isSpectator || !FanBlades.touches(e.boundingBox,center,front,type.radius))continue
            touching+=e.uuid
            if(world.gameTime>=(hitAfter[e.uuid] ?: Long.MIN_VALUE)) {
                // Two hits per second respect vanilla hurt cooldown; full speed is ten health/s.
                e.hurtServer(world,world.damageSources().generic(),(5.0*rotor.speed*rotor.speed).toFloat())
                hitAfter[e.uuid]=world.gameTime+10
            }
        }
        hitAfter.keys.retainAll(touching)
    }
    override fun onBreakElement() {
        if(cord.moving) { if(useUuid())stop(getUuid());return }
        super.onBreakElement()
    }
    override fun getElectricalLoad(side: Direction,lrdu: LRDU)=if(!cord.plugged && side.isNotY && lrdu==LRDU.Down)load else null
    override fun getConnectionMask(side: Direction,lrdu: LRDU)=if(getElectricalLoad(side,lrdu)!=null)NodeBase.maskElectricalPower else 0
    override fun getThermalLoad(side: Direction,lrdu: LRDU)=null
    override fun thermoMeterString(side:Direction)=""
    override fun onBlockActivated(player:Player,side:Direction,x:Float,y:Float,z:Float)=false
    override fun hasGui()=true
    override fun networkUnserialize(stream: DataInputStream): Byte {
        val id=super.networkUnserialize(stream)
        when(id.toInt()) {
            80 -> enabled=!enabled
            81 -> stream.readDouble().takeIf { it.isFinite() && it in .25..1.0 }?.let { setting=it }
        }
        configure();needPublish();return id
    }
    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream);stream.writeBoolean(enabled);stream.writeDouble(setting);stream.writeDouble(rotor.speed)
        stream.writeDouble(load.u);stream.writeDouble(motor.p);node.lrduCubeMask.getTranslate(Direction.YN).serialize(stream);
    }
    override fun readFromNBT(nbt: CompoundTag) {
        super.readFromNBT(nbt);enabled=!nbt.contains("fanEnabled") || nbt.getBooleanOr("fanEnabled",false)
        setting=nbt.getDoubleOr("fanSetting",0.0).takeIf { it.isFinite() && it in .25..1.0 } ?: 1.0
        rotor.read(nbt);
    }
    override fun writeToNBT(nbt: CompoundTag) { super.writeToNBT(nbt);nbt.putBoolean("fanEnabled",enabled);nbt.putDouble("fanSetting",setting);rotor.write(nbt) }
    fun settleCarried(tag:CompoundTag) {
        if(tag.contains("carriedAt"))rotor.step((world().gameTime-tag.getLongOr("carriedAt",0L)).coerceAtLeast(0)/20.0,0.0)
    }
    override fun multiMeterString(side: Direction)=Utils.plotVolt("U:",load.u)+Utils.plotPower("P:",motor.p)
    override fun getWaila()=mapOf(tr("Speed") to tr("%1$%",(rotor.speed*100).roundToInt()),tr("Power") to Utils.plotPower("",motor.p))
}

object FanBlades {
    fun touches(box: AABB,center: Vec3,front: Direction,radius: Double): Boolean {
        val normalX=front==Direction.XP || front==Direction.XN
        val near=if(normalX)box.minX-center.x else box.minZ-center.z
        val far=if(normalX)box.maxX-center.x else box.maxZ-center.z
        if(near>.18 || far<-.18)return false
        val lo=if(normalX)box.minZ-center.z else box.minX-center.x
        val hi=if(normalX)box.maxZ-center.z else box.maxX-center.x
        val x=0.0.coerceIn(lo,hi);val y=0.0.coerceIn(box.minY-center.y,box.maxY-center.y)
        return x*x+y*y<radius*radius
    }
}
