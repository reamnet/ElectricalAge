package mods.eln.sixnode.lampsocket

import mods.eln.misc.Direction
import net.minecraft.world.phys.AABB

/** Supporting collision surface under an appliance's central footprint. */
object LampMounting {
    fun surface(boxes:List<AABB>,side:Direction):Double? {
        val central=boxes.filter{it.minX<=.5&&it.maxX>=.5&&it.minZ<=.5&&it.maxZ>=.5}
        return when(side){Direction.YP->central.minOfOrNull{it.minY};Direction.YN->central.maxOfOrNull{it.maxY};else->null}
    }
}
