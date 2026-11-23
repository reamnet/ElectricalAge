package mods.eln.transparentnode.electricalmachine;

import mods.eln.Eln;
import mods.eln.gl.GL11;
import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.UtilsClient;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sim.ThermalLoadInitializer;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.Objects;

import static mods.eln.i18n.I18N.TR_NAME;
import static mods.eln.i18n.I18N.Type.NONE;

public final class OldArcFurnaceDescriptor extends ElectricalMachineDescriptor {
    public static final int ID = 2176;
    private final Obj3DPart main;
    private final Obj3DPart electrode;

    public OldArcFurnaceDescriptor() {
        super(TR_NAME(NONE, "Old 480V Arc Furnace"), 480.0, 10_000.0, 600.0,
            new ThermalLoadInitializer(80, -100, 10, 100_000),
            ElnDeviceRegistry.highVoltageCableDescriptor, Eln.arcFurnaceRecipes);
        var model = Objects.requireNonNull(Eln.obj.getObj("arcfurnaceold"));
        main = Objects.requireNonNull(model.getPart("main"));
        electrode = Objects.requireNonNull(model.getPart("move"));
        setDefaultIcon("old800varcfurnace");
        setRunningSound("eln:arcfurnace_loop");
    }

    private static final class Animation {
        final RcInterpolator progress = new RcInterpolator(0.25f);
        float itemAngle;
    }

    @Override
    Object newDrawHandle() {
        return new Animation();
    }

    @Override
    void draw(ElectricalMachineRender render, Object handle, ItemEntity input, ItemEntity output,
              float powerFactor, float processState) {
        Animation animation = (Animation) handle;
        UtilsClient.drawEntityItem(input, -0.35, 0.04, 0.3, animation.itemAngle, 1);
        UtilsClient.drawEntityItem(output, 0.35, 0.04, 0.3, -animation.itemAngle + 139, 1);
        main.draw();
        float off = electrode.getFloat("tyoff");
        float on = electrode.getFloat("tyon");
        GL11.glPushMatrix();
        GL11.glTranslatef(0, off + (on - off) * (float) Math.sqrt(animation.progress.get()), 0);
        electrode.draw();
        GL11.glPopMatrix();
    }

    @Override
    void refresh(float deltaT, ElectricalMachineRender render, Object handle, ItemEntity input, ItemEntity output,
                 float powerFactor, float processState) {
        Animation animation = (Animation) handle;
        animation.progress.setTarget(processState);
        animation.progress.step(deltaT);
        animation.itemAngle = (animation.itemAngle + deltaT * 90) % 360;
    }

    @Override
    public float volumeForRunningSound(float processState, float powerFactor) {
        return processState;
    }

    @Override
    public boolean powerLrdu(Direction side, Direction front) {
        return side != front && side != front.getInverse();
    }
}
