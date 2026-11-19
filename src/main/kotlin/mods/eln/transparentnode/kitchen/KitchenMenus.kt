package mods.eln.transparentnode.kitchen

import mods.eln.gui.*
import mods.eln.i18n.I18N.tr

object KitchenMenus {
    @JvmStatic fun spec(d:KitchenDescriptor):MenuSpec {
        val t=d.type
        val slots=Array(t.slotCount){i->
            val x=if(i>=3)8+(i-3)%9*18 else when(i){0->17;1->63;else->135}
            val y=if(i>=3)110+(i-3)/9*18 else 79
            SlotSpec.filter(i,x,y,64,{KitchenRecipe.allowed(t,i,it)},SlotSpec.MEDIUM,tr(if(i==2)"Output" else if(i==1)"Water / returned container" else if(i>=3)"Food storage" else "Input"))
        }
        return MenuSpec.of(*slots).panel(176,if(t.refrigerator)212+t.storageSlots/9*18 else 211,8,if(t.refrigerator)130+t.storageSlots/9*18 else 129)
    }
}
