package com.gtnewhorizons.dashboard.client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.client.BarSlot.Layer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Bar looks from resource packs, drawn instead of vanilla's icons. Each theme is a sheet listed in themes.json. */
@SideOnly(Side.CLIENT)
public final class BarThemes {

    public static final float PREVIEW_FILL = 2 / 3f;
    public static final int PADDING = 8;

    private static final int SHEET_SIZE = 512;
    private static final String FOLDER = "textures/gui/";
    private static final String CROSSHAIRS_KEY = "crosshairs";
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private static final Map<String, String> THEMES = new LinkedHashMap<>();
    private static final List<String> THEME_IDS = new ArrayList<>();

    private BarThemes() {}

    /** Themes whose texture exists, in the order they were listed. */
    public static List<String> getThemes() {
        return Collections.unmodifiableList(THEME_IDS);
    }

    public static boolean isAvailable(String theme) {
        return THEMES.containsKey(theme);
    }

    public static String getDisplayName(String theme) {
        return THEMES.getOrDefault(theme, theme);
    }

    /** Called again whenever resource packs change. */
    public static void load(IResourceManager resources) {
        THEMES.clear();
        Crosshairs.setStyleCount(0);
        List<?> lists;
        try {
            lists = resources.getAllResources(new ResourceLocation(Dashboard.MODID, FOLDER + "themes.json"));
        } catch (IOException e) {
            lists = Collections.emptyList();
        }
        for (Object entry : lists) {
            IResource list = (IResource) entry;
            try (Reader reader = new InputStreamReader(list.getInputStream(), StandardCharsets.UTF_8)) {
                for (Map.Entry<String, JsonElement> theme : new JsonParser().parse(reader)
                    .getAsJsonObject()
                    .entrySet()) {
                    // Not a theme: how many crosshairs crosshairs.png holds
                    if (theme.getKey()
                        .equals(CROSSHAIRS_KEY)) {
                        Crosshairs.setStyleCount(
                            theme.getValue()
                                .getAsInt());
                    } else if (ResourceUtil.resourceExists(getTexture(theme.getKey()))) {
                        THEMES.put(
                            theme.getKey(),
                            theme.getValue()
                                .getAsString());
                    } else {
                        Dashboard.LOG.warn("Bar theme {} has no texture, skipping it", theme.getKey());
                    }
                }
            } catch (IOException | RuntimeException e) {
                Dashboard.LOG.error("Could not read a bar themes.json", e);
            }
        }
        THEME_IDS.clear();
        THEME_IDS.addAll(THEMES.keySet());
    }

    private static ResourceLocation getTexture(String theme) {
        return TEXTURES.computeIfAbsent(theme, id -> new ResourceLocation(Dashboard.MODID, FOLDER + id + ".png"));
    }

    /** Vanilla's side for the bar, unless the player flipped it. */
    public static boolean fillsFromRight(HudElement element, BarSlot slot) {
        return slot.fromRight != HudLayout.isFlipped(element);
    }

    /** The empty bar, the fill, then one ornament: the active one while the bar is full. */
    public static void drawBar(String theme, BarSlot slot, int x, int y, float fraction, boolean fromRight) {
        drawWhole(theme, slot, Layer.EMPTY, x, y);
        drawFillRange(theme, slot, x, y, 0, fraction, fromRight);
        drawOrnament(theme, slot, x, y, isFull(slot, fraction));
    }

    /**
     * Health in its state, with the flash row and the health just lost while flashing (flash null when not), and the
     * hardcore overlay on top. In hardcore, the hardcore ornament replaces the state's.
     */
    public static void drawHealth(String theme, BarSlot state, BarSlot flash, int x, int y, float fill, float lastFill,
        boolean hardcore, boolean fromRight) {
        drawWhole(theme, flash != null ? BarSlot.HEALTH_FLASH : state, Layer.EMPTY, x, y);
        if (hardcore) {
            drawWhole(theme, BarSlot.HEALTH_HARDCORE, Layer.EMPTY, x, y);
        }
        if (flash != null) {
            drawFillRange(theme, flash, x, y, fill, lastFill, fromRight);
        }
        drawFillRange(theme, state, x, y, 0, fill, fromRight);
        if (hardcore) {
            drawFillRange(theme, BarSlot.HEALTH_HARDCORE, x, y, 0, fill, fromRight);
        }
        drawOrnament(theme, hardcore ? BarSlot.HEALTH_HARDCORE : state, x, y, isFull(state, fill));
    }

    /** A layer drawn with its 8 px space, like the empty bar or a single piece. */
    public static void drawWhole(String theme, BarSlot slot, Layer layer, int x, int y) {
        bindTexture(theme);
        drawFromSheet(
            x - PADDING,
            y - PADDING,
            slot.getU(layer),
            slot.getV(layer),
            slot.width + PADDING * 2,
            slot.height + PADDING * 2);
    }

    public static void drawOrnament(String theme, BarSlot slot, int x, int y, boolean active) {
        drawWhole(theme, slot, active ? Layer.ORNAMENT_ACTIVE : Layer.ORNAMENT, x, y);
    }

    /** Full once the fill covers every pixel of the bar. */
    public static boolean isFull(BarSlot slot, float fraction) {
        return Math.round(MathHelper.clamp_float(fraction, 0, 1) * slot.width) >= slot.width;
    }

    /** Overlays like the regen shine sit on a health bar, since on their own they would mean nothing. */
    public static void drawSample(String theme, BarSlot slot, int x, int y, boolean fromRight) {
        switch (slot) {
            case HEALTH_REGEN -> {
                drawBar(theme, BarSlot.HEALTH, x, y, PREVIEW_FILL, fromRight);
                drawRegenShine(theme, x, y, getSampleRegenStep(), fromRight);
            }
            case HEALTH_HARDCORE -> drawHealth(theme, BarSlot.HEALTH, null, x, y, PREVIEW_FILL, 0, true, fromRight);
            case HEALTH_FLASH -> drawFlashSample(theme, BarSlot.HEALTH, x, y, fromRight);
            case HEALTH_FLASH_POISONED -> drawFlashSample(theme, BarSlot.HEALTH_POISONED, x, y, fromRight);
            case HEALTH_FLASH_WITHERED -> drawFlashSample(theme, BarSlot.HEALTH_WITHERED, x, y, fromRight);
            case AIR_POP -> {
                drawBar(theme, BarSlot.AIR, x, y, PREVIEW_FILL, fromRight);
                drawAirPop(theme, x, y, PREVIEW_FILL, fromRight);
            }
            case FOOD_SATURATION, FOOD_EXHAUSTION -> {
                drawBar(theme, BarSlot.FOOD, x, y, PREVIEW_FILL, fromRight);
                drawFillRange(theme, slot, x, y, 0, PREVIEW_FILL / 2, fromRight);
            }
            default -> drawBar(theme, slot, x, y, PREVIEW_FILL, fromRight);
        }
    }

    /** The rest of the bar shows as just lost. */
    private static void drawFlashSample(String theme, BarSlot state, int x, int y, boolean fromRight) {
        drawHealth(theme, state, state.getFlashFill(), x, y, PREVIEW_FILL, 1, false, fromRight);
    }

    /** From the clock, since samples have no game ticks. */
    public static int getSampleRegenStep() {
        return getRegenStep((int) (System.currentTimeMillis() / 50 % 100_000));
    }

    /** Same timing as vanilla's regen heart wave. */
    public static int getRegenStep(int ticks) {
        int step = ticks % 25;
        return step < 10 ? step : -1;
    }

    public static void drawRegenShine(String theme, int barX, int barY, int step, boolean fromRight) {
        if (step < 0) {
            return;
        }
        int offset = step * 8;
        int x = fromRight ? barX + BarSlot.HEALTH.width - BarSlot.HEALTH_REGEN.width - offset : barX + offset;
        drawWhole(theme, BarSlot.HEALTH_REGEN, Layer.EMPTY, x, barY);
    }

    public static void drawAirPop(String theme, int barX, int barY, float fill, boolean fromRight) {
        int width = BarSlot.AIR.width;
        int pop = BarSlot.AIR_POP.width;
        int edge = Math.min(Math.round(MathHelper.clamp_float(fill, 0, 1) * width), width - pop);
        int x = fromRight ? barX + width - edge - pop : barX + edge;
        drawWhole(theme, BarSlot.AIR_POP, Layer.EMPTY, x, barY);
    }

    /** Only part of the fill, like the health lost in a damage flash. */
    public static void drawFillRange(String theme, BarSlot slot, int x, int y, float from, float to,
        boolean fromRight) {
        drawFillRange(theme, slot, x, y, from, to, fromRight, 1);
    }

    /** See-through, like the blinking held food preview. */
    public static void drawFillRange(String theme, BarSlot slot, int x, int y, float from, float to, boolean fromRight,
        float alpha) {
        bindTexture(theme);
        GL11.glColor4f(1, 1, 1, alpha);
        drawFill(slot, x, y, from, to, fromRight);
        GL11.glColor4f(1, 1, 1, 1);
    }

    private static void drawFill(BarSlot slot, int x, int y, float from, float to, boolean fromRight) {
        int start = Math.round(MathHelper.clamp_float(from, 0, 1) * slot.width);
        int end = Math.round(MathHelper.clamp_float(to, 0, 1) * slot.width);
        if (end <= start) {
            return;
        }
        int offset = fromRight ? slot.width - end : start;
        drawFromSheet(
            x + offset,
            y,
            slot.getU(Layer.FULL) + PADDING + offset,
            slot.getV(Layer.FULL) + PADDING,
            end - start,
            slot.height);
    }

    private static void drawFromSheet(int x, int y, int u, int v, int width, int height) {
        Gui.func_146110_a(x, y, u, v, width, height, SHEET_SIZE, SHEET_SIZE);
    }

    private static void bindTexture(String theme) {
        PreviewDrawing.bindTexture(getTexture(theme));
    }
}
