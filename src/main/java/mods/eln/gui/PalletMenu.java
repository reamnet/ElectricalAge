package mods.eln.gui;

import mods.eln.transparentnode.industrial.TransportItem;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Native chest layout with server slot guards: transport containers cannot recursively store each other. */
public final class PalletMenu extends ChestMenu {
    public PalletMenu(int id, Inventory player, Container items) {
        super(MenuType.GENERIC_9x3,id,player,items,3);
        for(int n=0;n<27;n++) {
            Slot prior=slots.get(n);
            Slot guarded=new Slot(items,n,prior.x,prior.y) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return !(GenericItemUsingDamageDescriptor.getDescriptor(stack) instanceof TransportItem);
                }
            };
            guarded.index=n;
            slots.set(n,guarded);
        }
    }
}
