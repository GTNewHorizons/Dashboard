package com.gtnewhorizons.dashboard.mixins.late.galaxyspace;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.GalacticraftCompat;
import com.gtnewhorizons.dashboard.compat.GalaxySpaceCompat;

/** Moved right after {@code setupOverlayRendering}, like Galacticraft's overlays, since that resets the matrix. */
@Pseudo
@Mixin(targets = "galaxyspace.core.gui.overlay.OverlayAtmosphericPressure", remap = false)
public class MixinOverlayAtmosphericPressure_Hud {

    @Inject(
        method = "renderPressureTankIndicator",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/EntityRenderer;setupOverlayRendering()V",
            shift = At.Shift.AFTER,
            remap = true),
        cancellable = true)
    private static void dashboard$move(CallbackInfo ci) {
        if (!GalacticraftCompat.beginOverlay(GalaxySpaceCompat.PRESSURE)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPressureTankIndicator", at = @At("RETURN"))
    private static void dashboard$restore(CallbackInfo ci) {
        GalacticraftCompat.endOverlay();
    }
}
