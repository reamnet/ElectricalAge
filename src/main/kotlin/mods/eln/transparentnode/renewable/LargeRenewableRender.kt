package mods.eln.transparentnode.renewable

import mods.eln.Eln
import mods.eln.cable.CableRenderType
import mods.eln.misc.*
import mods.eln.node.transparent.*
import java.io.DataInputStream

class LargeRenewableRender(e: TransparentNodeEntity,d: TransparentNodeDescriptor):TransparentNodeElementRender(e,d) {
    var environment=0.0;private set
    var availableWatts=0.0;private set
    var watts=0.0;private set
    private var angle=0f
    private val connections=LRDUMask()
    private var cable: CableRenderType?=null
    override fun draw() { cable=drawCable(Direction.YN,Eln.stdCableRender3200V,connections,cable);(transparentNodedescriptor as LargeRenewableDescriptor).draw(front,angle) }
    override fun refresh(deltaT: Float) { super.refresh(deltaT);val speed=if((transparentNodedescriptor as LargeRenewableDescriptor).water)environment*35 else environment*18;angle=((angle+speed*deltaT)%360).toFloat() }
    override fun networkUnserialize(input: DataInputStream) { super.networkUnserialize(input);environment=input.readDouble();availableWatts=input.readDouble();watts=input.readDouble();connections.deserialize(input);cable=null }
    private val bounds=tileEntity.blockPos.let { p->net.minecraft.world.phys.AABB(p.x-6.0,p.y.toDouble(),p.z-6.0,p.x+7.0,p.y+16.0,p.z+7.0) }
    override fun renderBounds()=bounds
    override fun cameraDrawOptimisation()=false
}
