package com.gtnewhorizons.dashboard.mixins.late.hee;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.HeeCompat;

/** Moves or hides HEE's notes and energy cluster info, which are drawn one after the other. */
@Pseudo
@Mixin(targets = "chylex.hee.render.OverlayManager", remap = false)
public class MixinOverlayManager_Hud {

    /** HEE switches to its small font first thing, once it has notes to draw. */
    @Inject(
        method = "onPostRenderGameOverlay",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getUnicodeFlag()Z", remap = true))
    private void dashboard$beginNotifications(CallbackInfo ci) {
        HeeCompat.beginOrHide(HeeCompat.NOTIFICATIONS);
    }

    /** Right after HEE switched its font back. */
    @Inject(
        method = "onPostRenderGameOverlay",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;setUnicodeFlag(Z)V",
            ordinal = 1,
            shift = At.Shift.AFTER,
            remap = true))
    private void dashboard$endNotifications(CallbackInfo ci) {
        HeeCompat.end();
    }

    /** The cluster info is the second place the method reads the screen width. */
    @Inject(
        method = "onPostRenderGameOverlay",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/ScaledResolution;getScaledWidth()I",
            ordinal = 1,
            remap = true))
    private void dashboard$beginEnergyCluster(CallbackInfo ci) {
        HeeCompat.beginOrHide(HeeCompat.ENERGY_CLUSTER);
    }

    @Inject(method = "onPostRenderGameOverlay", at = @At("RETURN"))
    private void dashboard$endEnergyCluster(CallbackInfo ci) {
        HeeCompat.end();
    }
}
