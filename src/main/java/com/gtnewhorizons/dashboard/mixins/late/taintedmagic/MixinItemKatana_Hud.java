package com.gtnewhorizons.dashboard.mixins.late.taintedmagic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.TaintedMagicCompat;

/** Moves or hides the Katana charge runes, around Tainted Magic's own push and pop of the matrix. */
@Pseudo
@Mixin(targets = "taintedmagic.common.items.tools.ItemKatana", remap = false)
public class MixinItemKatana_Hud {

    @Inject(
        method = "renderHUD",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPushMatrix()V"),
        cancellable = true)
    private static void dashboard$moveCharge(CallbackInfo ci) {
        if (!TaintedMagicCompat.beginKatanaCharge()) {
            ci.cancel();
        }
    }

    @Inject(
        method = "renderHUD",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPopMatrix()V", shift = At.Shift.AFTER))
    private static void dashboard$restoreCharge(CallbackInfo ci) {
        TaintedMagicCompat.endKatanaCharge();
    }
}
