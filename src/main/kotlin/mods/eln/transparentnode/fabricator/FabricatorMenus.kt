package mods.eln.transparentnode.fabricator

import mods.eln.gui.*
import mods.eln.i18n.I18N.tr

object FabricatorMenus {
    @JvmStatic fun spec(descriptor: FabricatorDescriptor): MenuSpec = MenuSpec.of(
        SlotSpec.output(0, 244, 110, SlotSpec.BIG),
        SlotSpec.filter(1, 16, 110, 64, { FabricationRecipes.wafer(it) }, SlotSpec.MEDIUM, tr("Silicon wafer")),
        SlotSpec.filter(2, 64, 110, 64, { FabricationRecipes.copper(it) }, SlotSpec.MEDIUM, tr("Copper plate"))
    ).panel(368, 260, 8, 176)
}
