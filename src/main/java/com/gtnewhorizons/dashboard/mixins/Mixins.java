package com.gtnewhorizons.dashboard.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    HOTBAR_TEXT(
        new MixinBuilder("Make the text above the hotbar movable").addClientMixins("MixinGuiIngameForge_HudText")
            .setPhase(Phase.EARLY)),
    HOTBAR_NUMBERS(new MixinBuilder("Draw the hotbar slot numbers under the items")
        .addClientMixins("MixinGuiIngameForge_HotbarNumbers")
        .setPhase(Phase.EARLY)),
    ACHIEVEMENT(new MixinBuilder("Make the achievement popup movable").addClientMixins("MixinGuiAchievement")
        .setPhase(Phase.EARLY)),
    BOSS_HEALTH(new MixinBuilder("Remember the boss for its health number").addClientMixins("MixinBossStatus")
        .setPhase(Phase.EARLY)),
    SCREEN_OVERLAYS(new MixinBuilder("Change the screen overlays")
        .addClientMixins(
            "MixinGuiIngame_Overlays",
            "MixinItemRenderer_Fire",
            "MixinItemRenderer_Water",
            "MixinGuiIngameForge_SleepFade",
            "MixinEntityRenderer_Camera")
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
