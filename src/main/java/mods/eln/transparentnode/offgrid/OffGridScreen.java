package mods.eln.transparentnode.offgrid;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import static mods.eln.i18n.I18N.tr;

/** The server owns stored energy and validates all setting packets. */
public final class OffGridScreen extends ElnScreen {
    private final OffGridRender render;
    private final OffGridType type;
    public OffGridScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title, 320, 240);
        this.render=(OffGridRender)render;
        type=this.render.getCabinet().getType();
    }
    @Override protected void init() {
        super.init();
        button(8,128,148,()->render.getDisplay().getEnabled()?tr("Disconnect"):tr("Connect"),()->render.clientSendId((byte)81));
        button(164,128,148,()->tr("Reset protection"),()->render.clientSendId((byte)83));
        if(type==OffGridType.INVERTER) {
            button(8,152,304,()->OffGridDisplayKt.islandMode(render.getDisplay().getMode()),()->render.clientSendId((byte)82));
            button(8,178,148,()->tr("120 V output"),()->render.sendValue(85,120));
            button(164,178,148,()->tr("240 V output"),()->render.sendValue(85,240));
        }
        if(type==OffGridType.INVERTER || type==OffGridType.AC_CHARGER) {
            field(8,208,148,()->Double.toString(render.getDisplay().getChargeAmps()),text->{
                try { render.sendValue(84,Double.parseDouble(text)); } catch(NumberFormatException ignored) { }
            },tr("Battery charging current: 0 to 200 A"));
        }
    }
    @Override protected void extractDevice(GuiGraphicsExtractor g,int mx,int my,float delta) {
        OffGridDisplay d=render.getDisplay();
        label(g,8,22,OffGridDisplayKt.offGridStatus(d.getStatus()));
        label(g,8,38,tr("Battery bus: %1$",Utils.plotVolt("",d.getDc()).trim()));
        if(type.getBank()) {
            label(g,8,54,tr("Stored: %1$ / %2$ kWh",String.format(java.util.Locale.ROOT,"%.2f",d.getEnergy()/3.6e6),type.getKwh()));
            label(g,8,70,tr("Battery current: %1$",Utils.plotAmpere("",d.getAmps()).trim()));
            label(g,8,86,tr("Cell temperature: %1$ C",(int)d.getTemperature()));
            label(g,8,106,tr("A separate inverter supplies household voltage."));
        } else if(type==OffGridType.COMBINER || type==OffGridType.DISCONNECT) {
            label(g,8,54,tr("DC current: %1$",Utils.plotAmpere("",d.getAmps()).trim()));
            label(g,8,70,tr("Output power: %1$",Utils.plotPower("",d.getWatts()).trim()));
            if(type==OffGridType.COMBINER)label(g,8,160,tr("Replace blown links with 30 A or smaller fuse items."));
        } else {
            label(g,8,54,tr("Input: %1$ / %2$",Utils.plotVolt("",d.getGrid()).trim(),Utils.plotPower("",d.getInputWatts()).trim()));
            label(g,8,70,tr("Battery charging: %1$",Utils.plotPower("",d.getChargeWatts()).trim()));
            label(g,8,86,tr("Load: %1$ / %2$",Utils.plotVolt("",d.getOutput()).trim(),Utils.plotPower("",d.getWatts()).trim()));
            if(type==OffGridType.INVERTER)label(g,8,106,tr("Grid input and backed-up output are separate."));
        }
    }
}
