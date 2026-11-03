package com.gtnewhorizons.dashboard.compat;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.common.MinecraftForge;

import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.client.HudLayout;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import journeymap.client.hud.MiniMapHud;
import journeymap.client.hud.MiniMapRenderEvent;

/**
 * JourneyMap's minimap, through its MiniMapRenderEvent. Older JourneyMap versions do not have it, so nothing here may
 * touch its classes before {@link #register()} checked for them.
 */
@SideOnly(Side.CLIENT)
public final class JourneyMapCompat {

    private static final String RENDER_EVENT = "journeymap.client.hud.MiniMapRenderEvent";

    private static final HudElement MINIMAP = new HudElement("journeymap:minimap", JourneyMapCompat::getDefaultBounds)
        .setActiveCheck(() -> MiniMapHud.isEnabled());

    private JourneyMapCompat() {}

    public static void register() {
        try {
            Class.forName(RENDER_EVENT);
        } catch (ClassNotFoundException e) {
            Dashboard.LOG.info("This JourneyMap has no MiniMapRenderEvent yet, its minimap can not be moved");
            return;
        }
        MinecraftForge.EVENT_BUS.register(new RenderListener());
        HudRegistry.registerFallback(MINIMAP);
    }

    /** JourneyMap measures in screen pixels, the HUD in GUI scaled pixels. */
    private static HudBounds getDefaultBounds(int screenWidth, int screenHeight) {
        int[] bounds = MiniMapHud.getBounds();
        int scale = HudRegistry.getResolution()
            .getScaleFactor();
        int left = bounds[0] / scale;
        int top = bounds[1] / scale;
        int right = (bounds[2] + scale - 1) / scale;
        int bottom = (bounds[3] + scale - 1) / scale;
        return new HudBounds(left, top, right - left, bottom - top);
    }

    /** Its own class, since Forge reads the event type from the method when registering. */
    public static final class RenderListener {

        @SubscribeEvent
        public void onMiniMapRender(MiniMapRenderEvent event) {
            GuiScreen screen = event.screen;
            boolean editorOpen = HudRegistry.isLayoutScreenOpen();
            // Behind other screens the minimap does not draw, so it should not count as showing
            if (screen != null && !(screen instanceof GuiChat) && !editorOpen) {
                return;
            }
            event.drawWhileScreenOpen = editorOpen;
            if (!HudRegistry.shouldRender(MINIMAP)) {
                event.setCanceled(true);
                return;
            }
            applyPosition(event);
        }

        /**
         * JourneyMap resizes around the top left of its own bounds, in screen pixels, while the player's position is
         * measured from the default bounds in GUI scaled pixels.
         */
        private static void applyPosition(MiniMapRenderEvent event) {
            ScaledResolution resolution = HudRegistry.getResolution();
            int screenWidth = resolution.getScaledWidth();
            int screenHeight = resolution.getScaledHeight();
            int factor = resolution.getScaleFactor();
            HudBounds from = MINIMAP.getDefaultBounds(screenWidth, screenHeight);
            HudBounds to = HudLayout.getBounds(MINIMAP, from, screenWidth, screenHeight);
            float scale = HudLayout.getScale(MINIMAP);
            int[] journeyMapBounds = MiniMapHud.getBounds();
            event.scale = scale;
            event.offsetX = to.x * factor - journeyMapBounds[0] - scale * (from.x * factor - journeyMapBounds[0]);
            event.offsetY = to.y * factor - journeyMapBounds[1] - scale * (from.y * factor - journeyMapBounds[1]);
        }
    }
}
