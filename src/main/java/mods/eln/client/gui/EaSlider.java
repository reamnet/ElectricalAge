package mods.eln.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * A slider over [min, max] in steps, with a source-style vertical track for fixed layouts. The value is sent
 * when the mouse is released or a key changes it, as 1.12 did; while not dragged it follows the device (setFromDevice).
 */
public class EaSlider extends AbstractSliderButton {
    final double min, max, step;
    final DoubleFunction<Component> label;
    final DoubleConsumer onCommit;
    private boolean held;
    private final boolean vertical;

    public EaSlider(int x, int y, int w, double min, double max, double step, double initial,
                    DoubleFunction<Component> label, DoubleConsumer onCommit) {
        this(x, y, w, 20, false, min, max, step, initial, label, onCommit);
    }

    public EaSlider(int x, int y, int w, int h, boolean vertical, double min, double max, double step, double initial,
                    DoubleFunction<Component> label, DoubleConsumer onCommit) {
        super(x, y, w, h, Component.empty(), 0);
        this.vertical = vertical;
        this.min = min;
        this.max = max;
        this.step = step;
        this.label = label;
        this.onCommit = onCommit;
        this.value = toUnit(initial);
        updateMessage();
    }

    double toUnit(double v) {
        return Math.max(0, Math.min(1, (v - min) / (max - min)));
    }

    public double get() {
        double v = min + value * (max - min);
        if (step > 0) v = Math.round(v / step) * step;
        return Math.max(min, Math.min(max, v));
    }

    /** Follow the device's value unless the player is moving the slider. */
    public void setFromDevice(double v) {
        if (held || isFocused() && dragging) return;
        value = toUnit(v);
        updateMessage();
    }

    /** Re-read the label (it may show live values too). */
    public void refreshLabel() {
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(label.apply(get()));
        if (vertical) setTooltip(net.minecraft.client.gui.components.Tooltip.create(getMessage()));
    }

    @Override
    protected void applyValue() {
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        held = true;
        if (vertical) { dragging = active; fromVerticalMouse(event); }
        else super.onClick(event, doubleClick);
    }

    private void fromVerticalMouse(MouseButtonEvent event) {
        double v = min + Math.max(0,Math.min(1,1-(event.y()-getY())/height))*(max-min);
        if(step>0)v=Math.round(v/step)*step;
        setValue(toUnit(v));
    }

    @Override protected void onDrag(MouseButtonEvent event,double dx,double dy) {
        if(vertical)fromVerticalMouse(event);else super.onDrag(event,dx,dy);
    }

    @Override public void extractWidgetRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my,float dt) {
        if(!vertical) { super.extractWidgetRenderState(g,mx,my,dt);return; }
        int x=getX(),y=getY(),cursor=y+height-(int)Math.round(value*height);
        g.fill(x,y-2,x+width,y+height+2,0xFF404040);
        g.fill(x+1,y-1,x+width-1,y+height+1,0xFF606060);
        g.fill(x+2,y,x+width-2,y+height,0xFF808080);
        g.fill(x-2,cursor-2,x+width+2,cursor+2,0xFF202020);
        g.fill(x-1,cursor-1,x+width+1,cursor+1,0xFF606060);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        super.onRelease(event);
        held = false;
        onCommit.accept(get());
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if(vertical && (event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_UP || event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_DOWN)) {
            double increment=step>0?step:(max-min)/100;
            setValue(toUnit(get()+(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_UP?increment:-increment)));
            onCommit.accept(get());return true;
        }
        boolean r = super.keyPressed(event);
        if (r) onCommit.accept(get());
        return r;
    }
}
