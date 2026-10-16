package com.gtnewhorizons.dashboard.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import com.gtnewhorizons.dashboard.compat.Mods;

public enum Mixins implements IMixins {

    HOTBAR_TEXT(
        new MixinBuilder("Make the text above the hotbar movable").addClientMixins("MixinGuiIngameForge_HudText")
            .setPhase(Phase.EARLY)),
    HOTBAR_NUMBERS(new MixinBuilder("Draw the hotbar slot numbers under the items")
        .addClientMixins("MixinGuiIngameForge_HotbarNumbers")
        .setPhase(Phase.EARLY)),
    HIDDEN_HELD_ITEMS(new MixinBuilder("Draw an empty hand for hidden items in first person")
        .addClientMixins("MixinItemRenderer_HiddenItems")
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
        .setPhase(Phase.EARLY)),
    GRAVISUITE_HUD(new MixinBuilder("Make the GraviSuite status text movable")
        .addClientMixins("gravisuite.MixinClientTickHandler_Hud")
        .addRequiredMod(Mods.GRAVISUITE)
        .setPhase(Phase.LATE)),
    GALACTICRAFT_HUD(new MixinBuilder("Make the Galacticraft overlays movable")
        .addClientMixins(
            "galacticraft.MixinOverlayOxygenTanks_Hud",
            "galacticraft.MixinOverlayOxygenWarning_Hud",
            "galacticraft.MixinOverlayLaunchCountdown_Hud",
            "galacticraft.MixinOverlayLander_Hud",
            "galacticraft.MixinOverlayRocket_Hud",
            "galacticraft.MixinTickHandlerClient_Preview")
        .addRequiredMod(Mods.GALACTICRAFT)
        .setPhase(Phase.LATE)),
    GALAXYSPACE_HUD(new MixinBuilder("Make the GalaxySpace pressure bar movable")
        .addClientMixins("galaxyspace.MixinOverlayAtmosphericPressure_Hud")
        .addRequiredMod(Mods.GALAXYSPACE)
        .setPhase(Phase.LATE)),
    WAILA_HUD(new MixinBuilder("Make the Waila info box movable")
        .addClientMixins("waila.MixinOverlayRenderer_Hud", "waila.MixinWailaTickHandler_Hud")
        .addRequiredMod(Mods.WAILA)
        .setPhase(Phase.LATE)),
    ADVENTURE_BACKPACK_HUD(new MixinBuilder("Make the Adventure Backpack potion icons and tanks movable")
        .addClientMixins("adventurebackpack.MixinGuiOverlay_Hud")
        .addRequiredMod(Mods.ADVENTURE_BACKPACK)
        .setPhase(Phase.LATE)),
    BOTANIA_HUD(
        new MixinBuilder("Make the Botania bars and texts movable").addClientMixins("botania.MixinHUDHandler_Hud")
            .addRequiredMod(Mods.BOTANIA)
            .setPhase(Phase.LATE)),
    BACKHAND_HUD(new MixinBuilder("Make the Backhand off-hand slot movable apart from the hotbar")
        .addClientMixins("backhand.MixinClientEventHandler_Hud")
        .addRequiredMod(Mods.BACKHAND)
        .setPhase(Phase.LATE)),
    INGAMEINFO_HUD(new MixinBuilder("Make the InGameInfoXML text areas movable")
        .addClientMixins(
            "ingameinfo.MixinInfoText_Hud",
            "ingameinfo.MixinInGameInfoCore_Hud",
            "ingameinfo.MixinTicker_Preview")
        .addRequiredMod(Mods.INGAMEINFO)
        .setPhase(Phase.LATE));

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
