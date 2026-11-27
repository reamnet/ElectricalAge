package mods.eln.sixnode.wirelesssignal.source;

import mods.eln.Eln;
import mods.eln.gl.GL11;
import mods.eln.misc.Obj3D;
import mods.eln.misc.UtilsClient;
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceRenderObj;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The ForkPatch wall plate shared by the switch and button variants. */
final class WallWirelessModel extends ElectricalGateSourceRenderObj {
    private final Obj3D model;
    private final float travel;

    WallWirelessModel(boolean paddle) {
        super(null);
        model = java.util.Objects.requireNonNull(Eln.obj.getObj(paddle ? "wirelesspaddleswitch" : "wirelesstoggleswitch"));
        travel = paddle ? 16 : 40;
        speed = .025f;
    }

    @Override
    public void draw(float factor, float distance, BlockEntity entity) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(0xFFFFF);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor3f(.89f, .88f, .83f);
            model.getPart("Plate").drawNoBind();
            GL11.glColor3f(.52f, .54f, .56f);
            model.getPart("Trim").drawNoBind();
            boolean on = factor > .5f;
            if (on) UtilsClient.disableLight();
            if (on) GL11.glColor3f(.22f, .64f, .30f);
            else GL11.glColor3f(.19f, .21f, .20f);
            model.getPart("Indicator").drawNoBind();
            if (on) UtilsClient.enableLight();
            GL11.glRotatef((factor - .5f) * travel, 0, 0, 1);
            GL11.glColor3f(.97f, .96f, .92f);
            model.getPart("Lever").drawNoBind();
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }
}
