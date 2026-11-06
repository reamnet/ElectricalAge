package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.gl.GlFrame
import mods.eln.misc.Direction
import mods.eln.node.ElnRenders
import mods.eln.node.transparent.*
import net.minecraft.world.level.block.Blocks
import java.io.*

/** Exercise the actual client publication/factory path, not direct render construction. */
object ForkPatchClientFactoryCases {
    private val names=listOf("MPPT Solar Controller","Wall-mounted Hybrid Solar Inverter","Programmable UPS","Wall-mounted Programmable UPS",
        "Automatic Transfer Switch","Standby Gasoline Generator","16 kWh LiFePO4 Battery Cabinet","16 kWh Wall-mounted LiFePO4 Battery",
        "Off-grid Inverter Charger","Protective Relay Cabinet","Automatic Load Shedding Controller","Pad Mounted Neighbourhood Transformer",
        "North American Meter Base","120/240V Service Weatherhead","120/240V Underground Service Handhole","Mains Step-down Unit","Four Pole Changeover Relay","Programmable Load Bank")
    @JvmStatic fun cases(): List<TestCase> = listOf(Registry())+names.map(::Published)
    private open class Base:TestCase {
        override fun id()="forkpatch_client_factory_base"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {}
    }
    private class Registry:Base() {
        override fun id()="forkpatch_client_factory_registry_complete"
        override fun measure(c: TestContext) {
            mods.eln.client.ClientRenders.registerAll()
            val missing=Eln.transparentNodeItem.all().values.filter { ElnRenders.transparent(it)==null }.map { it.name }
            c.check("client factories: every registered transparent device has a native renderer factory",missing.isEmpty(),missing.joinToString())
        }
    }
    private class Published(val name: String):Base() {
        lateinit var e: TransparentNodeElement
        override fun id()="forkpatch_client_factory_"+name.lowercase().replace(Regex("[^a-z0-9]+"),"_")
        override fun build(c: TestContext) {
            mods.eln.client.ClientRenders.registerAll()
            val stack=Eln.findItemStack(name,1);val d=GenericItemBlockUsingDamageDescriptor.getDescriptor(stack) as TransparentNodeDescriptor
            val at=c.at(8,1,8)
            if(d.mustHaveWallFrontInverse()) {
                c.level().setBlockAndUpdate(at.south(),Blocks.STONE.defaultBlockState());c.level().setBlockAndUpdate(at.south().above(),Blocks.STONE.defaultBlockState())
                val player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
                val node=(d.parentItem as TransparentNodeItem).placeAt(stack,player,c.level(),at,Direction.ZP)
                check(node!=null) { "Wall factory fixture could not be placed: $name" };e=node.element
            }else e=c.placeTransparent(d.parentItemDamage,at)
        }
        override fun measure(c: TestContext) {
            val at=e.node.coordonate.pos();val client=TransparentNodeEntity(at,c.level().getBlockState(at));client.setLevel(c.level())
            try {
                val packet=e.node.publishPacket.toByteArray();val input=DataInputStream(ByteArrayInputStream(packet))
                input.readByte();repeat(4) { input.readInt() };input.readUTF();client.serverPublishUnserialize(input)
                val render=client.elementRender
                c.check("client factories: actual published node constructs its declared renderer",render!=null && render.javaClass==e.descriptor.RenderClass,name)
                c.check("client factories: renderer consumes the complete native publication",input.available()==0,"$name: ${input.available()} bytes remain")
                if(render!=null) {
                    GlFrame.beginRenderFrame();val frame=GlFrame.open()
                    try { render.draw();c.check("client factories: registered renderer emits real world geometry",frame.batches().values.sumOf { it.vertices() }>0,name) }finally { GlFrame.close() }
                }
            }finally { client.clientDestroy() }
        }
    }
}
