package mods.eln.transparentnode.ups;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.Locale;
import static mods.eln.i18n.I18N.tr;

/** Both real 24-cell racks are visible; the generic native menu owns all item transfers. */
public final class UpsScreen extends ElnScreen {
    private final UpsRender render;
    public UpsScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu,inventory,title,320,314);
        this.render=(UpsRender)render;
    }
    @Override protected void init() {
        super.init();
        field(8,110,96,()->Double.toString(render.getDisplay().getTarget()),s->{
            try { render.setVoltage(Double.parseDouble(s)); } catch(NumberFormatException ignored) { }
        },tr("Output voltage: 1 to 3200 V"));
        button(112,106,92,()->render.getDisplay().getEnabled()?tr("Output: On"):tr("Output: Off"),()->render.setFlag(81,!render.getDisplay().getEnabled()));
        button(212,106,100,()->render.getDisplay().getBypass()?tr("Bypass: On"):tr("Bypass: Off"),()->render.setFlag(82,!render.getDisplay().getBypass()));
    }
    private static String number(double x) { return String.format(Locale.ROOT,"%.1f",x); }
    @Override protected void extractDevice(GuiGraphicsExtractor g,int mx,int my,float delta) {
        UpsDisplay d=render.getDisplay();
        label(g,8,22,UpsDisplayKt.upsStatusText(d.getStatus(),false));
        label(g,8,38,tr("Charge: %1$% / %2$ Wh",number(d.getCapacity()>0?100*d.getEnergy()/d.getCapacity():0),number(d.getCapacity()/3600)));
        label(g,8,50,tr("Batteries: %1$/%2$ (%3$ active)",d.getInstalled(),48,d.getActive()));
        label(g,8,62,tr("In: %1$ / %2$",Utils.plotVolt("",d.getInputVolts()).trim(),Utils.plotPower("",d.getInputWatts()).trim()));
        label(g,8,74,tr("Out: %1$ / %2$",Utils.plotVolt("",d.getOutputVolts()).trim(),Utils.plotPower("",d.getOutputWatts()).trim()));
        label(g,8,86,d.getBypass()?tr("Bypass: no battery backup"):d.getOutputWatts()>1?tr("Backup: about %1$ min",number(d.getEnergy()*UpsPowerUnit.EFFICIENCY/d.getOutputWatts()/60)):tr("Backup: no load"));
        label(g,16,136,tr("Rack %1$/2",1));label(g,160,136,tr("Rack %1$/2",2));
        label(g,16,212,tr("Each rack requires 24 compatible battery cells."));
    }
}
