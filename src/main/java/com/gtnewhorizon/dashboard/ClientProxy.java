package com.gtnewhorizon.dashboard;

import com.gtnewhorizon.dashboard.client.HudEditorKeybind;
import com.gtnewhorizon.dashboard.client.VanillaHudElements;

import cpw.mods.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        HudEditorKeybind.register();
        VanillaHudElements.register();
    }
}
