package mods.eln.transparentnode.fabricator

import mods.eln.Eln
import net.minecraft.world.item.ItemStack

/** Read-only prototypes keep readiness checks free of item/NBT copies. */
object FabricationRecipes {
    private val wafer by lazy { required("Silicon Wafer").item }
    private val copper by lazy { required("Copper Plate").item }
    private val outputs by lazy { FabricatorOperation.entries.map { required(it.outputName) } }

    private fun required(name: String) = Eln.findItemStack(name, 1).also {
        require(!it.isEmpty) { "Fabricator item must be registered: $name" }
    }

    fun wafer(stack: ItemStack) = !stack.isEmpty && stack.item === wafer
    fun copper(stack: ItemStack) = !stack.isEmpty && stack.item === copper
    fun accepts(slot: Int, stack: ItemStack) = when (slot) {
        1 -> wafer(stack)
        2 -> copper(stack)
        else -> false
    }
    fun canOutput(operation: FabricatorOperation, stack: ItemStack): Boolean {
        val prototype = outputs[operation.ordinal]
        return if (stack.isEmpty) operation.outputCount <= prototype.maxStackSize
        else ItemStack.isSameItemSameComponents(stack, prototype) &&
            stack.count + operation.outputCount <= stack.maxStackSize
    }
    data class Example(val operation: FabricatorOperation, val inputs: List<ItemStack>, val output: ItemStack)
    @JvmStatic fun examples() = FabricatorOperation.entries.map {
        Example(it, listOf(required("Silicon Wafer"), required("Copper Plate")), output(it))
    }
    @JvmStatic fun output(operation: FabricatorOperation) = outputs[operation.ordinal].copyWithCount(operation.outputCount)
}
