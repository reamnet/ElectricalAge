package mods.eln.transparentnode.laundry

import mods.eln.fluid.ElementFluidHandler
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.transfer.fluid.FluidResource
import net.neoforged.neoforge.transfer.transaction.TransactionContext

class WasherWater {
    companion object { const val PER_CYCLE=1000;const val CAPACITY=10000 }
    val tank=object:ElementFluidHandler(CAPACITY) {
        override fun isValid(index:Int,resource:FluidResource)=!resource.isEmpty&&resource.fluid===Fluids.WATER
        override fun extract(index:Int,resource:FluidResource,amount:Int,transaction:TransactionContext)=0
    }
    fun reserve():Boolean {
        if(tank.fluidAmount<PER_CYCLE)return false
        val remaining=tank.fluidAmount-PER_CYCLE
        tank.set(0,FluidResource.of(Fluids.WATER),remaining)
        return true
    }
    fun save(tag:CompoundTag) {tag.putInt("washerWaterMb",tank.fluidAmount)}
    fun load(tag:CompoundTag) {tank.set(0,FluidResource.of(Fluids.WATER),tag.getIntOr("washerWaterMb",0).coerceIn(0,CAPACITY))}
}
class MachineWork {
    var joules=0.0;private set
    fun account(seconds:Double,watts:Double,required:Double) {
        if(seconds.isFinite()&&seconds>0&&watts.isFinite()&&watts>0&&required.isFinite()&&required>0)joules=(joules+seconds*watts).coerceAtMost(required)
    }
    fun load(value:Double,required:Double){joules=if(value.isFinite())value.coerceIn(0.0,required.coerceAtLeast(0.0))else 0.0}
    fun reset(){joules=0.0}
}
