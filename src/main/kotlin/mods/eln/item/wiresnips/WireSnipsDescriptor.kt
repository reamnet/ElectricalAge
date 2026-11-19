package mods.eln.item.wiresnips

import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class WireSnipsDescriptor(name:String):GenericItemUsingDamageDescriptor(name,"wiresnips") {
    companion object { const val ID=7873 }
    override fun onItemRightClick(stack:ItemStack,level:Level,player:Player):ItemStack {
        if(!level.isClientSide && player is ServerPlayer)WireSnipsMenu.open(player,stack)
        return stack
    }
    override fun addInformation(stack:ItemStack?,player:Player?,lines:MutableList<Any?>,advanced:Boolean) {
        lines.add(tr("Right click to cut utility wire in your inventory"))
    }
}
