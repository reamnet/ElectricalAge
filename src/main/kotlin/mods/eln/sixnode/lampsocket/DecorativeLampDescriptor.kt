package mods.eln.sixnode.lampsocket

import mods.eln.Eln
import mods.eln.ghost.GhostBlock
import mods.eln.ghost.GhostGroup
import mods.eln.misc.Direction
import mods.eln.misc.LRDU

/** Fixed fixtures reuse native bulbs, channel supplies and controls. */
class DecorativeLampDescriptor(val style: DecorativeLampStyle) : LampSocketDescriptor(
    style.itemName, DecorativeLampObjRender(style), LampSocketType.Douille, false, style.range, 0f, 0f, 0f) {
    init {
        ElementClass = DecorativeLampElement::class.java
        modelName = style.model
        setPlaceDirection(when {
            style.ceiling -> arrayOf(Direction.YP)
            style.floor -> arrayOf(Direction.YN)
            style == DecorativeLampStyle.HOLDER -> Direction.values()
            else -> arrayOf(Direction.XN, Direction.XP, Direction.ZN, Direction.ZP)
        })
        val clearance = GhostGroup()
        fun add(x: Int, y: Int, z: Int) = clearance.addElement(x,y,z,Eln.ghostBlock,GhostBlock.tClearance)
        when {
            style == DecorativeLampStyle.OUTDOOR_POST -> { add(1,0,0); add(2,0,0) }
            style == DecorativeLampStyle.SHOP || style == DecorativeLampStyle.FACTORY -> {
                add(0,0,-1); add(0,0,1)
                if (style == DecorativeLampStyle.FACTORY) for(z in -1..1) add(1,0,z)
            }
            style.foyer -> for(y in -1..1) for(z in -1..1) add(1,y,z)
        }
        if(clearance.size()>0) setGhostGroup(clearance)
    }

    companion object {
        @JvmStatic fun register() {
            for(style in DecorativeLampStyle.entries) Eln.sixNodeItem.addDescriptor(style.lookup, DecorativeLampDescriptor(style))
        }
    }
}
