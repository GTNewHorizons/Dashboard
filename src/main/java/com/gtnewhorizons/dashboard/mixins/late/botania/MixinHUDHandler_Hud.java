package com.gtnewhorizons.dashboard.mixins.late.botania;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.compat.BotaniaCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/** Botania draws these during the normal HUD without resetting the matrix, so wrapping each draw call is enough. */
@Pseudo
@Mixin(targets = "vazkii.botania.client.core.handler.HUDHandler", remap = false)
public class MixinHUDHandler_Hud {

    @Inject(method = "renderManaInvBar", at = @At("HEAD"), cancellable = true)
    private void dashboard$moveManaBar(ScaledResolution resolution, boolean creative, int totalMana, int totalMaxMana,
        CallbackInfo ci) {
        if (!BotaniaCompat.beginManaBar(resolution, creative, totalMana)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderManaInvBar", at = @At("RETURN"))
    private void dashboard$restoreManaBar(CallbackInfo ci) {
        BotaniaCompat.endManaBar();
    }

    /** Drawn by BotaniaCompat at the end of the HUD instead. */
    @WrapOperation(
        method = "onDrawScreenPre",
        at = @At(
            value = "INVOKE",
            target = "Lvazkii/botania/common/item/equipment/bauble/ItemFlightTiara;renderHUD(Lnet/minecraft/client/gui/ScaledResolution;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)V"))
    private void dashboard$skipFlightBar(ScaledResolution resolution, EntityPlayer player, ItemStack tiara,
        Operation<Void> original) {}

    @Inject(
        method = "onDrawScreenPost",
        at = @At(
            value = "INVOKE",
            target = "Lvazkii/botania/client/core/handler/BossBarHandler;render(Lnet/minecraft/client/gui/ScaledResolution;)V"))
    private void dashboard$drawFlightBar(RenderGameOverlayEvent.Post event, CallbackInfo ci) {
        BotaniaCompat.drawFlightBar(event.resolution);
    }

    /** Only counts as showing while a Botania boss is around. */
    @WrapOperation(
        method = "onDrawScreenPost",
        at = @At(
            value = "INVOKE",
            target = "Lvazkii/botania/client/core/handler/BossBarHandler;render(Lnet/minecraft/client/gui/ScaledResolution;)V"))
    private void dashboard$moveBossBar(ScaledResolution resolution, Operation<Void> original) {
        if (BotaniaCompat.hasBoss()) {
            HudRegistry.render(BotaniaCompat.BOSS_BAR, () -> original.call(resolution));
        } else {
            original.call(resolution);
        }
    }

    /** Only the text, which is drawn just while it is fading out after a mode switch. Botania ignores the result. */
    @WrapOperation(
        method = "renderWandModeDisplay",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I",
            remap = true))
    private int dashboard$moveWandMode(FontRenderer font, String text, int x, int y, int color,
        Operation<Integer> original) {
        HudRegistry.render(BotaniaCompat.WAND_MODE, () -> original.call(font, text, x, y, color));
        return 0;
    }

    @Inject(method = "renderNearIndexDisplay", at = @At("HEAD"), cancellable = true)
    private void dashboard$moveCorporeaHint(ScaledResolution resolution, CallbackInfo ci) {
        if (!BotaniaCompat.beginCorporeaHint(resolution)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderNearIndexDisplay", at = @At("RETURN"))
    private void dashboard$restoreCorporeaHint(CallbackInfo ci) {
        BotaniaCompat.endCorporeaHint();
    }

    @Inject(
        method = "onDrawScreenPost",
        at = { @At(value = "INVOKE", target = WAND_HUD), @At(value = "INVOKE", target = LEXICON),
            @At(value = "INVOKE", target = POOL_RECIPE), @At(value = "INVOKE", target = ALTAR),
            @At(value = "INVOKE", target = RUNE_ALTAR), @At(value = "INVOKE", target = CRYSTAL_CUBE),
            @At(value = "INVOKE", target = ANCHOR_TEXT, remap = true), @At(value = "INVOKE", target = CRAFTING_HALO),
            @At(value = "INVOKE", target = SEXTANT), @At(value = "INVOKE", target = MONOCLE) })
    private void dashboard$beginCrosshairInfo(CallbackInfo ci) {
        BotaniaCompat.beginCrosshairInfo();
    }

    @Inject(
        method = "onDrawScreenPost",
        at = { @At(value = "INVOKE", target = WAND_HUD, shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = LEXICON, shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = POOL_RECIPE, shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = ALTAR, shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = RUNE_ALTAR, shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = CRYSTAL_CUBE, shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = ANCHOR_TEXT, shift = At.Shift.AFTER, remap = true),
            @At(value = "INVOKE", target = CRAFTING_HALO, shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = SEXTANT, shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = MONOCLE, shift = At.Shift.AFTER) })
    private void dashboard$endCrosshairInfo(CallbackInfo ci) {
        BotaniaCompat.endCrosshairInfo();
    }

    /** Called every frame, but only counts as showing while the popup is up. */
    @WrapOperation(
        method = "onDrawScreenPost",
        at = @At(
            value = "INVOKE",
            target = "Lvazkii/botania/client/core/handler/ItemsRemainingRenderHandler;render(Lnet/minecraft/client/gui/ScaledResolution;F)V"))
    private void dashboard$moveItemsRemaining(ScaledResolution resolution, float partialTicks,
        Operation<Void> original) {
        if (!BotaniaCompat.isShowingItemsRemaining()) {
            original.call(resolution, partialTicks);
            return;
        }
        BotaniaCompat.beginCrosshairInfo();
        try {
            original.call(resolution, partialTicks);
        } finally {
            BotaniaCompat.endCrosshairInfo();
        }
    }

    @Unique
    private static final String WAND_HUD = "Lvazkii/botania/api/wand/IWandHUD;renderHUD(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/ScaledResolution;Lnet/minecraft/world/World;III)V";
    @Unique
    private static final String LEXICON = "Lvazkii/botania/client/core/handler/HUDHandler;drawLexiconHUD(Lnet/minecraft/item/ItemStack;Lnet/minecraft/block/Block;Lnet/minecraft/util/MovingObjectPosition;Lnet/minecraft/client/gui/ScaledResolution;)V";
    @Unique
    private static final String POOL_RECIPE = "Lvazkii/botania/client/core/handler/HUDHandler;renderPoolRecipeHUD(Lnet/minecraft/client/gui/ScaledResolution;Lvazkii/botania/common/block/tile/mana/TilePool;Lnet/minecraft/item/ItemStack;)V";
    @Unique
    private static final String ALTAR = "Lvazkii/botania/common/block/tile/TileAltar;renderHUD(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/ScaledResolution;)V";
    @Unique
    private static final String RUNE_ALTAR = "Lvazkii/botania/common/block/tile/TileRuneAltar;renderHUD(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/ScaledResolution;)V";
    @Unique
    private static final String CRYSTAL_CUBE = "Lvazkii/botania/client/core/handler/HUDHandler;renderCrystalCubeHUD(Lnet/minecraft/client/gui/ScaledResolution;Lvazkii/botania/common/block/tile/corporea/TileCorporeaCrystalCube;)V";
    @Unique
    private static final String ANCHOR_TEXT = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I";
    @Unique
    private static final String CRAFTING_HALO = "Lvazkii/botania/common/item/ItemCraftingHalo;renderHUD(Lnet/minecraft/client/gui/ScaledResolution;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)V";
    @Unique
    private static final String SEXTANT = "Lvazkii/botania/common/item/ItemSextant;renderHUD(Lnet/minecraft/client/gui/ScaledResolution;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)V";
    @Unique
    private static final String MONOCLE = "Lvazkii/botania/common/item/equipment/bauble/ItemMonocle;renderHUD(Lnet/minecraft/client/gui/ScaledResolution;Lnet/minecraft/entity/player/EntityPlayer;)V";
}
