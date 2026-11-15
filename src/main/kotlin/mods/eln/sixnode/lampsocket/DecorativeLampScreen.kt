package mods.eln.sixnode.lampsocket

import mods.eln.client.gui.ElnScreen
import mods.eln.client.gui.EaField
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

/** Fixed-fixture controls keep the reviewed source's 176×166 panel and coordinates. */
class DecorativeLampScreen(menu: ElnNodeMenu, inventory: Inventory, title: Component, render: Any) : ElnScreen(menu,inventory,title) {
    private val lamp=render as DecorativeLampRender
    private lateinit var channel: EaField
    private lateinit var grounded: Button
    override fun init() {
        super.init()
        button("supply",{if(lamp.poweredByLampSupply)tr("Powered by lamp supply") else tr("Powered by cable")},
            {lamp.clientSend(LampSocketElement.tooglePowerSupplyType)}).active=!(lamp.descriptor as DecorativeLampDescriptor).style.wirelessOnly
        // One occupied region changes between the source channel field and disabled ground indicator.
        // They are never visible together; validate the complete region once in the layout.
        val mode=rect("field","channel")
        grounded=button(mode.x(),mode.y(),mode.w(),{if(lamp.grounded)tr("Grounded") else tr("Ungrounded")},{})
        grounded.active=false
        channel=field(mode.x()+1,mode.y()+4,mode.w()-2,12,{lamp.channel ?: ""},
            {lamp.clientSetString(LampSocketElement.setChannel.toByte(),it)},tr("Specify the lamp supply channel"))
        update()
    }
    override fun update() {
        if(!::channel.isInitialized) return
        channel.visible=lamp.poweredByLampSupply
        channel.active=lamp.poweredByLampSupply
        grounded.visible=!lamp.poweredByLampSupply
        channel.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
            if(lamp.isConnectedToLampSupply)tr("Connected to %1$",lamp.channel) else tr("%1$ is not in range!",lamp.channel))))
    }
}
