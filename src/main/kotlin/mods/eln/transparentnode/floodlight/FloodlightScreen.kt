package mods.eln.transparentnode.floodlight

import mods.eln.client.gui.*
import mods.eln.gui.ElnNodeMenu
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.io.DataOutputStream

/** Original 176x166 panel, two bulb slots and three source-positioned aim tracks. */
class FloodlightScreen(m:ElnNodeMenu,i:Inventory,t:Component,r:Any):ElnScreen(m,i,Component.empty(),176,166) {
    private val lamp=r as FloodlightRender
    private val sliders=ArrayList<EaSlider>()
    override fun init(){super.init();sliders.clear()
        repeat(3){axis->val max=doubleArrayOf(360.0,180.0,45.0)[axis]
            val slider=EaSlider(leftPos+7,topPos+8+18*axis,162,12,false,0.0,max,1.0,value(axis),
                {v->Component.literal(listOf("Swivel","Head","Beam width")[axis]+": "+v.toInt()+"°")},
                {v->val bytes=java.io.ByteArrayOutputStream();DataOutputStream(bytes).apply{lamp.preparePacketForServer(this);writeByte(axis);writeDouble(v)};lamp.sendPacketToServer(bytes)})
            slider.active=!lamp.motorized;addRenderableWidget(slider);sliders.add(slider)
        }
    }
    private fun value(axis:Int)=when(axis){0->lamp.swivelAngle;1->lamp.headAngle;else->lamp.beamWidth}
    override fun extractDevice(g:GuiGraphicsExtractor,mx:Int,my:Int,dt:Float){sliders.forEachIndexed{axis,s->s.setFromDevice(value(axis));s.active=!lamp.motorized}}
}
