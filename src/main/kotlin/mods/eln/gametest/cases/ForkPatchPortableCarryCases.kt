package mods.eln.gametest.cases

import com.mojang.authlib.GameProfile
import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.ItemNbt
import mods.eln.misc.Direction
import mods.eln.node.six.SixNodeItem
import mods.eln.node.transparent.TransparentNodeItem
import mods.eln.sixnode.lampsocket.DecorativeLampElement
import mods.eln.transparentnode.cord.*
import mods.eln.transparentnode.ups.*
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import java.util.UUID

/** Real removal and replacement transactions, rather than copying settings onto a second live owner. */
object ForkPatchPortableCarryCases {
    @JvmStatic fun cases(): List<TestCase> = listOf(Lamp(),Ups(),Guards(),VoxelContents())
    private open class Base:TestCase {
        override fun width()=10;override fun height()=5;override fun depth()=10
        override fun build(c:TestContext) {}
        override fun measure(c:TestContext) {}
        fun player(c:TestContext)=TransportTestPlayer(c.level(),GameProfile(UUID.nameUUIDFromBytes(id().toByteArray()),"carryTester"))
        fun outlet(c:TestContext):OutletElement {
            val p=c.at(1,1,2);c.floor(p.north())
            val d=Eln.transparentNodeItem.getDescriptor(7968)!!
            return (d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),player(c),c.level(),p,Direction.ZN)!!.element as OutletElement
        }
        fun ups(c:TestContext,x:Int):CompactUpsElement { c.floor(c.at(x,0,2));return c.placeTransparent(8076,c.at(x,1,2)) as CompactUpsElement }
        fun drops(c:TestContext)=c.level().getEntitiesOfClass(ItemEntity::class.java,AABB(c.at(0,0,0)).inflate(15.0)).sumOf {it.item.count}
        fun cord(e:Any)=requireNotNull(CordSupport.device(e)).cord
    }
    private class Lamp:Base() {
        override fun id()="forkpatch_portable_carry_lamp_paid_transfer"
        override fun build(c:TestContext) {
            val o=outlet(c);c.floor(c.at(3,0,2))
            val e=c.placeSix(4118,c.at(3,1,2)) as DecorativeLampElement
            val bulb=Eln.findItemStack("120V LED Light Bulb",1)
            e.inventory.setItem(0,bulb)
            check(cord(e).attach(o.anchor(0),true))
            val token=cord(e).token;val anchor=cord(e).anchor;val p=player(c);val before=drops(c)
            c.check("portable lamp: actual removal produces one held full snapshot",cord(e).carry(p) && !CordSupport.resident(e) && ItemNbt.get(p.mainHandItem)?.contains("carriedAppliance")==true,"")
            c.check("portable lamp: removal refunds neither contents nor paid lead",drops(c)==before && o.occupied(0) && !cord(e).connected,"")
            val stack=p.mainHandItem;val at=c.at(5,1,2);c.floor(at.below())
            val item=stack.item as SixNodeItem
            check(item.placeAt(stack,p,c.level(),at,Direction.YN))
            stack.shrink(1)
            val fresh=(mods.eln.node.NodeManager.instance.getNodeFromCoordonate(mods.eln.misc.Coordonate(at,c.level())) as mods.eln.node.six.SixNode).getElement(Direction.YN) as DecorativeLampElement
            cord(fresh).poll()
            c.check("portable lamp: replacement retains installed bulb and original paid ownership",fresh.inventory.getItem(0).item===bulb.item && cord(fresh).token==token && cord(fresh).anchor==anchor && cord(fresh).paid && cord(fresh).connected,cord(fresh).state)
            cord(e).detach(p);cord(e).onBroken()
            c.check("portable lamp: stale removed owner cannot reclaim or refund transferred lead",cord(fresh).connected && o.occupied(0) && drops(c)==before,"")
            cord(fresh).detach(p)
            val count=(0 until p.inventory.containerSize).sumOf {p.inventory.getItem(it).let {s->if(s.item===Eln.findItemStack(CordRating.V120.cordName,1).item)s.count else 0}}
            c.check("portable lamp: final unplug returns exactly one paid lead",count==1 && !o.occupied(0),"$count")
        }
    }
    private class Ups:Base() {
        override fun id()="forkpatch_portable_carry_ups_energy_and_downstream_release"
        override fun build(c:TestContext) {
            val o=outlet(c);val e=ups(c,3);val child=ups(c,5)
            val cell=Eln.findItemStack("UPS AGM Battery Cell",1);val type=upsBattery(cell)!!
            val joules=type.getEnergyMax(cell)*.375;type.setEnergy(cell,joules);e.inventory.setItem(0,cell)
            check(e.cord.attach(o.anchor(0),true));check(child.cord.attach(e.anchor(2),true))
            val id=e.id;val token=e.cord.token;val p=player(c);val before=drops(c)
            check(e.cord.carry(p))
            c.check("portable UPS: downstream lead releases before socket snapshot",!child.cord.plugged && drops(c)==before+1,"")
            val stack=p.mainHandItem;val at=c.at(4,1,4);c.floor(at.below())
            val fresh=(stack.item as TransparentNodeItem).placeAt(stack,p,c.level(),at,Direction.YN)!!.element as CompactUpsElement
            stack.shrink(1);fresh.cord.poll()
            val restored=fresh.inventory.getItem(0)
            c.check("portable UPS: exact paid cell energy, identity and upstream token survive",fresh.id==id && fresh.cord.token==token && fresh.cord.connected && fresh.cord.paid && type.getEnergy(restored)==joules && (0 until fresh.capacity).none(fresh::occupied),"energy=${type.getEnergy(restored)}")
            c.check("portable UPS: moving has no inventory or appliance drops",drops(c)==before+1,"")
        }
    }
    private class Guards:Base() {
        override fun id()="forkpatch_portable_carry_guards"
        override fun build(c:TestContext) {
            val o=outlet(c);val e=ups(c,3);val p=player(c)
            c.check("portable carry: unplugged owners cannot be carried",!e.cord.carry(p) && CordSupport.active(e),"")
            check(e.cord.attach(o.anchor(0),true));p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack(Items.STICK))
            c.check("portable carry: occupied hand cannot overwrite player inventory",!e.cord.carry(p) && p.mainHandItem.item===Items.STICK && e.cord.connected,"")
            c.floor(c.at(5,0,2));val fixed=c.placeSix(4123,c.at(5,1,2))
            c.check("portable carry: fixed fixtures stay fixed",CordSupport.device(fixed)?.cordPortable==false,"")
        }
    }
    private class VoxelContents:Base() {
        override fun id()="forkpatch_portable_carry_preserves_other_voxel_hardware"
        override fun build(c:TestContext) {
            val previous=mods.eln.node.voxel.CableConverter.convertOnLoad
            mods.eln.node.voxel.CableConverter.convertOnLoad=true
            try {
                val o=outlet(c);val at=c.at(4,1,2);c.floor(at.below());c.floor(at.above())
                val e=c.placeSix(4118,at) as DecorativeLampElement
                val node=e.sixNode as mods.eln.node.voxel.VoxelNode
                val part=mods.eln.node.voxel.CablePart(0,Direction.YP.int,intArrayOf(0,15,8,16,15,8))
                val error=node.addPart(part);check(error==null){"voxel fixture cable: $error"}
                check(cord(e).attach(o.anchor(0),true));val p=player(c);val before=drops(c)
                check(cord(e).carry(p))
                c.check("portable voxel: carrying its final fixture preserves the cable block and exact cable part",!CordSupport.resident(e) && !node.isDestructing && node.parts.size==1 && node.parts[0]===part && c.level().getBlockState(at).block is mods.eln.node.voxel.VoxelNodeBlock,"")
                c.check("portable voxel: remaining hardware is neither dropped nor duplicated",drops(c)==before && p.mainHandItem.item===Eln.sixNodeItem.getDescriptor(4118)!!.newItemStack().item,"")
            }finally{mods.eln.node.voxel.CableConverter.convertOnLoad=previous}
        }
    }

}
