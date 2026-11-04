package mods.eln.utility
import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N
import net.minecraft.world.item.ItemStack

/** Paid, replaceable fuse links; remaining utility hardware is not registered yet. */
object UtilityItems {
    const val FUSE_ID=4+(123 shl 6)
    private lateinit var link: GenericItemUsingDamageDescriptor
    fun registerFuseLink() {
        link=GenericItemUsingDamageDescriptor(I18N.TR_NAME(I18N.Type.NONE,"Pole Fuse Link"),"polefuselink")
        Eln.sharedItem.addElement(FUSE_ID,link)
        PoleHardware.register()
    }
    fun isFuse(stack: ItemStack): Boolean {
        val descriptor=GenericItemUsingDamageDescriptor.getDescriptor(stack)
        return descriptor===link || descriptor is PoleFuseDescriptor && !descriptor.spent
    }
}
