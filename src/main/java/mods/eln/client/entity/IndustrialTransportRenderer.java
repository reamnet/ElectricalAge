package mods.eln.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mods.eln.client.render.GlSubmit;
import mods.eln.gl.GL11;
import mods.eln.gl.GlFrame;
import mods.eln.transparentnode.industrial.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/** Source transport geometry submitted through native 26.x render states. */
public final class IndustrialTransportRenderer<T extends Entity> extends EntityRenderer<T,IndustrialTransportRenderer.State> {
    public static final class State extends EntityRenderState {
        public float yaw;
        public double lift;
        public boolean pallet,cargo;
        public void capture(Entity entity,float partial) {
            yaw=entity.getYRot(partial);
            pallet=entity instanceof PalletEntity;
            lift=entity instanceof ForkliftEntity forklift ? forklift.getLift() : 0;
            cargo=entity instanceof ForkliftEntity forklift ? forklift.getHasCargo() : entity instanceof PalletEntity p && p.getLoaded();
        }
    }
    public IndustrialTransportRenderer(EntityRendererProvider.Context context,boolean pallet) {
        super(context);shadowRadius=pallet?.5f:.8f;
    }
    /** Source mesh extents, including translated forks and the carried pallet, rotated about the entity origin. */
    public static AABB localBounds(boolean pallet,float yaw,double lift,boolean cargo) {
        double minX=pallet?-.48:-.56,maxX=pallet?.48:.56;
        double minZ=pallet?-.45:-.9,maxZ=pallet?.515:(cargo?1.865:1.69);
        double top=pallet?(cargo?.81:.185):Math.max(2.12,lift+(cargo?1.05:.33));
        double angle=Math.toRadians(yaw),cos=Math.cos(angle),sin=Math.sin(angle);
        double cx=(minX+maxX)*.5,cz=(minZ+maxZ)*.5,hx=(maxX-minX)*.5,hz=(maxZ-minZ)*.5;
        double centerX=cx*cos-cz*sin,centerZ=cx*sin+cz*cos;
        double extentX=Math.abs(cos)*hx+Math.abs(sin)*hz,extentZ=Math.abs(sin)*hx+Math.abs(cos)*hz;
        return new AABB(centerX-extentX,0,centerZ-extentZ,centerX+extentX,top,centerZ+extentZ).inflate(.02);
    }
    @Override protected AABB getBoundingBoxForCulling(T entity,float partial) {
        boolean pallet=entity instanceof PalletEntity;
        double lift=entity instanceof ForkliftEntity forklift?forklift.getLift():0;
        boolean cargo=entity instanceof ForkliftEntity forklift?forklift.getHasCargo():entity instanceof PalletEntity p && p.getLoaded();
        return localBounds(pallet,entity.getYRot(partial),lift,cargo).move(entity.getPosition(partial));
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(T entity,State state,float partial) {
        super.extractRenderState(entity,state,partial);state.capture(entity,partial);
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector output,CameraRenderState camera) {
        GlFrame frame=GlSubmit.open(pose);
        try {
            GL11.glRotatef(-state.yaw,0,1,0);
            IndustrialTransportModels.INSTANCE.draw(state.pallet,state.lift,state.cargo);
        }finally { GlFrame.close(); }
        GlSubmit.submit(frame,pose,output,state.lightCoords);
        super.submit(state,pose,output,camera);
    }
}
