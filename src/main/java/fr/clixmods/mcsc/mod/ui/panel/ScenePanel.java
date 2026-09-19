/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import fr.clixmods.mcsc.mod.scene.CameraMode;
import fr.clixmods.mcsc.mod.scene.GameCamera;
import fr.clixmods.mcsc.mod.scene.SceneBackdrop;
import fr.clixmods.mcsc.mod.scene.SceneCamera;
import fr.clixmods.mcsc.mod.scene.SceneShot;
import fr.clixmods.mcsc.mod.scene.ScenePose;
import fr.clixmods.mcsc.mod.skin.FrontSprite;
import fr.clixmods.mcsc.mod.skin.PreviewSkin;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Figure;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.widget.Dropdown;
import fr.clixmods.mcsc.mod.ui.widget.PixelButton;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * The middle column: the player, and the few controls that belong on top of them.
 *
 * <p>This is the one panel that never goes away. Two other arrangements were tried on
 * the site and dropped — stacking the three panels left the preview 21 pixels, and
 * swapping them through a single slot made the model vanish the moment the library
 * opened. Whatever the width, the scene shrinks and stays.
 *
 * <p>The figure itself is not this panel's: it is handed one, and only works out how much
 * room it may have and what it should be doing. What <em>is</em> this panel's is the
 * camera — the turn, the tilt, the zoom and the pan — because two of the three cameras do
 * not draw a figure at all. They let the game draw the real character, in the real world,
 * and the panel's whole job under those is to keep out of the way.
 *
 * <p>The settings bar sits on a rule at the top rather than floating over the figure, and
 * the dock sits in the top right corner under it. Both were tried the other way: a
 * floating strip reads as three loose boxes, and a dock at the foot of the view lands
 * exactly where the game draws the first-person hand.
 */
public class ScenePanel extends Element {
    /** How the middle column is showing the skin. */
    public enum View {
        MODEL("model"),
        TEXTURE("texture"),
        BOTH("both");

        private final String id;

        View(String id) {
            this.id = id;
        }

        public String labelKey() {
            return "view.mcskincreator." + this.id;
        }

        public String tooltipKey() {
            return labelKey() + ".tooltip";
        }
    }

    /** Vanilla's proportions for a player portrait, from the skin customisation screen. */
    private static final int PORTRAIT_WIDTH = 85;
    private static final int PORTRAIT_HEIGHT = 120;

    private final PreviewSkin preview;
    private final Figure figure;
    private final GameCamera gameCamera;
    private final SceneCamera camera = new SceneCamera();
    private final Runnable relayout;
    private final Supplier<Component> hoveredLabel;
    private final List<Element> controls = new ArrayList<>();

    private View view = View.MODEL;
    private CameraMode cameraMode = CameraMode.WORKSHOP;
    private SceneBackdrop backdrop = SceneBackdrop.PANEL;
    private ScenePose pose = ScenePose.IDLE;
    private boolean playing;

    /** The animation clock: only what has been played counts, so stopping holds it. */
    private long playedMillis;
    private long playingSince;

    /** The gesture in progress: whether it is a pan rather than a turn. */
    private boolean dragging;
    private boolean panning;

    private int[] dock = {0, 0, 0, 0};
    private int[] viewport = {0, 0, 0, 0};

    /** Everything below the top bar: how far the figure may paint, columns included. */
    private int[] stage = {0, 0, 0, 0};

    public ScenePanel(PreviewSkin preview, Figure figure, GameCamera gameCamera,
                      Runnable relayout, Supplier<Component> hoveredLabel) {
        this.preview = preview;
        this.figure = figure;
        this.gameCamera = gameCamera;
        this.relayout = relayout;
        this.hoveredLabel = hoveredLabel;
    }

    public View view() {
        return this.view;
    }

    /**
     * Where the figure is allowed to paint, which is not where it stands.
     *
     * <p>Set by the chrome, because only the chrome knows the window. See
     * {@link Figure#stage}.
     */
    public void setStage(int x, int y, int width, int height) {
        this.stage = new int[] {x, y, width, height};
    }

    public List<Element> controls() {
        return this.controls;
    }

    /**
     * Whether nothing may paint over the world.
     *
     * <p>True under a camera that shows it, and true for a game backdrop behind the
     * workshop figure. The screen asks before it lays down its own backdrop.
     */
    public boolean showsWorld() {
        return this.cameraMode.overlaysGame()
                || this.backdrop.needsWorld() && this.gameCamera.available();
    }

    /**
     * Whether the game is drawing the character itself, and so needs the window.
     *
     * <p>Narrower than {@link #showsWorld()} on purpose: a game <em>backdrop</em> behind
     * the workshop figure is a change of scenery, and folding the library away for it
     * would take the catalogue off the screen in the middle of picking from it.
     */
    public boolean overlaysGame() {
        return this.cameraMode.overlaysGame();
    }

    /**
     * Keeps the camera honest between ticks.
     *
     * <p>The world can disappear from under a camera that needs one — a disconnect with
     * the editor open — and a camera showing a world that is gone shows nothing at all.
     */
    public void tick() {
        if (this.cameraMode.needsWorld() && !this.gameCamera.available()) {
            chooseCamera(CameraMode.WORKSHOP);
            return;
        }
        if (this.backdrop.needsWorld() && !this.gameCamera.available()) {
            this.backdrop = SceneBackdrop.PANEL;
            this.gameCamera.take(this.cameraMode, false);
            this.relayout.run();
        }
    }

    /** Gives the game its camera back, and the character their own movements. */
    public void release() {
        this.gameCamera.release();
    }

    public void layout(Canvas canvas) {
        this.controls.clear();

        int barHeight = Metrics.TAB_HEIGHT + Metrics.PAD_TIGHT;
        int cursorX = this.x + Metrics.PAD_TIGHT;
        int barY = this.y + Metrics.PAD_TIGHT;

        List<PixelButton> viewButtons = new ArrayList<>();
        int segmentedWidth = 0;
        for (View candidate : View.values()) {
            PixelButton button = new PixelButton(Component.translatable(candidate.labelKey()),
                    PixelButton.Style.TAB, () -> {
                        this.view = candidate;
                        this.relayout.run();
                    });
            button.fit(canvas).setActive(this.view == candidate);
            button.withTooltip(Component.translatable(candidate.tooltipKey()));
            viewButtons.add(button);
            segmentedWidth += button.width() + Metrics.SEGMENT_GAP;
        }

        // Three buttons side by side is the site's segmented group, and it is what this
        // shows whenever the scene is wide enough for it. Below that width the same
        // choice becomes a dropdown rather than spilling off the bar: a control that
        // runs past the edge of its strip is a control nobody can reach.
        if (segmentedWidth + Metrics.ui(90) <= this.width) {
            for (PixelButton button : viewButtons) {
                button.setBounds(cursorX, barY, button.width(), Metrics.TAB_HEIGHT);
                this.controls.add(button);
                cursorX += button.width() + Metrics.SEGMENT_GAP;
            }
        } else {
            Dropdown<View> chooser = new Dropdown<>(List.of(View.values()),
                    candidate -> Component.translatable(candidate.labelKey()),
                    () -> this.view,
                    candidate -> {
                        this.view = candidate;
                        this.relayout.run();
                    },
                    candidate -> true);
            int chooserWidth = Math.min(Metrics.ui(150), Math.max(1, this.width - Metrics.PAD_TIGHT * 2));
            chooser.setBounds(cursorX, barY, chooserWidth, Metrics.TAB_HEIGHT);
            chooser.inScreen(this.y + this.height);
            this.controls.add(chooser);
        }

        int viewTop = this.y + barHeight;
        int viewHeight = Math.max(0, this.height - (viewTop - this.y));
        this.viewport = new int[] {this.x, viewTop, this.width, viewHeight};

        layoutModel(viewTop, viewHeight);
        layoutDock(canvas);
    }

    /**
     * Sizes the portrait so the player stays whole.
     *
     * <p>Whichever of width and height runs out first decides, so the figure keeps its
     * proportions at any window size and any of the four GUI scales.
     */
    private void layoutModel(int viewTop, int viewHeight) {
        int space = this.view == View.BOTH ? this.width / 2 : this.width;
        int inset = Metrics.PAD;
        int usableWidth = Math.max(0, space - inset * 2);
        int usableHeight = (int) Math.max(0, (viewHeight - inset * 2) * Metrics.SCENE_MODEL_SHARE);

        int height = Math.min(usableHeight, usableWidth * PORTRAIT_HEIGHT / Math.max(1, PORTRAIT_WIDTH));
        int width = height * PORTRAIT_WIDTH / PORTRAIT_HEIGHT;
        if (width <= 0 || height <= 0) {
            this.figure.place(this.x, viewTop, 0, 0);
            this.figure.stage(this.stage[0], this.stage[1], this.stage[2], this.stage[3]);
            return;
        }
        this.figure.place(this.x + (space - width) / 2, viewTop + (viewHeight - height) / 2,
                width, height);
        this.figure.stage(this.stage[0], this.stage[1], this.stage[2], this.stage[3]);
    }

    /**
     * The dock, top right: the camera, the backdrop, the animation, and putting the view
     * back.
     *
     * <p>Laid out right to left and wrapped onto as many rows as it takes, because the
     * scene is the column that gives up its width first — on a phone in portrait it is
     * the whole screen and on a desktop with both panels open it is a third of it, and
     * the same row of controls has to sit in both.
     */
    private void layoutDock(Canvas canvas) {
        List<Element> docked = new ArrayList<>();

        List<CameraMode> cameras = availableCameras();
        if (cameras.size() > 1) {
            docked.add(chooser(cameras, mode -> Component.translatable(mode.labelKey()),
                    () -> this.cameraMode, this::chooseCamera));
        }

        if (this.cameraMode == CameraMode.WORKSHOP && this.gameCamera.available()) {
            // Only where there is a world to put behind the figure. On the title screen
            // the choice has one answer, and a chooser with one answer is a dead control.
            docked.add(chooser(List.of(SceneBackdrop.values()),
                    candidate -> Component.translatable(candidate.labelKey()),
                    () -> this.backdrop, this::chooseBackdrop));
        }

        // Both figures animate: the workshop one because the mod builds it, the one in
        // the world because the mixin poses it on its way to being drawn.
        if (this.cameraMode != CameraMode.FIRST_PERSON) {
            PixelButton play = new PixelButton(
                    Component.translatable(this.playing
                            ? "gui.mcskincreator.stop"
                            : "gui.mcskincreator.play"),
                    PixelButton.Style.NORMAL, this::togglePlaying);
            play.fit(canvas).setActive(this.playing);
            play.withTooltip(Component.translatable(this.playing
                    ? "gui.mcskincreator.stop.tooltip"
                    : "gui.mcskincreator.play.tooltip"));
            docked.add(play);

            docked.add(chooser(List.of(ScenePose.values()),
                    candidate -> Component.translatable(candidate.labelKey()),
                    () -> this.pose, this::choosePose));
        }

        if (this.cameraMode == CameraMode.FIRST_PERSON) {
            PixelButton swing = new PixelButton(Component.translatable("gui.mcskincreator.swing"),
                    PixelButton.Style.NORMAL, this.gameCamera::swing);
            swing.fit(canvas);
            swing.withTooltip(Component.translatable("gui.mcskincreator.swing.tooltip"));
            docked.add(swing);
        }

        PixelButton recentre = new PixelButton(
                Component.translatable("gui.mcskincreator.recentre"),
                PixelButton.Style.GHOST, this::recentre);
        recentre.fit(canvas);
        recentre.withTooltip(Component.translatable("gui.mcskincreator.recentre.tooltip"));
        docked.add(recentre);

        placeDock(docked);
    }

    private <T> Dropdown<T> chooser(List<T> options, java.util.function.Function<T, Component> naming,
                                    Supplier<T> read, java.util.function.Consumer<T> write) {
        Dropdown<T> dropdown = new Dropdown<>(options, naming, read, write, candidate -> true);
        dropdown.setBounds(0, 0, Metrics.ui(96), Metrics.TAB_HEIGHT);
        dropdown.inScreen(this.y + this.height);
        return dropdown;
    }

    /** Which cameras have something to show, so which ones are worth offering. */
    private List<CameraMode> availableCameras() {
        List<CameraMode> cameras = new ArrayList<>();
        for (CameraMode mode : CameraMode.values()) {
            if (!mode.needsWorld() || this.gameCamera.available()) {
                cameras.add(mode);
            }
        }
        return cameras;
    }

    /**
     * Fills the dock from the top right, wrapping downwards.
     *
     * <p>The rows are filled last control first, so the one that matters least is the
     * one that gets pushed onto a row of its own.
     */
    private void placeDock(List<Element> docked) {
        int gap = Metrics.SEGMENT_GAP;
        int rowHeight = Metrics.TAB_HEIGHT;
        int usable = Math.max(rowHeight,
                this.width - Metrics.PAD_TIGHT * 2 - Metrics.PANEL_INSET * 2);

        List<List<Element>> rows = new ArrayList<>();
        List<Element> row = new ArrayList<>();
        int rowWidth = 0;
        for (int index = docked.size() - 1; index >= 0; index--) {
            Element control = docked.get(index);
            int wanted = Math.min(control.width(), usable);
            control.setBounds(0, 0, wanted, rowHeight);
            int added = row.isEmpty() ? wanted : wanted + gap;
            if (!row.isEmpty() && rowWidth + added > usable) {
                rows.add(row);
                row = new ArrayList<>();
                rowWidth = 0;
                added = wanted;
            }
            row.add(control);
            rowWidth += added;
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }

        int widest = 0;
        for (List<Element> line : rows) {
            int lineWidth = -gap;
            for (Element control : line) {
                lineWidth += control.width() + gap;
            }
            widest = Math.max(widest, lineWidth);
        }

        int dockWidth = widest + Metrics.PANEL_INSET * 2;
        int dockHeight = rows.size() * rowHeight + (rows.size() - 1) * gap
                + Metrics.PANEL_INSET * 2;
        int dockX = this.x + this.width - Metrics.PAD_TIGHT - dockWidth;
        // Top right, in every camera. It used to sit at the bottom and move out of the
        // way in first person, where the game draws the hand — but a control that
        // changes corner depending on the mode is a control you have to look for twice.
        int dockY = this.viewport[1] + Metrics.PAD_TIGHT;
        this.dock = new int[] {dockX, dockY, dockWidth, dockHeight};

        // Rows were gathered last-first and each row right-to-left, so both loops walk
        // back out again to put the first control top-left of the dock.
        int rowY = dockY + dockHeight - Metrics.PANEL_INSET - rowHeight;
        for (List<Element> line : rows) {
            int controlX = dockX + dockWidth - Metrics.PANEL_INSET;
            for (Element control : line) {
                controlX -= control.width();
                control.setBounds(controlX, rowY, control.width(), rowHeight);
                this.controls.add(control);
                controlX -= gap;
            }
            rowY -= rowHeight + gap;
        }
    }

    private void chooseBackdrop(SceneBackdrop backdrop) {
        if (this.backdrop == backdrop) {
            return;
        }
        this.backdrop = backdrop;
        // The world backdrop needs the game's camera too: first person so the level
        // renderer leaves the real character out, and no HUD — which takes the hand with
        // it. What should be behind the figure is the world and nothing else.
        this.gameCamera.take(this.cameraMode, backdrop.needsWorld());
        this.relayout.run();
    }

    private void chooseCamera(CameraMode mode) {
        if (this.cameraMode == mode) {
            return;
        }
        this.cameraMode = mode;
        this.gameCamera.take(mode, this.backdrop.needsWorld());
        this.relayout.run();
    }

    private void choosePose(ScenePose pose) {
        if (this.pose == pose) {
            return;
        }
        this.pose = pose;
        // A new animation starts at its beginning: picking "walk" halfway through a
        // swim cycle would land the figure mid-stride for no reason anybody could see.
        this.playedMillis = 0;
        this.playingSince = 0;
        this.relayout.run();
    }

    /**
     * Starts or stops the animation.
     *
     * <p>Stops rather than pauses, like the site: the figure goes back to standing
     * instead of freezing mid-step, because a frozen half-stride reads as a bug.
     */
    private void togglePlaying() {
        this.playing = !this.playing;
        if (!this.playing) {
            this.playedMillis = 0;
            this.playingSince = 0;
        }
        this.relayout.run();
    }

    /** Puts the view back where it started, whichever camera is looking. */
    private void recentre() {
        this.camera.recentre();
        this.gameCamera.recentre();
        this.relayout.run();
    }

    @Override
    public void draw(Paint paint) {
        draw(paint, 0.0F);
    }

    public void draw(Paint paint, float delta) {
        Canvas canvas = paint.canvas();
        this.gameCamera.poseInWorld(this.pose, this.playing, animationSeconds(paint.time()));
        SceneShot shot = shot(paint.time());

        // The one place in the editor that paints its own ground, and only because the
        // player asked for it: the backdrop chooser says panel or world, and a panel that
        // let the world through would be a chooser with one answer.
        if (!showsWorld()) {
            Surface.flat(canvas, this.x, this.viewport[1], this.width, this.viewport[3],
                    Palette.VOID);
        }

        switch (this.view) {
            case MODEL -> this.figure.draw(canvas, shot, paint.mouseX(), paint.mouseY(), delta);
            case TEXTURE -> drawTexture(canvas, this.x, this.viewport[1], this.width, this.viewport[3]);
            case BOTH -> {
                this.figure.draw(canvas, shot, paint.mouseX(), paint.mouseY(), delta);
                int half = this.width / 2;
                canvas.fill(this.x + half, this.viewport[1], 1, this.viewport[3], Palette.RULE);
                drawTexture(canvas, this.x + half, this.viewport[1], this.width - half, this.viewport[3]);
            }
        }

        // The view chooser is a tab bar, so it sits on a rule rather than floating over
        // the figure. Floating it was the site's idea and it cost more than it gave: a
        // strip with nothing under it reads as three loose boxes, and it was over the
        // one thing on this screen worth looking at.
        Surface.rule(canvas, this.x, this.y + Metrics.TAB_HEIGHT + Metrics.PAD_TIGHT, this.width);

        // The two corner boxes are backdrops, so they go down before what sits on
        // them. Drawing them afterwards is what left the dock looking like an empty
        // frame: the panel was covering its own button.
        drawDock(canvas);
        drawCorner(canvas);

        for (Element control : this.controls) {
            control.draw(paint);
        }
    }

    private SceneShot shot(long now) {
        return new SceneShot(this.cameraMode, this.pose, this.playing, animationSeconds(now),
                this.camera);
    }

    /**
     * Whether shift is down, which is the keyboard's way of asking to pan.
     *
     * <p>The one thing here that has to ask the game directly: a modifier is not part of
     * a mouse event. It tolerates there being no game, because the preview tool lays this
     * panel out without one — and a drag it never sends cannot want shift.
     */
    private boolean shiftHeld() {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.hasShiftDown();
    }

    /** How far into the animation we are: time spent playing, and none spent stopped. */
    private float animationSeconds(long now) {
        long played = this.playedMillis;
        if (this.playing) {
            if (this.playingSince == 0) {
                this.playingSince = now;
            }
            played += now - this.playingSince;
        }
        return played / 1000.0F;
    }

    /** The 64x64 sheet itself, on a transparency checker, at a whole scale. */
    private void drawTexture(Canvas canvas, int left, int top, int width, int height) {
        if (!this.preview.hasTexture()) {
            return;
        }
        int size = FrontSprite.SKIN_SIZE;
        int scale = Math.max(1, Math.min((width - Metrics.PAD * 2) / size,
                (height - Metrics.PAD * 2) / size));
        int drawnX = left + (width - size * scale) / 2;
        int drawnY = top + (height - size * scale) / 2;
        Surface.checker(canvas, drawnX, drawnY, size * scale, size * scale);
        Surface.slot(canvas, drawnX - Metrics.SLOT_INSET, drawnY - Metrics.SLOT_INSET,
                size * scale + Metrics.SLOT_INSET * 2, size * scale + Metrics.SLOT_INSET * 2);
        canvas.blit(this.preview.texture(), drawnX, drawnY, size * scale, size * scale,
                0, 0, size, size, size, size);
    }

    private void drawDock(Canvas canvas) {
        Surface.panel(canvas, this.dock[0], this.dock[1], this.dock[2], this.dock[3]);
    }

    /**
     * Bottom left: the preview label above the reminder of what the gestures are.
     *
     * <p>Both are laid straight on the scene, so the text is ringed in black on all
     * four sides. A shadow is enough over a flat panel and not enough over a figure.
     */
    private void drawCorner(Canvas canvas) {
        List<Component> lines = new ArrayList<>();
        Component hovered = this.hoveredLabel.get();
        if (hovered != null) {
            lines.add(hovered);
        }
        for (String key : gestureKeys()) {
            lines.add(Component.translatable(key));
        }

        int lineHeight = canvas.lineHeight() + 1;
        int boxHeight = lines.size() * lineHeight + Metrics.PANEL_INSET * 2 - 1;
        int boxWidth = 0;
        for (Component line : lines) {
            boxWidth = Math.max(boxWidth, canvas.textWidth(line));
        }
        boxWidth += Metrics.PANEL_INSET * 2;

        int boxX = this.x + Metrics.PAD_TIGHT;
        int boxY = this.y + this.height - Metrics.PAD_TIGHT - boxHeight;
        Surface.panel(canvas, boxX, boxY, boxWidth, boxHeight);

        for (int index = 0; index < lines.size(); index++) {
            boolean isLabel = hovered != null && index == 0;
            canvas.textRinged(lines.get(index), boxX + Metrics.PANEL_INSET,
                    boxY + Metrics.PANEL_INSET + index * lineHeight,
                    isLabel ? Palette.INK_HOVERED : Palette.INK_MUTED);
        }
    }

    /**
     * What the gestures are, for the camera that is looking.
     *
     * <p>The first-person view has none, and says so by listing none: the arm is fixed
     * to the camera, so turning the view would move the landscape behind it and not the
     * arm — the one thing being looked at.
     */
    private List<String> gestureKeys() {
        return switch (this.cameraMode) {
            case WORKSHOP -> List.of("gesture.mcskincreator.turn", "gesture.mcskincreator.pan",
                    "gesture.mcskincreator.zoom");
            case IN_GAME -> List.of("gesture.mcskincreator.orbit");
            case FIRST_PERSON -> List.of("gesture.mcskincreator.first_person");
        };
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY) || this.cameraMode == CameraMode.FIRST_PERSON) {
            return false;
        }
        if (button != 0 && button != 1) {
            return false;
        }
        this.dragging = true;
        // The right button pans, and so does shift with the left: the site offers both
        // because a trackpad has no comfortable right-drag.
        this.panning = button == 1 || shiftHeld();
        return true;
    }

    @Override
    public void mouseDrag(double mouseX, double mouseY, double dragX, double dragY, int button) {
        if (!this.dragging) {
            return;
        }
        // The figure turns by how far the mouse moved, so the deltas are what matter
        // here rather than where the pointer ended up.
        if (this.cameraMode == CameraMode.IN_GAME) {
            // Turning the character, not a camera: see GameCamera for why that is the
            // only way round them.
            this.gameCamera.turn(this.cameraMode, dragX, dragY);
        } else if (this.panning) {
            this.camera.pan(dragX, dragY);
        } else {
            this.camera.turn(dragX, dragY);
        }
    }

    @Override
    public void mouseUp(double mouseX, double mouseY, int button) {
        this.dragging = false;
        this.panning = false;
    }

    @Override
    public boolean scroll(double mouseX, double mouseY, double amount) {
        // The game fixes its own third-person distance, so there is nothing to zoom
        // under either camera that lets the game draw.
        if (!contains(mouseX, mouseY) || this.cameraMode.overlaysGame()) {
            return false;
        }
        this.camera.zoom(amount);
        return true;
    }
}
