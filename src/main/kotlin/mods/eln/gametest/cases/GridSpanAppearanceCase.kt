package mods.eln.gametest.cases

import mods.eln.gametest.*
import mods.eln.gl.GlFrame
import mods.eln.gridnode.*
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sixnode.electricalcable.*
import mods.eln.utility.*
import net.minecraft.world.phys.Vec3
import java.io.*

/** Measure the native mesh emitted from actual paid-span publications for all seventy AWG variants. */
class GridSpanAppearanceCase: TestCase {
    override fun id()="grid_span_awg_material_and_thickness"
    override fun width()=32
    override fun height()=28
    override fun depth()=16
    override fun floorDepth()=16
    override fun ticks()=60
    override fun build(c: TestContext) {
        val kind=TransmissionKind.POLE_69
        val a=c.placeTransparent(kind.id,c.at(6,1,7)) as TransmissionElement
        val b=c.placeTransparent(kind.id,c.at(20,1,7)) as TransmissionElement
        val render=TransmissionRender(c.level().getBlockEntity(a.coordonate().pos()) as TransparentNodeEntity,a.descriptor)
        var last: GridCableAppearance?=null
        var lastRevision=0
        var finalBytes=byteArrayOf()
        fun publication(): ByteArray=ByteArrayOutputStream().also { a.networkSerialize(DataOutputStream(it)) }.toByteArray()
        try {
            val catalog=UtilityCables.all.filter { it.actsAsSingleConductor && !it.melted }
            c.check("all seventy original AWG descriptors exercised",catalog.size==70,"${catalog.size}")
            for(wire in catalog) {
                check(GridLink.addLink(a,b,32,32,wire,14))
                val bytes=publication();finalBytes=bytes
                val appearance=GridCableAppearance.of(wire)
                render.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes)))
                val revision=render.spanGeometryRevision
                if(last!=null)c.check("only width/material changes rebuild native span geometry",revision==lastRevision+if(last==appearance)0 else 1,wire.name)
                render.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes)))
                c.check("ordinary repeated publications reuse native geometry",render.spanGeometryRevision==revision,wire.name)
                GlFrame.beginRenderFrame();val frame=GlFrame.open()
                try {
                    render.drawCables()
                    val verticesByTint=HashMap<Int, MutableList<Double>>()
                    val edges=HashMap<Set<Vec3>,Int>()
                    var facesValid=true
                    for ((key,batch) in frame.batches()) {
                        c.check("AWG span uses a neutral surface without a T1 texture", key.texture()?.path=="sprites/white.png", wire.name)
                        for (index in 0 until batch.floats step GlFrame.STRIDE) {
                            val tint=java.lang.Float.floatToRawIntBits(batch.data[index+5])
                            verticesByTint.getOrPut(tint, ::ArrayList).add(batch.data[index+2].toDouble())
                        }
                        // GlFrame represents each triangle as a quad with a repeated last vertex.
                        for (offset in 0 until batch.floats step 4*GlFrame.STRIDE) {
                            val (u,v,w)=List(3) { vertex(batch,offset+it*GlFrame.STRIDE) }
                            val normal=vertex(batch,offset+6)
                            facesValid=facesValid && v.subtract(u).cross(w.subtract(u)).lengthSqr()>0 &&
                                kotlin.math.abs(normal.lengthSqr()-1)<1e-5
                            for (edge in listOf(setOf(u,v),setOf(v,w),setOf(w,u))) edges.merge(edge,1,Int::plus)
                        }
                    }
                    c.check("sag mesh is closed through terminations and jacket shoulders",
                        facesValid && edges.isNotEmpty() && edges.values.all { it==2 },wire.name)
                    val metal=if(wire.material==UtilityCableMaterial.COPPER)0xffd96b2e.toInt() else 0xffc7ccd6.toInt()
                    val expectedColours=if(wire.insulated)setOf(metal,0xff222629.toInt()) else setOf(metal)
                    c.check("installed wire emits the correct jacket and exposed conductor colours",verticesByTint.keys==expectedColours,wire.name)
                    for ((tint,z) in verticesByTint) {
                        val radius=kotlin.math.sqrt(wire.conductorAreaMm2/Math.PI)*.006
                        val expectedWidth=2*(if(tint==metal)radius else radius*1.18+.002)
                        c.check("emitted half-width follows the source dimensions",kotlin.math.abs(z.max()-z.min()-expectedWidth)<1e-5,wire.name)
                    }
                    c.check("conductor emitted a complete sag mesh",verticesByTint.values.sumOf { it.size }>=16*24,wire.name)
                }finally { GlFrame.close();GlFrame.beginRenderFrame() }
                val link=a.gridLinkList.single();val refund=link.onBreakElement()
                c.check("appearance publication preserves exact paid material",refund.item===wire.parentItem && wire.getRemainingLengthMeters(refund)==14.0 && link.onBreakElement().isEmpty,wire.name)
                last=appearance;lastRevision=revision
            }
            // Replay both earlier packet layouts from the same real one-wire publication.
            val magic=GridCableAppearance.PACKET_VERSION
            val start=(0..finalBytes.size-4).first { java.nio.ByteBuffer.wrap(finalBytes,it,4).int==magic }
            val input=DataInputStream(ByteArrayInputStream(finalBytes,start+4,finalBytes.size-start-4))
            check(input.readInt()==1)
            val old=ByteArrayOutputStream();old.write(finalBytes,0,start)
            val output=DataOutputStream(old);output.writeInt(1)
            val count=input.readInt();check(count==1);output.writeInt(count)
            repeat(6) { output.writeFloat(input.readFloat()) };output.writeBoolean(input.readBoolean())
            GridCableAppearance.read(input,false)
            val tail=input.readAllBytes()
            val prior=ByteArrayOutputStream()
            prior.write(finalBytes,0,start)
            val priorOutput=DataOutputStream(prior)
            priorOutput.writeInt(GridCableAppearance.PREVIOUS_PACKET_VERSION)
            priorOutput.write(old.toByteArray(),start,old.size()-start)
            GridCableAppearance.LEGACY.write(priorOutput)
            priorOutput.write(tail)
            render.networkUnserialize(DataInputStream(ByteArrayInputStream(prior.toByteArray())))
            c.check("GRD1 spans retain the original texture and geometry",render.spanGeometryRevision==lastRevision+1,"")
            output.write(tail)
            render.networkUnserialize(DataInputStream(ByteArrayInputStream(old.toByteArray())))
            c.check("old unversioned spans produce the same original geometry",render.spanGeometryRevision==lastRevision+1,"")
            GlFrame.beginRenderFrame();val legacy=GlFrame.open()
            try {
                render.drawCables()
                c.check("legacy render still binds its T1 wire texture",legacy.batches().any { (key,batch)->key.texture()?.path=="textures/wire.png" && batch.vertices()>0 },"")
            } finally { GlFrame.close();GlFrame.beginRenderFrame() }
            var thinNative=0.0
            for(index in listOf(0,13)) {
                val spool=OverheadCableItem.make(index,40)
                c.check("native overhead conductor preserves the same paid span",GridLink.addBareLink(a,b,32,32,spool)==14,"")
                render.networkUnserialize(DataInputStream(ByteArrayInputStream(publication())))
                GlFrame.beginRenderFrame();val frame=GlFrame.open()
                try {
                    render.drawCables()
                    val zs=ArrayList<Double>()
                    for((key,batch)in frame.batches()) {
                        check(key.texture()?.path=="sprites/white.png")
                        for(i in 0 until batch.floats step GlFrame.STRIDE) {
                            zs.add(batch.data[i+2].toDouble())
                            check(java.lang.Float.floatToRawIntBits(batch.data[i+5])==0xffb87345.toInt())
                        }
                    }
                    val extent=zs.max()-zs.min()
                    if(index==0)thinNative=extent else
                        c.check("native overhead cross-section also controls actual mesh thickness",kotlin.math.abs(extent/thinNative-2.0)<1e-4,"${extent/thinNative}")
                }finally { GlFrame.close();GlFrame.beginRenderFrame() }
                c.check("native visual changes preserve fourteen metre refund",OverheadCableItem.blocks(a.gridLinkList.single().onBreakElement())==14,"")
            }
        }finally { a.gridLinkList.toList().forEach { it.onBreakElement() };render.destructor() }
    }
    override fun measure(c: TestContext) { }
}

private fun vertex(batch: GlFrame.Batch,index: Int) =
    Vec3(batch.data[index].toDouble(),batch.data[index+1].toDouble(),batch.data[index+2].toDouble())

/** Check a matching paid span through the concrete attachment's client publication. */
internal fun checkCoveredSpan(c: TestContext, link: GridLink, wire: UtilityCableDescriptor) {
    val owner=checkNotNull(link.elementA())
    val remote=checkNotNull(link.elementB())
    val entity=c.level().getBlockEntity(owner.coordonate().pos()) as TransparentNodeEntity
    val descriptor=owner.descriptor
    val render=descriptor.RenderClass.getConstructor(
        TransparentNodeEntity::class.java,TransparentNodeDescriptor::class.java).newInstance(entity,descriptor) as GridRender
    GlFrame.beginRenderFrame()
    try {
        render.networkUnserialize(c.stream(owner::networkSerialize))
        val frame=GlFrame.open()
        try {
            render.drawCables()
            val metal=if(wire.material==UtilityCableMaterial.COPPER)0xffd96b2e.toInt() else 0xffc7ccd6.toInt()
            val jacket=0xff222629.toInt()
            val points=HashMap<Int, MutableList<Vec3>>()
            for ((key,batch) in frame.batches()) {
                if (key.texture()?.path!="sprites/white.png") continue
                for (i in 0 until batch.floats step GlFrame.STRIDE) {
                    val tint=java.lang.Float.floatToRawIntBits(batch.data[i+5])
                    points.getOrPut(tint,::ArrayList).add(vertex(batch,i))
                }
            }
            val from=link.getTerminal(owner)
            val to=link.getTerminal(remote)
            val conductorCount=owner.renderWireCount(from)
            val offset=Vec3.atLowerCornerOf(remote.coordonate().pos().subtract(owner.coordonate().pos()))
            val radius=kotlin.math.sqrt(wire.conductorAreaMm2/Math.PI)*.006
            val reach=Math.sqrt(2.0)*radius+1e-5
            c.check("client renders the paid jacket and continuity to every original clamp",
                points[jacket]?.isNotEmpty()==true && (0 until conductorCount).all { phase ->
                    val a=owner.renderedGridCablePoint(from,phase)
                    val b=remote.renderedGridCablePoint(to,phase).add(offset)
                    val ends=points[metal].orEmpty()
                    ends.any { it.distanceTo(a)<=reach } && ends.any { it.distanceTo(b)<=reach }
                },"${descriptor.name}: ${wire.name}, $conductorCount")
        } finally { GlFrame.close() }
    } finally { render.destructor();GlFrame.beginRenderFrame() }
}
