package com.gtnewhorizons.dashboard.compat;

import java.lang.reflect.Method;

import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The GalaxySpace pressure bar, drawn like Galacticraft's overlays and placed by Galacticraft's corner config. */
@SideOnly(Side.CLIENT)
public final class GalaxySpaceCompat {

    private static final int PRESSURE_WIDTH = 47;
    private static final int PRESSURE_HEIGHT = 10;
    private static final int PRESSURE_RIGHT_MARGIN = 12;
    private static final int PRESSURE_LEFT_MARGIN = 10;
    private static final int PRESSURE_OFFSET_UNDER_GAUGE = 57;
    private static final int PRESSURE_OFFSET_ALONE = 6;
    private static final int CORNER_AREA_HEIGHT = 68;

    public static final HudElement PRESSURE = new HudElement(
        "galaxyspace:pressure",
        "dashboard.element.galaxyspace.pressure",
        GalaxySpaceCompat::getPressureBounds);

    private GalaxySpaceCompat() {}

    public static void register() {
        try {
            Method drawPressure = Class.forName("galaxyspace.core.gui.overlay.OverlayAtmosphericPressure")
                .getMethod("renderPressureTankIndicator", int.class, boolean.class, boolean.class, boolean.class);
            // A medium pressure level. GalaxySpace takes "right" and "top", not left and bottom
            PRESSURE.setPreview((screenWidth, screenHeight) -> {
                try {
                    drawPressure.invoke(
                        null,
                        20,
                        false,
                        !GalacticraftCompat.isIndicatorOnLeft(),
                        !GalacticraftCompat.isIndicatorAtBottom());
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Could not draw the GalaxySpace pressure bar", e);
                }
            });
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not find the GalaxySpace pressure bar, its HUD editor preview is off", e);
        }
        HudRegistry.registerFallback(PRESSURE);
    }

    private static HudBounds getPressureBounds(int screenWidth, int screenHeight) {
        int x = GalacticraftCompat.isIndicatorOnLeft() ? PRESSURE_LEFT_MARGIN
            : screenWidth - PRESSURE_RIGHT_MARGIN - PRESSURE_WIDTH;
        int areaTop = GalacticraftCompat.isIndicatorAtBottom() ? screenHeight - CORNER_AREA_HEIGHT : 0;
        int offset = GalacticraftCompat.isCurrentWorldAirless() ? PRESSURE_OFFSET_UNDER_GAUGE : PRESSURE_OFFSET_ALONE;
        return new HudBounds(x, areaTop + offset, PRESSURE_WIDTH, PRESSURE_HEIGHT);
    }
}
