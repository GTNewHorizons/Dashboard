package com.gtnewhorizons.dashboard.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.DashboardConfig;
import com.gtnewhorizons.dashboard.client.EditorModFilter;
import com.gtnewhorizons.dashboard.client.FrameEndHud;
import com.gtnewhorizons.dashboard.client.GuiHudEditor;
import com.gtnewhorizons.dashboard.client.HudLayout;

import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Register a HUD element and wrap its drawing in {@link #beginRender}, so players can move, resize and hide
 * it in the editor.
 */
@SideOnly(Side.CLIENT)
public final class HudRegistry {

    private static final Map<String, HudElement> ELEMENTS = new LinkedHashMap<>();
    /** A read-only view, so it always shows the current elements. */
    private static final Collection<HudElement> ELEMENTS_VIEW = Collections.unmodifiableCollection(ELEMENTS.values());
    private static final List<Runnable> LAYOUT_LISTENERS = new ArrayList<>();

    private static boolean drawingPreview;

    private HudRegistry() {}

    /**
     * Register during {@code preInit} or {@code init}, so code that registers an element only when nobody else did can
     * check with {@link #getElement} in {@code postInit}.
     */
    public static HudElement register(HudElement element) {
        if (ELEMENTS.putIfAbsent(element.getId(), element) != null) {
            throw new IllegalArgumentException("HUD element " + element.getId() + " is already registered");
        }
        return element;
    }

    public static Collection<HudElement> getElements() {
        return ELEMENTS_VIEW;
    }

    /** Null if no element with this id is registered. */
    public static HudElement getElement(String id) {
        return ELEMENTS.get(id);
    }

    /** The game's screen size, measured again only when it changes, since the HUD asks for it every frame. */
    public static ScaledResolution getResolution() {
        return HudLayout.getResolution();
    }

    /** Where the element is drawn now, after the player moved or resized it. */
    public static HudBounds getBounds(HudElement element) {
        ScaledResolution resolution = getResolution();
        return HudLayout.getBounds(element, resolution.getScaledWidth(), resolution.getScaledHeight());
    }

    /** Runs whenever the player moves, resizes, hides or resets elements, for code that remembers positions. */
    public static void addLayoutListener(Runnable listener) {
        LAYOUT_LISTENERS.add(listener);
    }

    /** Called by the layout after it changed. */
    public static void notifyLayoutChanged() {
        for (Runnable listener : LAYOUT_LISTENERS) {
            try {
                listener.run();
            } catch (RuntimeException e) {
                Dashboard.LOG.error("HUD layout listener {} failed", listener, e);
            }
        }
    }

    /**
     * For HUD drawn in a render tick handler at the end of the frame, which draws over open screens. Call at the start
     * of the handler, passing a way to run it, so it can be run at another time instead.
     *
     * @return true to skip the handler this time
     */
    public static boolean skipFrameEndHandler(Object handler, Consumer<TickEvent.RenderTickEvent> run,
        TickEvent.RenderTickEvent event) {
        return FrameEndHud.skip(handler, run, event);
    }

    public static boolean isLayoutScreenOpen() {
        return Minecraft.getMinecraft().currentScreen instanceof GuiHudEditor;
    }

    /**
     * In the editor, the element skips drawing when Preview is turned off or its mod is unticked in the Mods list, so
     * only its box is shown, or nothing.
     */
    public static boolean isHidingContent(HudElement element) {
        return !drawingPreview && isLayoutScreenOpen()
            && (!DashboardConfig.showPreview || !EditorModFilter.isShown(element));
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
        return beginRender(element, getResolution());
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

    /** A preview that breaks is turned off, so one broken mod can not break the whole editor. */
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
        translate(element, screenWidth, screenHeight, false);
    }

    /** Undoes {@link #translateToPosition}, for another element drawn while this element's move is active. */
    public static void undoTranslateToPosition(HudElement element, int screenWidth, int screenHeight) {
        translate(element, screenWidth, screenHeight, true);
    }

    /** From the default bounds to the player's, or back when undoing. */
    private static void translate(HudElement element, int screenWidth, int screenHeight, boolean undo) {
        HudBounds defaultBounds = element.getDefaultBounds(screenWidth, screenHeight);
        HudBounds bounds = HudLayout.getBounds(element, defaultBounds, screenWidth, screenHeight);
        float playerScale = HudLayout.getScale(element);
        if (bounds == defaultBounds && playerScale == 1) {
            return;
        }
        HudBounds from = undo ? bounds : defaultBounds;
        HudBounds to = undo ? defaultBounds : bounds;
        float scale = undo ? 1 / playerScale : playerScale;
        if (scale == 1) {
            GL11.glTranslatef(to.x - from.x, to.y - from.y, 0);
            return;
        }
        GL11.glTranslatef(to.x, to.y, 0);
        GL11.glScalef(scale, scale, 1);
        GL11.glTranslatef(-from.x, -from.y, 0);
    }

    public static void translateToPosition(HudElement element) {
        ScaledResolution resolution = getResolution();
        translateToPosition(element, resolution.getScaledWidth(), resolution.getScaledHeight());
    }
}
