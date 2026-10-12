package com.gtnewhorizons.dashboard;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizons.dashboard.client.Crosshairs;

@Config(modid = Dashboard.MODID, category = "editor", configSubDirectory = "dashboard", filename = "editor")
@Config.LangKey("dashboard.config.editor")
public class DashboardConfig {

    @Config.Comment("Show the grid in the HUD editor and snap elements to it")
    @Config.LangKey("dashboard.config.editor.show_grid")
    @Config.DefaultBoolean(true)
    public static boolean showGrid;

    @Config.Comment("Show sample content for elements that are not showing in the HUD editor")
    @Config.LangKey("dashboard.config.editor.show_preview")
    @Config.DefaultBoolean(true)
    public static boolean showPreview;

    @Config.Comment("Show the element names inside their boxes in the HUD editor")
    @Config.LangKey("dashboard.config.editor.show_labels")
    @Config.DefaultBoolean(true)
    public static boolean showLabels;

    @Config.Comment("Show the element position, measured from the middle of the screen, next to its box in the HUD editor")
    @Config.LangKey("dashboard.config.editor.show_positions")
    @Config.DefaultBoolean(false)
    public static boolean showPositions;

    @Config.Comment("Crosshair shape: 0 is vanilla, 1 and up are the shapes on the crosshair sheet")
    @Config.LangKey("dashboard.config.editor.crosshair_style")
    @Config.DefaultInt(0)
    @Config.RangeInt(min = 0, max = Crosshairs.STYLE_COUNT)
    public static int crosshairStyle;

    @Config.Comment("Crosshair size, 1 to 5 times")
    @Config.LangKey("dashboard.config.editor.crosshair_size")
    @Config.DefaultInt(1)
    @Config.RangeInt(min = 1, max = Crosshairs.MAX_SIZE)
    public static int crosshairSize;
}
