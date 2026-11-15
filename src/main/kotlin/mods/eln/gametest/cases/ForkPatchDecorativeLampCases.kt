package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.ItemNbt
import mods.eln.gl.GlFrame
import mods.eln.gui.MenuSpecs
import mods.eln.item.LampDescriptor
import mods.eln.misc.*
import mods.eln.node.six.*
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sixnode.lampsocket.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeType
import java.io.*
import kotlin.math.abs

/** Real placement, electrical simulation, publication and saved owners for every fixed fixture. */
object ForkPatchDecorativeLampCases {
    @JvmStatic fun cases(): List<TestCase> = DecorativeLampStyle.entries.map(::Fixture) + listOf(Insertion(),Recipes(),Wireless(DecorativeLampStyle.CHANDELIER,120),Wireless(DecorativeLampStyle.POT_BAFFLE,240))
    private open class Base: TestCase {
        override fun id()="forkpatch_decorative_base"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {}
        fun place(c: TestContext, style: DecorativeLampStyle): DecorativeLampElement {
            // Ceiling fixtures hang ceil(depth) blocks below their mount; keep that clearance fully above the room floor.
            val y=if(style.ceiling)kotlin.math.ceil(style.depth).toInt()+1 else 5
            val pos=c.at(8,y,8)
            val side=if(style.ceiling)Direction.YP else if(style.floor)Direction.YN else Direction.XN
            c.floor(pos.relative(side.toEnumFacing()))
            return c.placeSix(style.lookup,pos,side) as DecorativeLampElement
        }
        fun bulb(n: Int=1)=Eln.findItemStack("200V LED Bulb",n)
    }
    private class Fixture(val style: DecorativeLampStyle): Base() {
        lateinit var e: DecorativeLampElement
        override fun id()="forkpatch_decorative_${style.name.lowercase()}_power_save_and_render"
        var source: VoltageSource? = null
        override fun build(c: TestContext) {
            e=place(c,style)
            e.inventory.setItem(0,bulb(style.bulbs));e.inventoryChange(e.inventory)
            if(!style.wirelessOnly) {
                e.networkUnserialize(c.stream { it.writeByte(3) })
                source=VoltageSource("fixture test",e.positiveLoad,null).setU(200.0)
                Eln.simulator.addElectricalComponent(source)
            }
        }
        override fun measure(c: TestContext) {
            val d=e.sixNodeElementDescriptor as DecorativeLampDescriptor
            val b=bulb(style.bulbs);val bd=Utils.getItemObject(b) as LampDescriptor
            c.check("decorative: source identity and bulb capacity are real native inventory/menu data",d.parentItemDamage==style.lookup && e.installedBulbs==style.bulbs && MenuSpecs.of(d).slots().single().limit()==style.bulbs && MenuSpecs.of(d).slots().single().filter().test(b),style.name)
            c.check("decorative: non-bulb materials cannot enter the menu",!MenuSpecs.of(d).slots().single().filter().test(ItemStack(Items.STONE)),style.name)
            c.check("decorative: all installed bulbs contribute parallel conductance",abs(e.lampResistor.r-bd.r/style.bulbs)<1e-8 && e.getNominalLampPower(bd)==bd.nominalP*style.bulbs,style.name)
            // Exercise the real component at voltage, then the actual native lighting/aging process.

            // Cable mode for direct fixtures; wireless-only has no exposed ports.
            if(!style.wirelessOnly) {
                try {
                    c.check("decorative: source pays N bulbs rather than one",abs(e.lampResistor.p-style.bulbs*bd.nominalP)<1e-5,"${style.name}: ${e.lampResistor.p}")
                }finally { Eln.simulator.removeElectricalComponent(source!!);source!!.breakConnection() }
            } else c.check("decorative: recessed wireless-only fittings expose no cable ports",mods.eln.misc.LRDU.values().all { e.getElectricalLoad(it)==null && e.getConnectionMask(it)==0 },style.name)
            val old=e
            val reload=Reload.node(c.level(),e.coordonate.pos())
            e=(reload.fresh as SixNode).getElement(old.side) as DecorativeLampElement
            c.check("decorative: a fresh owner restores the exact installed stack",e!==old && ItemStack.matches(e.inventory.getItem(0),b) && e.installedBulbs==style.bulbs,style.name)
            mods.eln.client.ClientRenders.registerAll()
            val pos=e.coordonate.pos();val client=SixNodeEntity(pos,c.level().getBlockState(pos));client.setLevel(c.level())
            try {
                val input=DataInputStream(ByteArrayInputStream(e.sixNode.publishPacket.toByteArray()))
                input.readByte();repeat(4) { input.readInt() };input.readUTF();client.serverPublishUnserialize(input)
                val render=client.elementRenderList[e.side.int] as? DecorativeLampRender
                c.check("decorative: complete native publication creates the right renderer and bulb count",render!=null && render.bulbCount==style.bulbs && input.available()==0,style.name)
                GlFrame.beginRenderFrame();val frame=GlFrame.open()
                try {
                    // Check source mesh emission separately; whole-catalog cases also exercise render.draw().
                    DecorativeLampObjRender(style).draw(requireNotNull(render),0.0)
                    c.check("decorative: original mesh emits actual geometry",frame.batches().values.sumOf { it.vertices() }>0,style.name)
                } finally { GlFrame.close() }
            } finally { client.clientDestroy() }
        }
    }
    private class Wireless(val style: DecorativeLampStyle,val volts: Int): Base() {
        lateinit var e: DecorativeLampElement
        lateinit var supply: mods.eln.sixnode.lampsupply.LampSupplyElement
        lateinit var source: VoltageSource
        override fun id()="forkpatch_decorative_${style.name.lowercase()}_${volts}v_wireless_real_source_power"
        override fun build(c: TestContext) {
            e=place(c,style)
            e.inventory.setItem(0,bulb(style.bulbs));e.inventoryChange(e.inventory)
            e.inventory.setItem(0,Eln.findItemStack("${volts}V LED Light Bulb",style.bulbs));e.inventoryChange(e.inventory)
            supply=c.placeSix(if(volts==120)4162 else 4161,c.at(3,1,3)) as mods.eln.sixnode.lampsupply.LampSupplyElement
            val channel="fixture ${java.util.UUID.randomUUID()}"
            e.channel=channel
            supply.networkUnserialize(c.stream { it.writeByte(mods.eln.sixnode.lampsupply.LampSupplyElement.setPowerName.toInt());it.writeByte(0);it.writeUTF(channel) })
            source=VoltageSource("wireless fixture source",supply.powerLoad,null).setU(volts.toDouble())
            Eln.simulator.addElectricalComponent(source)
        }
        override fun measure(c: TestContext) { try {
            val bd=Utils.getItemObject(e.inventory.getItem(0)) as LampDescriptor
            c.check("decorative wireless: the supply pays all installed bulb loads",abs(supply.loadResistor.p-style.bulbs*bd.nominalP)<1e-4 && abs(abs(source.u*source.current)-supply.loadResistor.p)<1e-4,"${style.name}: supply=${supply.loadResistor.p}")
            c.check("decorative wireless: supplied voltage produces real block light",e.lightValue>=13 && e.installedBulbs==style.bulbs,"${style.name}: ${e.lightValue}")
        } finally { Eln.simulator.removeElectricalComponent(source);source.breakConnection() } }
    }
    private class Insertion: Base() {
        override fun id()="forkpatch_decorative_bulb_insertion_capacity_and_condition"
        override fun measure(c: TestContext) {
            val e=place(c,DecorativeLampStyle.CHANDELIER)
            val p=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
            val held=bulb(8);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,held)
            repeat(5) { check(e.onBlockActivated(p,e.side,.5f,.5f,.5f)) }
            c.check("decorative: repeated insertions fill each real bulb position exactly once",held.count==3 && e.installedBulbs==5 && !e.onBlockActivated(p,e.side,.5f,.5f,.5f),"")
            e.inventory.setItem(0,ItemStack.EMPTY)
            val used=bulb(3);ItemNbt.edit(used) { it.putDouble("life",.2) };p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,used)
            check(e.onBlockActivated(p,e.side,.5f,.5f,.5f))
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,bulb(2))
            c.check("decorative: fresh and used bulb components cannot silently merge",!e.onBlockActivated(p,e.side,.5f,.5f,.5f) && e.installedBulbs==1 && ItemNbt.get(e.inventory.getItem(0))!!.getDoubleOr("life",0.0)==.2,"")
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY)
        }
    }
    private class Recipes: Base() {
        override fun id()="forkpatch_decorative_paid_source_recipe_chain"
        override fun measure(c: TestContext) {
            val input=CraftingInput.of(3,2,listOf(ItemStack.EMPTY,ItemStack(Items.COPPER_INGOT),ItemStack.EMPTY,ItemStack.EMPTY,Eln.findItemStack("Iron Plate",1),ItemStack.EMPTY))
            val result=c.level().recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,c.level()).map { it.value().assemble(input) }.orElse(ItemStack.EMPTY)
            c.check("decorative: paid source holder recipe retains its two-item yield",result.item===Eln.findItemStack("Basic Lamp Holder",1).item && result.count==2,result.toString())
            val stripPairs=listOf(
                "Standing Drum Floor Lamp" to listOf(Eln.findItemStack("Basic Lamp Holder",1),ItemStack(Items.IRON_INGOT)),
                "Under Cabinet LED Strip" to listOf(Eln.findItemStack("Flat Lamp Socket",1),ItemStack(Items.IRON_NUGGET)),
                "Recessed LED Channel" to listOf(Eln.findItemStack("Flat Lamp Socket",1),ItemStack(Items.QUARTZ)),
                "Indirect Cove LED Strip" to listOf(Eln.findItemStack("Flat Lamp Socket",1),ItemStack(Items.DYE.white())),
                "Surface Linear LED Strip" to listOf(Eln.findItemStack("Thin Fluorescent Shop Light",1),ItemStack(Items.IRON_INGOT)),
                "Flexible Neon LED Strip" to listOf(Eln.findItemStack("Flat Lamp Socket",1),ItemStack(Items.SLIME_BALL)),
                "Grand Halo Ring Chandelier" to listOf(Eln.findItemStack("Modern Sputnik Foyer Chandelier",1),ItemStack(Items.GOLD_INGOT)),
                "Grand Crystal Cascade Chandelier" to listOf(Eln.findItemStack("Crystal Tier Foyer Chandelier",1),ItemStack(Items.DIAMOND)),
                "Sculptural Branch Chandelier" to listOf(Eln.findItemStack("Modern Sputnik Foyer Chandelier",1),ItemStack(Items.COPPER_INGOT)),
                "Spiral Rain Chandelier" to listOf(Eln.findItemStack("Crystal Tier Foyer Chandelier",1),ItemStack(Items.IRON_CHAIN)),
                "Grand Empire Chandelier" to listOf(Eln.findItemStack("Candle Arm Foyer Chandelier",1),ItemStack(Items.GOLD_INGOT)),
                "Window Candle Light" to listOf(Eln.findItemStack("Basic Lamp Holder",1),ItemStack(Items.HONEYCOMB)),
                "Armored Oval Bulkhead Light" to listOf(Eln.findItemStack("Robust Lamp Socket",1),ItemStack(Items.IRON_BARS)),
                "Industrial Wall Pack Light" to listOf(Eln.findItemStack("Robust Lamp Socket",1),ItemStack(Items.IRON_INGOT)),
                "Rugged Outdoor Canopy Light" to listOf(Eln.findItemStack("Robust Lamp Socket",1),ItemStack(Items.IRON_BLOCK)),
                "Industrial Yard Area Light" to listOf(Eln.findItemStack("Gooseneck Barn Light",1),ItemStack(Items.IRON_INGOT)),
                "Offshore Caged Wellglass Light" to listOf(Eln.findItemStack("Robust Lamp Socket",1),ItemStack(Items.DYE.yellow())),
                "Offshore Sealed Linear Light" to listOf(Eln.findItemStack("Thin Fluorescent Shop Light",1),ItemStack(Items.DYE.yellow())))
            for((name,ingredients) in stripPairs){
                val grid=CraftingInput.of(2,2,listOf(ingredients[0],ItemStack.EMPTY,ItemStack.EMPTY,ingredients[1]))
                val crafted=c.level().recipeAccess().getRecipeFor(RecipeType.CRAFTING,grid,c.level()).map { it.value().assemble(grid) }.orElse(ItemStack.EMPTY)
                c.check("decorative: the paid shapeless $name recipe resolves from its source pair",crafted.item===Eln.findItemStack(name,1).item,"$name: $crafted")
            }
            c.check("decorative bulbs: occupied source IDs keep both existing native bulb identities",(Eln.sharedItem.getDescriptor(293) as LampDescriptor).nominalU==50.0 && (Eln.sharedItem.getDescriptor(294) as LampDescriptor).nominalU==200.0 && RatedLampBulbs.destinationIdForSource(293)==302 && RatedLampBulbs.destinationIdForSource(294)==303,"")
            for(volts in listOf(120,240)) {
                val rated=Eln.findItemStack("${volts}V LED Light Bulb",1)
                val bd=Utils.getItemObject(rated) as LampDescriptor
                c.check("decorative bulbs: source LED voltage, power and lifetime are retained",bd.nominalU==volts.toDouble() && bd.nominalP==(if(volts==120)15.0 else 22.5) && bd.nominalLife==672.0*3600,"$volts V")
                val glass=ItemStack(Items.GLASS_PANE);val silicon=Eln.findItemStack("Silicon Ingot",1)
                val cable=Eln.findItemStack(if(volts==120)"Low Voltage Cable" else "Medium Voltage Cable",1)
                val plate=Eln.findItemStack("Iron Plate",1)
                fun craft(items: List<ItemStack>): ItemStack {
                    val grid=CraftingInput.of(3,3,items)
                    return c.level().recipeAccess().getRecipeFor(RecipeType.CRAFTING,grid,c.level()).map { it.value().assemble(grid) }.orElse(ItemStack.EMPTY)
                }
                val legacy=craft(listOf(glass,glass,glass,silicon,silicon,silicon,ItemStack.EMPTY,cable,ItemStack.EMPTY))
                val modern=craft(listOf(glass,glass,glass,silicon,silicon,silicon,plate,cable,plate))
                c.check("decorative bulbs: the old recipe still makes its established voltage",legacy.item===Eln.findItemStack(if(volts==120)"50V LED Bulb" else "200V LED Bulb",1).item && legacy.count==2,"$volts V: $legacy")
                c.check("decorative bulbs: the distinct paid recipe makes two rated bulbs",modern.item===rated.item && modern.count==2,"$volts V: $modern")
            }
        }
    }
}
