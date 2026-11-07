package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeType

/** Exercise loaded native recipes, including the two formerly substituted variable-converter ingredients. */
object ForkPatchConverterRecipeCases {
    @JvmStatic fun cases():List<TestCase> = listOf(Converters(),IncomingEquipment())
    private fun item(name:String)=Eln.findItemStack(name,1)
    private fun craft(c:TestContext,rows:List<String>,key:Map<Char,ItemStack>):ItemStack {
        val input=CraftingInput.of(rows[0].length,rows.size,rows.flatMap { row->row.map { key[it]?.copy() ?: ItemStack.EMPTY } })
        return c.level().recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,c.level()).map { it.value().assemble(input) }.orElse(ItemStack.EMPTY)
    }
    private open class Base:TestCase {
        override fun build(c:TestContext){}
        override fun measure(c:TestContext){}
        override fun ticks()=1
        fun check(c:TestContext,name:String,rows:List<String>,key:Map<Char,ItemStack>) {
            val out=craft(c,rows,key)
            c.check("converter recipe: real recipe manager crafts $name",out.item===item(name).item && out.count==1,out.toString())
        }
    }
    private class Converters:Base() {
        override fun id()="forkpatch_converter_recipes_native_identity_and_source_paid_progression"
        override fun measure(c:TestContext) {
            val iron=ItemStack(Items.IRON_INGOT);val redstone=ItemStack(Items.REDSTONE);val chip=item("Cheap Chip")
            check(c,"DC-DC Converter",listOf("C C","III"),mapOf('C' to item("Copper Coil"),'I' to iron))
            check(c,"Winding DC-DC Converter",listOf("C C","III"),mapOf('C' to item("Copper Cable"),'I' to iron))
            check(c,"Variable DC-DC Converter",listOf("CBC","III"),mapOf('C' to item("Copper Cable"),'B' to chip,'I' to iron))
            check(c,"One-way DC-DC Converter",listOf("D","R"),mapOf('D' to item("Winding DC-DC Converter"),'R' to redstone))
            check(c,"One-way Boost vDC/DC Converter",listOf("D","R"),mapOf('D' to item("Variable DC-DC Converter"),'R' to redstone))
            check(c,"One-way Buck vDC/DC Converter",listOf("D","B"),mapOf('D' to item("Variable DC-DC Converter"),'B' to chip))
            check(c,"One-way Boost/Buck vDC/DC Converter",listOf(" B ","DVD"," B "),mapOf('B' to chip,'D' to item("Signal Diode"),'V' to item("Variable DC-DC Converter")))
            val wrong=craft(c,listOf("R","D"),mapOf('D' to item("Variable DC-DC Converter"),'R' to redstone))
            c.check("converter recipe: redstone cannot substitute for the source buck control chip",wrong.item!==item("One-way Buck vDC/DC Converter").item,wrong.toString())
        }
    }
    private class IncomingEquipment:Base() {
        override fun id()="forkpatch_converter_recipes_restore_ups_and_step_down_variable_ingredient"
        override fun measure(c:TestContext) {
            val key=mapOf('C' to item("Copper Cable"),'F' to item("Optimal Ferromagnetic Core"),'M' to item("Machine Block"),'V' to item("Variable DC-DC Converter"))
            for((name,rows) in listOf("Mains Step-down Unit" to listOf("CFC","MVM","CCC"),"Programmable UPS" to listOf("CVC","MFM","CVC"))) {
                check(c,name,rows,key)
                val old=craft(c,rows,key+('V' to item("DC-DC Converter")))
                c.check("converter recipe: $name no longer uses the temporary native128 substitute",old.item!==item(name).item,old.toString())
            }
        }
    }
}
