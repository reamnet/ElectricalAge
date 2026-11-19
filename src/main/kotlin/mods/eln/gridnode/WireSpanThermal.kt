package mods.eln.gridnode
import mods.eln.Eln
import mods.eln.sim.IProcess
import mods.eln.sixnode.electricalcable.*
import net.minecraft.nbt.CompoundTag

/** Each active span core owns exactly its paid mass/cooling and I²R heat. */
class WireSpanThermal(val descriptor: UtilityCableDescriptor,val meters: Double) {
    val load=WireThermalLoad("span",WireThermalPhysics(descriptor.material,descriptor.conductorAreaMm2,meters))
    var insulationDamaged=descriptor.melted;private set
    private var sample: IProcess?=null;private var refresh: IProcess?=null;private var failure: IProcess?=null
    fun connect(connection: GridSpanConnection,ambient: ()->Double,burn: ()->Unit) {
        val resistance=SpanResistanceRefresh()
        fun update() {
            val air=ambient();load.updateProperties(air,air,descriptor.insulated && !insulationDamaged)
            resistance.update(descriptor.resistanceOhms(meters,load.absoluteCelsius)) { connection.spanOhms=it }
        }
        update();load.setAsSlow()
        sample=IProcess { dt->if(dt.isFinite() && dt>0)load.moveEnergyTo(dt*connection.current*connection.current*connection.spanOhms) }
        refresh=IProcess { update() }
        failure=IProcess { if(descriptor.insulated && load.absoluteCelsius>=descriptor.meltTemperatureCelsius)insulationDamaged=true;if(load.failed)burn() }
        Eln.simulator.addThermalLoad(load);Eln.simulator.addElectricalProcess(sample);Eln.simulator.addThermalSlowProcess(refresh);Eln.simulator.addSlowProcess(failure)
    }
    fun disconnect() {
        load.flushPendingEnergy();Eln.simulator.removeElectricalProcess(sample);Eln.simulator.removeThermalSlowProcess(refresh);Eln.simulator.removeSlowProcess(failure);Eln.simulator.removeThermalLoad(load)
        sample=null;refresh=null;failure=null
    }
    fun read(t: CompoundTag) { load.readFromNBT(t,"");insulationDamaged=descriptor.melted || t.getBooleanOr("spanInsulationDamaged",false) }
    fun write(t: CompoundTag) { load.writeToNBT(t,"");t.putBoolean("spanInsulationDamaged",insulationDamaged) }
}
