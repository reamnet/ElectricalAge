package mods.eln.transparentnode.wiremachine

import mods.eln.gui.*
import mods.eln.i18n.I18N.tr

object WireMachineMenus {
    @JvmStatic fun spec(d:WireMachineDescriptor):MenuSpec {
        val slots=(0 until d.kind.slotCount).map { slot->
            val x=when { slot==d.kind.outputSlot->244;slot>=6->16+(slot-6)*24;else->16+slot*24 }
            val y=if(d.kind==WireMachineKind.COMBINER && slot<5)134 else 158
            if(slot==d.kind.outputSlot)SlotSpec.output(slot,x,y,SlotSpec.BIG)
            else SlotSpec.filter(slot,x,y,if(d.kind==WireMachineKind.ROLLER && slot==0 || d.kind==WireMachineKind.INSULATOR && slot==1)64 else 1,{ WireProduction.accepts(d.kind,slot,it) },SlotSpec.MEDIUM,tr("Machine input"))
        }.toTypedArray()
        return MenuSpec.of(*slots).panel(368,294,8,210)
    }
}
