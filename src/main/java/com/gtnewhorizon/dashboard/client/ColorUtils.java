// TODO: Adjust colors
package com.gtnewhorizon.dashboard.client;

import com.gtnewhorizon.gtnhlib.color.ColorResource;

public final class ColorUtils {

    private static final ColorResource.Factory color = new ColorResource.Factory("dashboard");

    public static final ColorResource
    // spotless:off
        text              = color.rgb("text",               "0xFFFFFF"),
        textPosition      = color.rgb("textPosition",       "0xFFFF55"),
        textPreview       = color.rgb("textPreview",        "0xFFFFFF"),

        showingFill       = color.argb("showingFill",       "0x2000FF00"),
        showingBorder     = color.argb("showingBorder",     "0xA000FF00"),
        notShowingFill    = color.argb("notShowingFill",    "0x40FFFFFF"),
        notShowingBorder  = color.argb("notShowingBorder",  "0xA0A0A0A0"),
        hiddenFill        = color.argb("hiddenFill",        "0x60FF0000"),
        hiddenBorder      = color.argb("hiddenBorder",      "0xC0FF4040"),
        highlightBorder   = color.argb("highlightBorder",   "0xFFFFFFFF"),

        menuBackground    = color.argb("menuBackground",    "0xA0202020"),
        helpIcon          = color.argb("helpIcon",          "0x30FFFFFF"),
        helpIconHovered   = color.argb("helpIconHovered",   "0x60FFFFFF"),
        grid              = color.argb("grid",              "0x20FFFFFF"),
        gridCenter        = color.argb("gridCenter",        "0x50FFFFFF")

        ;
    // spotless:on
}
