package mods.eln.client;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.transparentnode.industrial.ForkliftControls;
import mods.eln.transparentnode.industrial.ForkliftEntity;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import com.mojang.blaze3d.platform.InputConstants;

/** One control payload per client tick while driving; opening a screen sends neutral input. */
public final class ForkliftKeys {
    private ForkliftKeys() {}
    static final KeyMapping.Category CATEGORY=new KeyMapping.Category(Identifier.fromNamespaceAndPath(Eln.MODID,"vehicles"));
    static final KeyMapping RAISE=new KeyMapping("key.eln.forklift.raise",InputConstants.KEY_PAGEUP,CATEGORY);
    static final KeyMapping LOWER=new KeyMapping("key.eln.forklift.lower",InputConstants.KEY_PAGEDOWN,CATEGORY);
    static final KeyMapping CARGO=new KeyMapping("key.eln.forklift.cargo",InputConstants.KEY_G,CATEGORY);
    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);event.register(RAISE);event.register(LOWER);event.register(CARGO);
    }
    private static int axis(boolean active,boolean positive,boolean negative) {
        return active ? (positive?1:0)-(negative?1:0) : 0;
    }
    public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();var player=mc.player;
        if(player==null || mc.getConnection()==null || !(player.getVehicle() instanceof ForkliftEntity forklift) || forklift.getControllingPassenger()!=player)return;
        boolean active=mc.gui.screen()==null;
        ClientPacketDistributor.sendToServer(new ForkliftControls(axis(active,mc.options.keyUp.isDown(),mc.options.keyDown.isDown()),
            axis(active,mc.options.keyLeft.isDown(),mc.options.keyRight.isDown()),axis(active,RAISE.isDown(),LOWER.isDown()),active && CARGO.isDown()));
        if(player.tickCount%20==0)player.sendOverlayMessage(Component.literal(I18N.tr("Traction pack: %1$%; forks: %2$ cm",(int)(forklift.getStateOfCharge()*100),(int)(forklift.getLift()*100))));
    }
}
