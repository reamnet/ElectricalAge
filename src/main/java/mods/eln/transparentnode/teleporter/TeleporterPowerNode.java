package mods.eln.transparentnode.teleporter;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.GhostNode;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class TeleporterPowerNode extends GhostNode {

    @Override
    public void initializeFromThat(Direction front,
                                   LivingEntity entityLiving, ItemStack itemStack) {
        connect();

    }

    @Override
    public int getSideConnectionMask(Direction directionA, LRDU lrduA) {
        if (e == null || !mods.eln.transparentnode.cord.CordSupport.active(e) || e.isBurnt()) return 0;
        if (directionA == Direction.YP || directionA == Direction.YN) return 0;
        if (lrduA != LRDU.Down) return 0;
        return mods.eln.transparentnode.cord.CordSupport.mask(e, e.cordInput(), maskElectricalPower);
    }

    @Override
    public ThermalLoad getThermalLoad(Direction directionA, LRDU lrduA) {

        return null;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction directionB, LRDU lrduB) {
        if (e == null || !mods.eln.transparentnode.cord.CordSupport.active(e) || e.isBurnt()) return null;
        return mods.eln.transparentnode.cord.CordSupport.load(e, e.cordInput());
    }

    @Override
    public void initializeFromNBT() {


    }

    void setElement(TeleporterElement e) {
        this.e = e;
        //reconnect();
    }

    TeleporterElement e;
}
