package com.gtnewhorizons.dashboard.client;

import java.util.Locale;

import net.minecraft.util.StatCollector;

/** How a bar number is written, like 16/20. */
public enum NumberStyle {

    SLASH,
    SPACED_SLASH,
    CURRENT,
    MAX,
    PERCENT;

    public String format(String current, String max, String percent) {
        return switch (this) {
            case SLASH -> current + "/" + max;
            case SPACED_SLASH -> current + " / " + max;
            case CURRENT -> current;
            case MAX -> max;
            case PERCENT -> percent + "%";
        };
    }

    public String getDisplayName() {
        return StatCollector.translateToLocal("dashboard.customize.number_style." + name().toLowerCase(Locale.ROOT));
    }
}
