package mods.eln.utility
import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Player

/** Paid transformer ratings and replaceable primary fuse cartridges. */
class PoleFuseDescriptor(name: String,val amps: Double,val spent: Boolean=false): GenericItemUsingDamageDescriptor(name,"polefuselink") {
    override fun addInformation(stack: ItemStack?,player: Player?,list: MutableList<Any?>,advanced: Boolean) {
        if(spent)list.add(tr("Blown fuse; replace with a new cartridge")) else list.add(tr("Pole fuse cartridge: %1$ A",amps.toInt()))
        list.add(tr("Right-click a cutout to install; sneak with an empty hand to remove its fuse"))
    }
}
class PoleTransformerDescriptor(name: String,val watts: Int): GenericItemUsingDamageDescriptor(name,"poletransformer") {
    override fun hasItemDraw()=true
    override fun drawItem(stack: ItemStack,entity: Boolean)=PoleTransformerItemVisual.draw(watts)
    override fun addInformation(stack: ItemStack?,player: Player?,list: MutableList<Any?>,advanced: Boolean) {
        list.add(tr("Physical pole transformer: %1$ kW",watts/1000));list.add(tr("12.47 kV nominal primary; install in the transformer slot"))
        list.add(tr("Capacity is supplied by this item, not the voltage settings"))
    }
}
object PoleHardware {
    val transformerNames=listOf(I18N.TR_NAME(I18N.Type.NONE,"10 kW Pole Transformer"),I18N.TR_NAME(I18N.Type.NONE,"25 kW Pole Transformer"),I18N.TR_NAME(I18N.Type.NONE,"50 kW Pole Transformer"),I18N.TR_NAME(I18N.Type.NONE,"75 kW Pole Transformer"),I18N.TR_NAME(I18N.Type.NONE,"100 kW Pole Transformer"))
    val transformerRatings=listOf(10_000,25_000,50_000,75_000,100_000)
    fun transformerWatts(stack: ItemStack?): Int?=if(stack==null || stack.isEmpty)null else (GenericItemUsingDamageDescriptor.getDescriptor(stack) as? PoleTransformerDescriptor)?.watts

    val fuseNames=listOf(
        I18N.TR_NAME(I18N.Type.NONE,"1 A Pole Fuse"),I18N.TR_NAME(I18N.Type.NONE,"2 A Pole Fuse"),I18N.TR_NAME(I18N.Type.NONE,"3 A Pole Fuse"),
        I18N.TR_NAME(I18N.Type.NONE,"5 A Pole Fuse"),I18N.TR_NAME(I18N.Type.NONE,"8 A Pole Fuse"),I18N.TR_NAME(I18N.Type.NONE,"10 A Pole Fuse"),
        I18N.TR_NAME(I18N.Type.NONE,"20 A Pole Fuse"),I18N.TR_NAME(I18N.Type.NONE,"30 A Pole Fuse"),I18N.TR_NAME(I18N.Type.NONE,"50 A Pole Fuse"),
        I18N.TR_NAME(I18N.Type.NONE,"100 A Pole Fuse"),I18N.TR_NAME(I18N.Type.NONE,"200 A Pole Fuse"))
    val fuseAmps=listOf(1.0,2.0,3.0,5.0,8.0,10.0,20.0,30.0,50.0,100.0,200.0)
    val spentName=I18N.TR_NAME(I18N.Type.NONE,"Blown Pole Fuse")
    fun register() {
        transformerNames.forEachIndexed { i,n->Eln.sharedItem.addElement(7+i+(123 shl 6),PoleTransformerDescriptor(n,transformerRatings[i])) }
        fuseNames.forEachIndexed { i,n -> Eln.sharedItem.addElement(12+i+(123 shl 6),PoleFuseDescriptor(n,fuseAmps[i])) }
        Eln.sharedItem.addElement(23+(123 shl 6),PoleFuseDescriptor(spentName,0.0,true))
    }
    fun fuse(stack: ItemStack?)=if(stack==null || stack.isEmpty)null else GenericItemUsingDamageDescriptor.getDescriptor(stack) as? PoleFuseDescriptor
    fun extract(player: Player,fuses: List<CutoutFuse>): Boolean {
        if(!player.isShiftKeyDown || !player.mainHandItem.isEmpty)return false
        val f=fuses.firstOrNull { it.hasCartridge } ?: return false
        val item=f.removeCartridge();player.inventory.placeItemBackInInventory(item,net.minecraft.util.Prediction.SERVER_ONLY);return true
    }
}
