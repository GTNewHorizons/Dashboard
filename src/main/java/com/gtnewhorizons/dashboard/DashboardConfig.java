package com.gtnewhorizons.dashboard;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizons.dashboard.client.Crosshairs;
import com.gtnewhorizons.dashboard.client.StatusEffects;

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

    @Config.Comment("Show the background behind each status effect")
    @Config.LangKey("dashboard.config.editor.status_effect_background")
    @Config.DefaultBoolean(true)
    public static boolean statusEffectBackground;

    @Config.Comment("Show the icon of each status effect")
    @Config.LangKey("dashboard.config.editor.status_effect_icon")
    @Config.DefaultBoolean(true)
    public static boolean statusEffectIcon;

    @Config.Comment("Show the name and level of each status effect")
    @Config.LangKey("dashboard.config.editor.status_effect_name")
    @Config.DefaultBoolean(true)
    public static boolean statusEffectName;

    @Config.Comment("Show the time left of each status effect")
    @Config.LangKey("dashboard.config.editor.status_effect_time")
    @Config.DefaultBoolean(true)
    public static boolean statusEffectTime;

    @Config.Comment("Put the status effects in a row instead of a column")
    @Config.LangKey("dashboard.config.editor.status_effect_horizontal")
    @Config.DefaultBoolean(false)
    public static boolean statusEffectHorizontal;

    @Config.Comment("Pixels between the status effects, negative to overlap them")
    @Config.LangKey("dashboard.config.editor.status_effect_spacing")
    @Config.DefaultInt(StatusEffects.DEFAULT_SPACING)
    @Config.RangeInt(min = StatusEffects.MIN_SPACING, max = StatusEffects.MAX_SPACING)
    public static int statusEffectSpacing;

    @Config.Comment("Pulse status effects with this many seconds left, in steps of 5, 0 to never pulse")
    @Config.LangKey("dashboard.config.editor.status_effect_pulse_seconds")
    @Config.DefaultInt(StatusEffects.DEFAULT_PULSE_SECONDS)
    @Config.RangeInt(min = 0, max = StatusEffects.MAX_PULSE_SECONDS)
    public static int statusEffectPulseSeconds;

    @Config.Comment("Show good status effects before bad ones")
    @Config.LangKey("dashboard.config.editor.status_effect_group")
    @Config.DefaultBoolean(true)
    public static boolean statusEffectGroup;

    @Config.Comment("Sort status effects by time left instead of the order they were added")
    @Config.LangKey("dashboard.config.editor.status_effect_sort_by_time")
    @Config.DefaultBoolean(false)
    public static boolean statusEffectSortByTime;

    @Config.Comment("Show infinite status effects and the ones kept up by a beacon")
    @Config.LangKey("dashboard.config.editor.status_effect_permanent")
    @Config.DefaultBoolean(true)
    public static boolean statusEffectPermanent;
}
