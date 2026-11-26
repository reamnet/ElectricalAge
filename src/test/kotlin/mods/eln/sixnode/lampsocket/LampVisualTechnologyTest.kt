package mods.eln.sixnode.lampsocket

import mods.eln.item.LampDescriptor
import org.junit.Assert.*
import org.junit.Test

class LampVisualTechnologyTest {
    @Test fun ratedBulbsUseTheirTechnologyDespitePositivePlantGrowth() {
        for ((index,r) in RatedLampBulbs.ratings.withIndex()) {
            val bulb=LampDescriptor(r.title,r.icon,r.type,LampSocketType.Douille,r.volts,r.watts,r.light,r.seconds,r.growth)
            bulb.parentItemDamage=r.id
            assertTrue(r.growth>0)
            assertEquals(r.title,SourceLampLife.technologies[index/3],SourceLampLife.visualTechnology(bulb))
        }
    }
    @Test fun legacyBulbsKeepTheirNormalColoursAndActualGrowLampsStayPink() {
        fun bulb(type:LampDescriptor.Type,growth:Double)=LampDescriptor("legacy","ledlamp",type,LampSocketType.Douille,48.0,10.0,1.0,1000.0,growth).apply { parentItemDamage=-1 }
        assertEquals("led",SourceLampLife.visualTechnology(null))
        assertEquals("led",SourceLampLife.visualTechnology(bulb(LampDescriptor.Type.LED,0.0)))
        assertEquals("fluorescent",SourceLampLife.visualTechnology(bulb(LampDescriptor.Type.eco,0.0)))
        assertEquals("incandescent",SourceLampLife.visualTechnology(bulb(LampDescriptor.Type.Incandescent,0.0)))
        assertEquals("farming",SourceLampLife.visualTechnology(bulb(LampDescriptor.Type.Incandescent,.5)))
    }
}
