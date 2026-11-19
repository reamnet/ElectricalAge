package mods.eln.sim.mna.component

import mods.eln.sim.mna.SubSystem

/** Disabled means an open branch (I=0), not the short imposed by a zero-volt source. */
class SwitchableVoltageSource(name: String) : VoltageSource(name) {
    var enabled = false
        set(value) {
            if (field != value) {
                field = value
                dirty()
            }
        }

    override fun applyTo(system: SubSystem) {
        if (enabled) super.applyTo(system)
        else system.addToA(currentState, currentState, 1.0)
    }

    override fun simProcessI(system: SubSystem) {
        if (enabled) super.simProcessI(system)
    }

    override fun getI(): Double = if (enabled) super.getI() else 0.0

    override fun getCurrent(): Double = if (enabled) super.getCurrent() else 0.0
    override fun getP(): Double = if (enabled) super.getP() else 0.0
}
