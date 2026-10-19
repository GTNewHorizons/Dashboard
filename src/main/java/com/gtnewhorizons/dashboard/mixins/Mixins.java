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
    CHAT_IMAGES(
        new MixinBuilder("Move and zoom image elements while chat is open").addClientMixins("MixinGuiChat_Images")
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
        .setPhase(Phase.LATE)),
    DRACONIC_HUD(new MixinBuilder("Make the Draconic Evolution info box, armor HUD and magnet icon movable")
        .addClientMixins("draconic.MixinHudHandler_Hud", "draconic.MixinItemDisplayManager_Hud")
        .addRequiredMod(Mods.DRACONIC)
        .setPhase(Phase.LATE)),
    THAUMCRAFT_HUD(new MixinBuilder("Make the Thaumcraft HUD movable")
        .addClientMixins(
            "thaumcraft.MixinClientTickEventsFML_Hud",
            "thaumcraft.MixinREHNotifyHandler_Hud",
            "thaumcraft.ClientTickEventsFMLInvoker")
        .addRequiredMod(Mods.THAUMCRAFT)
        .setPhase(Phase.LATE)),
    THAUMIC_TINKERER_HUD(new MixinBuilder("Make the Thaumic Tinkerer HUD movable")
        .addClientMixins(
            "thaumictinkerer.MixinHUDHandler_Hud",
            "thaumictinkerer.MixinToolModeHUDHandler_Hud",
            "thaumictinkerer.MixinSoulHeartClientHandler_Hud")
        .addRequiredMod(Mods.THAUMIC_TINKERER)
        .setPhase(Phase.LATE)),
    TAINTED_MAGIC_HUD(new MixinBuilder("Make the Tainted Magic Katana charge and Disassembler mode movable")
        .addClientMixins("taintedmagic.MixinItemKatana_Hud", "taintedmagic.MixinItemThaumicDisassembler_Hud")
        .addRequiredMod(Mods.TAINTED_MAGIC)
        .setPhase(Phase.LATE)),
    WITCHERY_HUD(new MixinBuilder("Make the Witchery HUD movable")
        .addClientMixins("witchery.MixinClientEvents_Hud", "witchery.MixinPlayerRender_Hud")
        .addRequiredMod(Mods.WITCHERY)
        .setPhase(Phase.LATE)),
    IC2_HUD(new MixinBuilder("Make the IC2 helmet HUD movable").addClientMixins("ic2.MixinGuiOverlayer_Hud")
        .addRequiredMod(Mods.IC2)
        .setPhase(Phase.LATE)),
    BATTLEGEAR_HUD(new MixinBuilder("Make the Battlegear shield bar and quiver slots movable")
        .addClientMixins("battlegear2.MixinBattlegearInGameGUI_Hud")
        .addRequiredMod(Mods.BATTLEGEAR)
        .setPhase(Phase.LATE)),
    NATURES_COMPASS_HUD(new MixinBuilder("Make the Nature's Compass biome search text movable")
        .addClientMixins("naturescompass.MixinRenderTickHandler_Hud")
        .addRequiredMod(Mods.NATURES_COMPASS)
        .setPhase(Phase.LATE)),
    BOGOSORTER_HUD(new MixinBuilder("Make the Bogosorter usage ticker movable and its hotbar swap follow the hotbar")
        .addClientMixins("bogosorter.MixinUsageTickerElement_Hud", "bogosorter.MixinHotbarSwap_Hud")
        .addRequiredMod(Mods.BOGOSORTER)
        .setPhase(Phase.LATE)),
    GTNHLIB_HUD(new MixinBuilder("Make the GTNHLib text above the hotbar and titles movable")
        .addClientMixins("gtnhlib.MixinAboveHotbarHUD_Hud", "gtnhlib.MixinTitleRenderer_Hud")
        .setPhase(Phase.LATE)),
    INFERNAL_MOBS_HUD(new MixinBuilder("Make the Infernal Mobs health bar movable")
        .addClientMixins("infernalmobs.MixinInfernalMobsClient_Hud")
        .addRequiredMod(Mods.INFERNAL_MOBS)
        .setPhase(Phase.LATE)),
    ET_FUTURUM_HUD(new MixinBuilder("Make the Et Futurum sound captions movable")
        .addClientMixins("etfuturum.MixinGuiSubtitles_Hud")
        .addRequiredMod(Mods.ET_FUTURUM)
        .setPhase(Phase.LATE)),
    HEE_HUD(new MixinBuilder("Make the HEE notes and energy cluster info movable")
        .addClientMixins("hee.MixinOverlayManager_Hud")
        .addRequiredMod(Mods.HEE)
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
