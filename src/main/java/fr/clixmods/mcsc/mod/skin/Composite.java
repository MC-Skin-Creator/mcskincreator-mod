/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import fr.clixmods.mcsc.engine.ColorKey;
import fr.clixmods.mcsc.engine.Composition;
import fr.clixmods.mcsc.engine.Preset;
import fr.clixmods.mcsc.engine.PresetSource;
import fr.clixmods.mcsc.engine.TextureEngine;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.catalog.Rgb;
import fr.clixmods.mcsc.mod.project.Layer;
import fr.clixmods.mcsc.mod.project.SkinProject;

/**
 * Several 64x64 skin buffers stacked into one, bottom of the list first.
 *
 * <p>The stacking itself is {@code fr.clixmods.mcsc:mcsc-engine}, the library the
 * site's own composition was taken out into, so this is a translation between two
 * ways of writing a buffer and nothing more. It used to be a copy of the blend,
 * rounding halves to even the way a {@code Uint8ClampedArray} does — right, and one
 * more thing to keep right. There were three implementations of this calculation
 * held together by tests comparing them byte for byte; a fourth living here would
 * have been a fourth to watch.
 *
 * <p>Two things are stacked, and they are not the same thing. The editor's project
 * carries per-key colours, an opacity and hue/saturation/brightness per layer, which
 * the library applies; a ready-made stack is plain buffers, one on top of the other,
 * recoloured where the catalogue says so, and is
 * composed here rather than asked for — the catalogue offers two hundred and odd of
 * them, and two hundred requests to fill one panel is not a thing to do to the
 * service or to somebody waiting on it.
 */
public final class Composite {
    /** Bytes of one RGBA skin buffer. */
    public static final int BYTES = Composition.BYTES;

    /**
     * The library's recolouring, which lives on an engine because it caches each
     * element's decoded zone map. The pixels are always handed to it, so the source it
     * would otherwise read them from is never asked.
     */
    private static final TextureEngine ENGINE = new TextureEngine(new PresetSource() {
        @Override
        public Optional<Preset> preset(String cat, String id) {
            return Optional.empty();
        }

        @Override
        public Optional<int[]> pixels(Preset preset, boolean slim) {
            return Optional.empty();
        }
    });

    /**
     * Where an element's pixels come from: its rank in its category's atlas, which is
     * what the screen already holds. Null for a category still on its way.
     */
    @FunctionalInterface
    public interface Pixels {
        byte[] of(String categoryId, int atlasIndex);
    }

    private Composite() {
    }

    /**
     * @param buffers the layers, bottom first; a null or short one is skipped rather
     *                than throwing, since a category can still be on its way
     * @return a fresh buffer, transparent where nothing was stacked
     */
    public static byte[] of(List<byte[]> buffers) {
        List<Composition.ComposedLayer> stack = new ArrayList<>(buffers.size());
        for (byte[] source : buffers) {
            if (source == null || source.length < BYTES) {
                continue;
            }
            stack.add(new Composition.ComposedLayer(true, 1, toInts(source)));
        }
        return toBytes(Composition.composite(stack));
    }

    /**
     * The edited project: its visible layers, bottom to top, each with its opacity
     * and its adjustments.
     *
     * <p>A layer whose atlas is still in flight is left out rather than waited for —
     * the rest of the stack is on the model meanwhile, and the screen composes again
     * when the pixels land.
     */
    public static byte[] of(SkinProject project, Pixels pixels) {
        List<Composition.ComposedLayer> stack = new ArrayList<>(project.layers().size());
        for (Layer layer : project.layers()) {
            if (!layer.visible()) {
                continue;
            }
            byte[] buffer = pixels.of(layer.categoryId(), layer.atlasIndex(project.isSlim()));
            if (buffer == null || buffer.length < BYTES) {
                continue;
            }
            // Recoloured first, then shifted: the order the library's own project
            // composition applies them in, and so the order the site does.
            int[] recolored = recolor(layer.categoryId(), layer.item(), layer.changedColors(),
                    project.isSlim(), toInts(buffer));
            int[] adjusted = Composition.adjustBuffer(recolored, adjustments(layer));
            stack.add(new Composition.ComposedLayer(true, layer.opacity() / 100.0, adjusted));
        }
        return toBytes(Composition.composite(stack));
    }

    /**
     * One element's buffer with some of its colour keys moved, bytes in and bytes out:
     * what a ready-made piece with a colour override needs before it is stacked.
     */
    public static byte[] recolor(String categoryId, CatalogItem item, Map<String, Integer> colors,
                                 boolean slim, byte[] buffer) {
        return colors.isEmpty() ? buffer : toBytes(recolor(categoryId, item, colors, slim, toInts(buffer)));
    }

    /**
     * Moves each key the library is told about by the HSL distance between the
     * element's own colour for that key and the one chosen, pixel by pixel through the
     * element's zone map — which is what keeps its shading and its grain.
     */
    private static int[] recolor(String categoryId, CatalogItem item, Map<String, Integer> colors,
                                 boolean slim, int[] buffer) {
        if (colors.isEmpty()) {
            return buffer;
        }
        List<ColorKey> keys = new ArrayList<>(item.colors().size());
        item.colors().forEach((key, rgb) -> keys.add(new ColorKey(key, Rgb.format(rgb))));
        Map<String, String> wanted = new LinkedHashMap<>();
        colors.forEach((key, rgb) -> wanted.put(key, Rgb.format(rgb)));
        Preset element = new Preset(categoryId, item.id(), keys, item.hasSlim(),
                item.colorMap(), item.slimColorMap());
        return ENGINE.applyColors(element, buffer, wanted, slim);
    }

    /**
     * A layer's adjustments in the units the library works in, which are the sliders'
     * divided by a hundred — the percentages are the interface's way of saying them,
     * and {@code ProjectJson} converts them the same way for the server. Hue is the
     * exception: degrees on both sides.
     *
     * @return null when nothing was moved, which the library reads as "leave it
     *         alone" and answers without walking the buffer
     */
    private static Composition.Adjustments adjustments(Layer layer) {
        if (layer.isUnadjusted()) {
            return null;
        }
        return new Composition.Adjustments(
                layer.hue(), layer.saturation() / 100.0, layer.brightness() / 100.0);
    }

    /**
     * The library reads a buffer as ints of 0 to 255, the way JavaScript hands it a
     * {@code Uint8ClampedArray}; atlases arrive as bytes, which are signed.
     */
    private static int[] toInts(byte[] buffer) {
        int[] values = new int[BYTES];
        for (int index = 0; index < BYTES; index++) {
            values[index] = buffer[index] & 0xFF;
        }
        return values;
    }

    private static byte[] toBytes(int[] values) {
        byte[] buffer = new byte[values.length];
        for (int index = 0; index < values.length; index++) {
            buffer[index] = (byte) values[index];
        }
        return buffer;
    }
}
