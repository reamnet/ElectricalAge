package mods.eln.client.gui;

import mods.eln.item.wiresnips.WireSnipsMenu;
import mods.eln.misc.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import static mods.eln.i18n.I18N.tr;

/** Native inventory-button packets; all cutting and length validation remain on the owning server menu. */
public final class WireSnipsScreen extends AbstractContainerScreen<WireSnipsMenu> {
    private EditBox length;
    private Button cut;
    public WireSnipsScreen(WireSnipsMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title,176,200);
        inventoryLabelY=107;
    }
    @Override protected void init() {
        super.init();
        length=addRenderableWidget(new EditBox(font,leftPos+8,topPos+72,58,18,Component.literal(tr("Cut length in whole meters"))));
        length.setMaxLength(6);length.setValue("32");
        cut=addRenderableWidget(Button.builder(Component.literal(tr("Cut Wire")),button->{
            int action=action();var gameMode=Minecraft.getInstance().gameMode;
            if(action>=0 && gameMode!=null)gameMode.handleInventoryButtonClick(menu.containerId,action);
        }).bounds(leftPos+72,topPos+70,96,20).build());
    }
    private int action() {
        try { return WireSnipsMenu.encodeCutAction(Double.parseDouble(length.getValue())); }
        catch(NumberFormatException e) { return -1; }
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt) {
        super.extractBackground(g,mx,my,dt);
        g.blitSprite(RenderPipelines.GUI_TEXTURED,ElnScreen.PANEL,leftPos,topPos,imageWidth,imageHeight);
        for(var slot:menu.slots)g.blitSprite(RenderPipelines.GUI_TEXTURED,ElnScreen.SLOT,leftPos+slot.x-1,topPos+slot.y-1,18,18);
        var wire=menu.getSelectedDescriptor();int encoded=action();
        cut.active=wire!=null && WireSnipsMenu.validWire(menu.getSelectedStack()) && encoded>=0 && encoded-WireSnipsMenu.ACTION_BASE<wire.getRemainingLengthMeters(menu.getSelectedStack());
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my) {
        g.text(font,title,8,6,ElnScreen.LABEL,false);
        g.text(font,Component.literal(tr("Input Wire")),8,20,ElnScreen.LABEL,false);
        g.text(font,Component.literal(tr("Length (m)")),8,60,ElnScreen.LABEL,false);
        var wire=menu.getSelectedDescriptor();
        String text=wire==null?tr("Insert a wire coil to cut"):tr("Remaining: %1$ m",Utils.plotValue(wire.getRemainingLengthMeters(menu.getSelectedStack())));
        g.text(font,Component.literal(text),8,96,ElnScreen.LABEL,false);
        g.text(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,ElnScreen.LABEL,false);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(getFocused() instanceof EditBox box && box.isFocused() && event.key()!=com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE)
            return box.keyPressed(event)||box.canConsumeInput();
        return super.keyPressed(event);
    }
}
