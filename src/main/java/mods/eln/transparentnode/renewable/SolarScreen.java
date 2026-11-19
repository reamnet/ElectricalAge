package mods.eln.transparentnode.renewable;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Native 26.3 solar controls, fed by element publication packets. */
public final class SolarScreen extends ElnScreen {
    private final RenewableRender render;
    private final boolean hybrid;
    public SolarScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title, 300, 238);
        this.render = (RenewableRender) render;
        hybrid = this.render.getRenewable().getKind() == RenewableKind.HYBRID;
    }
    @Override protected void init() {
        super.init();
        button(8, 88, 284, () -> render.getDisplay().getEnabled() ? tr("Controller: On") : tr("Controller: Off"),
            () -> render.clientSendId((byte) 81));
        field(8, 116, 174, () -> Double.toString(render.getDisplay().getTarget()), text -> {
            try { render.setVoltage(Double.parseDouble(text)); } catch (NumberFormatException ignored) { }
        }, hybrid ? tr("Output must be 120 or 240 V") : tr("Output voltage: 12 to 3200 V"));
        if (hybrid) {
            button(8, 140, 284, () -> SolarDisplayKt.priorityText(render.getDisplay().getPriority()), () -> render.clientSendId((byte) 82));
            button(8, 166, 138, () -> tr("120 V output"), () -> render.setVoltage(120));
            button(154, 166, 138, () -> tr("240 V output"), () -> render.setVoltage(240));
        } else {
            button(8, 140, 284, () -> render.getDisplay().getGridFeed() ? tr("Output mode: Grid feed") : tr("Output mode: Regulated supply"),
                () -> render.clientSendId((byte) 83));
        }
    }
    @Override protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        SolarDisplay d = render.getDisplay();
        label(g, 8, 22, SolarDisplayKt.solarStatus(d.getStatus()));
        label(g, 8, 36, tr("Solar: %1$ / %2$", Utils.plotVolt("", d.getPv()), Utils.plotPower("", d.getPvWatts())));
        label(g, 8, 48, hybrid
            ? tr("Grid input: %1$ / %2$", Utils.plotVolt("", d.getGrid()), Utils.plotPower("", d.getGridWatts()))
            : tr("Grid feed: %1$; total %2$ kWh", Utils.plotPower("", d.getFeedWatts()), String.format(java.util.Locale.ROOT, "%.3f", d.getFedJoules() / 3.6e6)));
        label(g, 8, 60, tr("Out: %1$ / %2$", Utils.plotVolt("", d.getOutput()), Utils.plotPower("", d.getWatts())));
        label(g, 8, 74, tr("DC link: %1$ / 5000 J", (int) d.getEnergy()));
        label(g, 8, 198, tr("Blue: PV+; black: PV-; orange: out"));
        label(g, 8, 212, hybrid ? tr("Dark blue: optional grid input") : tr("Grid feed follows a live bus at 80-110% nominal."));
    }
}
