package mods.eln.transparentnode.ups
import mods.eln.sim.IProcess
import mods.eln.sim.mna.state.State
import mods.eln.sim.mna.component.Component
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.transparentnode.standby.StandbyFuelStore
import net.minecraft.nbt.CompoundTag
import kotlin.math.min

internal class MeteredUpsBank: UpsEnergyBank {
    var count=0
    var cellCapacity=120000.0;var charge=1500.0;var discharge=300.0
    override var energy=0.0
    override val activeCount get()=count/24*24
    override val capacity get()=activeCount*cellCapacity
    fun fill(count: Int,fraction: Double,small: Boolean=false) {
        this.count=count;cellCapacity=if(small)40000.0 else 120000.0
        charge=if(small)500.0 else 1500.0;discharge=if(small)100.0 else 300.0
        energy=capacity*fraction
    }
    override fun availableChargeWatts(seconds: Double)=min(activeCount*charge,(capacity-energy)/seconds)
    override fun availableDischargeWatts(seconds: Double)=min(activeCount*discharge,energy/seconds)
    override fun transfer(joules: Double,seconds: Double): Double {
        require(joules.isFinite() && seconds>0)
        val accepted=joules.coerceIn(-availableDischargeWatts(seconds)*seconds,availableChargeWatts(seconds)*seconds)
        energy=(energy+accepted).coerceIn(0.0,capacity);return accepted
    }
}
internal class UpsCircuitFixture {
    val electricalLoadList=mutableListOf<State>();val electricalComponentList=mutableListOf<Component>();val electricalProcessList=mutableListOf<IProcess>();val thermalLoadList=mutableListOf<NbtThermalLoad>()
    val bank=MeteredUpsBank()
    val unit=UpsPowerUnit(electricalLoadList,electricalComponentList,electricalProcessList,thermalLoadList,bank)
}
internal class UpsGeneratorFuel: StandbyFuelStore {
    override val capacity=20000
    override var availableJoules=20000*645.5
    override val availableMb get()=availableJoules/645.5
    override fun consume(joules: Double): Boolean { if(!joules.isFinite() || joules<0 || joules>availableJoules)return false;availableJoules-=joules;return true }
    override fun read(nbt: CompoundTag) { availableJoules=nbt.getDoubleOr("testFuelJ",0.0) }
    override fun write(nbt: CompoundTag) { nbt.putDouble("testFuelJ",availableJoules) }
}
