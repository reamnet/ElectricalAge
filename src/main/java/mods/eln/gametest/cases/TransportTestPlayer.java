package mods.eln.gametest.cases;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import java.util.Set;

/** A server player with a silent connection. NeoForge FakePlayer explicitly refuses startRiding. */
public final class TransportTestPlayer extends ServerPlayer {
    public TransportTestPlayer(ServerLevel level,GameProfile profile) {
        super(level.getServer(),level,profile,ClientInformation.createDefault());
        connection=new ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(PacketFlow.SERVERBOUND),this,
            CommonListenerCookie.createInitial(profile,false)) {
            @Override public void send(Packet<?> packet) {}
            @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener) {}
            @Override public void teleport(PositionMoveRotation change,Set<Relative> relative) {}
        };
    }
    @Override public boolean isClientAuthoritative() { return false; }
}
