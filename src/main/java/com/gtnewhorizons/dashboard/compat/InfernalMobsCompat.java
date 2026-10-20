package com.gtnewhorizons.dashboard.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.api.HudShift;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Infernal Mobs' health bar for the elite mob being looked at, drawn like vanilla's boss bar at its spot while no boss
 * bar shows, with the mob's name above and its modifiers below.
 */
@SideOnly(Side.CLIENT)
public final class InfernalMobsCompat {

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int NAME_TOP = 1;
    private static final int BAR_TOP = 12;
    private static final int MODIFIERS_TOP = 19;
    private static final int TEXT_HEIGHT = 8;
    private static final int SAMPLE_NAME_COLOR = 0xFFFF55;
    private static final int MODIFIERS_COLOR = 0xFFFFFF;
    private static final String SAMPLE_NAME = "Rare draining Zombie of Repulsion";
    private static final String SAMPLE_MODIFIERS = "Fiery Weakness Exhaust Gravity";

    /** Sized for one line of modifiers, which most mobs have. */
    public static final HudElement HEALTH_BAR = new HudElement(
        "infernalmobs:health_bar",
        "dashboard.element.infernalmobs.health_bar",
        (screenWidth, screenHeight) -> new HudBounds(
            screenWidth / 2 - BAR_WIDTH / 2,
            NAME_TOP,
            BAR_WIDTH,
            MODIFIERS_TOP + TEXT_HEIGHT - NAME_TOP))
                .setDescription("dashboard.element.infernalmobs.health_bar.description");

    private static final HudShift SHIFT = new HudShift();

    private InfernalMobsCompat() {}

    public static void register() {
        PreviewDrawing.setImitationInBounds(HEALTH_BAR, InfernalMobsCompat::drawSample);
        HudRegistry.registerFallback(HEALTH_BAR);
    }

    /** Called by the mixin right before drawing. False means hidden, so the drawing should be skipped. */
    public static boolean begin() {
        return SHIFT.begin(HEALTH_BAR);
    }

    public static void end() {
        SHIFT.end();
    }

    /** Vanilla's boss bar two thirds full, with a rare mob's name and modifiers, like in game. */
    private static void drawSample(HudBounds bounds) {
        int top = bounds.y - NAME_TOP;
        PreviewDrawing.bindTexture(Gui.icons);
        PreviewDrawing.drawTexture(bounds.x, top + BAR_TOP, 0, 74, BAR_WIDTH, BAR_HEIGHT);
        PreviewDrawing.drawTexture(bounds.x, top + BAR_TOP, 0, 79, BAR_WIDTH * 2 / 3, BAR_HEIGHT);
        int centerX = bounds.x + bounds.width / 2;
        drawCentered(SAMPLE_NAME, centerX, top + NAME_TOP, SAMPLE_NAME_COLOR);
        drawCentered(SAMPLE_MODIFIERS, centerX, top + MODIFIERS_TOP, MODIFIERS_COLOR);
    }

    private static void drawCentered(String text, int centerX, int y, int color) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        font.drawStringWithShadow(text, centerX - font.getStringWidth(text) / 2, y, color);
    }
}
