package mods.eln.sixnode.lampsocket

import mods.eln.generic.ItemNbt
import mods.eln.item.LampDescriptor
import net.minecraft.world.item.ItemStack
import kotlin.math.*

/** Absolute source hours with a normalized native life tag; configuration changes never renew used bulbs. */
object SourceLampLife {
    val technologies=listOf("incandescent","carbon","fluorescent","farming","led","halogen")
    val defaults=doubleArrayOf(24.0,12.0,168.0,48.0,672.0,96.0)
    @JvmStatic fun technology(d:LampDescriptor):Int=RatedLampBulbs.ratings.indexOfFirst{it.id==d.parentItemDamage}.let{if(it<0)-1 else it/3}
    /** Growth is not a technology tag: ordinary rated bulbs also help plants grow. */
    @JvmStatic fun visualTechnology(d:LampDescriptor?):String {
        if(d==null)return "led"
        val rated=technology(d)
        if(rated>=0)return technologies[rated]
        return when {
            d.type==LampDescriptor.Type.LED -> "led"
            d.type==LampDescriptor.Type.eco -> "fluorescent"
            d.vegetableGrowRate>0 -> "farming"
            else -> "incandescent"
        }
    }
    @JvmStatic fun source(d:LampDescriptor)=technology(d)>=0
    @JvmStatic fun hours(d:LampDescriptor):Double {val t=technology(d);return if(t<0)d.nominalLife/3600.0 else mods.eln.ElnConfig.sourceLampHours(t)}
    @JvmStatic fun infinite(d:LampDescriptor):Boolean {val t=technology(d);return if(t<0)d.type==LampDescriptor.Type.LED&&mods.eln.Eln.ledLampInfiniteLife else mods.eln.ElnConfig.sourceLampInfinite(t)}
    @JvmStatic fun effectiveSeconds(d:LampDescriptor)=hours(d)*3600.0
    fun lossHours(seconds:Double,volts:Double,nominal:Double,minimal:Double):Double {
        if(!seconds.isFinite()||seconds<=0||!volts.isFinite()||!nominal.isFinite()||nominal<=0)return 0.0
        val u=abs(volts)
        val rate=when{u>nominal->10.0.pow((4.0*(u/nominal-1.0)).coerceAtMost(300.0));u>=minimal->((u-minimal)/(nominal-minimal)).coerceIn(0.0,1.0);else->0.0}
        return (seconds/3600.0*rate).coerceAtMost(Double.MAX_VALUE)
    }
    /** Reset only installed lamps in resident, active nodes; never renew future/offline inventory. */
    @JvmStatic fun requestReset():Int {
        var reset=0
        for(node in mods.eln.node.NodeManager.instance.nodeArray.values.toList()) {
            if(!node.isAdded)continue
            val c=node.coordonate;val w=c.world() as? net.minecraft.server.level.ServerLevel?:continue
            if(w.chunkSource.getChunkNow(c.x shr 4,c.z shr 4)==null)continue
            val inventories=when(node) {
                is mods.eln.node.six.SixNode -> node.sideElementList.filterIsInstance<LampSocketElement>().map { it.inventory }
                is mods.eln.node.transparent.TransparentNode -> listOfNotNull(when(val e=node.element) {
                    is mods.eln.transparentnode.floodlight.FloodlightElement -> e.inventory
                    is mods.eln.transparentnode.worklight.WorkLightElement -> e.inventory
                    else -> null
                })
                else -> emptyList()
            }
            for(inventory in inventories)for(slot in 0 until inventory.containerSize) {
                val stack=inventory.getItem(slot);val bulb=mods.eln.misc.Utils.getItemObject(stack) as? LampDescriptor?:continue
                if(!source(bulb))continue
                ItemNbt.edit(stack){it.putDouble("life",1.0);it.putDouble("sourceLampHours",hours(bulb))}
                inventory.setChanged();reset+=stack.count
            }
        }
        return reset
    }
    @JvmStatic fun account(stack:ItemStack,d:LampDescriptor,volts:Double,seconds:Double):Double {
        if(stack.isEmpty)return 0.0
        if(!source(d)){
            val old=d.getLifeInTag(stack)
            if(mods.eln.server.SaveConfig.instance.electricalLampAging&&seconds.isFinite()&&seconds>0&&volts.isFinite()&&!infinite(d))d.setLifeInTag(stack,(old-seconds*(volts/d.nominalU).pow(2)/d.nominalLife).coerceAtLeast(0.0))
            return d.getLifeInTag(stack)
        }
        val tag=ItemNbt.get(stack);val max=hours(d)
        val fraction=tag?.getDoubleOr("life",1.0)?:1.0
        val stored=tag?.getDoubleOr("sourceLampHours",Double.NaN)?:Double.NaN
        var left=if(stored.isFinite())stored.coerceIn(0.0,max)else if(fraction.isFinite())(fraction.coerceIn(0.0,1.0)*max)else 0.0
        if(!infinite(d)&&mods.eln.server.SaveConfig.instance.electricalLampAging)left=(left-lossHours(seconds,volts,d.nominalU,d.minimalU)).coerceAtLeast(0.0)
        ItemNbt.edit(stack){it.putDouble("life",left/max);it.putDouble("sourceLampHours",left)}
        return left/max
    }
    fun brightness(d:LampDescriptor,volts:Double,warmup:Double):Int {
        if(!volts.isFinite())return 0
        val fraction=((abs(volts)-d.minimalU)/(d.nominalU-d.minimalU)).coerceIn(0.0,1.0)
        if(d.type==LampDescriptor.Type.eco&&warmup<d.stableTime&&Math.random()>warmup/d.stableTime)return 0
        return (fraction*(d.nominalLight*15).roundToInt()).roundToInt().coerceIn(0,15)
    }
}
