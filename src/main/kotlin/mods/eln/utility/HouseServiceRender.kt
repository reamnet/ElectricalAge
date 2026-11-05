package mods.eln.utility
import mods.eln.node.transparent.*
import mods.eln.misc.*
import java.io.*

class HouseServiceRender(entity: TransparentNodeEntity,d: TransparentNodeDescriptor): TransparentNodeElementRender(entity,d) {
    val desc=d as HouseServiceDescriptor
    val display=HouseServiceDisplay()
    private var clock=0.0;private var disk=0.0
    override fun draw()=desc.drawService(front,display,disk,((clock/5).toInt()%4))
    override fun refresh(deltaT: Float) { super.refresh(deltaT);val dt=deltaT.coerceIn(0f,.25f);clock+=dt;disk=(disk+ServiceMeters.diskDegrees(display.watts*dt))%360 }
    override fun cameraDrawOptimisation()=false
    override fun networkUnserialize(s: DataInputStream) { super.networkUnserialize(s);display.read(s);disk=ServiceMeters.diskPosition(display.imported,display.exported) }
    override fun getCableRender(side: Direction,lrdu: LRDU): mods.eln.cable.CableRenderDescriptor? {
        val matches=if(desc.wide)side==Direction.YN && (lrdu==Direction.YN.getLRDUGoingTo(front.inverse) || lrdu==Direction.YN.getLRDUGoingTo(front.inverse)?.inverse()) else desc.acceptsCable(front,side,lrdu) && side in listOf(Direction.YP,Direction.YN,front.left(),front.right())
        return if(matches)mods.eln.Eln.stdCableRender200V else null
    }
}
