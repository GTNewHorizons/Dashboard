package com.gtnewhorizons.dashboard;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizons.dashboard.client.Crosshairs;
import com.gtnewhorizons.dashboard.client.HotbarNumbers;
import com.gtnewhorizons.dashboard.client.PickupFeed;
import com.gtnewhorizons.dashboard.client.ScreenOverlays;
import com.gtnewhorizons.dashboard.client.StatusEffects;

@Config(modid = Dashboard.MODID, category = "editor", configSubDirectory = "dashboard", filename = "editor")
@Config.LangKey("dashboard.config.editor")
public class DashboardConfig {

    @Config.Comment("Show the grid in the HUD editor and snap elements to it")
    @Config.LangKey("dashboard.config.editor.show_grid")
    @Config.DefaultBoolean(false)
    public static boolean showGrid;

    @Config.Comment("Show sample content for elements that are not showing in the HUD editor")
    @Config.LangKey("dashboard.config.editor.show_preview")
    @Config.DefaultBoolean(false)
    public static boolean showPreview;

    @Config.Comment("Show the element names inside their boxes in the HUD editor")
    @Config.LangKey("dashboard.config.editor.show_labels")
    @Config.DefaultBoolean(true)
    public static boolean showLabels;

    @Config.Comment("Show the element position, measured from the middle of the screen, next to its box in the HUD editor")
    @Config.LangKey("dashboard.config.editor.show_positions")
    @Config.DefaultBoolean(false)
    public static boolean showPositions;

    @Config.Comment("Show pictures from resource packs and the config/dashboard/images folder, toggled with a key")
    @Config.LangKey("dashboard.config.editor.show_images")
    @Config.DefaultBoolean(true)
    public static boolean showImages;

    @Config.Comment("Mod ids whose elements show in the HUD editor. Other mods are left out, to tidy it. They still show in game")
    @Config.LangKey("dashboard.config.editor.shown_editor_mods")
    @Config.DefaultStringList({ "minecraft", Dashboard.MODID })
    public static String[] shownEditorMods;

    @Config.Comment("Crosshair shape: 0 is vanilla, 1 and up are the shapes on the crosshair sheet")
    @Config.LangKey("dashboard.config.editor.crosshair_style")
    @Config.DefaultInt(0)
    @Config.RangeInt(min = 0, max = Crosshairs.MAX_STYLES)
    public static int crosshairStyle;

    @Config.Comment("Crosshair size, 1 to 5 times")
    @Config.LangKey("dashboard.config.editor.crosshair_size")
    @Config.DefaultInt(1)
    @Config.RangeInt(min = 1, max = Crosshairs.MAX_SIZE)
    public static int crosshairSize;

    @Config.Comment("Let Tinkers' Construct draw its own crosshair while holding its bows, crossbows and throwing weapons")
    @Config.LangKey("dashboard.config.editor.tinkers_crosshair")
    @Config.DefaultBoolean(true)
    public static boolean tinkersCrosshair;

    @Config.Comment("Show the bound key on each hotbar slot")
    @Config.LangKey("dashboard.config.editor.hotbar_numbers")
    @Config.DefaultBoolean(false)
    public static boolean hotbarNumbers;

    @Config.Comment("Show the key bound in Controls instead of the slot number 1 to 9")
    @Config.LangKey("dashboard.config.editor.hotbar_number_bound_key")
    @Config.DefaultBoolean(true)
    public static boolean hotbarNumberBoundKey;

    @Config.Comment("Draw the hotbar slot keys under the items instead of over them")
    @Config.LangKey("dashboard.config.editor.hotbar_number_under_items")
    @Config.DefaultBoolean(false)
    public static boolean hotbarNumberUnderItems;

    @Config.Comment("Where the key sits in each hotbar slot: 0 top left, 1 top, 2 top right, 3 left, 4 middle, 5 right, 6 bottom left, 7 bottom, 8 bottom right")
    @Config.LangKey("dashboard.config.editor.hotbar_number_position")
    @Config.DefaultInt(0)
    @Config.RangeInt(min = 0, max = HotbarNumbers.POSITION_COUNT - 1)
    public static int hotbarNumberPosition;

    @Config.Comment("Size of the hotbar slot keys, in percent of normal text")
    @Config.LangKey("dashboard.config.editor.hotbar_number_size")
    @Config.DefaultInt(HotbarNumbers.DEFAULT_SIZE)
    @Config.RangeInt(min = HotbarNumbers.MIN_SIZE, max = HotbarNumbers.MAX_SIZE)
    public static int hotbarNumberSize;

    @Config.Comment("Color of the hotbar slot keys, one of the 16 chat colors: 0 black to 15 white")
    @Config.LangKey("dashboard.config.editor.hotbar_number_color")
    @Config.DefaultInt(HotbarNumbers.DEFAULT_COLOR)
    @Config.RangeInt(min = 0, max = HotbarNumbers.COLOR_COUNT - 1)
    public static int hotbarNumberColor;

    @Config.Comment("Items drawn as an empty hand in first person, as modid:name for every meta or modid:name:meta")
    @Config.LangKey("dashboard.config.editor.hidden_held_items")
    @Config.DefaultStringList({})
    public static String[] hiddenHeldItems;

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

    @Config.Comment("How visible the pumpkin blur is, in percent")
    @Config.LangKey("dashboard.config.editor.pumpkin_opacity")
    @Config.DefaultInt(ScreenOverlays.MAX_PERCENT)
    @Config.RangeInt(min = 0, max = ScreenOverlays.MAX_PERCENT)
    public static int pumpkinOpacity;

    @Config.Comment("How visible the nether portal swirl is, in percent")
    @Config.LangKey("dashboard.config.editor.portal_opacity")
    @Config.DefaultInt(ScreenOverlays.MAX_PERCENT)
    @Config.RangeInt(min = 0, max = ScreenOverlays.MAX_PERCENT)
    public static int portalOpacity;

    @Config.Comment("How visible the flames are while burning, in percent")
    @Config.LangKey("dashboard.config.editor.fire_opacity")
    @Config.DefaultInt(ScreenOverlays.MAX_PERCENT)
    @Config.RangeInt(min = 0, max = ScreenOverlays.MAX_PERCENT)
    public static int fireOpacity;

    @Config.Comment("How high the flames reach while burning, in percent")
    @Config.LangKey("dashboard.config.editor.fire_height")
    @Config.DefaultInt(ScreenOverlays.MAX_PERCENT)
    @Config.RangeInt(min = 0, max = ScreenOverlays.MAX_PERCENT)
    public static int fireHeight;

    @Config.Comment("How visible the underwater overlay is, in percent")
    @Config.LangKey("dashboard.config.editor.water_opacity")
    @Config.DefaultInt(ScreenOverlays.MAX_PERCENT)
    @Config.RangeInt(min = 0, max = ScreenOverlays.MAX_PERCENT)
    public static int waterOpacity;

    @Config.Comment("How dark the corners of the screen get, in percent")
    @Config.LangKey("dashboard.config.editor.vignette_strength")
    @Config.DefaultInt(ScreenOverlays.MAX_PERCENT)
    @Config.RangeInt(min = 0, max = ScreenOverlays.MAX_PERCENT)
    public static int vignetteStrength;

    @Config.Comment("How dark the screen gets while falling asleep, in percent")
    @Config.LangKey("dashboard.config.editor.sleep_fade_strength")
    @Config.DefaultInt(ScreenOverlays.MAX_PERCENT)
    @Config.RangeInt(min = 0, max = ScreenOverlays.MAX_PERCENT)
    public static int sleepFadeStrength;

    @Config.Comment("How much the camera tilts when you get hurt, in percent")
    @Config.LangKey("dashboard.config.editor.hurt_shake_strength")
    @Config.DefaultInt(ScreenOverlays.MAX_PERCENT)
    @Config.RangeInt(min = 0, max = ScreenOverlays.MAX_PERCENT)
    public static int hurtShakeStrength;

    @Config.Comment("How much the screen wobbles from nausea and portals, in percent")
    @Config.LangKey("dashboard.config.editor.nausea_strength")
    @Config.DefaultInt(ScreenOverlays.MAX_PERCENT)
    @Config.RangeInt(min = 0, max = ScreenOverlays.MAX_PERCENT)
    public static int nauseaStrength;

    @Config.Comment("Flash the screen while night vision runs out")
    @Config.LangKey("dashboard.config.editor.night_vision_flicker")
    @Config.DefaultBoolean(true)
    public static boolean nightVisionFlicker;

    @Config.Comment("How long each row of the item pickups shows, in seconds")
    @Config.LangKey("dashboard.config.editor.pickup_feed_seconds")
    @Config.DefaultInt(PickupFeed.DEFAULT_SECONDS)
    @Config.RangeInt(min = 1, max = PickupFeed.MAX_SECONDS)
    public static int pickupFeedSeconds;

    @Config.Comment("How long each row of the item pickups takes to slide away, in milliseconds, 0 to just disappear")
    @Config.LangKey("dashboard.config.editor.pickup_feed_slide_out")
    @Config.DefaultInt(PickupFeed.DEFAULT_SLIDE_OUT_MILLIS)
    @Config.RangeInt(min = 0, max = PickupFeed.MAX_SLIDE_OUT_MILLIS)
    public static int pickupFeedSlideOutMillis;
}
