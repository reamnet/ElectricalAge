package mods.eln.sixnode.lampsocket

import mods.eln.Eln
import mods.eln.ghost.GhostBlock
import mods.eln.ghost.GhostGroup
import mods.eln.misc.Direction
import kotlin.math.ceil

/** Source placement and clearance contracts; native bulbs, saves and solver ownership remain authoritative. */
class DecorativeLampDescriptor(val style: DecorativeLampStyle) : LampSocketDescriptor(
    style.itemName, DecorativeLampObjRender(style), LampSocketType.Douille, false, style.range, 0f, 0f, 0f) {
    init {
        ElementClass = DecorativeLampElement::class.java
        modelName = style.model
        val wall = arrayOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP)
        val ceilingAndWall = arrayOf(Direction.YP,*wall)
        val floor = arrayOf(Direction.YN)
        val ceiling = arrayOf(Direction.YP)
        setPlaceDirection(when(style) {
            DecorativeLampStyle.PARKING, DecorativeLampStyle.OUTDOOR_POST,
            DecorativeLampStyle.DESK, DecorativeLampStyle.TABLE_DRUM, DecorativeLampStyle.TABLE_BANKER,
            DecorativeLampStyle.TABLE_TASK, DecorativeLampStyle.OUTDOOR_BOLLARD -> floor
            DecorativeLampStyle.SHOP, DecorativeLampStyle.FACTORY,
            DecorativeLampStyle.CANDLE, DecorativeLampStyle.CRYSTAL, DecorativeLampStyle.WAGON,
            DecorativeLampStyle.LANTERN, DecorativeLampStyle.SPUTNIK,
            DecorativeLampStyle.POT_BAFFLE, DecorativeLampStyle.POT_TRIMLESS, DecorativeLampStyle.POT_GIMBAL,
            DecorativeLampStyle.RECESSED, DecorativeLampStyle.CHANDELIER -> ceiling
            DecorativeLampStyle.SCONCE_CYLINDER, DecorativeLampStyle.SCONCE_FABRIC, DecorativeLampStyle.SCONCE_GLOBE,
            DecorativeLampStyle.SCONCE_CAGE, DecorativeLampStyle.SCONCE_CANDLE, DecorativeLampStyle.OUTDOOR_BARN,
            DecorativeLampStyle.WAREHOUSE, DecorativeLampStyle.PORCH, DecorativeLampStyle.MOTION -> wall
            DecorativeLampStyle.DOT -> ceilingAndWall
            DecorativeLampStyle.HOLDER -> Direction.values()
            else -> when {
                style.miniDot || style == DecorativeLampStyle.EAVE_TWIN -> ceilingAndWall
                style.floor -> floor
                style.pool -> Direction.values()
                style.wallLamp -> wall
                style.ceiling -> ceiling
                else -> Direction.values()
            }
        })
        val clearance = GhostGroup()
        fun add(x: Int,y: Int,z: Int) = clearance.addElement(x,y,z,Eln.ghostBlock,GhostBlock.tClearance)
        when {
            style == DecorativeLampStyle.PARKING -> { for(x in 1..4) add(x,0,0); for(y in -1..1) for(z in -1..1) add(5,y,z) }
            style == DecorativeLampStyle.OUTDOOR_POST -> { add(1,0,0);add(2,0,0) }
            style == DecorativeLampStyle.SHOP || style == DecorativeLampStyle.FACTORY -> {
                add(0,0,-1);add(0,0,1)
                if(style == DecorativeLampStyle.FACTORY) for(z in -1..1) add(1,0,z)
            }
            style.foyer -> for(y in -1..1) for(z in -1..1) add(1,y,z)
            style.parking -> { for(x in 1..4) add(x,0,0);for(x in 5..6) for(y in -2..2) for(z in -2..2) add(x,y,z) }
            style == DecorativeLampStyle.PARTY_TIKI -> add(1,0,0)
            style.outdoorFloor -> if(style.depth>1) for(x in 1 until ceil(style.depth).toInt()) add(x,0,0)
            style == DecorativeLampStyle.FLOOR_DRUM -> add(1,0,0)
            style.street -> {
                val firstHead = if(style == DecorativeLampStyle.STREET_BOULEVARD) 4 else 5
                for(x in 1 until firstHead) add(x,0,0)
                for(x in firstHead..5) for(y in -1..1) for(z in -1..1) add(x,y,z)
            }
            style.luxury -> for(x in 1..ceil(style.depth).toInt()) for(y in -2..2) for(z in -2..2) add(x,y,z)
        }
        if(style == DecorativeLampStyle.STRIP_SURFACE || style == DecorativeLampStyle.RIG_LINEAR || style.seasonalString || style.partyString) {
            add(0,0,-1);add(0,0,1)
        }
        if(style.fan) {
            val radius = if(style == DecorativeLampStyle.FAN_INDUSTRIAL) 2 else 1
            for(y in -radius..radius) for(z in -radius..radius) if(y!=0 || z!=0) add(0,y,z)
        }
        if(clearance.size()>0) setGhostGroup(clearance)
    }
    val panelHeight get() = if(style.hangingWarehouse) 198 else if(style.aimHeadCount>0) 242 else 166
    val inventoryY get() = panelHeight - 82
    val layoutName get() = if(style.hangingWarehouse) "decorative_lamp_suspension" else if(style.aimHeadCount>0) "decorative_lamp_aim" else "decorative_lamp"
    companion object {
        @JvmStatic fun register() {
            for(style in DecorativeLampStyle.entries) Eln.sixNodeItem.addDescriptor(style.lookup,DecorativeLampDescriptor(style))
        }
    }
}
