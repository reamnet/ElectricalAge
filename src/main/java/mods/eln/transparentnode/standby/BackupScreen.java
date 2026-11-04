package mods.eln.transparentnode.standby;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import static mods.eln.i18n.I18N.tr;

/** Native controls publish settings only; fuel remains server-owned. */
public final class BackupScreen extends ElnScreen {
    private final BackupRender render;
    private final boolean generator;
    public BackupScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu,inventory,title,320,256);
        this.render=(BackupRender)render;
        generator=this.render.getCabinet().getGenerator();
    }
    @Override protected void init() {
        super.init();
        button(8,92,304,()->{
            int mode=render.getDisplay().getMode();
            if(!generator)return mode==0?tr("Automatic: On"):tr("Automatic: Off");
            return switch(mode) { case 0 -> tr("Mode: Auto"); case 1 -> tr("Mode: Manual run"); default -> tr("Mode: Off"); };
        },()->render.clientSendId((byte)82));
        field(8,122,128,()->Double.toString(render.getDisplay().getTarget()),text->{
            try { render.setVoltage(Double.parseDouble(text)); } catch(NumberFormatException ignored) { }
        },tr("Rated voltage: 12 to 51200 V; match both supplies"));
        button(144,118,80,()->tr("120 V"),()->render.setVoltage(120));
        button(232,118,80,()->tr("240 V"),()->render.setVoltage(240));
        field(8,152,304,()->render.getDisplay().getChannel(),render::setChannel,
            tr("Wireless start channel (case-sensitive); blank disables remote starting"));
        button(8,178,304,()->generator?tr("Reset protection"):tr("Reset linked generator"),()->render.clientSendId((byte)83));
    }
    @Override protected void extractDevice(GuiGraphicsExtractor g,int mx,int my,float delta) {
        BackupDisplay d=render.getDisplay();
        label(g,8,22,BackupDisplayKt.backupStatus(d.getStatus()));
        if(generator) {
            label(g,8,38,tr("Gasoline: %1$ / %2$ mB",(int)d.getFuel(),render.getCabinet().getTier().getTankMb()));
            label(g,8,54,tr("Rated: %1$ V / %2$ W",(int)d.getTarget(),(int)Math.min(render.getCabinet().getTier().getWatts(),d.getTarget()*render.getCabinet().getTier().getAmps())));
        } else {
            label(g,8,38,tr("Mains: %1$",Utils.plotVolt("",d.getMains()).trim()));
            label(g,8,54,tr("Generator: %1$",Utils.plotVolt("",d.getBackup()).trim()));
        }
        label(g,8,70,tr("Out: %1$ / %2$",Utils.plotVolt("",d.getOutput()).trim(),Utils.plotPower("",d.getWatts()).trim()));
        label(g,8,208,d.getChannel().isBlank()?tr("Wireless start: unlinked"):d.getRequest()?tr("Wireless start: requested"):tr("Wireless start: ready"));
        if(!generator && !d.getResetResult().isEmpty())label(g,8,224,switch(d.getResetResult()) {
            case "RESET" -> tr("Reset complete: %1$",d.getResetCount());
            case "READY" -> tr("Linked generator ready");
            case "MISSING" -> tr("No linked generator loaded");
            default -> tr("Set a sync channel first");
        });
        label(g,8,240,tr("Wireless control; electrical power still needs cables."));
    }
}
