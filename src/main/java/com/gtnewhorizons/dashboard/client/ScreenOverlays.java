package com.gtnewhorizons.dashboard.client;

import net.minecraft.util.MathHelper;

import com.gtnewhorizons.dashboard.DashboardConfig;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Turns the overlay percentages into what the mixins need, like an opacity from 0 to 1. */
@SideOnly(Side.CLIENT)
public final class ScreenOverlays {

    public static final int MAX_PERCENT = 100;
    /** How far down the flames go at 0% height. */
    private static final float MAX_FIRE_DROP = 0.5f;

    private ScreenOverlays() {}

    public static float getPumpkinOpacity() {
        return toFraction(DashboardConfig.pumpkinOpacity);
    }

    public static float getPortalOpacity() {
        return toFraction(DashboardConfig.portalOpacity);
    }

    public static float getFireOpacity() {
        return toFraction(DashboardConfig.fireOpacity);
    }

    public static float getFireDrop() {
        return (1 - toFraction(DashboardConfig.fireHeight)) * MAX_FIRE_DROP;
    }

    public static float getWaterOpacity() {
        return toFraction(DashboardConfig.waterOpacity);
    }

    public static float getVignetteStrength() {
        return toFraction(DashboardConfig.vignetteStrength);
    }

    public static float getSleepFadeStrength() {
        return toFraction(DashboardConfig.sleepFadeStrength);
    }

    public static float getHurtShakeStrength() {
        return toFraction(DashboardConfig.hurtShakeStrength);
    }

    public static float getNauseaStrength() {
        return toFraction(DashboardConfig.nauseaStrength);
    }

    public static int scaleAlpha(int color, float scale) {
        int alpha = Math.round((color >>> 24) * scale);
        return alpha << 24 | color & 0xFFFFFF;
    }

    private static float toFraction(int percent) {
        return MathHelper.clamp_int(percent, 0, MAX_PERCENT) / (float) MAX_PERCENT;
    }
}
