package mods.eln.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mods.eln.client.render.GlSubmit;
import mods.eln.gl.GL11;
import mods.eln.gl.GlFrame;
import mods.eln.vehicle.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;

/** Original meshes and clear glazing through the native render-state pipeline. */
public final class RoadVehicleRenderer extends EntityRenderer<RoadVehicleEntity,RoadVehicleRenderer.State> {
    public static final class State extends EntityRenderState {
        public RoadVehicleKind kind=RoadVehicleKind.CART;
        public float yaw,charge,spin;
        public boolean plugged;
        public void capture(RoadVehicleEntity entity,float partial) {
            kind=entity.getKind();yaw=entity.getYRot(partial);charge=entity.getCharge();
            plugged=entity.getPlugged();spin=(entity.tickCount+partial)*entity.getDisplayedSpeed()*8;
        }
    }
    public RoadVehicleRenderer(EntityRendererProvider.Context context) { super(context);shadowRadius=1f; }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(RoadVehicleEntity entity,State state,float partial) {
        super.extractRenderState(entity,state,partial);
        state.capture(entity,partial);
    }
    public static AABB localBounds(RoadVehicleKind kind,float yaw) {
        double radians=Math.toRadians(yaw);
        boolean cart=kind==RoadVehicleKind.CART;
        double width=cart?.83:.98,length=cart?1.35:2.03;
        double x=Math.abs(Math.cos(radians))*width+Math.abs(Math.sin(radians))*length;
        double z=Math.abs(Math.sin(radians))*width+Math.abs(Math.cos(radians))*length;
        return new AABB(-x,0,-z,x,cart?2.15:1.85,z).inflate(.15);
    }
    @Override protected AABB getBoundingBoxForCulling(RoadVehicleEntity entity,float partial) {
        return localBounds(entity.getKind(),entity.getYRot(partial)).move(entity.getPosition(partial));
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector output,CameraRenderState camera) {
        GlFrame frame=GlSubmit.open(pose);
        try {GL11.glRotatef(-state.yaw,0,1,0);VehicleModels.INSTANCE.draw(state.kind,state.charge,state.plugged,state.spin);}
        finally {GlFrame.close();}
        GlSubmit.submit(frame,pose,output,state.lightCoords);
        super.submit(state,pose,output,camera);
    }
}
