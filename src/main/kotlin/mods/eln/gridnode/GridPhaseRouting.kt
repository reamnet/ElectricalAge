package mods.eln.gridnode

import mods.eln.sim.ElectricalLoad
import java.util.Collections
import java.util.IdentityHashMap

/** Resolve single-conductor phase inheritance at link/configuration time, never once per simulation step. */
object GridPhaseRouting {
    fun phases(start: GridElement,terminal: Int,ignore: GridElement?=null): Set<Int> {
        val result=HashSet<Int>();val visited=Collections.newSetFromMap(IdentityHashMap<ElectricalLoad,Boolean>())
        val queue=ArrayDeque<Pair<GridElement,Int>>();queue.add(start to terminal)
        while(queue.isNotEmpty()) {
            val (element,port)=queue.removeFirst()
            val explicit=element.gridPhase(port)
            if(explicit!=null) { result.add(explicit);continue }
            val bus=element.phaseBusTerminals(port).flatMap(element::gridLoads).toSet()
            var fresh=false;for(load in bus)if(visited.add(load))fresh=true
            if(!fresh)continue
            for(link in element.gridLinkList) {
                val local=element.gridLoads(link.getTerminal(element));val index=local.indexOfFirst { it in bus }
                if(index<0)continue
                val other=link.getOtherElement(element) ?: continue
                if(other===ignore)continue
                val remote=link.getTerminal(other);val single=when(remote) { 52->32+index;53->36+index;else->remote }
                if(other.gridLoads(single).size==1)queue.add(other to single)
            }
        }
        return result
    }
    @JvmStatic fun compatible(a: GridElement,from: Int,b: GridElement,to: Int)=(phases(a,from)+phases(b,to)).size<=1
}
