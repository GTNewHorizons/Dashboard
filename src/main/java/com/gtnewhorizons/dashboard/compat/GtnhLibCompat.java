package com.gtnewhorizons.dashboard.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.IChatComponent;

import com.gtnewhorizon.gtnhlib.GTNHLibConfig;
import com.gtnewhorizon.gtnhlib.client.title.TitleAPI;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.api.HudShift;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * GTNHLib's HUD that other GTNH mods use: short messages above the hotbar, and big titles in the middle of the screen
 * with their subtitle, icon, particles and effects, which all move together.
 */
@SideOnly(Side.CLIENT)
public final class GtnhLibCompat {

    private static final int HOTBAR_WIDTH = 182;
    private static final int TEXT_HEIGHT = 8;
    /** GTNHLib draws the title 10 text pixels above its spot and the subtitle 5 below, each at its own size. */
    private static final int TITLE_TOP = -10;
    private static final int SUBTITLE_TOP = 5;
    private static final String SAMPLE_TITLE = "Title";
    private static final String SAMPLE_SUBTITLE = "Subtitle";
    private static final String SAMPLE_TEXT = "Text above the hotbar";

    public static final HudElement ABOVE_HOTBAR_TEXT = new HudElement(
        "gtnhlib:above_hotbar_text",
        "dashboard.element.gtnhlib.above_hotbar_text",
        (screenWidth, screenHeight) -> new HudBounds(
            screenWidth / 2 - HOTBAR_WIDTH / 2,
            screenHeight - 70,
            HOTBAR_WIDTH,
            TEXT_HEIGHT)).setDescription("dashboard.element.gtnhlib.above_hotbar_text.description");

    public static final HudElement TITLE = new HudElement(
        "gtnhlib:title",
        "dashboard.element.gtnhlib.title",
        GtnhLibCompat::getTitleBounds).setOwnSettings("dashboard.element.gtnhlib.title.own_settings");

    private static final HudShift SHIFT = new HudShift();

    private GtnhLibCompat() {}

    public static void register() {
        PreviewDrawing.setImitationInBounds(ABOVE_HOTBAR_TEXT, GtnhLibCompat::drawAboveHotbarText);
        PreviewDrawing.setImitation(TITLE, GtnhLibCompat::drawTitle);
        HudRegistry.register(ABOVE_HOTBAR_TEXT);
        HudRegistry.register(TITLE);
    }

    /** Called by the mixins right before drawing. False means hidden, so the drawing should be skipped. */
    public static boolean begin(HudElement element) {
        return SHIFT.begin(element);
    }

    public static void end() {
        SHIFT.end();
    }

    /** As wide as the current title or subtitle, or the sample, and from the title's top to the subtitle's bottom. */
    private static HudBounds getTitleBounds(int screenWidth, int screenHeight) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        float titleScale = getTitleScale();
        float subtitleScale = getSubtitleScale();
        int width = Math.round(
            Math.max(
                font.getStringWidth(getTitleText()) * titleScale,
                font.getStringWidth(getSubtitleText()) * subtitleScale));
        int top = Math.round(TITLE_TOP * titleScale);
        int bottom = Math.round((SUBTITLE_TOP + TEXT_HEIGHT) * subtitleScale);
        return new HudBounds(
            getTitleCenterX(screenWidth) - width / 2,
            getTitleCenterY(screenHeight) + top,
            width,
            bottom - top);
    }

    /** Like GTNHLib: what the current title asks for, else GTNHLib's config. */
    private static float getTitleScale() {
        return TitleAPI.getTitleScale() > 0 ? TitleAPI.getTitleScale() : GTNHLibConfig.titleScale;
    }

    private static float getSubtitleScale() {
        return TitleAPI.getSubtitleScale() > 0 ? TitleAPI.getSubtitleScale() : GTNHLibConfig.subtitleScale;
    }

    private static int getTitleCenterX(int screenWidth) {
        int offset = TitleAPI.getTitleOffsetX();
        return screenWidth / 2 + (offset != Integer.MIN_VALUE ? offset : GTNHLibConfig.titleOffsetX);
    }

    private static int getTitleCenterY(int screenHeight) {
        int offset = TitleAPI.getTitleOffsetY();
        return screenHeight / 2 + (offset != Integer.MIN_VALUE ? offset : GTNHLibConfig.titleOffsetY);
    }

    private static String getTitleText() {
        IChatComponent title = TitleAPI.getTitle();
        return title != null ? title.getFormattedText() : SAMPLE_TITLE;
    }

    private static String getSubtitleText() {
        IChatComponent subtitle = TitleAPI.getSubtitle();
        return subtitle != null ? subtitle.getFormattedText() : SAMPLE_SUBTITLE;
    }

    private static void drawAboveHotbarText(HudBounds bounds) {
        int width = Minecraft.getMinecraft().fontRenderer.getStringWidth(SAMPLE_TEXT);
        PreviewDrawing.drawScaledString(SAMPLE_TEXT, bounds.x + (bounds.width - width) / 2f, bounds.y, 1, 0xFFFFFF);
    }

    /** The sample title and subtitle, centered on the title's spot at GTNHLib's sizes. */
    private static void drawTitle(int screenWidth, int screenHeight) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int centerX = getTitleCenterX(screenWidth);
        int centerY = getTitleCenterY(screenHeight);
        float titleScale = getTitleScale();
        float subtitleScale = getSubtitleScale();
        PreviewDrawing.drawScaledString(
            SAMPLE_TITLE,
            centerX - font.getStringWidth(SAMPLE_TITLE) * titleScale / 2,
            centerY + TITLE_TOP * titleScale,
            titleScale,
            0xFFFFFF);
        PreviewDrawing.drawScaledString(
            SAMPLE_SUBTITLE,
            centerX - font.getStringWidth(SAMPLE_SUBTITLE) * subtitleScale / 2,
            centerY + SUBTITLE_TOP * subtitleScale,
            subtitleScale,
            0xFFFFFF);
    }
}
