package mods.eln.vehicle;

import mods.eln.Eln;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** The client sends only bounded driver input; vehicle state and energy stay server-owned. */
public record VehicleControls(int forward,int turn) implements CustomPacketPayload {
    public VehicleControls { forward=Math.clamp(forward,-1,1);turn=Math.clamp(turn,-1,1); }
    public static final Type<VehicleControls> TYPE=new Type<>(Identifier.fromNamespaceAndPath(Eln.MODID,"road_vehicle_controls"));
    public static final StreamCodec<RegistryFriendlyByteBuf,VehicleControls> CODEC=StreamCodec.of(
        (buf,p)->{buf.writeByte(p.forward);buf.writeByte(p.turn);},buf->new VehicleControls(buf.readByte(),buf.readByte()));
    @Override public Type<VehicleControls> type(){return TYPE;}
    public static void handle(ServerPlayer player,VehicleControls input) {
        if(player.getVehicle() instanceof RoadVehicleEntity vehicle && vehicle.getControllingPassenger()==player)
            vehicle.controls(player,input.forward,input.turn);
    }
}
