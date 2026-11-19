package mods.eln.transparentnode.industrial

import mods.eln.Eln
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.generic.ItemNbt
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/** Paid kit, world save and removal ledger; control snapshots never carry installation state. */
class PoleSurgeUpgrade(private val owner:TransparentNodeElement,lines:List<State>,val eligible:Boolean) {
    var fitted=false;private set
    val bank=SurgeBank(if(eligible)lines else emptyList(),owner.electricalComponentList,owner.electricalProcessList) {fitted && eligible}
    val remaining get()=bank.remaining
    fun lightning():Boolean {val ok=bank.lightning();if(ok)owner.needPublish();return ok}
    fun item():ItemStack=Eln.findItemStack(IndustrialKind.ARRESTER.title,1).also {ItemNbt.set(it,CompoundTag().apply {putDouble("industrialSurge",remaining)})}
    fun interact(player:Player):Boolean {
        if(!eligible)return false
        val stack=player.mainHandItem
        if(player.isShiftKeyDown && stack.isEmpty && fitted) {
            val refund=item();fitted=false;bank.restore(SurgeBank.CAPACITY);player.inventory.placeItemBackInInventory(refund,net.minecraft.util.Prediction.SERVER_ONLY);owner.needPublish();return true
        }
        if(GenericItemBlockUsingDamageDescriptor.getDescriptor(stack) !is SurgeArresterDescriptor)return false
        val tag=ItemNbt.get(stack)
        val capacity=if(tag?.contains("industrialSurge")==true)tag.getDoubleOr("industrialSurge",0.0).takeIf {it.isFinite()}?.coerceIn(0.0,SurgeBank.CAPACITY) ?: 0.0 else SurgeBank.CAPACITY
        if(capacity<=0 || fitted && remaining>0)return true
        if(fitted)player.inventory.placeItemBackInInventory(item(),net.minecraft.util.Prediction.SERVER_ONLY)
        fitted=true;bank.restore(capacity);if(!player.isCreative)stack.shrink(1);player.inventory.setChanged();owner.needPublish();return true
    }
    fun write(t:CompoundTag) {t.putBoolean("poleArrester",fitted);bank.write(t,"poleArresterJoules")}
    fun read(t:CompoundTag) {fitted=eligible && t.getBooleanOr("poleArrester",false);bank.read(t,"poleArresterJoules",if(fitted)0.0 else SurgeBank.CAPACITY)}
    fun drop() {if(fitted) {val refund=item();fitted=false;bank.prepare();owner.node.dropItem(refund)}}
}
