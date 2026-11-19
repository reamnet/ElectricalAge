package mods.eln.client;

import mods.eln.i18n.I18N;
import mods.eln.vehicle.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class RoadVehicleKeys {
    private RoadVehicleKeys(){}
    public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();var player=mc.player;
        if(player==null||mc.getConnection()==null||!(player.getVehicle() instanceof RoadVehicleEntity vehicle)||vehicle.getControllingPassenger()!=player)return;
        boolean active=mc.gui.screen()==null;
        int forward=active?(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0):0;
        int turn=active?(mc.options.keyLeft.isDown()?1:0)-(mc.options.keyRight.isDown()?1:0):0;
        ClientPacketDistributor.sendToServer(new VehicleControls(forward,turn));
        if(player.tickCount%10==0)player.sendOverlayMessage(Component.literal(I18N.tr("%1$ km/h | Battery %2$% | Packs %3$/%4$",
            (int)(Math.abs(vehicle.getDisplayedSpeed())*3.6),(int)(vehicle.getCharge()*100),vehicle.getInstalledPacks(),vehicle.getKind().getPacks())));
    }
}
