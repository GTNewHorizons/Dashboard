package com.gtnewhorizons.dashboard.client;

import com.gtnewhorizon.gtnhlib.color.ColorResource;

public final class ColorUtils {

    private static final ColorResource.Factory color = new ColorResource.Factory("dashboard");

    public static final ColorResource
    // spotless:off
        text                    = color.rgb("text",                     "0xFFFFFF"),
        textPosition            = color.rgb("textPosition",             "0xE2BD3A"),
        textState               = color.rgb("textState",                "0xA0A0A0"),
        textInput               = color.rgb("textInput",                "0xE0E0E0"),
        textUnknown             = color.rgb("textUnknown",              "0xFF5555"),
        textHint                = color.rgb("textHint",                 "0x707070"),

        buttonText              = color.rgb("buttonText",               "0xE0E0E0"),
        buttonTextHovered       = color.rgb("buttonTextHovered",        "0xFFFFA0"),
        buttonTextDisabled      = color.rgb("buttonTextDisabled",       "0xA0A0A0"),

        showingFill             = color.argb("showingFill",             "0x507AC25F"),
        showingBorder           = color.argb("showingBorder",           "0xA04E8F46"),
        hiddenFill              = color.argb("hiddenFill",              "0x50B13E53"),
        hiddenBorder            = color.argb("hiddenBorder",            "0xA0B13E53"),
        notShowingFill          = color.argb("notShowingFill",          "0x40FFFFFF"),
        notShowingBorder        = color.argb("notShowingBorder",        "0xA0A0A0A0"),

        highlightBorder         = color.argb("highlightBorder",         "0xFFFFFFFF"),

        menuBackground          = color.argb("menuBackground",          "0xA0202020"),
        grid                    = color.argb("grid",                    "0x20FFFFFF"),
        gridCenter              = color.argb("gridCenter",              "0x50FFFFFF"),
        sectionHeader           = color.argb("sectionHeader",           "0x50A0A0A0"),
        
        scrollbarTrack          = color.argb("scrollbarTrack",          "0x50A0A0A0"),
        scrollbarHandle         = color.argb("scrollbarHandle",         "0xFFE0E0E0"),
        scrollbarHandleHovered  = color.argb("scrollbarHandleHovered",  "0xFFFFFFA0"),

        crosshairBackground1    = color.argb("crosshairBackground1",    "0xFFC6C6C6"),
        crosshairBackground2    = color.argb("crosshairBackground2",    "0xFF2B2B2B"),
        crosshairBackground3    = color.argb("crosshairBackground3",    "0xFF7BA4FF"),
        crosshairBackground4    = color.argb("crosshairBackground4",    "0xFF5D8C3A"),
        crosshairBackground5    = color.argb("crosshairBackground5",    "0xFFDBD3A0")

        ;
    // spotless:on
}
