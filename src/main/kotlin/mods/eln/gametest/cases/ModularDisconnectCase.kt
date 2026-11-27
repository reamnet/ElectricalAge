package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.Reload
import mods.eln.gametest.TestCase
import mods.eln.gametest.TestContext
import mods.eln.gridnode.GridLink
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.utility.modular.*
import kotlin.math.abs

class ModularDisconnectCase : TestCase {
    override fun id() = "modular_gang_disconnect_under_load"
    override fun width() = 32
    override fun depth() = 16
    override fun height() = 28
    override fun floorDepth() = 16
    override fun ticks() = 65
    private lateinit var device: ModularDisconnectElement
    private val components=ArrayList<Component>()
    private lateinit var outputs: List<Resistor>
    private var wear=0.0

    override fun build(c: TestContext) {
        device=c.placeTransparent(ModularDisconnectKind.GANG.id,c.at(10,1,8)) as ModularDisconnectElement
        val support=c.placeTransparent(SubstationSupportKind.SUPPORT.id,c.at(22,1,8)) as SubstationSupportElement
        val bar=c.placeTransparent(Busbars.FIRST_ID,c.at(22,2,8)) as BusbarElement
        c.check("a ceramic post provides physical support without an electrical contact",bar.supported &&
            bar.node.nodeConnectionList.isEmpty() && support.node.nodeConnectionList.isEmpty(),"")
        val p=c.player()
        for(terminal in 32..34) {
            val nearest=device.gridTerminalAt(Direction.XN,device.renderedGridCablePoint(terminal,0))
            c.check("terminal geometry selects its actual phase and bundle",nearest==terminal &&
                device.gridBundleFor(terminal)==52,"$terminal")
        }
        val voltage=listOf(800.0,1000.0,1200.0)
        for(i in 0..2)components.add(VoltageSource("yard phase $i",device.inputs[i],null).apply { u=voltage[i] })
        outputs=device.outputs.map { Resistor(it,null).apply { r=10.0 } }
        components.addAll(outputs)
        components.forEach(Eln.simulator::addElectricalComponent)
        c.after(12) {
            c.check("open gang leaves all separated branches deenergized",outputs.all { abs(it.i)<1e-7 },"")
            check(device.onBlockActivated(p,Direction.XP,.5f,.5f,.5f))
        }
        c.after(30) {
            outputs.forEachIndexed { i, branch ->
                c.checkValue("independent switched phase ${'A'+i}",voltage[i]/10.002,branch.i)
            }
            check(device.onBlockActivated(p,Direction.XP,.5f,.5f,.5f))
            wear=device.wear
            c.check("opening under load wears the contacts and opens all phases",wear>.1 && !device.closed,"$wear")
        }
    }

    override fun measure(c: TestContext) {
        try {
            c.check("gang remains isolated after loaded opening",outputs.all { abs(it.i)<1e-7 },"")
        } finally {
            for (part in components) { Eln.simulator.removeElectricalComponent(part);part.breakConnection() }
        }
        val pos=device.coordonate().pos()
        Reload.node(c.level(),pos)
        val fresh=GridLink.getElementFromCoordinate(Coordonate(pos,c.level())) as ModularDisconnectElement
        c.check("loaded-opening wear and open state survive a node reload",!fresh.closed && abs(fresh.wear-wear)<1e-9,"")
    }
}
