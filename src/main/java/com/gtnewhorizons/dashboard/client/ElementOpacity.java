package com.gtnewhorizons.dashboard.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.MathHelper;

import com.gtnewhorizons.dashboard.api.HudElement;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A see-through background for elements that allow it, changed with Ctrl+scroll in the editor and saved with the
 * layout. The element's own drawing reads it with {@link #get}.
 */
@SideOnly(Side.CLIENT)
public final class ElementOpacity {

    public static final int STEP = 5;
    private static final int MAX_PERCENT = 100;
    private static final Map<HudElement, Integer> DEFAULT_PERCENTS = new HashMap<>();

    private ElementOpacity() {}

    public static void allow(HudElement element, int defaultPercent) {
        DEFAULT_PERCENTS.put(element, defaultPercent);
    }

    public static boolean isAllowed(HudElement element) {
        return DEFAULT_PERCENTS.containsKey(element);
    }

    public static int getPercent(HudElement element) {
        Integer saved = HudLayout.getOpacity(element);
        return saved != null ? saved : DEFAULT_PERCENTS.getOrDefault(element, MAX_PERCENT);
    }

    /** From 0 for an invisible background to 1 for a solid one. */
    public static float get(HudElement element) {
        return getPercent(element) / (float) MAX_PERCENT;
    }

    /** The element's default is saved as nothing, so untouched elements stay out of the layout file. */
    static void change(HudElement element, int steps) {
        int percent = MathHelper.clamp_int(getPercent(element) + steps * STEP, 0, MAX_PERCENT);
        HudLayout.setOpacity(element, percent == DEFAULT_PERCENTS.get(element) ? null : percent);
    }
}
