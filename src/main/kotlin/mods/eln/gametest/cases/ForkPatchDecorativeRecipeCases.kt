package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeType

/** Independent patterns transcribed from pinned ForkPatch b7444a7 CraftingRecipes/UtilityRecipes.
 * This oracle never reads native recipe definitions to construct expected ingredients. */
object ForkPatchDecorativeRecipeCases {
    private data class Source(val name: String,val count: Int,val rows: List<String>?,val ingredients: Map<Char,ItemStack>)
    @JvmStatic fun cases(): List<TestCase> = listOf(ExactSourceRecipes())
    private class ExactSourceRecipes: TestCase {
        override fun id()="forkpatch_decorative_all_108_exact_paid_source_recipes"
        override fun width()=4; override fun height()=4; override fun depth()=4; override fun floorDepth()=4
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {
            fun craft(grid: CraftingInput): ItemStack = c.level().recipeAccess().getRecipeFor(RecipeType.CRAFTING,grid,c.level()).map { it.value().assemble(grid) }.orElse(ItemStack.EMPTY)
            fun same(a: ItemStack,b: ItemStack)= !a.isEmpty && ItemStack.isSameItemSameComponents(a,b)
            val source=sourceRecipes()
            c.check("decorative source oracle covers every one of the 108 fixture identities",source.size==108 && source.map { it.name }.toSet().size==108,source.size.toString())
            val led=Eln.findItemStack("120V LED Light Bulb",1)
            val string=ItemStack(Items.STRING)
            val stringGrid=CraftingInput.of(2,1,listOf(led,string))
            val strings=craft(stringGrid)
            c.check("source String Lights bootstrap consumes rated120VLED and materialString with yield two",same(strings,Eln.findItemStack("String Lights",2)) && strings.count==2,strings.toString())
            for(single in listOf(led,string,Eln.findItemStack("48V LED Bulb",1))) {
                val missing=craft(CraftingInput.of(1,1,listOf(single)))
                c.check("String Lights bootstrap rejects missing ingredient or legacy bulb alone",!same(missing,Eln.findItemStack("String Lights",1)),missing.toString())
            }
            val wrongRating=craft(CraftingInput.of(2,1,listOf(Eln.findItemStack("48V LED Bulb",1),string)))
            c.check("String Lights bootstrap requires the source rated120VLED",!same(wrongRating,Eln.findItemStack("String Lights",1)),wrongRating.toString())
            for(s in source) {
                val expected=Eln.findItemStack(s.name,s.count)
                val rows=s.rows
                val width=rows?.first()?.length?:3;val height=rows?.size?:3
                val cells=if(rows==null) s.ingredients.values.map { it.copy() }+List(9-s.ingredients.size) { ItemStack.EMPTY }
                    else rows.flatMap { row->row.map { s.ingredients[it]?.copy()?:ItemStack.EMPTY } }
                val grid=CraftingInput.of(width,height,cells)
                val result=craft(grid)
                c.check("decorative source exact shape, materials and yield: ${s.name}",same(result,expected) && result.count==s.count,result.toString())
                // Every source ingredient is paid: deleting any occupied cell cannot yield this fixture.
                for(i in cells.indices.filter { !cells[it].isEmpty }) {
                    val short=cells.mapIndexed { j,item->if(i==j)ItemStack.EMPTY else item.copy() }
                    val missing=craft(CraftingInput.of(width,height,short))
                    c.check("decorative rejects missing paid ingredient ${s.name} cell $i",!same(missing,expected),missing.toString())
                }
                if(rows==listOf(" D ","DLD"," P ")) {
                    val base=s.ingredients.getValue('L');val material=s.ingredients.getValue('D')
                    val cheap=craft(CraftingInput.of(2,2,listOf(base.copy(),ItemStack.EMPTY,ItemStack.EMPTY,material.copy())))
                    c.check("decorative rejects previously underpaid shapeless pair: ${s.name}",!same(cheap,expected),cheap.toString())
                    val stale=when {
                        s.name=="Standing Drum Floor Lamp"->"Basic Lamp Holder"
                        s.name=="Industrial Wall Pack Light"->"Robust Lamp Socket"
                        same(base,Eln.findItemStack("String Lights",1))->"Flexible Neon LED Strip"
                        else->null
                    }
                    if(stale!=null) {
                        val wrong=rows.flatMap { row->row.map { ch->if(ch=='L')Eln.findItemStack(stale,1) else s.ingredients[ch]?.copy()?:ItemStack.EMPTY } }
                        val resultWrong=craft(CraftingInput.of(3,3,wrong))
                        c.check("decorative rejects stale substituted base: ${s.name}",!same(resultWrong,expected),resultWrong.toString())
                    }
                }
            }
        }
    }
    private fun vanilla(path: String)=ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(path)))
    private fun sourceRecipes(): List<Source> = listOf(
        Source("Twin Parking Lot Light",1,listOf("L L"," I "," P "),mapOf('L' to Eln.findItemStack("Robust Lamp Socket",1),'I' to ItemStack(Items.IRON_INGOT),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Warehouse Wall Floodlight",1,listOf("IPI","GLG"," I "),mapOf('I' to ItemStack(Items.IRON_INGOT),'P' to Eln.findItemStack("Iron Plate",1),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Lamp Socket B Projector",1))),
        Source("Household Desk Lamp",1,listOf("WWW"," L "," P "),mapOf('W' to vanilla("white_wool"),'L' to Eln.findItemStack("Lamp Socket A",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Round Recessed Ceiling Light",2,listOf(" P ","GLG"),mapOf('P' to Eln.findItemStack("Iron Plate",1),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Flat Lamp Socket",1))),
        Source("Basic Chandelier",1,listOf(" I ","LIL","LLL"),mapOf('I' to ItemStack(Items.IRON_INGOT),'L' to Eln.findItemStack("Lamp Socket A",1))),
        Source("Mini Dot Spotlight",2,listOf(" I ","GLG"),mapOf('I' to ItemStack(Items.IRON_INGOT),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Lamp Socket B Projector",1))),
        Source("Porch Lantern Sconce",1,listOf(" I ","GLG"," P "),mapOf('I' to ItemStack(Items.IRON_INGOT),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Lamp Socket A",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Basic Lamp Holder",2,listOf(" C "," P "),mapOf('C' to ItemStack(Items.COPPER_INGOT),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Thin Fluorescent Shop Light",1,listOf("PPP","LLL"),mapOf('P' to Eln.findItemStack("Iron Plate",1),'L' to Eln.findItemStack("Flat Lamp Socket",1))),
        Source("Suspended Factory Fluorescent Light",1,listOf("C C","PPP","LLL"),mapOf('C' to ItemStack(Items.IRON_CHAIN),'P' to Eln.findItemStack("Iron Plate",1),'L' to Eln.findItemStack("Thin Fluorescent Shop Light",1))),
        Source("Outdoor Motion Floodlight",1,listOf("L L"," S "," P "),mapOf('L' to Eln.findItemStack("Lamp Socket B Projector",1),'S' to Eln.findItemStack("Electrical Entity Sensor",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Candle Arm Foyer Chandelier",1,listOf(" C ","GLG","LLL"),mapOf('C' to ItemStack(Items.IRON_CHAIN),'G' to ItemStack(Items.GOLD_INGOT),'L' to Eln.findItemStack("Lamp Socket A",1))),
        Source("Crystal Tier Foyer Chandelier",1,listOf(" C ","GLG","GGG"),mapOf('C' to ItemStack(Items.IRON_CHAIN),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Candle Arm Foyer Chandelier",1))),
        Source("Wagon Wheel Foyer Chandelier",1,listOf("CCC","LPL","LLL"),mapOf('C' to ItemStack(Items.IRON_CHAIN),'P' to ItemStack(Blocks.DARK_OAK_PLANKS),'L' to Eln.findItemStack("Lamp Socket A",1))),
        Source("Lantern Cage Foyer Chandelier",1,listOf(" C ","ILI","ILI"),mapOf('C' to ItemStack(Items.IRON_CHAIN),'I' to ItemStack(Blocks.IRON_BARS),'L' to Eln.findItemStack("Lamp Socket A",1))),
        Source("Modern Sputnik Foyer Chandelier",1,listOf(" L ","LGL"," L "),mapOf('G' to ItemStack(Items.GOLD_INGOT),'L' to Eln.findItemStack("Lamp Socket A",1))),
        Source("Up Down Cylinder Wall Sconce",1,listOf(" L ","IPI"," L "),mapOf('L' to Eln.findItemStack("Lamp Socket B Projector",1),'I' to ItemStack(Items.IRON_INGOT),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Fabric Shade Wall Sconce",1,listOf("WWW"," L "," P "),mapOf('W' to vanilla("white_wool"),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Glass Globe Wall Sconce",1,listOf(" G ","GLG"," P "),mapOf('G' to ItemStack(Blocks.GLASS),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Industrial Cage Wall Sconce",1,listOf("III","ILI"," P "),mapOf('I' to ItemStack(Blocks.IRON_BARS),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Double Candle Wall Sconce",1,listOf("L L"," G "," P "),mapOf('L' to Eln.findItemStack("Basic Lamp Holder",1),'G' to ItemStack(Items.GOLD_INGOT),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Ceramic Drum Table Lamp",1,listOf("WWW"," L "," B "),mapOf('W' to vanilla("white_wool"),'L' to Eln.findItemStack("Basic Lamp Holder",1),'B' to ItemStack(Blocks.TERRACOTTA))),
        Source("Green Banker Table Lamp",1,listOf("GGG"," L "," P "),mapOf('G' to vanilla("green_stained_glass"),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to ItemStack(Items.COPPER_INGOT))),
        Source("Articulated Task Table Lamp",1,listOf(" IL"," I "," P "),mapOf('I' to ItemStack(Items.IRON_INGOT),'L' to Eln.findItemStack("Lamp Socket B Projector",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Deep Baffle Recessed Light",2,listOf(" I ","BLB"),mapOf('I' to Eln.findItemStack("Iron Plate",1),'B' to ItemStack(Items.DYE.black()),'L' to Eln.findItemStack("Flat Lamp Socket",1))),
        Source("Trimless Recessed Downlight",2,listOf(" W ","GLG"),mapOf('W' to ItemStack(Items.DYE.white()),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Flat Lamp Socket",1))),
        Source("Adjustable Gimbal Recessed Light",2,listOf(" I ","ILI"),mapOf('I' to ItemStack(Items.IRON_INGOT),'L' to Eln.findItemStack("Lamp Socket B Projector",1))),
        Source("Garden Bollard Light",1,listOf(" G ","ILI"," P "),mapOf('G' to ItemStack(Blocks.GLASS),'I' to ItemStack(Items.IRON_INGOT),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Gooseneck Barn Light",1,listOf("IIL","P  "),mapOf('I' to ItemStack(Items.IRON_INGOT),'L' to Eln.findItemStack("Robust Lamp Socket",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Traditional Lantern Post Light",1,listOf(" L "," I "," P "),mapOf('L' to Eln.findItemStack("Porch Lantern Sconce",1),'I' to ItemStack(Items.IRON_INGOT),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Window Candle Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.HONEYCOMB),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Standing Drum Floor Lamp",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.IRON_INGOT),'L' to Eln.findItemStack("Ceramic Drum Table Lamp",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Under Cabinet LED Strip",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.IRON_NUGGET),'L' to Eln.findItemStack("Flat Lamp Socket",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Recessed LED Channel",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.QUARTZ),'L' to Eln.findItemStack("Flat Lamp Socket",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Indirect Cove LED Strip",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.DYE.white()),'L' to Eln.findItemStack("Flat Lamp Socket",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Surface Linear LED Strip",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.IRON_INGOT),'L' to Eln.findItemStack("Thin Fluorescent Shop Light",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Flexible Neon LED Strip",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.SLIME_BALL),'L' to Eln.findItemStack("Flat Lamp Socket",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Grand Halo Ring Chandelier",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.GOLD_INGOT),'L' to Eln.findItemStack("Modern Sputnik Foyer Chandelier",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Grand Crystal Cascade Chandelier",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.DIAMOND),'L' to Eln.findItemStack("Crystal Tier Foyer Chandelier",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Sculptural Branch Chandelier",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.COPPER_INGOT),'L' to Eln.findItemStack("Modern Sputnik Foyer Chandelier",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Spiral Rain Chandelier",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.IRON_CHAIN),'L' to Eln.findItemStack("Crystal Tier Foyer Chandelier",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Grand Empire Chandelier",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.GOLD_INGOT),'L' to Eln.findItemStack("Candle Arm Foyer Chandelier",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Heavy Duty Stadium Wall Floodlight",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.IRON_BLOCK),'L' to Eln.findItemStack("Warehouse Wall Floodlight",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Armored Oval Bulkhead Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.IRON_BARS),'L' to Eln.findItemStack("Robust Lamp Socket",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Industrial Wall Pack Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.IRON_INGOT),'L' to Eln.findItemStack("Warehouse Wall Floodlight",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Rugged Outdoor Canopy Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.IRON_BLOCK),'L' to Eln.findItemStack("Robust Lamp Socket",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Industrial Yard Area Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.IRON_INGOT),'L' to Eln.findItemStack("Gooseneck Barn Light",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Offshore Caged Wellglass Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.DYE.yellow()),'L' to Eln.findItemStack("Robust Lamp Socket",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Offshore Sealed Linear Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.DYE.yellow()),'L' to Eln.findItemStack("Thin Fluorescent Shop Light",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Offshore Platform Floodlight",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.DYE.yellow()),'L' to Eln.findItemStack("Warehouse Wall Floodlight",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Modern LED Street Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.IRON_INGOT),'L' to Eln.findItemStack("Street Light",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Boulevard Lantern Street Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.IRON_CHAIN),'L' to Eln.findItemStack("Traditional Lantern Post Light",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Warm White Christmas Fairy Lights",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.DYE.yellow()),'L' to Eln.findItemStack("String Lights",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Multicolor Christmas String Lights",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.DYE.red()),'L' to Eln.findItemStack("String Lights",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Christmas Icicle Lights",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.DYE.lightBlue()),'L' to Eln.findItemStack("String Lights",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Christmas Star Wall Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.GOLD_NUGGET),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Halloween Pumpkin String Lights",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.PUMPKIN),'L' to Eln.findItemStack("String Lights",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Halloween Ghost String Lights",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.DYE.white()),'L' to Eln.findItemStack("String Lights",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Halloween Bat Wall Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.DYE.black()),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Jack O Lantern Table Lamp",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.CARVED_PUMPKIN),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Mini Pinhole Dot Light",1,null,mapOf('A' to Eln.findItemStack("Mini Dot Spotlight",1),'B' to Eln.findItemStack("Iron Plate",1),'C' to ItemStack(Items.IRON_NUGGET))),
        Source("Mini Bezel Dot Spotlight",1,null,mapOf('A' to Eln.findItemStack("Mini Dot Spotlight",1),'B' to Eln.findItemStack("Iron Plate",1),'C' to ItemStack(Items.GOLD_NUGGET))),
        Source("Mini Eyeball Dot Spotlight",1,null,mapOf('A' to Eln.findItemStack("Mini Dot Spotlight",1),'B' to Eln.findItemStack("Iron Plate",1),'C' to ItemStack(Blocks.LEVER))),
        Source("Mini Deep Baffle Dot Light",1,null,mapOf('A' to Eln.findItemStack("Mini Dot Spotlight",1),'B' to Eln.findItemStack("Iron Plate",1),'C' to ItemStack(Items.DYE.black()))),
        Source("Mini Twin Dot Light",1,null,mapOf('A' to Eln.findItemStack("Mini Dot Spotlight",1),'B' to Eln.findItemStack("Iron Plate",1),'C' to ItemStack(Blocks.GLASS_PANE))),
        Source("Twin Adjustable Eave Floodlight",1,listOf("L L"," P "),mapOf('L' to Eln.findItemStack("Mini Bezel Dot Spotlight",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Classic Four Blade Ceiling Fan",1,listOf(" M ","PLP"," P "),mapOf('M' to Eln.findItemStack("Electrical Motor",1),'P' to ItemStack(Blocks.OAK_PLANKS),'L' to Eln.findItemStack("Basic Lamp Holder",1))),
        Source("Modern Three Blade Ceiling Fan",1,listOf(" M ","PLP"," P "),mapOf('M' to Eln.findItemStack("Electrical Motor",1),'P' to Eln.findItemStack("Iron Plate",1),'L' to Eln.findItemStack("Basic Lamp Holder",1))),
        Source("Industrial Five Block Ceiling Fan",1,listOf("PMP","MLM","PPP"),mapOf('P' to Eln.findItemStack("Iron Plate",1),'M' to Eln.findItemStack("Electrical Motor",1),'L' to Eln.findItemStack("Basic Lamp Holder",1))),
        Source("Aura Five Blade Ceiling Fan",1,listOf("PMP","LLL","PPP"),mapOf('P' to Eln.findItemStack("Iron Plate",1),'M' to Eln.findItemStack("Electrical Motor",1),'L' to Eln.findItemStack("Basic Lamp Holder",1))),
        Source("Large Area Floodlight",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.DIAMOND),'L' to Eln.findItemStack("Warehouse Wall Floodlight",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Quad Head Parking Lot Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.IRON_BLOCK),'L' to Eln.findItemStack("Twin Parking Lot Light",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Round Canopy Parking Lot Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.QUARTZ),'L' to Eln.findItemStack("Twin Parking Lot Light",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Offset Twin Arm Parking Lot Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.COPPER_INGOT),'L' to Eln.findItemStack("Twin Parking Lot Light",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Round Niche Pool Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.PRISMARINE_CRYSTALS),'L' to Eln.findItemStack("Robust Lamp Socket",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Linear Pool Wall Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.PRISMARINE_CRYSTALS),'L' to Eln.findItemStack("Thin Fluorescent Shop Light",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Square Recessed Pool Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.PRISMARINE_CRYSTALS),'L' to Eln.findItemStack("Flat Lamp Socket",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Pool Corner Wash Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.PRISMARINE_CRYSTALS),'L' to Eln.findItemStack("Lamp Socket B Projector",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Pool Step Marker Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.PRISMARINE_CRYSTALS),'L' to Eln.findItemStack("Mini Dot Spotlight",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Patio Festoon Party Lights",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.DYE.orange()),'L' to Eln.findItemStack("String Lights",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Electric Poolside Tiki Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.BAMBOO),'L' to Eln.findItemStack("Traditional Lantern Post Light",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Multicolor Party Wash Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.DYE.magenta()),'L' to Eln.findItemStack("Warehouse Wall Floodlight",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Poolside Globe String Lights",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.GLASS_BOTTLE),'L' to Eln.findItemStack("String Lights",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Poolside Party Lantern",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.DYE.lightBlue()),'L' to Eln.findItemStack("Porch Lantern Sconce",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Deck Post Cap Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.OAK_SLAB),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Deck Step Riser Light",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.SPRUCE_SLAB),'L' to Eln.findItemStack("Flat Lamp Socket",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Tropical Patio Ceiling Fan",1,listOf(" D ","DLD"," P "),mapOf('D' to ItemStack(Items.JUNGLE_PLANKS),'L' to Eln.findItemStack("Classic Four Blade Ceiling Fan",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Adjustable Suspended Warehouse Light",1,listOf("CCC","PFP"," C "),mapOf('C' to ItemStack(Items.IRON_CHAIN),'P' to Eln.findItemStack("Iron Plate",1),'F' to Eln.findItemStack("Suspended Factory Fluorescent Light",1))),
        Source("Nautical Pier Bollard Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.white()),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to ItemStack(Blocks.IRON_BLOCK))),
        Source("Marine Pier Bulkhead Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.orange()),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to ItemStack(Blocks.IRON_BLOCK))),
        Source("Harbor Gooseneck Pier Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.magenta()),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to ItemStack(Blocks.IRON_BLOCK))),
        Source("Pier Lantern Post Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.lightBlue()),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to ItemStack(Blocks.IRON_BLOCK))),
        Source("Low Profile Dock Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.yellow()),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to ItemStack(Blocks.IRON_BLOCK))),
        Source("Louvered Walkway Bollard",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.lime()),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to ItemStack(Blocks.IRON_BLOCK))),
        Source("Shielded Walkway Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.pink()),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to ItemStack(Blocks.IRON_BLOCK))),
        Source("Modern Blade Walkway Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.gray()),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to ItemStack(Blocks.IRON_BLOCK))),
        Source("Mushroom Walkway Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.lightGray()),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to ItemStack(Blocks.IRON_BLOCK))),
        Source("Arched Walkway Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.cyan()),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to ItemStack(Blocks.IRON_BLOCK))),
        Source("Recessed Brick Patio Wall Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.purple()),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to ItemStack(Blocks.STONE_BRICKS))),
        Source("Round Stone Wall Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.blue()),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to ItemStack(Blocks.STONE_BRICKS))),
        Source("Slotted Patio Wall Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.brown()),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to ItemStack(Blocks.STONE_BRICKS))),
        Source("Recessed Square Patio Wall Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.green()),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to ItemStack(Blocks.STONE_BRICKS))),
        Source("Stone Wall Wash Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.red()),'G' to ItemStack(Blocks.GLASS_PANE),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to ItemStack(Blocks.STONE_BRICKS))),
        Source("Shoebox Parking Lot Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.white()),'G' to ItemStack(Blocks.GLASS),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Twin Wide Area Parking Lot Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.blue()),'G' to ItemStack(Blocks.GLASS),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Three Arm Parking Lot Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.black()),'G' to ItemStack(Blocks.GLASS),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Gumdrop Sidewalk Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.green()),'G' to ItemStack(Blocks.GLASS),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to Eln.findItemStack("Iron Plate",1))),
        Source("Dual Gumdrop Sidewalk Light",1,listOf(" D ","GLG"," P "),mapOf('D' to ItemStack(Items.DYE.red()),'G' to ItemStack(Blocks.GLASS),'L' to Eln.findItemStack("Basic Lamp Holder",1),'P' to Eln.findItemStack("Iron Plate",1)))
    )
}
