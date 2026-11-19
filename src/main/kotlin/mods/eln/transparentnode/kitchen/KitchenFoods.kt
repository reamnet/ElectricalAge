package mods.eln.transparentnode.kitchen

import mods.eln.registry.ElnRegistry
import mods.eln.i18n.I18N
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.*
import net.minecraft.world.item.component.Consumables
import java.util.function.Supplier

object KitchenFoods {
    lateinit var toast:Supplier<Item>;private set
    lateinit var coffee:Supplier<Item>;private set
    lateinit var hotWater:Supplier<Item>;private set
    lateinit var smoothie:Supplier<Item>;private set
    lateinit var grains:Supplier<Item>;private set
    private fun food(id:String,title:String,nutrition:Int,drink:Boolean,saturation:Float=.6f):Supplier<Item> {
        I18N.TR_NAME(I18N.Type.NONE,title)
        val item=ElnRegistry.ITEMS.registerItem(id,{p->Item(p)}, {p->
            val food=FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).apply{if(drink)alwaysEdible()}.build()
            if(drink)p.stacksTo(16).food(food,Consumables.DEFAULT_DRINK).usingConvertsTo(Items.GLASS_BOTTLE) else p.food(food)
        })
        ElnRegistry.ALL_ITEMS.add(item)
        return item
    }
    @JvmStatic fun register() {
        hotWater=food("hot_water","Hot Water",0,true)
        smoothie=food("fruit_smoothie","Fruit Smoothie",4,true)
        grains=food("steamed_grains","Steamed Grains",6,false)
        toast=food("toasted_bread","Toasted Bread",6,false)
        coffee=food("fresh_coffee","Fresh Coffee",1,true,.1f)
    }
}
