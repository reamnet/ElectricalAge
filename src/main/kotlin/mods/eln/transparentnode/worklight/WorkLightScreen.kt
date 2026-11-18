package mods.eln.transparentnode.worklight

import mods.eln.client.gui.ElnScreen
import mods.eln.gui.ElnNodeMenu
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.io.DataOutputStream

/** Source 176x290 layout and events; the two heads are independently adjustable. */
class WorkLightScreen(m:ElnNodeMenu,i:Inventory,t:Component,r:Any):ElnScreen(m,i,Component.empty(),176,290){
    private val lamp=r as WorkLightRender
    private var head=0
    private var engine:net.minecraft.client.gui.components.Button?=null
    private fun control(x:Int,y:Int,w:Int,label:()->String,press:()->Unit)=button(x,y,w,label,press).apply{height=14}
    private fun aim(axis:Int,value:Double){if(lamp.aim.set(head,axis,value)){
        val bytes=java.io.ByteArrayOutputStream();DataOutputStream(bytes).apply{lamp.preparePacketForServer(this);writeByte(83+head*2+axis);writeDouble(if(axis==0)lamp.aim.pan(head)else lamp.aim.tilt(head))};lamp.sendPacketToServer(bytes)
    }}
    override fun init(){super.init()
        engine=control(8,82,76,{if(lamp.running)"Stop engine"else "Start engine"},{lamp.clientSendId(80.toByte())})
        control(88,82,80,{if(lamp.wireless)"Wireless"else "Cable / cord"},{lamp.clientSendId(81.toByte())})
        field(9,111,158,12,{lamp.channel},{lamp.clientSendString(82.toByte(),it)},"Lamp supply channel").setMaxLength(128)
        control(8,132,76,{"Head "+(head+1)+"/2"},{head=1-head})
        control(88,132,80,{"Reset aim"},{aim(0,0.0);aim(1,-12.0)})
        control(8,156,28,{"<"},{aim(0,lamp.aim.pan(head)-5)})
        control(136,156,32,{">"},{aim(0,lamp.aim.pan(head)+5)})
        control(8,180,28,{"-"},{aim(1,lamp.aim.tilt(head)-5)})
        control(136,180,32,{"+"},{aim(1,lamp.aim.tilt(head)+5)})
        control(40,156,92,{"Pan: "+lamp.aim.pan(head).toInt()+"°"},{}).active=false
        control(40,180,92,{"Tilt: "+lamp.aim.tilt(head).toInt()+"°"},{}).active=false
    }
    override fun extractDevice(g:GuiGraphicsExtractor,mx:Int,my:Int,dt:Float){
        label(g,8,8,"Construction work light");label(g,8,23,"Fuel: "+(lamp.fuel*100).toInt()+"%")
        label(g,8,38,"Bulbs: "+lamp.count+" at "+lamp.voltage.toInt()+" V")
        engine?.active=!lamp.wireless&&lamp.fuel>0
    }
}
