package mods.eln.vehicle

import mods.eln.i18n.I18N.tr
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class VehicleScreen(menu:VehicleMenu,inventory:Inventory,title:Component):AbstractContainerScreen<VehicleMenu>(menu,inventory,title,176,188) {
    init{inventoryLabelY=95}
    override fun extractBackground(g:GuiGraphicsExtractor,mx:Int,my:Int,partial:Float) {
        super.extractBackground(g,mx,my,partial)
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff202d38.toInt())
        menu.slots.filter{it.isActive}.forEach{g.fill(leftPos+it.x-1,topPos+it.y-1,leftPos+it.x+17,topPos+it.y+17,0xff111a22.toInt())}
    }
    override fun extractLabels(g:GuiGraphicsExtractor,mx:Int,my:Int) {
        g.text(font,title,8,8,0xffffff,false)
        g.text(font,tr("Traction battery bays"),8,20,0xa7d8ff,false)
        g.text(font,tr("Storage"),8,62,0xd8dce0,false)
        g.text(font,playerInventoryTitle,8,95,0xd8dce0,false)
    }
}
