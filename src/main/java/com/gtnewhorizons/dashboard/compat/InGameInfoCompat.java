package com.gtnewhorizons.dashboard.compat;

import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * InGameInfoXML's text, one element per screen area its lines can be aligned to. The text comes from the player's XML
 * config, so each area's size is measured from the lines it drew in the last frame. Areas that never drew anything stay
 * out of the editor.
 */
@SideOnly(Side.CLIENT)
public final class InGameInfoCompat {

    private static final String PACKAGE = "com.github.lunatrius.ingameinfo.";
    /** In the order of InGameInfoXML's {@code Alignment} enum. */
    private static final String[] AREA_NAMES = { "top_left", "top_center", "top_right", "middle_left", "middle_center",
        "middle_right", "bottom_left", "bottom_center", "bottom_right" };
    private static final int PLACEHOLDER_WIDTH = 40;

    private static final HudElement[] AREAS = new HudElement[AREA_NAMES.length];
    private static final HudBounds[] lastFrameBounds = new HudBounds[AREA_NAMES.length];
    private static final HudBounds[] currentFrameBounds = new HudBounds[AREA_NAMES.length];

    private static Fields.ClassFields<Object>.Field<?> alignment;
    private static Method isActive;
    private static Method getX;
    private static Method getY;
    private static Method getWidth;
    private static Fields.ClassFields<Object>.Field<Float> scale;
    private static boolean lineShifted;

    private InGameInfoCompat() {}

    public static void register() {
        try {
            Class<?> infoText = Class.forName(PACKAGE + "client.gui.InfoText");
            alignment = ModFields.of(infoText)
                .getUntypedField(Fields.LookupType.DECLARED, "alignment");
            isActive = infoText.getMethod("isActive");
            getX = infoText.getMethod("getX");
            getY = infoText.getMethod("getY");
            getWidth = infoText.getMethod("getWidth");
            scale = ModFields.floatField(ModFields.of(PACKAGE + "handler.ConfigurationHandler"), "Scale");
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not read InGameInfoXML's text, its HUD elements are off", e);
            alignment = null;
        }
        for (int area = 0; area < AREAS.length; area++) {
            int index = area;
            AREAS[area] = new HudElement(
                "ingameinfoxml:" + AREA_NAMES[area],
                "dashboard.element.ingameinfo." + AREA_NAMES[area],
                (screenWidth, screenHeight) -> getAreaBounds(index, screenWidth, screenHeight))
                    .setActiveCheck(() -> lastFrameBounds[index] != null || currentFrameBounds[index] != null);
            HudEditor.register(AREAS[area]);
        }
    }

    /**
     * Called before InGameInfoXML draws a line: measures it into its area, and moves it to where the player put the
     * area.
     *
     * @return false if the player hid the line's area, so the line should not be drawn
     */
    public static boolean beginLine(Object infoText) {
        if (alignment == null) {
            return true;
        }
        try {
            if (!(boolean) isActive.invoke(infoText)) {
                return true;
            }
            int area = ((Enum<?>) alignment.getValue(infoText)).ordinal();
            float textScale = getScale();
            // InGameInfoXML places lines in its own scaled coordinates, the editor works in screen pixels
            HudBounds line = new HudBounds(
                Math.round((int) getX.invoke(infoText) * textScale),
                Math.round((int) getY.invoke(infoText) * textScale),
                Math.round((int) getWidth.invoke(infoText) * textScale),
                Math.round(Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT * textScale));
            currentFrameBounds[area] = union(currentFrameBounds[area], line);

            if (!HudEditor.shouldRender(AREAS[area])) {
                return false;
            }
            // The line is drawn with InGameInfoXML's scale applied, so the move is applied without it
            GL11.glPushMatrix();
            GL11.glScalef(1 / textScale, 1 / textScale, 1);
            HudEditor.translateToPosition(AREAS[area]);
            GL11.glScalef(textScale, textScale, 1);
            lineShifted = true;
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            alignment = null;
            Dashboard.LOG.error("Could not read InGameInfoXML's text, its HUD elements are turned off", e);
            return true;
        }
    }

    /** Safe to call also when the line was not moved. */
    public static void endLine() {
        if (lineShifted) {
            lineShifted = false;
            HudEditor.endRender();
        }
    }

    /** Called after InGameInfoXML drew all its text. Areas that drew nothing keep their last size. */
    public static void endFrame() {
        for (int area = 0; area < AREAS.length; area++) {
            if (currentFrameBounds[area] != null) {
                lastFrameBounds[area] = currentFrameBounds[area];
                currentFrameBounds[area] = null;
            }
        }
    }

    /** Before the area drew anything, a small box at its corner or edge. */
    private static HudBounds getAreaBounds(int area, int screenWidth, int screenHeight) {
        if (lastFrameBounds[area] != null) {
            return lastFrameBounds[area];
        }
        if (currentFrameBounds[area] != null) {
            return currentFrameBounds[area];
        }
        int fontHeight = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
        int x = area % 3 * (screenWidth - PLACEHOLDER_WIDTH) / 2;
        int y = area / 3 * (screenHeight - fontHeight) / 2;
        return new HudBounds(x, y, PLACEHOLDER_WIDTH, fontHeight);
    }

    /** InGameInfoXML's {@code Scale} config is in tenths, 10 being normal size. */
    private static float getScale() {
        float textScale = scale.getValue(null) / 10f;
        return textScale > 0 ? textScale : 1;
    }

    private static HudBounds union(HudBounds a, HudBounds b) {
        if (a == null) {
            return b;
        }
        int x = Math.min(a.x, b.x);
        int y = Math.min(a.y, b.y);
        return new HudBounds(
            x,
            y,
            Math.max(a.getRight(), b.getRight()) - x,
            Math.max(a.getBottom(), b.getBottom()) - y);
    }
}
