package mods.eln.transparentnode.offgrid
import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N
import mods.eln.transparentnode.ups.UpsLargeCell
import mods.eln.utility.UtilityItems

object OffGridDevices {
    val STACK=I18N.TR_NAME(I18N.Type.NONE,"16 kWh LiFePO4 Cell Stack")
    fun register() {
        UtilityItems.registerFuseLink()
        Eln.sharedItem.addElement(7+(117 shl 6),GenericItemUsingDamageDescriptor(STACK,"upsindustrialbatterymodule"))
        listOf(I18N.TR_NAME(I18N.Type.NONE,"UPS AGM Battery Cell"),I18N.TR_NAME(I18N.Type.NONE,"UPS LiFePO4 Battery Cell"),I18N.TR_NAME(I18N.Type.NONE,"UPS Industrial Battery Module")).forEachIndexed { i,n ->
            Eln.sharedItemStackOne.addElement(2+i+(122 shl 6),UpsLargeCell(n,i).apply { setDefaultIcon(arrayOf("upsagmbatterycell","upslifepo4batterycell","upsindustrialbatterymodule")[i]) })
        }
        OffGridType.entries.forEach { Eln.transparentNodeItem.addDescriptor(it.ordinal+(128 shl 6),OffGridDescriptor(it)) }
    }
}
