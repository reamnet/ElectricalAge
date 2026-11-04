package mods.eln.transparentnode.standby

import mods.eln.fluid.ElementFluidHandler
import mods.eln.fluid.FuelRegistry
import mods.eln.misc.Utils
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.level.material.Fluid
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.transfer.fluid.FluidResource
import kotlin.math.ceil

object StandbyFuel {
    val tags=listOf("eln:standby_gasoline","c:gasoline","c:fuels/gasoline","forge:gasoline","forge:fuels/gasoline")
        .map { TagKey.create(Registries.FLUID,Identifier.parse(it)) }
    fun namedGasoline(id: String)=id.substringAfter(':').substringAfterLast('/') in setOf("gasoline","flowing_gasoline","petrol","flowing_petrol")
    fun accepts(fluid: Fluid): Boolean = namedGasoline(BuiltInRegistries.FLUID.getKey(fluid).toString()) ||
        tags.any { fluid.builtInRegistryHolder().`is`(it) } || FuelRegistry.fluidName(fluid) in FuelRegistry.gasolineList && fluid.isSource(fluid.defaultFluidState())
    fun joulesPerMb(fluid: Fluid)=FuelRegistry.heatEnergyPerMilliBucket(fluid).takeIf { it.isFinite() && it>0 } ?: 2173.5
}

/** Electrical accounting depends on fuel energy, not a particular platform's tank API. */
interface StandbyFuelStore {
    val capacity: Int
    val availableMb: Double
    val availableJoules: Double
    fun consume(joules: Double): Boolean
    fun read(nbt: CompoundTag)
    fun write(nbt: CompoundTag)
}

/** Native transactional pipe/container capability. A paid whole-mB reserve cannot be extracted twice. */
class StandbyTank(override val capacity: Int=CAPACITY,private val changed: ()->Unit) : StandbyFuelStore {
    companion object { const val CAPACITY=20000;const val EFFICIENCY=.25 }
    var reserveJoules=0.0;private set
    private var reserveCost=2173.5*EFFICIENCY
    val tank=object: ElementFluidHandler(capacity) {
        override fun isValid(index: Int,resource: FluidResource): Boolean = !resource.isEmpty && StandbyFuel.accepts(resource.fluid) &&
            (fluid.isEmpty || resource.matches(fluid))
        override fun onContentsChanged(index: Int,previous: FluidStack) { super.onContentsChanged(index,previous);changed() }
    }
    override val availableMb get()=tank.fluidAmount+reserveJoules/reserveCost
    override val availableJoules get()=reserveJoules+if(tank.fluid.isEmpty)0.0 else tank.fluidAmount*StandbyFuel.joulesPerMb(tank.fluid.fluid)*EFFICIENCY
    override fun consume(joules: Double): Boolean {
        if(!joules.isFinite() || joules<0 || joules>availableJoules)return false
        if(joules==0.0)return true
        if(joules>reserveJoules) {
            reserveCost=StandbyFuel.joulesPerMb(tank.fluid.fluid)*EFFICIENCY
            val whole=ceil((joules-reserveJoules)/reserveCost).toInt().coerceAtMost(tank.fluidAmount)
            reserveJoules+=tank.drain(whole,true).amount*reserveCost
        }
        reserveJoules=(reserveJoules-joules).coerceAtLeast(0.0)
        return true
    }
    private fun ops()=(Utils.registries() ?: RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)).createSerializationContext(NbtOps.INSTANCE)
    override fun read(nbt: CompoundTag) {
        val fluid=FluidStack.OPTIONAL_CODEC.parse(ops(),nbt.getCompoundOrEmpty("standbyTank")).result().orElse(FluidStack.EMPTY)
        val accepted=if(!fluid.isEmpty && StandbyFuel.accepts(fluid.fluid))fluid.copyWithAmount(fluid.amount.coerceIn(0,capacity)) else FluidStack.EMPTY
        tank.set(0,FluidResource.of(accepted),accepted.amount)
        reserveCost=nbt.getDoubleOr("standbyReserveCost",0.0).takeIf { it.isFinite() && it>0 } ?: 2173.5*EFFICIENCY
        reserveJoules=nbt.getDoubleOr("standbyReserveJoules",0.0).takeIf { it.isFinite() && it>=0 && it<=reserveCost } ?: 0.0
    }
    override fun write(nbt: CompoundTag) {
        nbt.put("standbyTank",FluidStack.OPTIONAL_CODEC.encodeStart(ops(),tank.fluid).getOrThrow())
        nbt.putDouble("standbyReserveCost",reserveCost);nbt.putDouble("standbyReserveJoules",reserveJoules)
    }
}
