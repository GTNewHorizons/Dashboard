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
    PICKUP_FEED(
        new MixinBuilder("Show picked up items in the pickup feed").addClientMixins("MixinNetHandlerPlayClient_Pickup")
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
    GTNHLIB_HUD(new MixinBuilder("Make the GTNHLib text above the hotbar and titles movable")
        .addClientMixins("gtnhlib.MixinAboveHotbarHUD_Hud", "gtnhlib.MixinTitleRenderer_Hud")
        .setPhase(Phase.LATE)),
    SERVER_UTILITIES_HUD(new MixinBuilder("Move ServerUtilities notifications with the text above the hotbar")
        .addClientMixins("serverutilities.MixinNotificationWidget_Hud")
        .addRequiredMod(Mods.SERVER_UTILITIES)
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
