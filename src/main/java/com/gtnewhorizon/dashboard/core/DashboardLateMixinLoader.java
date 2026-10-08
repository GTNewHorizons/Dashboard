package com.gtnewhorizon.dashboard.core;

import java.util.List;
import java.util.Set;

import com.gtnewhorizon.dashboard.mixins.Mixins;
import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;

/** Loads the mixins into other mods, which can only be applied once those mods are found. */
@LateMixin
public class DashboardLateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.dashboard.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        return IMixins.getLateMixins(Mixins.class, loadedMods);
    }
}
