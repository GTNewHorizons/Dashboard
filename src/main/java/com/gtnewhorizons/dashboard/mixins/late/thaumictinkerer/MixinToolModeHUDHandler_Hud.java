package com.gtnewhorizons.dashboard.mixins.late.thaumictinkerer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.ThaumicTinkererCompat;

/** Moves or hides Thaumic Tinkerer's Kami tool mode text, around its own push and pop of the matrix. */
@Pseudo
@Mixin(targets = "thaumic.tinkerer.client.core.handler.kami.ToolModeHUDHandler", remap = false)
public class MixinToolModeHUDHandler_Hud {

    @Inject(
        method = "drawDislocationFocusHUD",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPushMatrix()V"),
        cancellable = true)
    private void dashboard$moveToolMode(CallbackInfo ci) {
        if (!ThaumicTinkererCompat.beginToolMode()) {
            ci.cancel();
        }
    }

    @Inject(
        method = "drawDislocationFocusHUD",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPopMatrix()V", shift = At.Shift.AFTER))
    private void dashboard$restoreToolMode(CallbackInfo ci) {
        ThaumicTinkererCompat.endToolMode();
    }
}
