package mods.eln.transparentnode.festive

import mods.eln.ghost.GhostGroup
import mods.eln.misc.Direction
import mods.eln.misc.Obj3D
import mods.eln.misc.UtilsClient
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElementRender
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.gl.GL11
import java.io.DataInputStream
import java.io.IOException

class ChristmasTreeDescriptor(val name: String, val obj: Obj3D): TransparentNodeDescriptor(name, StringLightsElement::class.java, ChristmasTreeRender::class.java) {
    private var star: Obj3D.Obj3DPart? = null
    private var string1: Obj3D.Obj3DPart? = null
    private var string2: Obj3D.Obj3DPart? = null
    private var tree: Obj3D.Obj3DPart? = null

    init {
        star = obj.getPart("StarOn_Star.002")
        string1 = obj.getPart("Strip1_Star.000")
        string2 = obj.getPart("Strip2_Star.001")
        tree = obj.getPart("Tree_Cone.006")
        val gg = GhostGroup()
        for(x in 0..1)for(y in 0..2)for(z in -1..1)gg.addElement(x,y,z,mods.eln.Eln.ghostBlock,mods.eln.ghost.GhostBlock.tClearance)
        gg.addElement(1,2,0,mods.eln.Eln.ghostBlock,mods.eln.ghost.GhostBlock.tClearance)
        gg.addElement(1,3,0,mods.eln.Eln.ghostBlock,mods.eln.ghost.GhostBlock.tClearance)
        gg.removeElement(0, 0, 0)
        ghostGroup = gg
    }

    override fun hasItemDraw()=true
    override fun drawItem(entity:Boolean)=mods.eln.misc.preserveMatrix { GL11.glScaled(.4,.4,.4);draw(Direction.ZP,0,false) }
    fun draw(front: Direction, delta: Int, powered: Boolean, treeOnly: Boolean?=null) {
        if (star != null && tree != null && string1 != null && string2 != null) {
            front.glRotateZnRef()
            GL11.glTranslatef(0.5f, -0.5f, 0.5f)
            if(treeOnly!=true) {
            if (powered) {
                UtilsClient.drawLight(star)
                if (delta > 10) {
                    UtilsClient.drawLight(string2)
                    string1?.draw()
                } else {
                    UtilsClient.drawLight(string1)
                    string2?.draw()
                }
            } else {
                star?.draw()
                string1?.draw()
                string2?.draw()
            }
            }
            if(treeOnly!=false)tree?.draw()
        }
    }
}

class ChristmasTreeRender(tileEntity: TransparentNodeEntity, transparentNodedescriptor: TransparentNodeDescriptor): StringLightsRender(tileEntity, transparentNodedescriptor) {
    var x = 0
    private var phase=0f

    override fun draw() {
        mods.eln.misc.preserveMatrix { (transparentNodedescriptor as ChristmasTreeDescriptor).draw(front,x,powered) }
    }
    override fun renderBounds()=net.minecraft.world.phys.AABB(tileEntity.blockPos).inflate(4.0)
    override fun refresh(deltaT: Float) {
        if(deltaT.isFinite()&&deltaT>0){phase=(phase+deltaT)%1f;x=(phase*20).toInt()}
        super.refresh(deltaT)
    }

    override fun cameraDrawOptimisation(): Boolean {
        return false
    }
}
