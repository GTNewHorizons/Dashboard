package com.gtnewhorizons.dashboard.client;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizons.dashboard.DashboardConfig;
import com.gtnewhorizons.dashboard.api.HudElement;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Which mods' elements the HUD editor shows. Only Minecraft and Dashboard at first, so the editor starts tidy, and
 * players tick the mods they want to arrange. Left out mods' HUD still shows in game.
 */
@SideOnly(Side.CLIENT)
public final class EditorModFilter {

    private static final Set<String> SHOWN = new HashSet<>();
    private static String[] parsedList;

    private EditorModFilter() {}

    /** Lower case, since mod ids are matched ignoring case. */
    static String getModKey(HudElement element) {
        return element.getModId()
            .toLowerCase(Locale.ROOT);
    }

    public static boolean isShown(HudElement element) {
        return isShown(getModKey(element));
    }

    static boolean isShown(String modKey) {
        return getShown().contains(modKey);
    }

    /** Several mods at once, so the config is saved once. */
    static void setShown(Collection<String> modKeys, boolean shown) {
        Set<String> shownMods = new HashSet<>(getShown());
        if (shown) {
            shownMods.addAll(modKeys);
        } else {
            shownMods.removeAll(modKeys);
        }
        DashboardConfig.shownEditorMods = shownMods.toArray(new String[0]);
        ConfigurationManager.save(DashboardConfig.class);
    }

    /** Asked every frame for every element, so the list is only read again when it changes. */
    private static Set<String> getShown() {
        // The config gives a new list whenever it changes
        String[] list = DashboardConfig.shownEditorMods;
        if (list != parsedList) {
            parsedList = list;
            SHOWN.clear();
            for (String modId : list) {
                SHOWN.add(
                    modId.trim()
                        .toLowerCase(Locale.ROOT));
            }
        }
        return SHOWN;
    }
}
