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
    private val style get()=lamp.style
    private var head=0
    private fun setAim(axis: Int,value: Double) {
        if(lamp.aim.set(head,axis,value))lamp.clientSetDouble((DecorativeLampElement.AIM_FIRST+head*2+axis).toByte(),if(axis==0)lamp.aim.pan(head) else lamp.aim.tilt(head))
    }
    private var raise: Button?=null
    private var lower: Button?=null
    private lateinit var supply: Button
    private lateinit var channel: EaField
    private lateinit var grounded: Button
    override fun init() {
        super.init()
        supply=button("supply",{if(lamp.cordDisplay.active())tr("Powered by cord") else if(lamp.poweredByLampSupply)tr("Powered by lamp supply") else tr("Powered by cable")},
            {lamp.clientSend(LampSocketElement.tooglePowerSupplyType)})
        supply.active=!style.wirelessOnly
        // One occupied region changes between the source channel field and disabled ground indicator.
        // They are never visible together; validate the complete region once in the layout.
        val mode=rect("field","channel")
        grounded=button(mode.x(),mode.y(),mode.w(),{if(lamp.grounded)tr("Grounded") else tr("Ungrounded")},{})
        grounded.active=false
        channel=field(mode.x()+1,mode.y()+4,mode.w()-2,12,{lamp.channel ?: ""},
            {lamp.clientSetString(LampSocketElement.setChannel.toByte(),it)},tr("Specify the lamp supply channel"))
        if(style==DecorativeLampStyle.MOTION)button(40,57,96,{tr(arrayOf("Motion: night","Motion: all day","Always on","Always off")[lamp.motionMode])},{lamp.clientSetByte(DecorativeLampElement.MOTION_MODE.toByte(),((lamp.motionMode+1)%4).toByte())})
        if(style.fan)button(40,57,96,{if(lamp.fanEnabled)tr("Fan: On") else tr("Fan: Off")},{lamp.clientSend(DecorativeLampElement.FAN_SWITCH)})
        if(style.aimHeadCount>0) {
            button("head",{tr("Head %1$/%2$",head+1,style.aimHeadCount)},{head=(head+1)%style.aimHeadCount}).active=style.aimHeadCount>1
            button("reset",{tr("Reset aim")},{setAim(0,0.0);setAim(1,style.defaultTilt)})
            button("pan_minus",{"<"},{setAim(0,lamp.aim.pan(head)-5)})
            button("pan_plus",{">"},{setAim(0,lamp.aim.pan(head)+5)})
            button("tilt_minus",{"-"},{setAim(1,lamp.aim.tilt(head)-5)})
            button("tilt_plus",{"+"},{setAim(1,lamp.aim.tilt(head)+5)})
            button("pan_label",{tr("Pan: %1$°",lamp.aim.pan(head).toInt())},{}).active=false
            button("tilt_label",{tr("Tilt: %1$°",lamp.aim.tilt(head).toInt())},{}).active=false
        }
        if(style.hangingWarehouse) {
            raise=button("drop_minus",{"-"},{lamp.clientSetDouble(DecorativeLampElement.SUSPENSION.toByte(),lamp.suspension.drop-.25)})
            lower=button("drop_plus",{"+"},{lamp.clientSetDouble(DecorativeLampElement.SUSPENSION.toByte(),lamp.suspension.drop+.25)})
            button("drop_label",{tr("Drop: %1$ m",lamp.suspension.drop)},{}).active=false
        }
        update()
    }
    override fun update() {
        if(!::channel.isInitialized) return
        supply.active=!style.wirelessOnly && !lamp.cordDisplay.active()
        channel.visible=lamp.poweredByLampSupply && !lamp.cordDisplay.active()
        channel.active=lamp.poweredByLampSupply && !lamp.cordDisplay.active()
        grounded.visible=!lamp.poweredByLampSupply && !lamp.cordDisplay.active()
        raise?.active=lamp.suspension.drop>0.0
        lower?.active=lamp.suspension.drop<7.0
        channel.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
            if(lamp.isConnectedToLampSupply)tr("Connected to %1$",lamp.channel) else tr("%1$ is not in range!",lamp.channel))))
    }
}
