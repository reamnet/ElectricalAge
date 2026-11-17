package mods.eln.transparentnode.laundry

import mods.eln.gui.*
import net.minecraft.world.item.BlockItem

object MatterMachineMenus {
    @JvmStatic fun spec(d:MatterMachineDescriptor)=MenuSpec.of(
        SlotSpec.filter(0,35,101,1,{when(d.kind){MatterMachineKind.WASHER->WetEquipment.washable(it);MatterMachineKind.DRYER->!WetEquipment.unpack(it).isEmpty;MatterMachineKind.FUSION->it.item is BlockItem}},SlotSpec.MEDIUM),
        SlotSpec.output(1,139,101,SlotSpec.MEDIUM)
    ).panel(208,240,16,158)
}
