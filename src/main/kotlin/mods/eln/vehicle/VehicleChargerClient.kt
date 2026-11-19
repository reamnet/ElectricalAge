package mods.eln.vehicle

import mods.eln.Eln
import mods.eln.client.power.PowerPanelDisplay
import mods.eln.gl.GL11
import mods.eln.i18n.I18N
import mods.eln.misc.*
import mods.eln.node.transparent.*
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.io.DataInputStream

class VehicleChargerDescriptor:TransparentNodeDescriptor(TITLE,VehicleChargerElement::class.java,VehicleChargerRender::class.java) {
    companion object{val TITLE=I18N.TR_NAME(I18N.Type.NONE,"240 V 100 A Vehicle Charger")}
    private val shapes=java.util.concurrent.ConcurrentHashMap<Direction,VoxelShape>()
    override fun blockProperties(p:net.minecraft.world.level.block.state.BlockBehaviour.Properties)=super.blockProperties(p).dynamicShape()
    override fun hasOrientedShape()=true
    override fun orientedShape(front:Direction)=shapes.computeIfAbsent(front) {
        val side=front==Direction.XN||front==Direction.XP
        if(side)Shapes.or(Shapes.create(AABB(.225,0.0,.18,.775,.13,.82)),Shapes.create(AABB(.35,.13,.28,.65,1.63,.72)))
        else Shapes.or(Shapes.create(AABB(.18,0.0,.225,.82,.13,.775)),Shapes.create(AABB(.28,.13,.35,.72,1.63,.65)))
    }
    fun draw(front:Direction=Direction.ZP,volts:Double=0.0,watts:Double=0.0,on:Boolean=false) {
        GL11.glPushMatrix();front.inverse.glRotateZnRef();GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);val model=requireNotNull(Eln.obj.getObj("vehicle_charger"))
            fun p(name:String,r:Float,g:Float,b:Float){GL11.glColor3f(r,g,b);model.getPart(name)?.drawNoBind()}
            p("Body",.76f,.79f,.82f);p("Trim",.08f,.11f,.14f);p("Lead",.06f,.07f,.08f);p("Socket",.16f,.48f,.72f)
            PowerPanelDisplay.equipmentNumber(model,volts,volts>1,"Voltage");PowerPanelDisplay.equipmentNumber(model,watts,volts>1,"Power");PowerPanelDisplay.lamp(model,"Indicator",on,.12f,.9f,.4f)
        }finally{GL11.glPopAttrib();GL11.glPopMatrix()}
    }
    override fun hasItemDraw()=true
    override fun drawItem(entity:Boolean)=draw(Direction.ZN)
}
class VehicleChargerRender(entity:TransparentNodeEntity,descriptor:TransparentNodeDescriptor):TransparentNodeElementRender(entity,descriptor) {
    var volts=0.0;var watts=0.0;var stored=0.0;var state="IDLE";var target=-1
    override fun networkUnserialize(input:DataInputStream){super.networkUnserialize(input);volts=input.readDouble();watts=input.readDouble();stored=input.readDouble();state=input.readUTF();target=input.readLong().toInt()}
    override fun draw() {
        (transparentNodedescriptor as VehicleChargerDescriptor).draw(front,volts,watts,state=="CHARGING")
        val world=tileEntity.level?:return;val car=world.getEntity(target) as? RoadVehicleEntity?:return
        val origin=Vec3.atCenterOf(tileEntity.blockPos)
        if(car.position().distanceToSqr(origin)>40)return
        val start=origin.add(mods.eln.transparentnode.cord.CordGeometry.world(front,Vec3(0.0,.28,.23)))
        val end=car.position().add(Vec3(-.68,.72,if(car.kind==RoadVehicleKind.CART)-1.36 else -2.02).yRot(-car.yRot*kotlin.math.PI.toFloat()/180))
        mods.eln.transparentnode.cord.CordPath.draw(mods.eln.transparentnode.cord.CordPath.samples(start,end){x,z,y->mods.eln.transparentnode.cord.CordPath.floor(world,x,z,y,tileEntity.blockPos)},origin)
    }
    override fun renderBounds():AABB {
        val box=AABB(tileEntity.blockPos).inflate(1.0)
        val car=tileEntity.level?.getEntity(target) as? RoadVehicleEntity ?: return box
        return if(car.position().distanceToSqr(Vec3.atCenterOf(tileEntity.blockPos))<=40)box.minmax(car.boundingBox.inflate(1.5)).expandTowards(0.0,-5.0,0.0)else box
    }
    override fun cameraDrawOptimisation()=false
}
