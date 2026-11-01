package com.gtnewhorizons.dashboard.compat;

import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * JourneyMap's minimap, through its MiniMapHud API. JourneyMap does not know about Dashboard, so the API is found by
 * name, and older JourneyMap versions without it simply have no element.
 */
@SideOnly(Side.CLIENT)
public final class JourneyMapCompat {

    private static final String MINIMAP_HUD = "journeymap.client.api.MiniMapHud";

    private static final HudElement MINIMAP = new HudElement("journeymap:minimap", JourneyMapCompat::getDefaultBounds)
        .setActiveCheck(JourneyMapCompat::isEnabled);

    private static Method isEnabled;
    private static Method getBounds;

    private JourneyMapCompat() {}

    public static void register() {
        try {
            Class<?> miniMapHud = Class.forName(MINIMAP_HUD);
            isEnabled = miniMapHud.getMethod("isEnabled");
            getBounds = miniMapHud.getMethod("getBounds");
            BooleanSupplier layoutScreenOpen = HudRegistry::isLayoutScreenOpen;
            BooleanSupplier shouldDraw = () -> HudRegistry.shouldRender(MINIMAP);
            Runnable moveToPosition = JourneyMapCompat::moveToPosition;
            miniMapHud.getMethod("setHooks", BooleanSupplier.class, BooleanSupplier.class, Runnable.class)
                .invoke(null, layoutScreenOpen, shouldDraw, moveToPosition);
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.info("This JourneyMap has no MiniMapHud API yet, its minimap can not be moved");
            return;
        }
        HudRegistry.registerFallback(MINIMAP);
    }

    private static boolean isEnabled() {
        try {
            return (boolean) isEnabled.invoke(null);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /**
     * Moves and resizes the minimap to where the player put it. JourneyMap draws in screen pixels, the move is measured
     * in GUI scaled pixels, so it is scaled up for that and the scale is undone after.
     */
    private static void moveToPosition() {
        float scale = HudRegistry.getResolution()
            .getScaleFactor();
        GL11.glScalef(scale, scale, 1);
        HudRegistry.translateToPosition(MINIMAP);
        GL11.glScalef(1 / scale, 1 / scale, 1);
    }

    /** JourneyMap measures in screen pixels, the HUD in GUI scaled pixels. */
    private static HudBounds getDefaultBounds(int screenWidth, int screenHeight) {
        try {
            int[] bounds = (int[]) getBounds.invoke(null);
            int scale = HudRegistry.getResolution()
                .getScaleFactor();
            int left = bounds[0] / scale;
            int top = bounds[1] / scale;
            int right = (bounds[2] + scale - 1) / scale;
            int bottom = (bounds[3] + scale - 1) / scale;
            return new HudBounds(left, top, right - left, bottom - top);
        } catch (ReflectiveOperationException e) {
            return new HudBounds(0, 0, 0, 0);
        }
    }
}
