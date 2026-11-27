package mods.eln.utility.modular

import mods.eln.Eln
import mods.eln.gl.GL11.*
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.node.transparent.TransparentNodeElementRender
import mods.eln.node.transparent.TransparentNodeEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes

internal data class StockPart(val volume: AABB, val paint: Int)
private fun part(x0: Double,y0: Double,z0: Double,x1: Double,y1: Double,z1: Double,color: Int) =
    StockPart(AABB(x0,y0,z0,x1,y1,z1),color)

enum class SubstationSupportKind(val title: String) {
    SUPPORT("Busbar Support Insulator"),
    STACK("Substation Vertical Insulator Stack"),
    FRAME("Steel Equipment Support Frame"),
    BRACKET("Substation Horizontal Bracket"),
    GANTRY("Steel Substation Gantry Section"),
    BASE("Substation Transformer Foundation");

    val id get() = 14088 + ordinal
    internal val parts get() = when (this) {
        SUPPORT, STACK -> buildList {
            val radius = if (this@SubstationSupportKind == SUPPORT) .18 else .26
            add(part(-radius,-.50,-radius,radius,-.41,radius,0x525c61))
            add(part(-.070,-.43,-.070,.07,.43,.07,0x9aabae))
            val fins = if (this@SubstationSupportKind == SUPPORT) 4 else 6
            repeat(fins) { i ->
                val y=-.34+i*.68/(fins-1)
                // Overlap two offset profiles to chamfer the corners of each skirt.
                add(part(-radius,y,-.66*radius,radius,y+.04,.66*radius,0x708f91))
                add(part(-.66*radius,y+.001,-radius,.66*radius,y+.041,radius,0x748e91))
            }
            add(part(-.10,.39,-.10,.10,.50,.10,0x788185))
        }
        FRAME -> buildList {
            for(x in listOf(-.43,.35))for(z in listOf(-.43,.35)) {
                add(part(x,-.5,z,x+.08,.50,z+.08,0x5d6771))
                add(part(x-.045,-.5,z-.045,x+.125,-.44,z+.125,0x48505a))
            }
            for(z in listOf(-.43,.35)) {
                add(part(-.43,.37,z,.43,.46,z+.08,0x67727c))
                add(part(-.43,-.22,z,.43,-.15,z+.08,0x4d565f))
            }
            for(x in listOf(-.42,.34))add(part(x,.37,-.35,x+.08,.46,.35,0x67727c))
        }
        BRACKET -> listOf(
            part(-.50,-.34,-.27,-.41,.32,.27,0x4d5b63),
            part(-.41,.02,-.22,.42,.10,-.13,0x718088),
            part(-.41,.02,.13,.42,.10,.22,0x718088),
            part(-.02,.10,-.25,.25,.17,.25,0x5b666d),
            part(-.41,-.23,-.18,-.23,.03,.18,0x57636d))
        GANTRY -> buildList {
            for(z in listOf(-.38,.30)) {
                for(x in listOf(-.48,.40))add(part(x,-.50,z,x+.08,.50,z+.08,0x606c77))
                for(y in listOf(-.43,0.0,.35))add(part(-.40,y,z,.40,y+.075,z+.08,0x75818b))
                for((x,y)in listOf(-.29 to -.29,-.04 to -.12,.21 to .08))
                    add(part(x,y,z-.014,x+.16,y+.20,z+.095,0x50606b))
            }
            for(x in listOf(-.48,.40))add(part(x,.37,-.30,x+.08,.44,.30,0x74808a))
        }
        BASE -> listOf(
            part(-.5,-.50,-.5,.5,-.29,.5,0x747d7d),
            part(-.44,-.29,-.44,.44,-.18,.44,0x8e9694),
            part(-.38,-.18,-.37,.38,-.08,-.23,0x57636a),
            part(-.38,-.18,.23,.38,-.08,.37,0x57636a),
            part(-.29,-.08,-.34,-.23,-.02,-.26,0x444d54),
            part(.23,-.08,.26,.29,-.02,.34,0x444d54))
    }
}

class SubstationSupportDescriptor(val kind: SubstationSupportKind) :
    TransparentNodeDescriptor(kind.title, SubstationSupportElement::class.java, SubstationSupportRender::class.java) {
    private val parts = kind.parts
    private val shapes = Direction.values().associateWith { direction ->
        var shape = Shapes.empty()
        for (part in parts) {
            val box = part.volume
            val a = direction.rotateFromXN(Vec3(box.minX,box.minY,box.minZ)).add(.5,.5,.5)
            val b = direction.rotateFromXN(Vec3(box.maxX,box.maxY,box.maxZ)).add(.5,.5,.5)
            shape = Shapes.or(shape,Shapes.create(AABB(a,b)))
        }
        shape.optimize()
    }

    override fun mustHaveFloor() = false
    override fun shapeAt(entity: TransparentNodeEntity, collision: Boolean) = shapes.getValue(entity.front() ?:
        ((entity.node as? TransparentNode)?.element as? SubstationSupportElement)?.front ?: Direction.XN)

    override fun hasItemDraw() = true
    override fun drawItem(entity: Boolean) = drawMounted(Direction.XN)
    fun drawMounted(front: Direction) {
        glPushMatrix()
        try {
            front.glRotateXnRef()
            SubstationStock.draw { for (part in parts) box(part.volume,part.paint) }
        } finally { glPopMatrix() }
    }
}

class SubstationSupportRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) :
    TransparentNodeElementRender(entity,descriptor) {
    override fun draw() = (transparentNodedescriptor as SubstationSupportDescriptor).drawMounted(front)
}

class SubstationSupportElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) :
    TransparentNodeElement(node,descriptor) {
    override fun initialize() = connect()
    override fun getElectricalLoad(side: Direction, lrdu: LRDU) = null
    override fun getThermalLoad(side: Direction, lrdu: LRDU) = null
    override fun getConnectionMask(side: Direction, lrdu: LRDU) = 0
    override fun multiMeterString(side: Direction) = ""
    override fun thermoMeterString(side: Direction) = ""
    override fun onBlockActivated(player: Player, side: Direction, x: Float, y: Float, z: Float) = false
}

object SubstationSupports {
    @JvmStatic fun register() = SubstationSupportKind.entries.forEach {
        Eln.transparentNodeItem.addDescriptor(it.id,SubstationSupportDescriptor(it))
    }
}
