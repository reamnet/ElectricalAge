package mods.eln.utility
import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N
import net.minecraft.world.item.ItemStack

/** Paid, replaceable fuse links; remaining utility hardware is not registered yet. */
class UtilityPartDescriptor(name: String,val part: Int): GenericItemUsingDamageDescriptor(name,if(part==3)"polestreetlightattachment" else if(part==1)"analogservicemeter" else "smartservicemeter") {
    override fun addInformation(stack: ItemStack?,player: net.minecraft.world.entity.player.Player?,list: MutableList<Any?>,advanced: Boolean) {
        if(part==3) { list.add(mods.eln.i18n.I18N.tr("Install on a distribution pole; uses only its 120/240 V secondary"));list.add(mods.eln.i18n.I18N.tr("Open the pole menu for supply, dusk, aiming and removal controls"));return }
        list.add(mods.eln.i18n.I18N.tr("Fits compact and wide service sockets; readings and serial stay with this meter"))
        ServiceMeters.data(stack ?: ItemStack.EMPTY)?.let {
            list.add(mods.eln.i18n.I18N.tr("Serial: %1$",it.getStringOr("serial","")))
            list.add(mods.eln.i18n.I18N.tr("Import: %1$ kWh",String.format(java.util.Locale.ROOT,"%.3f",it.getDoubleOr("meterImportKwh",0.0))))
            list.add(mods.eln.i18n.I18N.tr("Export: %1$ kWh",String.format(java.util.Locale.ROOT,"%.3f",it.getDoubleOr("meterExportKwh",0.0))))
        }
    }
}
object UtilityItems {
    const val STREETLIGHT_ID=24+(123 shl 6)
    const val STREETLIGHT_NAME="Pole Streetlight Attachment"
    const val ANALOG_ID=5+(123 shl 6)
    const val SMART_ID=6+(123 shl 6)
    fun part(stack: ItemStack?): Int? {
        if(stack==null || stack.isEmpty)return null
        val d=GenericItemUsingDamageDescriptor.getDescriptor(stack)
        return if(d===link)0 else (d as? UtilityPartDescriptor)?.part
    }
    const val FUSE_ID=4+(123 shl 6)
    private lateinit var link: GenericItemUsingDamageDescriptor
    fun registerFuseLink() {
        link=GenericItemUsingDamageDescriptor(I18N.TR_NAME(I18N.Type.NONE,"Pole Fuse Link"),"polefuselink")
        Eln.sharedItem.addElement(FUSE_ID,link)
        PoleHardware.register()
        Eln.sharedItem.addElement(STREETLIGHT_ID,UtilityPartDescriptor(I18N.TR_NAME(I18N.Type.NONE,"Pole Streetlight Attachment"),3))
        Eln.sharedItem.addElement(ANALOG_ID,UtilityPartDescriptor(I18N.TR_NAME(I18N.Type.NONE,"Analog Glass Service Meter"),1))
        Eln.sharedItem.addElement(SMART_ID,UtilityPartDescriptor(I18N.TR_NAME(I18N.Type.NONE,"Smart Digital Service Meter"),2))
    }
    fun isFuse(stack: ItemStack): Boolean {
        val descriptor=GenericItemUsingDamageDescriptor.getDescriptor(stack)
        return descriptor===link || descriptor is PoleFuseDescriptor && !descriptor.spent
    }
}
