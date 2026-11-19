package mods.eln.transparentnode.offgrid

import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.sim.Ambient
import mods.eln.sim.PhysicalConstant
import mods.eln.sim.nbt.NbtThermalLoad

/** Native 26.3 Tc is measured above the fixed 20 C reference, not above local ambient. */
class CabinetTemperature(private val owner: TransparentNodeElement?, private val load: NbtThermalLoad) {
    private var remaining=0.0
    val celsius get()=PhysicalConstant.Tamb+load.Tc
    fun advance(seconds: Double) {
        if(!seconds.isFinite() || seconds<=0)return
        remaining-=seconds
        if(remaining<=0) {
            val at=owner?.node?.coordonate
            if(at?.blockExist==true)load.ambient=Ambient.relative(at)
            remaining=.5
        }
    }
}
