package mods.eln.transparentnode.industrial

import mods.eln.Eln
import mods.eln.node.NodeManager
import mods.eln.node.transparent.TransparentNode
import mods.eln.utility.DistributionPoleElement
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LightningBolt
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent

object SurgeProtection {
    /** Existing nodes only; a rare strike must not load terrain or search another dimension. */
    fun protect(world:ServerLevel,bolt:LightningBolt):Boolean {
        if(bolt.persistentData.getBooleanOr("elnSurgeProtected",false))return false
        val nodes=NodeManager.instance?.nodes?.asSequence()?.filterIsInstance<TransparentNode>() ?: return false
        val nearest=nodes.mapNotNull {n->
            val e=n.element ?: return@mapNotNull null
            val c=n.coordonate ?: return@mapNotNull null
            if(c.world()!==world || world.chunkSource.getChunkNow(c.x shr 4,c.z shr 4)==null)return@mapNotNull null
            when(e) {
                is SurgeArresterElement->if(e.unit.enabled && e.signal.normalized<.6 && e.unit.bank.remaining>=SurgeBank.LIGHTNING)Pair(c.pos().distSqr(bolt.blockPosition()),{e.absorbLightning()}) else null
                is DistributionPoleElement->if(e.arrester.fitted && e.arrester.remaining>=SurgeBank.LIGHTNING)Pair(c.pos().above(7).distSqr(bolt.blockPosition()),{e.arrester.lightning()}) else null
                else->null
            }
        }.filter {it.first<=64}.minByOrNull {it.first} ?: return false
        if(!nearest.second())return false
        bolt.persistentData.putBoolean("elnSurgeProtected",true);bolt.setVisualOnly(true);return true
    }
    @JvmStatic fun lightning(event:EntityJoinLevelEvent) {
        val bolt=event.entity as? LightningBolt ?: return
        val world=event.level as? ServerLevel ?: return
        protect(world,bolt)
    }
}
