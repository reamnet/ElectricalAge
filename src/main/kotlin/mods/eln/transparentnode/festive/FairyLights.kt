package mods.eln.transparentnode.festive

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.transparentnode.cord.CordPath
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.*
import net.minecraft.world.phys.*
import java.io.*
import kotlin.math.*

/** Suspended wire, distinct from the terrain-following power lead. Both ends are exact anchors. */
object FairyLightPath {
    fun bulbPoints(points: List<Vec3>,large: Boolean): List<Vec3> {
        if(!large || points.size<2)return points
        var distance=0.0
        return points.filterIndexed { i,p ->
            if(i>0)distance+=p.distanceTo(points[i-1])
            if(i==0 || distance>=.48 || i==points.lastIndex) { distance=0.0;true } else false
        }
    }
    fun points(start: Vec3,end: Vec3): List<Vec3> {
        val distance=start.distanceTo(end)
        if(!distance.isFinite() || distance>8.01)return emptyList()
        val n=ceil(distance/.10).toInt().coerceIn(2,96)
        val sag=min(1.25,hypot(end.x-start.x,end.z-start.z)*.15)
        return (0..n).map { i -> val t=i.toDouble()/n;start.lerp(end,t).add(0.0,-4*t*(1-t)*sag,0.0) }
    }
}
class FairyLightsDescriptor(name: String,val colored: Boolean,val largeBulbs: Boolean=false): TransparentNodeDescriptor(name,FairyLightsElement::class.java,FairyLightsRender::class.java) {
    override fun mustHaveFloor()=false
    private val preview by lazy { CachedFairyMesh(FairyLightPath.points(Vec3(-.6,.35,0.0),Vec3(.6,.35,0.0)),Vec3.ZERO,colored,largeBulbs) }
    override fun hasItemDraw()=true
    override fun drawItem(entity:Boolean)=preview.draw(true)
    override fun newBlock(p:net.minecraft.world.level.block.state.BlockBehaviour.Properties)=FairyLightsBlock(p,this)
    override fun addInformation(stack: ItemStack?,player: net.minecraft.world.entity.player.Player?,list: MutableList<String>,advanced: Boolean) {
        list.add(tr("Select two supporting surfaces up to 8 blocks apart"))
        list.add(if(largeBulbs)tr("Black suspended wire with hanging globe bulbs about 50 cm apart") else tr("Green suspended wire; miniature lamps every 10 cm"))
        list.add(tr("Use a 120 V or 240 V cord, or set a wireless lamp supply channel"))
    }
}
class FairyLightsElement(node: TransparentNode,descriptor: TransparentNodeDescriptor): StringLightsElement(node,descriptor) {
    var start=Vec3.ZERO
        private set
    var end=Vec3.ZERO
        private set
    private var lightPoints=emptyList<Vec3>()
    init { slowProcessList.add(mods.eln.sim.IProcess {
        val world=coordonate().world() as? ServerLevel
        if(world!=null && node.lightValue>0)lightPoints.filterIndexed { i,_->i%20==0 }.forEach { v ->
            val at=BlockPos.containing(v)
            if(world.chunkSource.getChunkNow(at.x shr 4,at.z shr 4)!=null)mods.eln.sixnode.lampsocket.LightBlockEntity.addLight(world,at.x,at.y,at.z,node.lightValue,5)
        }
    }) }
    private fun readSpan(tag: CompoundTag) {
        fun vec(prefix: String)=Vec3(tag.getDoubleOr(prefix+"X",0.0),tag.getDoubleOr(prefix+"Y",0.0),tag.getDoubleOr(prefix+"Z",0.0))
        val a=vec("fairyStart");val b=vec("fairyEnd")
        if(FairyLightPath.points(a,b).isNotEmpty() && a.distanceTo(b)>.1 && b.distanceTo(Vec3.atCenterOf(coordonate().pos()))<2) { start=a;end=b;lightPoints=FairyLightPath.points(a,b) }
    }
    private fun writeSpan(tag: CompoundTag) {
        for((key,v)in listOf("fairyStart" to start,"fairyEnd" to end)) { tag.putDouble(key+"X",v.x);tag.putDouble(key+"Y",v.y);tag.putDouble(key+"Z",v.z) }
    }
    override fun readFromNBT(tag: CompoundTag) { super.readFromNBT(tag);readSpan(tag) }
    override fun writeToNBT(tag: CompoundTag) { super.writeToNBT(tag);writeSpan(tag) }
    override fun readItemStackNBT(nbt: CompoundTag?) { super.readItemStackNBT(nbt);nbt?.let(::readSpan) }
    override fun networkSerialize(out: DataOutputStream) { super.networkSerialize(out);for(v in listOf(start,end)) { out.writeDouble(v.x);out.writeDouble(v.y);out.writeDouble(v.z) } }
}
class FairyLightsRender(entity: TransparentNodeEntity,descriptor: TransparentNodeDescriptor): StringLightsRender(entity,descriptor) {
    private var points=emptyList<Vec3>()
    private var mesh: CachedFairyMesh?=null
    override fun networkUnserialize(input: DataInputStream) {
        super.networkUnserialize(input)
        fun vec()=Vec3(input.readDouble(),input.readDouble(),input.readDouble())
        val next=FairyLightPath.points(vec(),vec())
        if(next!=points) { mesh?.destroy();mesh=null;points=next }
    }
    override fun draw() {
        if(mesh==null && points.isNotEmpty())mesh=CachedFairyMesh(points,Vec3.atCenterOf(tileEntity.blockPos),(transparentNodedescriptor as FairyLightsDescriptor).colored,(transparentNodedescriptor as FairyLightsDescriptor).largeBulbs)
        mesh?.draw(powered)
    }
    override fun destructor() { mesh?.destroy();mesh=null;super.destructor() }
    override fun cameraDrawOptimisation()=false
    override fun renderBounds(): AABB {
        val p=points.ifEmpty { listOf(Vec3.atCenterOf(tileEntity.blockPos)) }
        return AABB(p.minOf { it.x },p.minOf { it.y },p.minOf { it.z },p.maxOf { it.x },p.maxOf { it.y },p.maxOf { it.z }).inflate(.2)
    }
}
private class CachedFairyMesh(points: List<Vec3>,origin: Vec3,colored: Boolean,large: Boolean) {
    private fun record(draw: ()->Unit)=GL11.glGenLists(1).also { GL11.glNewList(it,GL11.GL_COMPILE);draw();GL11.glEndList() }
    private val wire=record { CordPath.draw(points,origin,if(large)floatArrayOf(.035f,.04f,.035f) else floatArrayOf(.06f,.22f,.09f),if(large).012 else .008) }
    private val lit=record { FairyLightMesh.bulbs(FairyLightPath.bulbPoints(points,large),origin,colored,true,large) }
    private val dark=record { FairyLightMesh.bulbs(FairyLightPath.bulbPoints(points,large),origin,colored,false,large) }
    fun draw(on: Boolean) {
        GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glCallList(wire)
            if(on)UtilsClient.disableLight()
            GL11.glCallList(if(on)lit else dark)
        } finally { GL11.glPopAttrib() }
    }
    fun destroy() { for(id in listOf(wire,lit,dark))GL11.glDeleteLists(id,1) }
}
object FairyLightMesh {
    private fun globe(center: Vec3,r: Double) {
        fun point(row: Int,col: Int) {
            val theta=PI*row/6;val phi=2*PI*col/10
            val x=sin(theta)*cos(phi);val y=cos(theta);val z=sin(theta)*sin(phi)
            GL11.glNormal3f((x).toFloat(),(y).toFloat(),(z).toFloat());GL11.glVertex3d(center.x+r*x,center.y+r*y,center.z+r*z)
        }
        for(row in 0 until 6)for(col in 0 until 10) { point(row,col);point(row,col+1);point(row+1,col+1);point(row+1,col) }
    }
    fun bulbs(points: List<Vec3>,origin: Vec3,colored: Boolean,on: Boolean,large: Boolean=false) {
        if(points.isEmpty())return
        GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            if(on)UtilsClient.disableLight()
            GL11.glBegin(GL11.GL_QUADS)
            points.forEachIndexed { i,point ->
                val color=if(!on)floatArrayOf(.16f,.19f,.17f) else if(!colored)floatArrayOf(1f,.91f,.70f) else when(i%4) {
                    0->floatArrayOf(1f,.08f,.04f);1->floatArrayOf(.12f,1f,.20f);2->floatArrayOf(.12f,.35f,1f);else->floatArrayOf(1f,.70f,.04f)
                }
                GL11.glColor3f(color[0],color[1],color[2])
                if(large) {
                    val center=point.subtract(origin).add(0.0,-.105,0.0)
                    globe(center,.055)
                    GL11.glColor3f(.07f,.075f,.07f)
                    val socket=point.subtract(origin).add(0.0,-.04,0.0)
                    for(axis in 0..2)for(sign in listOf(-1,1)) {
                        val n=DoubleArray(3);n[axis]=sign.toDouble();GL11.glNormal3f((n[0]).toFloat(),(n[1]).toFloat(),(n[2]).toFloat())
                        val offsets=if(sign>0)listOf(-1 to -1,1 to -1,1 to 1,-1 to 1) else listOf(-1 to 1,1 to 1,1 to -1,-1 to -1)
                        for((u,v)in offsets) {
                            val q=doubleArrayOf(0.0,0.0,0.0);q[axis]=sign.toDouble();q[(axis+1)%3]=u.toDouble();q[(axis+2)%3]=v.toDouble()
                            GL11.glVertex3d(socket.x+q[0]*.021,socket.y+q[1]*.035,socket.z+q[2]*.021)
                        }
                    }
                    return@forEachIndexed
                }
                val p=point.subtract(origin).add(0.0,-.025,0.0);val r=.018
                for(axis in 0..2)for(sign in listOf(-1,1)) {
                    val normal=DoubleArray(3);normal[axis]=sign.toDouble();GL11.glNormal3f((normal[0]).toFloat(),(normal[1]).toFloat(),(normal[2]).toFloat())
                    val offsets=if(sign>0)listOf(-1 to -1,1 to -1,1 to 1,-1 to 1) else listOf(-1 to 1,1 to 1,1 to -1,-1 to -1)
                    for((u,v)in offsets) {
                        val q=doubleArrayOf(p.x,p.y,p.z);q[axis]+=sign*r;q[(axis+1)%3]+=u*r;q[(axis+2)%3]+=v*r
                        GL11.glVertex3d(q[0],q[1],q[2])
                    }
                }
            }
            GL11.glEnd()
        } finally { GL11.glPopAttrib() }
    }
}

class FairyLightsBlock(p:net.minecraft.world.level.block.state.BlockBehaviour.Properties,d:FairyLightsDescriptor):TransparentNodeBlock(p,d){
    override fun getShape(s:net.minecraft.world.level.block.state.BlockState,w:net.minecraft.world.level.BlockGetter,p:BlockPos,c:net.minecraft.world.phys.shapes.CollisionContext)=net.minecraft.world.phys.shapes.Shapes.create(AABB(.42,.42,.42,.58,.58,.58))
}
