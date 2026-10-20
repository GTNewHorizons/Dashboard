package com.gtnewhorizons.dashboard.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.api.HudShift;
import com.gtnewhorizons.dashboard.client.HudLayout;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Hardcore Ender Expansion's notes floating up in the bottom right, and the energy cluster info above the crosshair.
 * Both are drawn in the same method, so a hidden one is drawn invisibly, which keeps HEE's fading working.
 */
@SideOnly(Side.CLIENT)
public final class HeeCompat {

    /** In the small unicode font, which HEE scales so it stays sharp. */
    private static final int NOTE_WIDTH = 120;
    private static final int TEXT_HEIGHT = 8;
    private static final int NOTE_START = 10;
    private static final int NOTE_FLOAT = 13;
    private static final int NOTE_MARGIN = 3;
    private static final String SAMPLE_NOTE = "Found a new location";

    private static final int CLUSTER_WIDTH = 140;
    private static final int CLUSTER_TOP = 44;
    private static final int CLUSTER_HEIGHT = 38;
    private static final int CLUSTER_TEXT = 220 << 24 | 0xDCDCDC;
    private static final int CLUSTER_HEALTHY = 220 << 24 | 0x54FF54;

    public static final HudElement NOTIFICATIONS = new HudElement(
        "hardcoreenderexpansion:notifications",
        "dashboard.element.hee.notifications",
        HeeCompat::getNotificationBounds);

    public static final HudElement ENERGY_CLUSTER = new HudElement(
        "hardcoreenderexpansion:energy_cluster",
        "dashboard.element.hee.energy_cluster",
        (screenWidth, screenHeight) -> new HudBounds(
            screenWidth / 2 - CLUSTER_WIDTH / 2,
            screenHeight / 2 - CLUSTER_TOP,
            CLUSTER_WIDTH,
            CLUSTER_HEIGHT)).setDescription("dashboard.element.hee.energy_cluster.description");

    private static final HudShift SHIFT = new HudShift();

    private HeeCompat() {}

    public static void register() {
        PreviewDrawing.setImitationInBounds(NOTIFICATIONS, HeeCompat::drawNoteSample);
        PreviewDrawing.setImitationInBounds(ENERGY_CLUSTER, HeeCompat::drawClusterSample);
        HudRegistry.registerFallback(NOTIFICATIONS);
        HudRegistry.registerFallback(ENERGY_CLUSTER);
    }

    /** Called by the mixin around each part. A hidden part is drawn invisibly, since the method can not be cut. */
    public static void beginOrHide(HudElement element) {
        SHIFT.beginOrHide(element);
    }

    public static void end() {
        SHIFT.end();
    }

    /** Like HEE: 2 times at GUI scale 1, 1 at 2, about two thirds at 3, a half above. */
    private static double getNoteScale() {
        int guiScale = HudLayout.getResolution()
            .getScaleFactor();
        return guiScale == 1 ? 2 : guiScale == 2 ? 1 : guiScale == 3 ? 0.677 : 0.5;
    }

    /** Right aligned in the bottom right, as high as a note floats. */
    private static HudBounds getNotificationBounds(int screenWidth, int screenHeight) {
        double scale = getNoteScale();
        int width = (int) Math.ceil(NOTE_WIDTH * scale);
        int top = screenHeight - (int) Math.ceil((NOTE_START + NOTE_FLOAT) * scale);
        int bottom = screenHeight - (int) Math.floor((NOTE_START - TEXT_HEIGHT) * scale);
        return new HudBounds(screenWidth - (int) Math.ceil(NOTE_MARGIN * scale) - width, top, width, bottom - top);
    }

    /** A note halfway up, in HEE's small font. */
    private static void drawNoteSample(HudBounds bounds) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        boolean unicode = font.getUnicodeFlag();
        font.setUnicodeFlag(true);
        double scale = getNoteScale();
        GL11.glPushMatrix();
        GL11.glTranslatef(bounds.getRight(), bounds.y + (float) (NOTE_FLOAT / 2 * scale), 0);
        GL11.glScaled(scale, scale, 1);
        font.drawStringWithShadow(SAMPLE_NOTE, -font.getStringWidth(SAMPLE_NOTE), 0, 0xFFFFFF);
        GL11.glPopMatrix();
        font.setUnicodeFlag(unicode);
    }

    /** HEE's four lines, with sample numbers and a healthy cluster. */
    private static void drawClusterSample(HudBounds bounds) {
        int centerX = bounds.x + bounds.width / 2;
        drawCentered(StatCollector.translateToLocal("energy.overlay.title"), centerX, bounds.y, 0xFFFFFF);
        drawCentered(
            StatCollector.translateToLocal("energy.overlay.holding")
                .replace("$", "12.50"),
            centerX,
            bounds.y + 10,
            CLUSTER_TEXT);
        drawCentered(
            StatCollector.translateToLocal("energy.overlay.regen")
                .replace("$", "25.00"),
            centerX,
            bounds.y + 20,
            CLUSTER_TEXT);
        drawCentered(StatCollector.translateToLocal("energy.status.healthy"), centerX, bounds.y + 30, CLUSTER_HEALTHY);
    }

    private static void drawCentered(String text, int centerX, int y, int color) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        font.drawStringWithShadow(text, centerX - font.getStringWidth(text) / 2, y, color);
    }
}
