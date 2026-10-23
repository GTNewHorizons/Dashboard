package com.gtnewhorizons.dashboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizons.dashboard.client.BarNumbers;
import com.gtnewhorizons.dashboard.client.BarThemes;
import com.gtnewhorizons.dashboard.client.HudEditorKeybind;
import com.gtnewhorizons.dashboard.client.ImageElements;
import com.gtnewhorizons.dashboard.client.InventoryHud;
import com.gtnewhorizons.dashboard.client.PickupFeed;
import com.gtnewhorizons.dashboard.client.StatusEffects;
import com.gtnewhorizons.dashboard.client.VanillaHudElements;
import com.gtnewhorizons.dashboard.compat.Mods;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        try {
            ConfigurationManager.registerConfig(DashboardConfig.class);
            ConfigurationManager.registerConfig(DashboardOtherConfig.class);
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
        InventoryHud.register();
        PickupFeed.register();
        IReloadableResourceManager resources = (IReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager();
        resources.registerReloadListener(BarThemes::load);
        resources.registerReloadListener(ImageElements::load);
    }

    /** After every mod's init, so mods that register their own elements come first. */
    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        Mods.registerLoadedCompat();
    }
}
