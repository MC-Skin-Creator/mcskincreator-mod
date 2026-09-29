/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.widget;

import java.util.List;
import java.util.function.IntSupplier;

import fr.clixmods.mcsc.mod.catalog.Rgb;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;

/**
 * One colour key of a layer: a slot filled with the colour it is shown in now.
 *
 * <p>The fill is the one colour on screen that is not the interface's — it is the
 * skin's, read from the layer on every frame, so an undo or a recolour elsewhere shows
 * here without anything being laid out again. Everything around it is the slot
 * material, lit when pointed at, like any other slot.
 *
 * <p>The key's name is the tooltip rather than a label: a row of five labelled
 * swatches does not fit in a side column, and the name is only needed to tell two
 * close colours apart, which is what pointing at one is for.
 */
public class ColorSwatch extends Element {
    private final Component name;
    private final IntSupplier read;
    private final Runnable onClick;

    public ColorSwatch(Component name, IntSupplier read, Runnable onClick) {
        this.name = name;
        this.read = read;
        this.onClick = onClick;
        this.width = Metrics.BUTTON_HEIGHT_COMPACT;
        this.height = Metrics.BUTTON_HEIGHT_COMPACT;
    }

    @Override
    public void draw(Paint paint) {
        if (!visible()) {
            return;
        }
        Canvas canvas = paint.canvas();
        if (paint.hot(this)) {
            Surface.slotHighlight(canvas, this.x, this.y, this.width, this.height);
        } else {
            Surface.slot(canvas, this.x, this.y, this.width, this.height);
        }
        int inset = Metrics.SLOT_INSET + 1;
        canvas.fill(this.x + inset, this.y + inset,
                this.width - inset * 2, this.height - inset * 2, Rgb.argb(this.read.getAsInt()));
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        return button == 0 && contains(mouseX, mouseY) && activate();
    }

    @Override
    public boolean activate() {
        this.onClick.run();
        return true;
    }

    @Override
    public List<Component> tooltip() {
        return List.of(this.name);
    }
}
