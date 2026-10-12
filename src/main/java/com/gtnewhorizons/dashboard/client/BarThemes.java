package com.gtnewhorizons.dashboard.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudElement;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class BarThemes {

    public static final List<String> THEMES = Collections.unmodifiableList(Arrays.asList("test"));
    public static final float PREVIEW_FILL = 2 / 3f;
    public static final int PADDING = 8;

    private static final int SHEET_SIZE = 512;
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

    private BarThemes() {}

    public static String getDisplayName(String theme) {
        return StatCollector.translateToLocal("dashboard.theme." + theme);
    }

    /** The slot's vanilla side. */
    public static boolean fillsFromRight(HudElement element, BarSlot slot) {
        return slot.fromRight != HudLayout.isFlipped(element);
    }

    public static void drawBar(String theme, BarSlot slot, int x, int y, float fraction, boolean fromRight) {
        drawEmpty(theme, slot, x, y);
        drawFill(slot, x, y, 0, fraction, fromRight);
    }

    public static void drawEmpty(String theme, BarSlot slot, int x, int y) {
        bindTexture(theme);
        drawFromSheet(
            x - PADDING,
            y - PADDING,
            slot.emptyU,
            slot.emptyV,
            slot.width + PADDING * 2,
            slot.height + PADDING * 2);
    }

    /** Two thirds full. Overlays like the regen shine are shown over a health bar. */
    public static void drawSample(String theme, BarSlot slot, int x, int y, boolean fromRight) {
        switch (slot) {
            case HEALTH_REGEN -> {
                drawBar(theme, BarSlot.HEALTH, x, y, PREVIEW_FILL, fromRight);
                drawRegenShine(theme, x, y, getSampleRegenStep(), fromRight);
            }
            case HEALTH_HARDCORE -> {
                drawBar(theme, BarSlot.HEALTH, x, y, PREVIEW_FILL, fromRight);
                drawEmpty(theme, BarSlot.HEALTH_HARDCORE, x, y);
            }
            case AIR_POP -> {
                drawBar(theme, BarSlot.AIR, x, y, PREVIEW_FILL, fromRight);
                drawAirPop(theme, x, y, PREVIEW_FILL, fromRight);
            }
            default -> drawBar(theme, slot, x, y, PREVIEW_FILL, fromRight);
        }
    }

    /** From the clock, since samples have no game ticks. */
    public static int getSampleRegenStep() {
        return getRegenStep((int) (System.currentTimeMillis() / 50 % 100_000));
    }

    /** Vanilla's regen heart wave. */
    public static int getRegenStep(int ticks) {
        int step = ticks % 25;
        return step < 10 ? step : -1;
    }

    /** Over the health bar at barX, barY. */
    public static void drawRegenShine(String theme, int barX, int barY, int step, boolean fromRight) {
        if (step < 0) {
            return;
        }
        int offset = step * 8;
        int x = fromRight ? barX + BarSlot.HEALTH.width - BarSlot.HEALTH_REGEN.width - offset : barX + offset;
        drawEmpty(theme, BarSlot.HEALTH_REGEN, x, barY);
    }

    public static void drawAirPop(String theme, int barX, int barY, float fill, boolean fromRight) {
        int width = BarSlot.AIR.width;
        int pop = BarSlot.AIR_POP.width;
        int edge = Math.min(Math.round(clamp(fill) * width), width - pop);
        int x = fromRight ? barX + width - edge - pop : barX + edge;
        drawEmpty(theme, BarSlot.AIR_POP, x, barY);
    }

    /** Only the stretch between two fractions. */
    public static void drawFillRange(String theme, BarSlot slot, int x, int y, float from, float to,
        boolean fromRight) {
        bindTexture(theme);
        drawFill(slot, x, y, from, to, fromRight);
    }

    private static void drawFill(BarSlot slot, int x, int y, float from, float to, boolean fromRight) {
        int start = Math.round(clamp(from) * slot.width);
        int end = Math.round(clamp(to) * slot.width);
        if (end <= start) {
            return;
        }
        int offset = fromRight ? slot.width - end : start;
        drawFromSheet(x + offset, y, slot.fullU + PADDING + offset, slot.fullV + PADDING, end - start, slot.height);
    }

    private static float clamp(float fraction) {
        return Math.max(0, Math.min(1, fraction));
    }

    private static void drawFromSheet(int x, int y, int u, int v, int width, int height) {
        Gui.func_146110_a(x, y, u, v, width, height, SHEET_SIZE, SHEET_SIZE);
    }

    private static void bindTexture(String theme) {
        ResourceLocation texture = TEXTURES
            .computeIfAbsent(theme, id -> new ResourceLocation(Dashboard.MODID, "textures/gui/bars/" + id + ".png"));
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1, 1, 1, 1);
    }
}
