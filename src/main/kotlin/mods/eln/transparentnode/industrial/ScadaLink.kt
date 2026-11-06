package mods.eln.transparentnode.industrial

import mods.eln.i18n.I18N.tr
import mods.eln.misc.Coordonate
import mods.eln.node.NodeManager
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.transparentnode.switchgear.SwitchgearElement
import mods.eln.utility.*
import net.minecraft.core.BlockPos

/** Resolve each operation afresh. No stale node cache, chunk requests or cross-world lookup. */
object ScadaLink {
    fun target(owner:ScadaElement,address:Long):TransparentNodeElement? {
        if(address==Long.MIN_VALUE)return null
        val c=owner.node?.coordonate ?: return null
        val at=BlockPos.of(address)
        if(at.distSqr(c.pos())>256.0*256)return null
        val world=c.world() as? net.minecraft.server.level.ServerLevel ?: return null
        if(world.chunkSource.getChunkNow(at.x shr 4,at.z shr 4)==null)return null
        return NodeManager.instance?.getTransparentNodeFromCoordinate(Coordonate(at,world))
    }
    fun read(owner:ScadaElement,address:Long,circuit:Int):String {
        val e=target(owner,address) ?: return tr("Target unavailable or chunk unloaded")
        val lines=e.waila.entries.take(4).joinToString("\n") { "${it.key}: ${it.value}" }
        return tr("%1$\n%2$",e.descriptor.name,lines)
    }
    fun command(owner:ScadaElement,address:Long,circuit:Int,closed:Boolean):Boolean {
        if(!owner.unit.canCommand)return false
        val e=target(owner,address) ?: return false
        val accepted=when(e) {
            is HouseServiceElement->e.setRemoteDisconnect(!closed)
            is DistributionPoleElement->e.unit.switch(closed)
            is SwitchgearElement->{e.unit.setEnabled(closed);true}
            is IndustrialElement->{e.unit.setEnabled(closed);true}
            is PadTransformerElement->{e.unit.setEnabled(closed);true}
            is LoadBankElement->{e.unit.setEnabled(closed);true}
            is MotorDriveElement->{e.unit.setEnabled(closed);true}
            is SurgeArresterElement->{e.unit.setEnabled(closed);true}
            else->false
        }
        if(accepted)e.needPublish()
        return accepted
    }
}
