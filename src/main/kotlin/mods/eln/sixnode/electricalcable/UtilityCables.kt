package mods.eln.sixnode.electricalcable
import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.ItemNbt
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

/** Source allocator order: piles 2176/2177, singles 2178..2267, multicore reserved 2268..2327; HV group 38. */
object UtilityCables {
    data class Spec(val label: String,val area: Double,val amps: Double,val volts: Double,val jacketC: Double,val aluminum: Boolean=false)
    val specs=listOf(
        Spec("26 AWG",0.1288,1.0,300.0,80.0,false),
        Spec("24 AWG",0.2047,2.0,300.0,80.0,false),
        Spec("22 AWG",0.3255,3.0,300.0,80.0,false),
        Spec("20 AWG",0.5176,5.0,300.0,80.0,false),
        Spec("18 AWG",0.8230,7.0,300.0,90.0,false),
        Spec("16 AWG",1.309,10.0,300.0,90.0,false),
        Spec("14 AWG",2.081,15.0,600.0,105.0,false),
        Spec("12 AWG",3.309,20.0,600.0,105.0,false),
        Spec("10 AWG",5.261,30.0,600.0,105.0,false),
        Spec("8 AWG",8.366,40.0,600.0,105.0,false),
        Spec("6 AWG",13.302,60.0,600.0,105.0,false),
        Spec("4 AWG",21.151,80.0,600.0,105.0,false),
        Spec("2 AWG",33.631,100.0,600.0,105.0,true),
        Spec("1/0 AWG",53.475,125.0,1000.0,110.0,true),
        Spec("2/0 AWG",67.431,175.0,1000.0,110.0,true),
        Spec("4/0 AWG",107.219,260.0,1000.0,110.0,true),
        Spec("250 kcmil",126.677,290.0,1000.0,110.0,true),
        Spec("350 kcmil",177.348,350.0,1000.0,110.0,true),
        Spec("500 kcmil",253.354,430.0,1000.0,110.0,true),
        Spec("750 kcmil",380.031,600.0,1000.0,110.0,true),
        Spec("1000 kcmil",506.707,750.0,1000.0,110.0,true)
    )
    val highVoltage=listOf(
        0 to Spec("12 AWG",3.309,20.0,1000.0,105.0),
        2 to Spec("12 AWG",3.309,20.0,5000.0,105.0),
        4 to Spec("12 AWG",3.309,20.0,20000.0,105.0),
        6 to Spec("12 AWG",3.309,20.0,40000.0,105.0),
        8 to Spec("12 AWG",3.309,20.0,150000.0,105.0),
        10 to Spec("2 AWG",33.631,100.0,1000.0,105.0),
        12 to Spec("2 AWG",33.631,100.0,5000.0,105.0),
        14 to Spec("2 AWG",33.631,100.0,20000.0,105.0),
        16 to Spec("2 AWG",33.631,100.0,40000.0,105.0),
        18 to Spec("2 AWG",33.631,100.0,150000.0,105.0)
    )
    val all=ArrayList<UtilityCableDescriptor>()
    val piles=java.util.EnumMap<UtilityCableMaterial,MoltenMetalPileDescriptor>(UtilityCableMaterial::class.java)
    val scraps=WireScrapDescriptor()
    @JvmStatic fun register() {
        for(m in UtilityCableMaterial.entries) {
            val d=MoltenMetalPileDescriptor(I18N.TR_NAME(I18N.Type.NONE,"${m.label} Molten Metal Pile"),m,CableRenderDescriptor("eln","sprites/cable_melted.png",2.4f,.9f))
            piles[m]=d;Eln.sixNodeItem.addWithoutRegistry(2176+m.ordinal,d)
        }
        Eln.sharedItem.addElement(7872,scraps)
        var next=2178
        for(m in UtilityCableMaterial.entries)for(spec in specs) {
            if(m==UtilityCableMaterial.ALUMINUM && !spec.aluminum)continue
            make(next++,m,spec,false,false,false)
            val intact=make(next++,m,spec,true,false,false);val damaged=make(next++,m,spec,false,true,false);intact.meltedDescriptor=damaged
        }
        check(next==2268)
        for((offset,spec)in highVoltage) {
            val intact=make(2432+offset,UtilityCableMaterial.COPPER,spec,true,false,true)
            intact.meltedDescriptor=make(2433+offset,UtilityCableMaterial.COPPER,spec,false,true,true)
        }
    }
    private fun make(id: Int,m: UtilityCableMaterial,s: Spec,insulated: Boolean,melted: Boolean,hv: Boolean): UtilityCableDescriptor {
        val label=if(melted) { if(hv)"${s.volts.toInt()}V Melted" else "Melted" } else if(insulated)"${s.volts.toInt()}V" else "Bare"
        val width=when { s.area<=.5->.65f;s.area<=2.5->.95f;s.area<=10->1.25f;s.area<=35->1.75f;s.area<=120->2.35f;else->3.25f }
        val render=CableRenderDescriptor("eln",if(melted)"sprites/cable_melted.png" else if(insulated)"sprites/cable.png" else "sprites/currentcable.png",width,maxOf(.65f,width-.3f))
        val d=UtilityCableDescriptor(s.label,m,s.area,s.amps,insulated,if(insulated)s.volts else 0.0,melted,if(insulated || melted)s.jacketC else 180.0,I18N.TR_NAME(I18N.Type.NONE,"${m.label} ${s.label} Cable $label"),render)
        val nominal=maxOf(50.0,s.volts);val power=nominal*s.amps
        d.setPhysicalConstantLikeNormalCable(nominal,power,d.resistanceOhms()*s.amps/nominal,maxOf(nominal*1.5,d.insulationVoltageRating),power*1.1,20.0,d.meltTemperatureCelsius,-100.0,Eln.cableHeatingTime,Eln.cableThermalConductionTao)
        val physics=WireThermalPhysics(m,s.area);d.thermalC=physics.capacity(20.0);d.thermalRp=1/physics.coolingConductance(20.0,20.0,insulated);d.thermalRs=physics.endpointThermalResistance
        d.electricalRsPerCelcius=d.electricalRs*WirePhysics.temperatureCoefficient(m)
        d.setDefaultIcon(when { !insulated && !melted->"utilitybareicon";s.area<=.5->"utilitysignalicon";s.area<=2.5->"utilityinsulatedsmallicon";s.area<=35->"utilityinsulatedmediumicon";else->"utilityinsulatedlargeicon" })
        all.add(d);if(melted)Eln.sixNodeItem.addWithoutRegistry(id,d) else Eln.sixNodeItem.addDescriptor(id,d)
        return d
    }
    fun scrap(d: UtilityCableDescriptor,meters: Double)=scrap(d.material,d.conductorAreaMm2,meters,d.name)
    fun scrap(material: UtilityCableMaterial,area: Double,meters: Double,source: String)=scraps.newItemStack().also { stack->
        ItemNbt.set(stack,CompoundTag().apply {
            putString("wireScrapSourceName",source);putString("wireScrapMaterial",material.label);putString("wireScrapSize",source);putString("wireScrapMetricSize",area.toString())
            putInt("wireScrapConductors",1);putDouble("wireScrapAreaMm2",area);putDouble("wireScrapMeters",meters);putDouble("wireScrapMassKg",WirePhysics.massKg(material,area,meters))
        })
    }
}

class WireScrapDescriptor: GenericItemUsingDamageDescriptor(I18N.TR_NAME(I18N.Type.NONE,"Wire Scraps"),"utilitybareicon") {
        override fun addInformation(stack: ItemStack?,player: net.minecraft.world.entity.player.Player?,list: MutableList<Any?>,advanced: Boolean) {
            val t=stack?.let(ItemNbt::get) ?: return
            list.add(tr("Recovered from: %1$",t.getStringOr("wireScrapSourceName","")))
            list.add(tr("Material: %1$",t.getStringOr("wireScrapMaterial","")))
            list.add(tr("Metal mass: %1$ kg",t.getDoubleOr("wireScrapMassKg",0.0)))
        }
    }
