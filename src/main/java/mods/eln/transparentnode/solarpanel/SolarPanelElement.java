package mods.eln.transparentnode.solarpanel;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.SolarTrackerDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.GhostPowerNode;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.AutoAcceptInventoryProxy;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.CurrentSource;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SolarPanelElement extends TransparentNodeElement {
    /** 1.12 SolarPanelContainer.trackerSlotId */
    public static final int trackerSlotId = 0;


    SolarPanelDescriptor descriptor;
    NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
    NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");
    CurrentSource panelSrc = new CurrentSource("panelSrc", positiveLoad, negativeLoad);
    Resistor panelShunt = new Resistor(positiveLoad, negativeLoad);
    SolarPanelPowerProcess powerSource;

    SolarPannelSlowProcess slowProcess = new SolarPannelSlowProcess(this);

    public double panelAlpha = Math.PI / 2;
    public SolarRoofMount roofMount = new SolarRoofMount(null);
    public boolean roofMountKnown;
    private GhostPowerNode groundNode = null;

    public SolarPanelElement(TransparentNode transparentNode,
                             TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);
        this.descriptor = (SolarPanelDescriptor) descriptor;

        grounded = false;

        powerSource = new SolarPanelPowerProcess(positiveLoad, negativeLoad, panelSrc, panelShunt,
            this.descriptor.openCircuitVoltage, this.descriptor.optimumVoltage,
            this.descriptor.optimumCurrent, this.descriptor.shortCircuitCurrent);


        electricalLoadList.add(positiveLoad);
        electricalLoadList.add(negativeLoad);


        electricalComponentList.add(panelSrc);
        electricalComponentList.add(panelShunt);
        electricalComponentList.add(new Resistor(negativeLoad, null).pullDown());

        slowProcessList.add(slowProcess);
    }

    @Override
    public void connectJob() {
        Eln.simulator.mna.addProcess(powerSource);
        super.connectJob();
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        Eln.simulator.mna.removeProcess(powerSource);
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return null;
        if (groundNode == null) {
            // Single-tile solar panel.
            if (side == front.left()) return positiveLoad;
            if (side == front.right() && !grounded) return negativeLoad;
        } else {
            return positiveLoad;
        }
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        if (groundNode == null) {
            // Single-tile solar panel.
            if (side == front.left()) return NodeBase.maskElectricalPower;
            if (side == front.right() && !grounded) return NodeBase.maskElectricalPower;
        } else {
            if (side == front) return NodeBase.maskElectricalPower;
        }
        return 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        double volts = positiveLoad.getU() - negativeLoad.getU();
        return Utils.plotUIP(volts, powerSource.getGeneratedCurrent(Math.max(0.0, volts)));
    }

    @Override
    public String thermoMeterString(Direction side) {
        return "";
    }

    @Override
    public void initialize() {
        if (!roofMountKnown) {
            roofMount = SolarRoofMount.fromSupport(node.coordonate.world(), node.coordonate.pos());
            roofMountKnown = true;
        }
        descriptor.applyTo(positiveLoad);
        descriptor.applyTo(negativeLoad);

        if (descriptor.groundCoordinate != null) {
            GhostPowerNode n = new GhostPowerNode(node.coordonate, front, roofMount.groundOffset(descriptor.groundCoordinate), negativeLoad);
            n.initialize();
            groundNode = n;
        }

        connect();
    }

    @Override
    public void onBreakElement() {
        super.onBreakElement();
        if (groundNode != null)
            groundNode.onBreakBlock();
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        return descriptor.canRotate && inventory.take(entityPlayer.getMainHandItem(), this, true, false);
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        powerSource.writeToNBT(nbt, "powerSource");
        nbt.putDouble("panelAlpha", panelAlpha);
        if (roofMountKnown) nbt.putInt("roofUphill", roofMount.getUphill() == null ? -1 : roofMount.getUphill().get3DDataValue());
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        powerSource.readFromNBT(nbt, "powerSource");
        double savedAngle = nbt.getDoubleOr("panelAlpha", Math.PI / 2);
        panelAlpha = descriptor.alphaTrunk(Double.isFinite(savedAngle) ? savedAngle : Math.PI / 2);
        // Global node restore must not sample supports or request unloaded chunks. Old saves are flat.
        roofMount = SolarRoofMount.fromId(nbt.getIntOr("roofUphill", -1));
        roofMountKnown = true;
    }


    public void networkSerialize(java.io.DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeBoolean(!Utils.isEmpty(getInventory().getItem(trackerSlotId)));
            stream.writeFloat((float) panelAlpha);
            node.lrduCubeMask.getTranslate(Direction.YN).serialize(stream);
            stream.writeByte(roofMount.getUphill() == null ? -1 : roofMount.getUphill().get3DDataValue());
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public static final byte unserializePannelAlpha = 0;

    public byte networkUnserialize(DataInputStream stream) {

        byte packetType = super.networkUnserialize(stream);
        try {
            switch (packetType) {
                case unserializePannelAlpha:
                    float requested = stream.readFloat();
                    if (Float.isFinite(requested)) {
                        panelAlpha = descriptor.alphaTrunk(requested);
                        needPublish();
                    }
                    break;

                default:
                    return packetType;
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
        return unserializeNulldId;
    }

    private final AutoAcceptInventoryProxy inventory =
        (new AutoAcceptInventoryProxy(new TransparentNodeElementInventory(1, 64, this)))
            .acceptIfEmpty(0, SolarTrackerDescriptor.class);

    @Override
    public Container getInventory() {
        return inventory.getInventory();
    }

    @Override
    public boolean hasGui() {
        return descriptor.canRotate;
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Sun angle"), Utils.plotValue(((slowProcess.getSolarAlpha()) * (180 / Math.PI)) - 90, "\u00B0"));
        info.put(I18N.tr("Panel angle"), Utils.plotValue((panelAlpha * (180 / Math.PI)) - 90, "\u00B0"));
        info.put(I18N.tr("Producing energy"), (slowProcess.getSolarLight() != 0 ? "Yes" : "No"));
        if (Eln.wailaEasyMode) {
            info.put(I18N.tr("Produced power"), Utils.plotPower("", powerSource.getPower()));
        }
        return info;
    }

}
