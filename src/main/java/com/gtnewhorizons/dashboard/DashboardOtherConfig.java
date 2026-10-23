package com.gtnewhorizons.dashboard;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizons.dashboard.client.PickupFeed;

/** Settings that can only be changed in the config, unlike the ones in {@link DashboardConfig}. */
@Config(modid = Dashboard.MODID, category = "other", configSubDirectory = "dashboard", filename = "editor")
@Config.LangKey("dashboard.config.other")
public class DashboardOtherConfig {

    @Config.Comment("Show pictures from resource packs and the config/dashboard/images folder, toggled with a key")
    @Config.LangKey("dashboard.config.other.show_images")
    @Config.DefaultBoolean(true)
    public static boolean showImages;

    @Config.Comment("How long each row of the item pickups shows, in seconds")
    @Config.LangKey("dashboard.config.other.pickup_feed_seconds")
    @Config.DefaultInt(PickupFeed.DEFAULT_SECONDS)
    @Config.RangeInt(min = 1, max = PickupFeed.MAX_SECONDS)
    public static int pickupFeedSeconds;

    @Config.Comment("How long each row of the item pickups takes to slide away, in milliseconds, 0 to just disappear")
    @Config.LangKey("dashboard.config.other.pickup_feed_slide_out")
    @Config.DefaultInt(PickupFeed.DEFAULT_SLIDE_OUT_MILLIS)
    @Config.RangeInt(min = 0, max = PickupFeed.MAX_SLIDE_OUT_MILLIS)
    public static int pickupFeedSlideOutMillis;
}
