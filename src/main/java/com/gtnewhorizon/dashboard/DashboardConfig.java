package com.gtnewhorizon.dashboard;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = Dashboard.MODID, category = "editor", configSubDirectory = "dashboard", filename = "editor")
@Config.LangKey("dashboard.config.editor")
public class DashboardConfig {

    @Config.Comment("Show the grid in the HUD editor and snap elements to it")
    @Config.LangKey("dashboard.config.editor.show_grid")
    @Config.DefaultBoolean(true)
    public static boolean showGrid;

    @Config.Comment("Show the element names inside their boxes in the HUD editor")
    @Config.LangKey("dashboard.config.editor.show_labels")
    @Config.DefaultBoolean(true)
    public static boolean showLabels;
}
