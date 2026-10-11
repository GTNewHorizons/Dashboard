package com.gtnewhorizons.dashboard.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    HOTBAR_TEXT(
        new MixinBuilder("Make the text above the hotbar movable").addClientMixins("MixinGuiIngameForge_HudText")
            .setPhase(Phase.EARLY));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
