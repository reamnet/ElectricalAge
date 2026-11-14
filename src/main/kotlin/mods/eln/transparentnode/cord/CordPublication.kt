package mods.eln.transparentnode.cord

import mods.eln.misc.Direction
import mods.eln.node.six.SixNodeElement
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.transparentnode.fabricator.FabricatorElement
import java.io.DataOutputStream
import net.minecraft.world.phys.Vec3

object CordPublication {
    @JvmStatic fun has(owner: Any)=CordSupport.device(owner)?.cord?.anchor!=null
    @JvmStatic fun write(owner: Any,s: DataOutputStream) {
        val c=requireNotNull(CordSupport.device(owner)).cord;val a=requireNotNull(c.anchor)
        s.writeLong(a.pos.asLong());s.writeByte(a.front.int);s.writeByte(a.rating.ordinal);s.writeByte(a.socket);s.writeBoolean(a.horizontal);s.writeBoolean(a.strip);s.writeBoolean(a.recessed)
        val entry=when(owner) {
            is FabricatorElement->Vec3(0.0,-.35,-.45)
            is TransparentNodeElement->CordGeometry.world(owner.front,Vec3(0.0,-.43,-.25))
            is SixNodeElement->{ val d=DoubleArray(3);owner.side.applyTo(d,.42);Vec3(d[0]+owner.ox/16.0,d[1]+owner.oy/16.0,d[2]+owner.oz/16.0) }
            else->Vec3.ZERO
        }
        s.writeDouble(entry.x);s.writeDouble(entry.y);s.writeDouble(entry.z);s.writeUTF(c.state)
    }
}
