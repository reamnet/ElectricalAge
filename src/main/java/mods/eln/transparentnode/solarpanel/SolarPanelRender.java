package mods.eln.transparentnode.solarpanel;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.cable.CableRenderType;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUMask;
import mods.eln.misc.RcInterpolator;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.world.entity.player.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SolarPanelRender extends TransparentNodeElementRender {

    public SolarPanelDescriptor descriptor;
    private CableRenderType renderPreProcess;
    public SolarRoofMount roofMount = new SolarRoofMount(null);
    public boolean roofMountKnown;

    public SolarPanelRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (SolarPanelDescriptor) descriptor;

    }

    RcInterpolator interpol = new RcInterpolator(1f);
    boolean boot = true;

    @Override
    public void draw() {
        renderPreProcess = drawCable(Direction.YN, descriptor.cableRender, eConn, renderPreProcess);
        mods.eln.gl.GL11.glPushMatrix();
        try {
            var up = roofMount.getUphill();
            if (up != null) {
                mods.eln.gl.GL11.glTranslated(0, -.5, 0);
                mods.eln.gl.GL11.glRotatef(45, -up.getStepZ(), 0, up.getStepX());
                mods.eln.gl.GL11.glTranslated(0, .5, 0);
            }
            descriptor.draw((float) (interpol.get() * 180 / Math.PI - 90), front);
        } finally { mods.eln.gl.GL11.glPopMatrix(); }
    }

    @Override
    public void refresh(float deltaT) {
        float alpha;
        if (hasTracker == false) {
            alpha = (float) descriptor.alphaTrunk(pannelAlphaSyncValue);
        } else {
            alpha = (float) descriptor.alphaTrunk(roofMount.targetAlpha(SolarPannelSlowProcess.getSolarAlpha(tileEntity.getLevel())));
        }
        interpol.setTarget(alpha);
        if (boot) {
            boot = false;
            interpol.setValueFromTarget();
        }

        interpol.step(deltaT);
    }

    @Override
    public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu) {
        return descriptor.cableRender;
    }

    public boolean pannelAlphaSyncNew = false;
    public float pannelAlphaSyncValue = -1234;

    public boolean hasTracker;

    LRDUMask eConn = new LRDUMask();

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);

        try {

            hasTracker = stream.readBoolean();

            float pannelAlphaIncoming = stream.readFloat();

            if (pannelAlphaIncoming != pannelAlphaSyncValue) {
                pannelAlphaSyncValue = pannelAlphaIncoming;
                pannelAlphaSyncNew = true;
            }

            eConn.deserialize(stream);
            roofMount = SolarRoofMount.fromId(stream.readByte());
            roofMountKnown = true;

            renderPreProcess = null;

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void clientSetPannelAlpha(float value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(SolarPanelElement.unserializePannelAlpha);
            stream.writeFloat(value);

            sendPacketToServer(bos);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }


    @Override
    public boolean cameraDrawOptimisation() {
        return false;
    }
}
