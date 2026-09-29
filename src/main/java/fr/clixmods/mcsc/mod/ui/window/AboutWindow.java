/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.window;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.Prose;
import fr.clixmods.mcsc.mod.ui.widget.PixelButton;
import net.minecraft.network.chat.Component;

/**
 * About: what the mod is, which build this is, who made it, and the places the site
 * points to, reachable from the game.
 *
 * <p>Laid out in blocks with a full gap between them, and a block says something the
 * others do not: where the catalogue comes from is the credits' business, so it is not
 * repeated here.
 */
public class AboutWindow extends ModalWindow {
    /** The site's own links, the ones its footer carries. */
    private static final List<Link> LINKS = List.of(
            new Link("about.mcskincreator.link.website", "https://mcskincreator.app/"),
            new Link("about.mcskincreator.link.discord", "https://discord.com/invite/tr9RZr6Vgp"),
            new Link("about.mcskincreator.link.x", "https://x.com/skincreatorapp"),
            new Link("about.mcskincreator.link.instagram",
                    "https://www.instagram.com/mcskincreatorapp/"),
            new Link("about.mcskincreator.link.youtube",
                    "https://www.youtube.com/channel/UCoOCgJgK_CwBX-tkkMd5SMg"));

    private record Link(String labelKey, String url) {
    }

    private final String version;
    private final String author;
    private final Consumer<URI> opener;

    /**
     * @param version the mod's own version
     * @param author  who made it
     * @param opener  what opens an address, so the window never reaches for the game
     */
    public AboutWindow(String version, String author, Consumer<URI> opener) {
        super("window.mcskincreator.about", null);
        this.version = version;
        this.author = author;
        this.opener = opener;
    }

    private int bodyWidth() {
        return width() - Metrics.PAD * 2;
    }

    private List<String> intro(Canvas canvas) {
        return Prose.wrap(canvas, Component.translatable("about.mcskincreator.what"), bodyWidth());
    }

    private List<String> note(Canvas canvas) {
        return Prose.wrap(canvas, Component.translatable("about.mcskincreator.local_only"),
                Prose.noteRoom(bodyWidth()));
    }

    private List<PixelButton> buttons(Canvas canvas) {
        List<PixelButton> buttons = new ArrayList<>();
        for (Link link : LINKS) {
            URI uri = URI.create(link.url());
            PixelButton button = new PixelButton(Component.translatable(link.labelKey()),
                    PixelButton.Style.NORMAL, () -> this.opener.accept(uri));
            button.fit(canvas);
            buttons.add(button);
        }
        return buttons;
    }

    /** Sets the buttons in rows that wrap at {@code width}; returns the height used. */
    private static int flow(List<PixelButton> buttons, int left, int top, int width) {
        int cursorX = left;
        int cursorY = top;
        for (PixelButton button : buttons) {
            if (cursorX > left && cursorX + button.width() > left + width) {
                cursorX = left;
                cursorY += Metrics.BUTTON_HEIGHT + Metrics.PAD_TIGHT;
            }
            button.setBounds(cursorX, cursorY, button.width(), Metrics.BUTTON_HEIGHT);
            cursorX += button.width() + Metrics.PAD_TIGHT;
        }
        return cursorY + Metrics.BUTTON_HEIGHT - top;
    }

    private int line(Canvas canvas) {
        return canvas.lineHeight() + 1;
    }

    /** Where the intro paragraph starts, relative to the top of the body. */
    private int introTop(Canvas canvas) {
        return line(canvas) * 3 + Metrics.PAD;
    }

    private int noteTop(Canvas canvas) {
        return introTop(canvas) + intro(canvas).size() * line(canvas) + Metrics.PAD;
    }

    private int communityTop(Canvas canvas) {
        return noteTop(canvas) + Prose.noteHeight(canvas, note(canvas).size()) + Metrics.PAD;
    }

    @Override
    protected int contentHeight(Canvas canvas) {
        int top = communityTop(canvas) + line(canvas) + Metrics.PAD_TIGHT;
        return top + flow(buttons(canvas), 0, 0, bodyWidth());
    }

    @Override
    protected void layoutBody(Canvas canvas, int left, int top, int width) {
        List<PixelButton> buttons = buttons(canvas);
        flow(buttons, left, top + communityTop(canvas) + line(canvas) + Metrics.PAD_TIGHT, width);
        for (PixelButton button : buttons) {
            addBodyChild(button);
        }
    }

    @Override
    protected void drawBody(Paint paint, int left, int top, int width) {
        Canvas canvas = paint.canvas();
        canvas.text(Component.translatable("gui.mcskincreator.brand"), left, top, Palette.INK);
        canvas.text(Component.translatable("about.mcskincreator.version", this.version),
                left, top + line(canvas), Palette.INK_FAINT);
        canvas.text(Component.translatable("about.mcskincreator.author", this.author),
                left, top + line(canvas) * 2, Palette.INK_FAINT);

        int rowY = top + introTop(canvas);
        for (String row : intro(canvas)) {
            canvas.text(Component.literal(row), left, rowY, Palette.INK_MUTED);
            rowY += line(canvas);
        }
        Prose.drawNote(canvas, left, top + noteTop(canvas), width, note(canvas));
        canvas.text(Component.translatable("about.mcskincreator.community"),
                left, top + communityTop(canvas), Palette.INK);
    }
}
