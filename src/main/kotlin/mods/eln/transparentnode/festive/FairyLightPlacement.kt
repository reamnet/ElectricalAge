package mods.eln.transparentnode.festive

import mods.eln.generic.ItemNbt
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.misc.Utils
import mods.eln.i18n.I18N.tr
import mods.eln.node.transparent.TransparentNodeItem
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.phys.Vec3

object FairyLightPlacement {
    fun use(item:TransparentNodeItem,c:UseOnContext):InteractionResult {
        val p=c.player?:return InteractionResult.PASS
        if(c.level.isClientSide)return InteractionResult.SUCCESS
        val world=c.level as? net.minecraft.server.level.ServerLevel?:return InteractionResult.FAIL
        val clicked=c.clickedPos
        if(world.chunkSource.getChunkNow(clicked.x shr 4,clicked.z shr 4)==null||world.getBlockState(clicked).getCollisionShape(world,clicked).isEmpty)return InteractionResult.FAIL
        val stack=c.itemInHand
        if(!world.mayInteract(p,clicked)||!p.mayUseItemAt(clicked,c.clickedFace,stack))return InteractionResult.FAIL
        val n=c.clickedFace.unitVec3;val point=c.clickLocation.add(n.x*.025,n.y*.025,n.z*.025)
        val dimension=Coordonate(clicked,world).dimention
        val tag=ItemNbt.get(stack)?:CompoundTag();val first=tag.getCompoundOrEmpty("fairyFirst")
        if(!tag.contains("fairyFirst")||first.getIntOr("dimension",Int.MIN_VALUE)!=dimension||p.isShiftKeyDown){
            ItemNbt.edit(stack){it.put("fairyFirst",CompoundTag().apply{putInt("dimension",dimension);putDouble("x",point.x);putDouble("y",point.y);putDouble("z",point.z);putInt("supportX",clicked.x);putInt("supportY",clicked.y);putInt("supportZ",clicked.z)})}
            Utils.sendMessage(p,tr("First fairy-light anchor selected; choose the other end"));return InteractionResult.SUCCESS
        }
        val start=Vec3(first.getDoubleOr("x",Double.NaN),first.getDoubleOr("y",Double.NaN),first.getDoubleOr("z",Double.NaN))
        val support=BlockPos(first.getIntOr("supportX",0),first.getIntOr("supportY",0),first.getIntOr("supportZ",0))
        if(!start.x.isFinite()||!start.y.isFinite()||!start.z.isFinite()||start.distanceTo(point)!in .25..8.0||world.chunkSource.getChunkNow(support.x shr 4,support.z shr 4)==null||world.getBlockState(support).getCollisionShape(world,support).isEmpty||!world.mayInteract(p,support))return InteractionResult.FAIL
        val at=clicked.relative(c.clickedFace)
        if(!p.mayUseItemAt(at,c.clickedFace,stack))return InteractionResult.FAIL
        val copy=stack.copyWithCount(1)
        ItemNbt.edit(copy){it.remove("fairyFirst");for((key,v)in listOf("fairyStart" to start,"fairyEnd" to point)){it.putDouble(key+"X",v.x);it.putDouble(key+"Y",v.y);it.putDouble(key+"Z",v.z)}}
        val node=item.placeAt(copy,p,world,at,Direction.fromEnumFacing(c.clickedFace).inverse)?:return InteractionResult.FAIL
        ItemNbt.edit(stack){it.remove("fairyFirst")};if(!p.isCreative)stack.shrink(1)
        return InteractionResult.SUCCESS
    }
}
