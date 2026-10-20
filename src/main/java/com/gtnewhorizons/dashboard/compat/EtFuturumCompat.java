package com.gtnewhorizons.dashboard.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.api.HudShift;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Et Futurum's sound captions, the rows in the bottom right that name nearby sounds, like newer Minecraft. Only in the
 * editor while turned on in Et Futurum's config, since they are off by default.
 */
@SideOnly(Side.CLIENT)
public final class EtFuturumCompat {

    /** Like Et Futurum: rows stack up from 30 pixels above the bottom, one text line plus a pixel apart. */
    private static final int BOTTOM_ROW_CENTER = 30;
    private static final int ROW_HEIGHT = 10;
    private static final int RIGHT_MARGIN = 1;
    private static final int ROW_BACKGROUND = 0xCC000000;
    private static final String[] SAMPLE_CAPTIONS = { "Footsteps", "Zombie groans", "Chest opens" };
    /** Captions fade from white to gray over their 3 seconds, so older ones look darker. */
    private static final int[] SAMPLE_BRIGHTNESS = { 255, 195, 135 };

    public static final HudElement SUBTITLES = new HudElement(
        "etfuturum:subtitles",
        "dashboard.element.etfuturum.subtitles",
        EtFuturumCompat::getBounds).setActiveCheck(EtFuturumCompat::areSubtitlesOn);

    private static final HudShift SHIFT = new HudShift();
    private static Fields.ClassFields<Object>.Field<Boolean> subtitlesOn;

    private EtFuturumCompat() {}

    public static void register() {
        try {
            subtitlesOn = ModFields.booleanField(
                ModFields.of("ganymedes01.etfuturum.configuration.configs.ConfigFunctions"),
                "enableSubtitles");
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not read whether Et Futurum's subtitles are on, showing them in the editor", e);
        }
        PreviewDrawing.setImitationInBounds(SUBTITLES, EtFuturumCompat::drawSample);
        HudRegistry.registerFallback(SUBTITLES);
    }

    /** Called by the mixin right before drawing. False means hidden, so the drawing should be skipped. */
    public static boolean begin() {
        return SHIFT.begin(SUBTITLES);
    }

    public static void end() {
        SHIFT.end();
    }

    private static boolean areSubtitlesOn() {
        return ModFields.readBoolean(subtitlesOn, true);
    }

    /** As wide as the sample rows and right aligned like them, from the top row to the bottom one. */
    private static HudBounds getBounds(int screenWidth, int screenHeight) {
        int width = getRowWidth() + 2;
        int bottom = screenHeight - BOTTOM_ROW_CENTER + ROW_HEIGHT / 2;
        int height = SAMPLE_CAPTIONS.length * ROW_HEIGHT;
        return new HudBounds(screenWidth - RIGHT_MARGIN - width, bottom - height, width, height);
    }

    /** Like Et Futurum: the widest caption, with room for an arrow on each side. */
    private static int getRowWidth() {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int widest = 0;
        for (String caption : SAMPLE_CAPTIONS) {
            widest = Math.max(widest, font.getStringWidth(caption));
        }
        return widest + font.getStringWidth("< ") + font.getStringWidth("> ");
    }

    /** The newest caption at the bottom, with arrows pointing where the sounds come from. */
    private static void drawSample(HudBounds bounds) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int centerX = bounds.x + bounds.width / 2;
        int halfWidth = bounds.width / 2 - 1;
        for (int row = 0; row < SAMPLE_CAPTIONS.length; row++) {
            int top = bounds.getBottom() - (row + 1) * ROW_HEIGHT;
            Gui.drawRect(bounds.x, top, bounds.getRight(), top + ROW_HEIGHT, ROW_BACKGROUND);
            int brightness = SAMPLE_BRIGHTNESS[row];
            int color = 0xFF000000 | brightness << 16 | brightness << 8 | brightness;
            String caption = SAMPLE_CAPTIONS[row];
            int textY = top + 1;
            font.drawString(caption, centerX - font.getStringWidth(caption) / 2, textY, color);
            if (row == 1) {
                font.drawString("<", centerX - halfWidth, textY, color);
            } else if (row == 2) {
                font.drawString(">", centerX + halfWidth - font.getStringWidth(">"), textY, color);
            }
        }
    }
}
