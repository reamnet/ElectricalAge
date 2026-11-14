package mods.eln.transparentnode.industrial;

import mods.eln.Eln;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Four bounded inputs; no client position, velocity, battery or cargo state is accepted. */
public record ForkliftControls(int forward, int turn, int lift, boolean pickup) implements CustomPacketPayload {
    public ForkliftControls {
        forward=Math.clamp(forward,-1,1);turn=Math.clamp(turn,-1,1);lift=Math.clamp(lift,-1,1);
    }
    public static final Type<ForkliftControls> TYPE=new Type<>(Identifier.fromNamespaceAndPath(Eln.MODID,"forklift_controls"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ForkliftControls> CODEC=StreamCodec.of(
        (buf,p)->{buf.writeByte(p.forward);buf.writeByte(p.turn);buf.writeByte(p.lift);buf.writeBoolean(p.pickup);},
        buf->new ForkliftControls(buf.readByte(),buf.readByte(),buf.readByte(),buf.readBoolean()));
    @Override public Type<ForkliftControls> type() { return TYPE; }
    public static void handle(ServerPlayer player,ForkliftControls input) {
        if(player.getVehicle() instanceof ForkliftEntity forklift && forklift.getControllingPassenger()==player)
            forklift.controls(player,input.forward,input.turn,input.lift,input.pickup);
    }
}
