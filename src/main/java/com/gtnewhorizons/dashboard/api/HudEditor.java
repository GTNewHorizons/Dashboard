package com.gtnewhorizons.dashboard.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.DashboardConfig;
import com.gtnewhorizons.dashboard.client.EditorModFilter;
import com.gtnewhorizons.dashboard.client.GuiHudEditor;
import com.gtnewhorizons.dashboard.client.HudLayout;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Register a HUD element and wrap its drawing in {@link #beginRender}, so players can move, resize and hide
 * it in the editor.
 */
@SideOnly(Side.CLIENT)
public final class HudEditor {

    private static final Map<String, HudElement> ELEMENTS = new LinkedHashMap<>();
    /** A read-only view, so it always shows the current elements. */
    private static final Collection<HudElement> ELEMENTS_VIEW = Collections.unmodifiableCollection(ELEMENTS.values());

    private static boolean drawingPreview;

    private HudEditor() {}

    public static HudElement register(HudElement element) {
        if (ELEMENTS.containsKey(element.getId())) {
            throw new IllegalArgumentException("HUD element " + element.getId() + " is already registered");
        }
        ELEMENTS.put(element.getId(), element);
        return element;
    }

    public static Collection<HudElement> getElements() {
        return ELEMENTS_VIEW;
    }

    public static boolean isEditorOpen() {
        return Minecraft.getMinecraft().currentScreen instanceof GuiHudEditor;
    }

    /**
     * In the editor, the element skips drawing when Preview is turned off or its mod is unticked in the Mods list, so
     * only its box is shown, or nothing.
     */
    public static boolean isHidingContent(HudElement element) {
        return !drawingPreview && isEditorOpen() && (!DashboardConfig.showPreview || !EditorModFilter.isShown(element));
    }

    /**
     * Call right before drawing the element.
     *
     * @return false if the element should not be drawn. Then skip drawing it and do not call {@link #endRender()}.
     */
    public static boolean beginRender(HudElement element, int screenWidth, int screenHeight) {
        if (!shouldRender(element)) {
            return false;
        }
        GL11.glPushMatrix();
        translateToPosition(element, screenWidth, screenHeight);
        return true;
    }

    public static boolean beginRender(HudElement element, ScaledResolution resolution) {
        return beginRender(element, resolution.getScaledWidth(), resolution.getScaledHeight());
    }

    public static boolean beginRender(HudElement element) {
        return beginRender(element, HudLayout.getResolution());
    }

    /** Call right after drawing the element, only if {@link #beginRender} returned true. */
    public static void endRender() {
        GL11.glPopMatrix();
    }

    /** Shorthand for {@link #beginRender}, drawing and {@link #endRender()}. */
    public static void render(HudElement element, Runnable draw) {
        if (beginRender(element)) {
            try {
                draw.run();
            } finally {
                endRender();
            }
        }
    }

    /**
     * The first half of {@link #beginRender}, for drawing code that resets the matrix itself, which would undo the
     * shift. Call this before the drawing code starts, then {@link #translateToPosition} right after its reset.
     *
     * @return false if the element should not be drawn
     */
    public static boolean shouldRender(HudElement element) {
        if (!element.isActive()) {
            return false;
        }
        if (!drawingPreview) {
            element.markRendered();
        }
        return HudLayout.isVisible(element) && !isHidingContent(element);
    }

    /** A preview that' broken, is turned off, so one broken mod can not break the whole editor. */
    public static void drawPreview(HudElement element, int screenWidth, int screenHeight) {
        HudElement.PreviewRenderer preview = element.getPreview();
        if (preview == null) {
            return;
        }

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        drawingPreview = true;
        try {
            preview.drawPreview(screenWidth, screenHeight);
        } catch (RuntimeException e) {
            Dashboard.LOG.error("HUD preview of {} failed, turning it off", element.getId(), e);
            element.setPreview(null);
        } finally {
            drawingPreview = false;
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    /**
     * Shifts and resizes everything drawn afterwards from the element's default position and size to the player's.
     * Does not push the matrix, so only use it where the drawing code restores the matrix itself.
     */
    public static void translateToPosition(HudElement element, int screenWidth, int screenHeight) {
        HudBounds defaultBounds = element.getDefaultBounds(screenWidth, screenHeight);
        HudBounds bounds = HudLayout.getBounds(element, defaultBounds, screenWidth, screenHeight);
        float scale = HudLayout.getScale(element);
        if (scale == 1) {
            GL11.glTranslatef(bounds.x - defaultBounds.x, bounds.y - defaultBounds.y, 0);
            return;
        }
        GL11.glTranslatef(bounds.x, bounds.y, 0);
        GL11.glScalef(scale, scale, 1);
        GL11.glTranslatef(-defaultBounds.x, -defaultBounds.y, 0);
    }

    public static void translateToPosition(HudElement element) {
        ScaledResolution resolution = HudLayout.getResolution();
        translateToPosition(element, resolution.getScaledWidth(), resolution.getScaledHeight());
    }
}
