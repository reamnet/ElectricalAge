package mods.eln.sixnode.lampsocket

import mods.eln.Eln
import mods.eln.ghost.GhostBlock
import mods.eln.ghost.GhostGroup
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import kotlin.math.ceil

/** Fixed fixtures reuse native bulbs, channel supplies and controls. */
class DecorativeLampDescriptor(val style: DecorativeLampStyle) : LampSocketDescriptor(
    style.itemName, DecorativeLampObjRender(style), LampSocketType.Douille, false, style.range, 0f, 0f, 0f) {
    init {
        ElementClass = DecorativeLampElement::class.java
        modelName = style.model
        setPlaceDirection(when {
            style.ceiling -> arrayOf(Direction.YP)
            style.floor || style.outdoorFloor -> arrayOf(Direction.YN)
            style.anyFace || style == DecorativeLampStyle.HOLDER -> Direction.values()
            style == DecorativeLampStyle.DOT || style.miniDot -> arrayOf(Direction.YP, Direction.XN, Direction.XP, Direction.ZN, Direction.ZP)
            else -> arrayOf(Direction.XN, Direction.XP, Direction.ZN, Direction.ZP)
        })
        val clearance = GhostGroup()
        fun add(x: Int, y: Int, z: Int) = clearance.addElement(x,y,z,Eln.ghostBlock,GhostBlock.tClearance)
        when {
            style == DecorativeLampStyle.OUTDOOR_POST -> { add(1,0,0); add(2,0,0) }
            style == DecorativeLampStyle.FLOOR_DRUM -> add(1,0,0)
            style == DecorativeLampStyle.SHOP || style == DecorativeLampStyle.FACTORY -> {
                add(0,0,-1); add(0,0,1)
                if (style == DecorativeLampStyle.FACTORY) for(z in -1..1) add(1,0,z)
            }
            style == DecorativeLampStyle.STRIP_SURFACE -> { add(0,0,-1); add(0,0,1) }
            style == DecorativeLampStyle.RIG_LINEAR -> { add(0,0,-1); add(0,0,1) }
            style == DecorativeLampStyle.PARTY_TIKI -> add(1,0,0)
            style.outdoorFloor -> if(style.depth>1) for(x in 1 until ceil(style.depth).toInt()) add(x,0,0)
            style.seasonalString || style.partyString -> { add(0,0,-1); add(0,0,1) }
            style.foyer -> for(y in -1..1) for(z in -1..1) add(1,y,z)
            style.luxury -> for(x in 1..ceil(style.depth).toInt()) for(y in -2..2) for(z in -2..2) add(x,y,z)
        }
        if(clearance.size()>0) setGhostGroup(clearance)
    }

    companion object {
        @JvmStatic fun register() {
            for(style in DecorativeLampStyle.entries) Eln.sixNodeItem.addDescriptor(style.lookup, DecorativeLampDescriptor(style))
        }
    }
}
