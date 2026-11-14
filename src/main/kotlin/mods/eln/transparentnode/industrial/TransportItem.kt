package mods.eln.transparentnode.industrial
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.core.BlockPos

class TransportItem(name: String,val pallet: Boolean): GenericItemUsingDamageDescriptor(name) {
    override fun onItemUse(stack: ItemStack?,player: Player?,world: Level?,x: Int,y: Int,z: Int,side: Int,vx: Float,vy: Float,vz: Float): Boolean {
        if(stack==null || stack.isEmpty || player==null || world==null)return false
        val at=BlockPos(x,y,z).relative(net.minecraft.core.Direction.from3DDataValue(side))
        val entity=if(pallet)PalletEntity(PalletEntity.TYPE.get(),world) else ForkliftEntity(ForkliftEntity.TYPE.get(),world)
        entity.snapTo(at.x+.5,at.y.toDouble(),at.z+.5,player.yRot,0f)
        if(!world.noCollision(entity,entity.boundingBox))return false
        if(!world.isClientSide) {
            if(entity is PalletEntity)entity.unpack(stack)
            if(!world.addFreshEntity(entity))return false
            if(!player.abilities.instabuild || pallet && stack.has(net.minecraft.core.component.DataComponents.CONTAINER))stack.shrink(1)
        }
        return true
    }
    override fun addInformation(stack: ItemStack?,player: Player?,list: MutableList<Any?>,advanced: Boolean) {
        if(pallet)list.add(tr("27 equipment slots; right-click to open; sneak empty-handed to pack up"))
        else {
            list.add(tr("Sneak with a traction pack to install; sneak empty-handed to remove it while parked"))
            list.add(tr("Right-click to drive; movement keys steer; Page Up/Down raise/lower; G loads/unloads a pallet"))
        }
    }
}
