package mods.eln.transparentnode.offgrid

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

enum class OffGridType(val title: String,val kwh: Int=0,val wall: Boolean=false) {
    BANK16(I18N.TR_NAME(I18N.Type.NONE,"16 kWh LiFePO4 Battery Cabinet"),16),
    BANK32(I18N.TR_NAME(I18N.Type.NONE,"32 kWh LiFePO4 Battery Cabinet"),32),
    BANK48(I18N.TR_NAME(I18N.Type.NONE,"48 kWh LiFePO4 Battery Cabinet"),48),
    BANK64(I18N.TR_NAME(I18N.Type.NONE,"64 kWh LiFePO4 Battery Cabinet"),64),
    WALL16(I18N.TR_NAME(I18N.Type.NONE,"16 kWh Wall-mounted LiFePO4 Battery"),16,true),
    WALL32(I18N.TR_NAME(I18N.Type.NONE,"32 kWh Wall-mounted LiFePO4 Battery"),32,true),
    WALL48(I18N.TR_NAME(I18N.Type.NONE,"48 kWh Wall-mounted LiFePO4 Battery"),48,true),
    WALL64(I18N.TR_NAME(I18N.Type.NONE,"64 kWh Wall-mounted LiFePO4 Battery"),64,true),
    INVERTER(I18N.TR_NAME(I18N.Type.NONE,"Off-grid Inverter Charger")),
    MPPT(I18N.TR_NAME(I18N.Type.NONE,"LiFePO4 MPPT Charge Controller")),
    AC_CHARGER(I18N.TR_NAME(I18N.Type.NONE,"AC LiFePO4 Battery Charger")),
    COMBINER(I18N.TR_NAME(I18N.Type.NONE,"Fused PV Combiner and Disconnect")),
    DISCONNECT(I18N.TR_NAME(I18N.Type.NONE,"400 A DC Battery Disconnect"));
    val bank get()=kwh>0
    val model get()=if(bank) { if(wall)"lifepo_wall" else "lifepo_cabinet" } else when(this) {
        INVERTER->"offgrid_inverter";MPPT->"offgrid_mppt";AC_CHARGER->"offgrid_charger";COMBINER->"pv_combiner";else->"battery_disconnect"
    }
}
