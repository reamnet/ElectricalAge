package mods.eln.sixnode.lampsocket

import mods.eln.Eln
import mods.eln.i18n.I18N
import mods.eln.item.LampDescriptor

/** Source technology/voltage matrix, preserving every occupied native bulb identity. */
object RatedLampBulbs {
    const val V120_ID=302
    const val V240_ID=303
    data class Rating(val sourceId:Int,val id:Int,val title:String,val icon:String,val type:LampDescriptor.Type,val volts:Double,val watts:Double,val light:Double,val seconds:Double,val growth:Double)
    val ratings=listOf(
        Rating(256,304,"12V Incandescent Light Bulb","incandescentironlamp",LampDescriptor.Type.Incandescent,12.0,12.0,0.7333333333333333,86400.0,1.0),
        Rating(257,305,"120V Incandescent Light Bulb","incandescentironlamp",LampDescriptor.Type.Incandescent,120.0,60.0,0.9333333333333333,86400.0,1.0),
        Rating(258,306,"240V Incandescent Light Bulb","incandescentironlamp",LampDescriptor.Type.Incandescent,240.0,90.0,0.9333333333333333,86400.0,1.0),
        Rating(260,307,"12V Carbon Light Bulb","incandescentcarbonlamp",LampDescriptor.Type.Incandescent,12.0,9.0,0.7333333333333333,43200.0,1.0),
        Rating(261,308,"120V Carbon Light Bulb","incandescentcarbonlamp",LampDescriptor.Type.Incandescent,120.0,45.0,0.9333333333333333,43200.0,1.0),
        Rating(262,309,"240V Carbon Light Bulb","incandescentcarbonlamp",LampDescriptor.Type.Incandescent,240.0,67.5,0.9333333333333333,43200.0,1.0),
        Rating(272,310,"12V Fluorescent Light Bulb","fluorescentlamp",LampDescriptor.Type.eco,12.0,6.0,0.7333333333333333,604800.0,2.0),
        Rating(273,311,"120V Fluorescent Light Bulb","fluorescentlamp",LampDescriptor.Type.eco,120.0,30.0,0.9333333333333333,604800.0,2.0),
        Rating(274,312,"240V Fluorescent Light Bulb","fluorescentlamp",LampDescriptor.Type.eco,240.0,45.0,0.9333333333333333,604800.0,2.0),
        Rating(287,313,"12V Farming Light Bulb","farminglamp",LampDescriptor.Type.Incandescent,12.0,24.0,0.7333333333333333,172800.0,4.0),
        Rating(288,314,"120V Farming Light Bulb","farminglamp",LampDescriptor.Type.Incandescent,120.0,120.0,0.9333333333333333,172800.0,4.0),
        Rating(292,315,"240V Farming Light Bulb","farminglamp",LampDescriptor.Type.Incandescent,240.0,180.0,0.9333333333333333,172800.0,4.0),
        Rating(295,316,"12V LED Light Bulb","ledlamp",LampDescriptor.Type.LED,12.0,3.0,0.7333333333333333,2419200.0,2.0),
        Rating(293,302,"120V LED Light Bulb","ledlamp",LampDescriptor.Type.LED,120.0,15.0,0.9333333333333333,2419200.0,2.0),
        Rating(294,303,"240V LED Light Bulb","ledlamp",LampDescriptor.Type.LED,240.0,22.5,0.9333333333333333,2419200.0,2.0),
        Rating(299,317,"12V Halogen Light Bulb","halogenlamp",LampDescriptor.Type.Incandescent,12.0,36.0,0.7333333333333333,345600.0,1.0),
        Rating(300,318,"120V Halogen Light Bulb","halogenlamp",LampDescriptor.Type.Incandescent,120.0,180.0,0.9333333333333333,345600.0,1.0),
        Rating(301,319,"240V Halogen Light Bulb","halogenlamp",LampDescriptor.Type.Incandescent,240.0,270.0,0.9333333333333333,345600.0,1.0)
    )
    @JvmStatic fun destinationIdForSource(source:Int):Int?=ratings.firstOrNull { it.sourceId==source }?.id
    @JvmStatic fun register() {
        ratings.forEach { r->Eln.sharedItem.addElement(r.id,LampDescriptor(I18N.TR_NAME(I18N.Type.NONE,r.title),r.icon,r.type,LampSocketType.Douille,r.volts,r.watts,r.light,r.seconds,r.growth).apply { if(r.type==LampDescriptor.Type.eco)minimalU=r.volts*.75 }) }
    }
}
