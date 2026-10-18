package com.gtnewhorizons.dashboard.mixins.late.thaumictinkerer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.ThaumicTinkererCompat;

/**
 * The soul hearts would move and hide with Health, since they are drawn in its event, so Dashboard draws them instead.
 */
@Pseudo
@Mixin(targets = "thaumic.tinkerer.client.core.handler.kami.SoulHeartClientHandler", remap = false)
public class MixinSoulHeartClientHandler_Hud {

    @Inject(method = "onDrawScreenPre", at = @At("HEAD"), cancellable = true)
    private void dashboard$skipOwnSoulHearts(CallbackInfo ci) {
        if (ThaumicTinkererCompat.drawsSoulHearts()) {
            ci.cancel();
        }
    }
}
