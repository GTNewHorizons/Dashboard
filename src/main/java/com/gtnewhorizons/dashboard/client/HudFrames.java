package com.gtnewhorizons.dashboard.client;

import net.minecraftforge.client.event.RenderGameOverlayEvent;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Counts HUD frames, so "Avoid Overlap" knows which elements are on screen right now. */
@SideOnly(Side.CLIENT)
@EventBusSubscriber(side = Side.CLIENT)
public final class HudFrames {

    private static int current;

    private HudFrames() {}

    public static int current() {
        return current;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            current++;
        }
    }
}
