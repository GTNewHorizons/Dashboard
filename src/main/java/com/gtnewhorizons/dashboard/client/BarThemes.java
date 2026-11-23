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

import net.minecraft.client.renderer.Tessellator;
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

    /**
     * The empty bar, the fill, then one decoration: the active one while the bar is full. Sheets are painted filling
     * from the left, so a bar filling from the right is drawn mirrored.
     */
    public static void drawBar(String theme, BarSlot slot, int x, int y, float fraction, boolean fromRight) {
        drawBarBase(theme, slot, x, y, fraction, fromRight);
        drawDecoration(theme, slot, x, y, isFull(slot, fraction), fromRight);
    }

    public static void drawBarBase(String theme, BarSlot slot, int x, int y, float fraction, boolean fromRight) {
        drawWhole(theme, slot, Layer.EMPTY, x, y, fromRight);
        drawFillRange(theme, slot, x, y, 0, fraction, fromRight);
    }

    /**
     * Health in its state, with the flash row and the health just lost while flashing (flash null when not), the
     * hardcore overlay and the regen shine (step -1 for none). In hardcore, the hardcore decoration replaces the
     * state's.
     */
    public static void drawHealth(String theme, BarSlot state, BarSlot flash, int x, int y, float fill, float lastFill,
        boolean hardcore, int regenStep, boolean fromRight) {
        drawWhole(theme, flash != null ? BarSlot.HEALTH_FLASH : state, Layer.EMPTY, x, y, fromRight);
        if (hardcore) {
            drawWhole(theme, BarSlot.HEALTH_HARDCORE, Layer.EMPTY, x, y, fromRight);
        }
        if (flash != null) {
            drawFillRange(theme, flash, x, y, fill, lastFill, fromRight);
        }
        drawFillRange(theme, state, x, y, 0, fill, fromRight);
        if (hardcore) {
            drawFillRange(theme, BarSlot.HEALTH_HARDCORE, x, y, 0, fill, fromRight);
        }
        drawRegenShine(theme, x, y, regenStep, fromRight);
        drawDecoration(theme, hardcore ? BarSlot.HEALTH_HARDCORE : state, x, y, isFull(state, fill), fromRight);
    }

    /** A layer drawn with its 8 px space, like the empty bar or a single piece. */
    public static void drawWhole(String theme, BarSlot slot, Layer layer, int x, int y, boolean mirrored) {
        bindTexture(theme);
        drawFromSheet(
            x - PADDING,
            y - PADDING,
            slot.getU(layer),
            slot.getV(layer),
            slot.width + PADDING * 2,
            slot.height + PADDING * 2,
            mirrored);
    }

    public static void drawDecoration(String theme, BarSlot slot, int x, int y, boolean active, boolean mirrored) {
        drawWhole(theme, slot, active ? Layer.DECORATION_ACTIVE : Layer.DECORATION, x, y, mirrored);
    }

    /** Full once the fill covers every pixel of the bar. */
    public static boolean isFull(BarSlot slot, float fraction) {
        return Math.round(MathHelper.clamp_float(fraction, 0, 1) * slot.width) >= slot.width;
    }

    /** Overlays like the regen shine sit on a health bar, since on their own they would mean nothing. */
    public static void drawSample(String theme, BarSlot slot, int x, int y, boolean fromRight) {
        switch (slot) {
            case HEALTH_REGEN -> drawHealth(
                theme,
                BarSlot.HEALTH,
                null,
                x,
                y,
                PREVIEW_FILL,
                0,
                false,
                getSampleRegenStep(),
                fromRight);
            case HEALTH_HARDCORE -> drawHealth(theme, BarSlot.HEALTH, null, x, y, PREVIEW_FILL, 0, true, -1, fromRight);
            case HEALTH_FLASH -> drawFlashSample(theme, BarSlot.HEALTH, x, y, fromRight);
            case HEALTH_FLASH_POISONED -> drawFlashSample(theme, BarSlot.HEALTH_POISONED, x, y, fromRight);
            case HEALTH_FLASH_WITHERED -> drawFlashSample(theme, BarSlot.HEALTH_WITHERED, x, y, fromRight);
            case AIR_POP -> {
                drawBarBase(theme, BarSlot.AIR, x, y, PREVIEW_FILL, fromRight);
                drawAirPop(theme, x, y, PREVIEW_FILL, fromRight);
                drawDecoration(theme, BarSlot.AIR, x, y, false, fromRight);
            }
            case FOOD_SATURATION, FOOD_EXHAUSTION -> {
                drawBarBase(theme, BarSlot.FOOD, x, y, PREVIEW_FILL, fromRight);
                drawFillRange(theme, slot, x, y, 0, PREVIEW_FILL / 2, fromRight);
                drawDecoration(theme, BarSlot.FOOD, x, y, false, fromRight);
            }
            default -> drawBar(theme, slot, x, y, PREVIEW_FILL, fromRight);
        }
    }

    /** The rest of the bar shows as just lost. */
    private static void drawFlashSample(String theme, BarSlot state, int x, int y, boolean fromRight) {
        drawHealth(theme, state, state.getFlashFill(), x, y, PREVIEW_FILL, 1, false, -1, fromRight);
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
        drawWhole(theme, BarSlot.HEALTH_REGEN, Layer.EMPTY, x, barY, fromRight);
    }

    public static void drawAirPop(String theme, int barX, int barY, float fill, boolean fromRight) {
        int width = BarSlot.AIR.width;
        int pop = BarSlot.AIR_POP.width;
        int edge = Math.min(Math.round(MathHelper.clamp_float(fill, 0, 1) * width), width - pop);
        int x = fromRight ? barX + width - edge - pop : barX + edge;
        drawWhole(theme, BarSlot.AIR_POP, Layer.EMPTY, x, barY, fromRight);
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
        drawFromSheet(
            x + (fromRight ? slot.width - end : start),
            y,
            slot.getU(Layer.FULL) + PADDING + start,
            slot.getV(Layer.FULL) + PADDING,
            end - start,
            slot.height,
            fromRight);
    }

    /** Like Gui.func_146110_a, which can not mirror. */
    private static void drawFromSheet(int x, int y, int u, int v, int width, int height, boolean mirrored) {
        float left = (float) u / SHEET_SIZE;
        float right = (float) (u + width) / SHEET_SIZE;
        float top = (float) v / SHEET_SIZE;
        float bottom = (float) (v + height) / SHEET_SIZE;
        if (mirrored) {
            float swap = left;
            left = right;
            right = swap;
        }
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, 0, left, bottom);
        tessellator.addVertexWithUV(x + width, y + height, 0, right, bottom);
        tessellator.addVertexWithUV(x + width, y, 0, right, top);
        tessellator.addVertexWithUV(x, y, 0, left, top);
        tessellator.draw();
    }

    private static void bindTexture(String theme) {
        PreviewDrawing.bindTexture(getTexture(theme));
    }
}
