package com.gtnewhorizons.dashboard.mixins.late.bogosorter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.BogosorterCompat;

/**
 * Moves Bogosorter's hotbar swap column with the hotbar, since it sits above the selected slot. It is drawn after the
 * HUD, when the hotbar's own move is already over.
 */
@Pseudo
@Mixin(targets = "com.cleanroommc.bogosorter.common.HotbarSwap", remap = false)
public class MixinHotbarSwap_Hud {

    /** The first thing drawn, once Bogosorter checked the column shows. */
    @Inject(
        method = "render",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V", ordinal = 0),
        cancellable = true)
    private void dashboard$begin(CallbackInfo ci) {
        if (!BogosorterCompat.beginHotbarSwap()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void dashboard$end(CallbackInfo ci) {
        BogosorterCompat.end();
    }
}
