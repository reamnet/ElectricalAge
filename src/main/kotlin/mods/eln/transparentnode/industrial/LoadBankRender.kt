package mods.eln.transparentnode.industrial

import mods.eln.Eln
import mods.eln.misc.*
import mods.eln.node.transparent.*
import java.io.*

class LoadBankRender(e: TransparentNodeEntity,d: TransparentNodeDescriptor):TransparentNodeElementRender(e,d) {
    val display=LoadBankDisplay()
    init { addLoopedSound(object: mods.eln.sound.LoopedSound("eln:appliance_industrial_fan",coordonate()) {
        override fun getVolume()=if(display.watts>50).25f else 0f
        override fun getPitch()=.8f
    }) }
    override fun draw()=(transparentNodedescriptor as LoadBankDescriptor).draw(front,display)
    override fun cameraDrawOptimisation()=false
    override fun getCableRender(side: Direction,lrdu: LRDU)=when {
        side==front && (lrdu==LRDU.Down || lrdu==LRDU.Up)->mods.eln.registry.ElnDeviceRegistry.signalCableDescriptor.render
        (side==front.left() || side==Direction.YN) && lrdu==LRDU.Down->Eln.stdCableRender3200V
        else->null
    }
    override fun networkUnserialize(s: DataInputStream) { super.networkUnserialize(s);display.read(s) }
    fun settings(values: DoubleArray): Boolean {
        if(!IndustrialKind.LOAD_BANK.valid(values))return false
        val b=ByteArrayOutputStream();DataOutputStream(b).use { preparePacketForServer(it);it.writeByte(80);values.forEach(it::writeDouble) };sendPacketToServer(b);return true
    }
}
