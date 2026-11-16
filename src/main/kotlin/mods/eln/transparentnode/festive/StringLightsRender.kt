package mods.eln.transparentnode.festive

import mods.eln.node.transparent.*
import java.io.DataInputStream

class StringLightsRender(e: TransparentNodeEntity,d: TransparentNodeDescriptor):TransparentNodeElementRender(e,d) {
    var powered=false;private set
    var channel="xmas";private set
    var volts=120;private set
    var plugged=false;private set
    override fun draw()=(transparentNodedescriptor as StringLightsDescriptor).draw(front,powered)
    override fun cameraDrawOptimisation()=false
    override fun networkUnserialize(input: DataInputStream) { super.networkUnserialize(input);powered=input.readBoolean();channel=input.readUTF();volts=input.readInt();plugged=input.readBoolean() }
}
