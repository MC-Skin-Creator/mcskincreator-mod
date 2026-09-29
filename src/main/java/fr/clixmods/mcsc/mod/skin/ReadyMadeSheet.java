/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;

/**
 * A sheet of ready-made pictures, drawn off the client thread.
 *
 * <p>Blending a few hundred stacks and projecting them is tens of milliseconds at the
 * least, and it was done on the client thread every time an atlas landed — which is
 * dozens of times in a row when the models window opens, since that window fetches
 * every category at once. This moves the drawing to a thread of its own and leaves
 * the client thread the one thing only it may do, the upload.
 *
 * <p>At most one drawing at a time. A change that arrives while one is under way is
 * held, and only the latest such change is drawn next; the one under way is still
 * shown when it finishes, since it is newer than what is on screen.
 *
 * <p>Only the client thread calls in, and every callback comes back on it.
 */
public final class ReadyMadeSheet implements AutoCloseable {
    /**
     * One thread for every sheet: they are drawn now and then, not continuously, and
     * a daemon so a game shutting down mid-drawing does not wait for it.
     */
    private static final ExecutorService DRAWING = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "mcskincreator-ready-made");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private final String purpose;
    private final Executor clientThread;
    private final ReadyMadeSkins blends = new ReadyMadeSkins();

    /** What the latest drawing asked for was drawn from; -1 before the first. */
    private int stamp = -1;
    private boolean drawing;
    private Runnable queued;
    private boolean closed;

    /**
     * @param purpose      names the texture, for logs and crash reports
     * @param clientThread where the finished sheet is uploaded: the game itself
     */
    public ReadyMadeSheet(String purpose, Executor clientThread) {
        this.purpose = purpose;
        this.clientThread = clientThread;
    }

    /** Whether a drawing from {@code stamp} has not been asked for yet. */
    public boolean stale(int stamp) {
        return !this.closed && stamp != this.stamp;
    }

    /**
     * Asks for the sheet to be drawn again.
     *
     * @param compose the skins to draw, given this sheet's own memory of blends. Runs
     *                on the drawing thread, so everything it reads has to be a
     *                snapshot taken before the call — never a live map of the screen.
     * @param onReady handed the new sheet, on the client thread; it owns it from then
     *                on, and closing the previous one is its business
     */
    public void redraw(int stamp, Function<ReadyMadeSkins, List<byte[]>> compose,
                       Consumer<CategorySprites> onReady) {
        this.stamp = stamp;
        Runnable job = () -> start(compose, onReady);
        if (this.drawing) {
            this.queued = job;
        } else {
            job.run();
        }
    }

    private void start(Function<ReadyMadeSkins, List<byte[]>> compose, Consumer<CategorySprites> onReady) {
        this.drawing = true;
        CompletableFuture.supplyAsync(() -> CategorySprites.prepare(compose.apply(this.blends)), DRAWING)
                .whenComplete((prepared, failure) -> this.clientThread.execute(() -> {
                    this.drawing = false;
                    if (this.closed) {
                        return;
                    }
                    if (failure != null) {
                        MCSkinCreatorClient.LOGGER.warn("Drawing the {} sheet failed", this.purpose, failure);
                    } else {
                        onReady.accept(prepared.upload(this.purpose));
                    }
                    Runnable next = this.queued;
                    this.queued = null;
                    if (next != null) {
                        next.run();
                    }
                }));
    }

    /**
     * Drops whatever is queued, and a drawing under way lands nowhere. The sheets
     * already handed over belong to whoever took them.
     */
    @Override
    public void close() {
        this.closed = true;
        this.queued = null;
    }
}
