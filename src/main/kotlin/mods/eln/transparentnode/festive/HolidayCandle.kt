package mods.eln.transparentnode.festive

import mods.eln.misc.Direction
import mods.eln.misc.Obj3D
import mods.eln.misc.UtilsClient
import mods.eln.node.transparent.*
import mods.eln.gl.GL11
import java.io.DataInputStream
import java.io.IOException

class HolidayCandleDescriptor(val name: String, val obj: Obj3D): TransparentNodeDescriptor(name, HolidayCandleElement::class.java, HolidayCandleRender::class.java) {
    private var base: Obj3D.Obj3DPart? = null
    private var glass: Obj3D.Obj3DPart? = null
    private var light: Obj3D.Obj3DPart? = null

    init {
        base = obj.getPart("CandleLamp_Cylinder.001")
        glass = obj.getPart("Glass_Cylinder.000")
        light = obj.getPart("LampOn_Cylinder.002")
    }

    override fun mustHaveFloor()=false
    override fun checkCanPlace(coord: mods.eln.misc.Coordonate?, front: Direction): String? {
        if(coord!=null) {
            val below=coord.pos().below();val world=coord.world()
            val top=mods.eln.sixnode.lampsocket.LampMounting.surface(world.getBlockState(below).getCollisionShape(world,below).toAabbs(),Direction.YN)
            if(top==null)return mods.eln.i18n.I18N.tr("Place the candle on a supporting surface")
        }
        return super.checkCanPlace(coord,front)
    }
    override fun hasItemDraw()=true
    override fun drawItem(entity:Boolean)=mods.eln.misc.preserveMatrix{draw(Direction.ZP,false)}
    override fun newBlock(p:net.minecraft.world.level.block.state.BlockBehaviour.Properties)=HolidayCandleBlock(p,this)
    fun draw(front: Direction, powered: Boolean) {
        if (base != null && light != null && glass != null) {
            front.glRotateZnRef()
            GL11.glTranslatef(-0.5f, -0.5f, 0.5f)
            //GL11.glScalef(0.5f, 0.5f, 0.5f)
            //UtilsClient.drawLight(led);
            base?.draw()
            UtilsClient.disableCulling()
            if (powered) UtilsClient.drawLight(light) else light?.draw()
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            glass?.draw()
            GL11.glDisable(GL11.GL_BLEND)
            UtilsClient.enableCulling()
        }
    }
}

class HolidayCandleRender(tileEntity: TransparentNodeEntity, transparentNodedescriptor: TransparentNodeDescriptor): StringLightsRender(tileEntity, transparentNodedescriptor) {


    override fun draw() {
        mods.eln.misc.preserveMatrix { GL11.glTranslated(0.0,offset,0.0);(transparentNodedescriptor as HolidayCandleDescriptor).draw(front,powered) }
    }
    var offset=0.0;private set
    override fun networkUnserialize(stream:DataInputStream){super.networkUnserialize(stream);offset=stream.readDouble()}
    override fun renderBounds()=net.minecraft.world.phys.AABB(tileEntity.blockPos).inflate(1.0)
}
class HolidayCandleElement(node:TransparentNode,d:TransparentNodeDescriptor):StringLightsElement(node,d) {
    val mountOffset:Double get(){val w=world();val p=coordonate().pos().below();return (mods.eln.sixnode.lampsocket.LampMounting.surface(w.getBlockState(p).getCollisionShape(w,p).toAabbs(),Direction.YN)?:1.0)-1.0}
    override fun networkSerialize(out:java.io.DataOutputStream){super.networkSerialize(out);out.writeDouble(mountOffset)}
    override fun onBreakElement(){if(mods.eln.transparentnode.cord.CordSupport.moving(this)){if(useUuid())stop(getUuid());return};super.onBreakElement()}
}
class HolidayCandleBlock(p:net.minecraft.world.level.block.state.BlockBehaviour.Properties,d:HolidayCandleDescriptor):TransparentNodeBlock(p,d){
    override fun getShape(s:net.minecraft.world.level.block.state.BlockState,w:net.minecraft.world.level.BlockGetter,p:net.minecraft.core.BlockPos,c:net.minecraft.world.phys.shapes.CollisionContext):net.minecraft.world.phys.shapes.VoxelShape {
        val below=p.below();val offset=(mods.eln.sixnode.lampsocket.LampMounting.surface(w.getBlockState(below).getCollisionShape(w,below).toAabbs(),Direction.YN)?:1.0)-1.0
        return net.minecraft.world.phys.shapes.Shapes.create(net.minecraft.world.phys.AABB(.38,offset,.38,.62,offset+.6,.62))
    }
}
