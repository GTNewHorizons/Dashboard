package com.gtnewhorizons.dashboard.mixins.late.thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.compat.ThaumcraftCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import thaumcraft.client.lib.ClientTickEventsFML;

/**
 * Moves or hides Thaumcraft's HUD. Each method pushes the matrix and resets it before drawing, then pops it at the end,
 * so hiding happens before the method starts and the move right after the modelview reset (the second
 * {@code glLoadIdentity}). Thaumcraft's own pop undoes the move.
 */
@Mixin(value = ClientTickEventsFML.class, remap = false)
public class MixinClientTickEventsFML_Hud {

    @Unique
    private static final String MODELVIEW_RESET = "Lorg/lwjgl/opengl/GL11;glLoadIdentity()V";

    @Inject(method = "<init>", at = @At("RETURN"))
    private void dashboard$rememberHandler(CallbackInfo ci) {
        ThaumcraftCompat.setTickEventsHandler((ClientTickEventsFML) (Object) this);
    }

    /** Thaumcraft only draws its HUD while the game has focus, which it has not while the HUD editor is open. */
    @ModifyExpressionValue(
        method = "renderTick",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;inGameHasFocus:Z", remap = true))
    private boolean dashboard$drawInHudEditor(boolean inGameHasFocus) {
        return inGameHasFocus || HudRegistry.isLayoutScreenOpen();
    }

    @Inject(method = "renderCastingWandHud", at = @At("HEAD"), cancellable = true)
    private void dashboard$hideWandDial(CallbackInfo ci) {
        if (!HudRegistry.shouldRender(ThaumcraftCompat.WAND_DIAL)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "renderCastingWandHud",
        at = @At(value = "INVOKE", target = MODELVIEW_RESET, ordinal = 1, shift = At.Shift.AFTER))
    private void dashboard$moveWandDial(CallbackInfo ci) {
        HudRegistry.translateToPosition(ThaumcraftCompat.WAND_DIAL);
    }

    @Inject(method = "renderSanityHud", at = @At("HEAD"), cancellable = true)
    private void dashboard$hideWarpMeter(CallbackInfo ci) {
        if (!HudRegistry.shouldRender(ThaumcraftCompat.WARP_METER)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "renderSanityHud",
        at = @At(value = "INVOKE", target = MODELVIEW_RESET, ordinal = 1, shift = At.Shift.AFTER))
    private void dashboard$moveWarpMeter(CallbackInfo ci) {
        HudRegistry.translateToPosition(ThaumcraftCompat.WARP_METER);
    }

    @Inject(method = "renderRunicArmorBar", at = @At("HEAD"), cancellable = true)
    private void dashboard$hideRunicShield(CallbackInfo ci) {
        if (!HudRegistry.shouldRender(ThaumcraftCompat.RUNIC_SHIELD)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "renderRunicArmorBar",
        at = @At(value = "INVOKE", target = MODELVIEW_RESET, ordinal = 1, shift = At.Shift.AFTER))
    private void dashboard$moveRunicShield(CallbackInfo ci) {
        HudRegistry.translateToPosition(ThaumcraftCompat.RUNIC_SHIELD);
    }

    @Inject(method = "renderHoverHUD", at = @At("HEAD"), cancellable = true)
    private void dashboard$hideHoverHarness(CallbackInfo ci) {
        if (!HudRegistry.shouldRender(ThaumcraftCompat.HOVER_HARNESS)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "renderHoverHUD",
        at = @At(value = "INVOKE", target = MODELVIEW_RESET, ordinal = 1, shift = At.Shift.AFTER))
    private void dashboard$moveHoverHarness(CallbackInfo ci) {
        HudRegistry.translateToPosition(ThaumcraftCompat.HOVER_HARNESS);
    }
}
