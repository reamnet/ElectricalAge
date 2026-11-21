package mods.eln.transparentnode.cord

import mods.eln.generic.*
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeManager
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.six.*
import mods.eln.node.voxel.VoxelNode
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult

/** Selection edits are settings; only a successful server attachment consumes the actual held cord. */
object CordInteraction {
    @JvmStatic fun heldAnchor(stack: ItemStack): CordAnchor? {
        val tag=ItemNbt.get(stack) ?: return null
        return if(tag.contains("carriedAppliance"))CordAnchor.read(tag.getCompoundOrEmpty("carriedAppliance").getCompoundOrEmpty("applianceCord"))
        else CordAnchor.read(tag.getCompoundOrEmpty("cordSelection"))
    }
    private fun owner(world: Level,hit: BlockHitResult): Any? {
        val n=NodeManager.instance.getNodeFromCoordonate(Coordonate(hit.blockPos,world))
        return when(n) {
            is TransparentNode->n.element
            is VoxelNode->{val p=hit.location;val x=p.x-hit.blockPos.x;val y=p.y-hit.blockPos.y;val z=p.z-hit.blockPos.z;var slot=n.elementAt(x,y,z,0);if(slot<0 && n.partAt(x,y,z)<0)slot=n.elementAt(x,y,z,2);n.getElement(slot)}
            is SixNode->{val entity = world.getBlockEntity(hit.blockPos) as? mods.eln.node.six.SixNodeEntity; entity?.let { SixNodeBlock.sideAt(it, hit.blockPos, hit) }?.let { n.getElement(it) }}
            else->null
        }
    }
    @JvmStatic fun carry(player: Player,world: Level,hit: BlockHitResult): InteractionResult {
        if(!player.isShiftKeyDown || !player.mainHandItem.isEmpty)return InteractionResult.PASS
        if(world.isClientSide) {
            val tile=world.getBlockEntity(hit.blockPos)
            val attached=when(tile) {
                is mods.eln.node.transparent.TransparentNodeEntity -> tile.elementRender?.let { it.cordDisplay.active() || it is OutletRender || it is mods.eln.transparentnode.ups.UpsRender && it.cabinet.compact } ?: false
                is mods.eln.node.six.SixNodeEntity -> tile.elementRenderList.any { it?.cordDisplay?.active()==true }
                else -> false
            }
            return if(attached)InteractionResult.SUCCESS else InteractionResult.PASS
        }
        if(player !is ServerPlayer || !player.mayUseItemAt(hit.blockPos,hit.direction,player.mainHandItem))return InteractionResult.FAIL
        val target=owner(world,hit) ?: return InteractionResult.PASS
        // An empty-handed sneak click on a socket retains source unplug behavior.
        if(target is CordSocketSource) {
            val slot=(0 until target.capacity).filter(target::occupied).minByOrNull { i->
                val a=target.anchor(i)
                net.minecraft.world.phys.Vec3.atCenterOf(hit.blockPos).add(CordGeometry.world(a.front,CordGeometry.tip(i,a.horizontal,a.strip,a.recessed,a.cabinet))).distanceToSqr(hit.location)
            }
            if(slot!=null) { target.disconnectSocket(slot,player);return InteractionResult.SUCCESS }
        }
        return if(CordSupport.device(target)?.cord?.carry(player)==true)InteractionResult.SUCCESS else InteractionResult.PASS
    }
    @JvmStatic fun use(held: ItemStack,player: Player,world: Level,hit: BlockHitResult): InteractionResult {
        val desc=GenericItemUsingDamageDescriptor.getDescriptor(held) as? CordDescriptor ?: return InteractionResult.PASS
        if(world.isClientSide)return InteractionResult.SUCCESS
        if(player !is ServerPlayer || player.mainHandItem!==held || !player.mayUseItemAt(hit.blockPos,hit.direction,held))return InteractionResult.FAIL
        val owner=owner(world,hit)
        val device=owner?.let(CordSupport::device)
        if(player.isShiftKeyDown) {
            if(owner is CordSocketSource) {
                val slot=(0 until owner.capacity).filter(owner::occupied).minByOrNull { i->net.minecraft.world.phys.Vec3.atCenterOf(hit.blockPos).add(owner.anchor(i).let { a->CordGeometry.world(a.front,CordGeometry.tip(i,a.horizontal,a.strip,a.recessed,a.cabinet)) }).distanceToSqr(hit.location) }
                if(slot!=null)owner.disconnectSocket(slot,player)
                return InteractionResult.SUCCESS
            }
            if(device?.cord?.plugged==true) { device.cord.detach(player);return InteractionResult.SUCCESS }
        }
        val data=ItemNbt.get(held);val selected=data?.getCompoundOrEmpty("cordSelection")?.let(CordAnchor::read)
        if(owner is CordSocketSource && !(CordSupport.device(owner.socketOwner)?.acceptsCord(desc.rating)==true && selected!=null && selected.id!=owner.id)) {
            if(owner.rating!=desc.rating) { Utils.sendMessage(player,tr("This cord requires a %1$ outlet",desc.rating.socket));return InteractionResult.SUCCESS }
            val slot=owner.freeSocket();if(slot==null) { Utils.sendMessage(player,tr("Every socket on this outlet is occupied"));return InteractionResult.SUCCESS }
            ItemNbt.edit(held) { it.put("cordSelection",owner.anchor(slot).write()) }
            Utils.sendMessage(player,tr("Outlet selected; connect this cord to a machine"));return InteractionResult.SUCCESS
        }
        if(device==null || selected==null || selected.rating!=desc.rating) { Utils.sendMessage(player,tr("Select a matching outlet, then a compatible machine"));return InteractionResult.SUCCESS }
        if(device.cord.attach(selected,!player.isCreative)) {
            ItemNbt.edit(held) { it.remove("cordSelection") }
            if(!player.isCreative)held.shrink(1)
            Utils.sendMessage(player,tr("Cord connected; sneak-use a cord to unplug"))
        }else Utils.sendMessage(player,tr("Could not connect: check rating, reach, socket and the existing plug"))
        return InteractionResult.SUCCESS
    }
}
