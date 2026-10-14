package com.gtnewhorizons.dashboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizons.dashboard.client.BarNumbers;
import com.gtnewhorizons.dashboard.client.BarThemes;
import com.gtnewhorizons.dashboard.client.HudEditorKeybind;
import com.gtnewhorizons.dashboard.client.StatusEffects;
import com.gtnewhorizons.dashboard.client.VanillaHudElements;
import com.gtnewhorizons.dashboard.compat.AdventureBackpackCompat;
import com.gtnewhorizons.dashboard.compat.GalacticraftCompat;
import com.gtnewhorizons.dashboard.compat.GalaxySpaceCompat;
import com.gtnewhorizons.dashboard.compat.GraviSuiteCompat;
import com.gtnewhorizons.dashboard.compat.Mods;
import com.gtnewhorizons.dashboard.compat.WailaCompat;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        try {
            ConfigurationManager.registerConfig(DashboardConfig.class);
        } catch (ConfigException e) {
            Dashboard.LOG.error("Failed to load the Dashboard config, using defaults", e);
        }
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        HudEditorKeybind.register();
        VanillaHudElements.register();
        BarNumbers.register();
        StatusEffects.register();
        if (Mods.GRAVISUITE.isModLoaded()) {
            GraviSuiteCompat.register();
        }
        if (Mods.GALACTICRAFT.isModLoaded()) {
            GalacticraftCompat.register();
        }
        if (Mods.GALAXYSPACE.isModLoaded()) {
            GalaxySpaceCompat.register();
        }
        if (Mods.WAILA.isModLoaded()) {
            WailaCompat.register();
        }
        if (Mods.ADVENTURE_BACKPACK.isModLoaded()) {
            AdventureBackpackCompat.register();
        }
        ((IReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(BarThemes::load);
    }
}
