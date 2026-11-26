package mods.eln.gametest.cases

import mods.eln.gametest.*
import mods.eln.gl.GlFrame
import mods.eln.gridnode.*
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sixnode.electricalcable.*
import mods.eln.utility.*
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
        var normalizedWidth: Double?=null
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
                    var low=Double.POSITIVE_INFINITY;var high=Double.NEGATIVE_INFINITY;var vertices=0
                    val expectedTint=if(wire.insulated)-1 else if(wire.material==UtilityCableMaterial.COPPER)0xffb87345.toInt() else 0xffaeb7bd.toInt()
                    for((key,batch)in frame.batches()) {
                        c.check("bare spans bind an existing neutral material texture",key.texture()?.path==if(wire.insulated)"textures/wire.png" else "sprites/white.png",wire.name)
                        for(index in 0 until batch.floats step GlFrame.STRIDE) {
                            val z=batch.data[index+2].toDouble();low=minOf(low,z);high=maxOf(high,z);vertices++
                            check(java.lang.Float.floatToRawIntBits(batch.data[index+5])==expectedTint) { "Wrong span material colour: ${wire.name}" }
                        }
                    }
                    val width=(high-low)/wire.render.width
                    if(normalizedWidth==null)normalizedWidth=width
                    c.check("native mesh thickness scales exactly with descriptor width",vertices>500 && kotlin.math.abs(width-normalizedWidth!!)<1e-4,"${wire.name}: $width")
                }finally { GlFrame.close();GlFrame.beginRenderFrame() }
                val link=a.gridLinkList.single();val refund=link.onBreakElement()
                c.check("appearance publication preserves exact paid material",refund.item===wire.parentItem && wire.getRemainingLengthMeters(refund)==14.0 && link.onBreakElement().isEmpty,wire.name)
                last=appearance;lastRevision=revision
            }
            // Replay the pre-style packet layout from the same real one-wire publication.
            val magic=GridCableAppearance.PACKET_VERSION
            val start=(0..finalBytes.size-4).first { java.nio.ByteBuffer.wrap(finalBytes,it,4).int==magic }
            val input=DataInputStream(ByteArrayInputStream(finalBytes,start+4,finalBytes.size-start-4))
            check(input.readInt()==1)
            val old=ByteArrayOutputStream();old.write(finalBytes,0,start)
            val output=DataOutputStream(old);output.writeInt(1)
            val count=input.readInt();check(count==1);output.writeInt(count)
            repeat(6) { output.writeFloat(input.readFloat()) };output.writeBoolean(input.readBoolean())
            GridCableAppearance.read(input);output.write(input.readAllBytes())
            render.networkUnserialize(DataInputStream(ByteArrayInputStream(old.toByteArray())))
            c.check("old unversioned span publications retain native fallback appearance",render.spanGeometryRevision>lastRevision,"")
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
