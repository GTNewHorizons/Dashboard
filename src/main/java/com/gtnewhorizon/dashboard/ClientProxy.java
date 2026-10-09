package com.gtnewhorizon.dashboard;

import com.gtnewhorizon.dashboard.client.HudEditorKeybind;
import com.gtnewhorizon.dashboard.client.VanillaHudElements;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

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
    }
}
