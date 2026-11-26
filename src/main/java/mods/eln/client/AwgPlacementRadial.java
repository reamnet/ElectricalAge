package mods.eln.client;

import mods.eln.client.gui.RadialMenu;
import mods.eln.network.ElnNet;
import mods.eln.node.six.SixNodeItem;
import mods.eln.node.voxel.AwgPlacementMode;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** The ordinary tool radial, selecting only an AWG spool's existing placement backend. */
final class AwgPlacementRadial {
    private AwgPlacementRadial() {}

    static RadialMenu create(ItemStack spool, KeyMapping key, boolean held) {
        boolean initial = AwgPlacementMode.legacy(spool);
        boolean[] legacy = {initial};
        int wireId = ((SixNodeItem) spool.getItem()).descriptor.parentItemDamage;
        var choices = new ArrayList<RadialMenu.Segment>();
        for (boolean value : new boolean[]{false, true})
            choices.add(new RadialMenu.Segment(Component.literal(AwgPlacementMode.label(value)),
                () -> legacy[0] == value, () -> legacy[0] = value));
        return new RadialMenu(Component.literal("Cable placement mode"), List.of(choices),
            () -> Component.literal(AwgPlacementMode.label(legacy[0])), key, held, () -> {
                if (legacy[0] != initial) ElnNet.sendToServer(AwgPlacementMode.packet(wireId, legacy[0]));
            });
    }
}
