package com.gtnewhorizons.dashboard.mixins.late.thaumictinkerer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.ThaumicTinkererCompat;

/** Moves or hides Thaumic Tinkerer's Dislocation focus box. Hidden before it switches to the small font. */
@Pseudo
@Mixin(targets = "thaumic.tinkerer.client.core.handler.HUDHandler", remap = false)
public class MixinHUDHandler_Hud {

    @Inject(
        method = "drawDislocationFocusHUD",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;drawRect(IIIII)V",
            ordinal = 0,
            remap = true),
        cancellable = true)
    private void dashboard$moveDislocationFocus(CallbackInfo ci) {
        if (!ThaumicTinkererCompat.beginDislocationFocus()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawDislocationFocusHUD", at = @At("RETURN"))
    private void dashboard$restoreDislocationFocus(CallbackInfo ci) {
        ThaumicTinkererCompat.endDislocationFocus();
    }
}
