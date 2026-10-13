package com.gtnewhorizons.dashboard.mixins.late.galacticraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.GalacticraftCompat;

/** Moved right after {@code setupOverlayRendering}, since that resets the matrix. */
@Pseudo
@Mixin(targets = "micdoodle8.mods.galacticraft.core.client.gui.overlay.OverlayOxygenWarning", remap = false)
public class MixinOverlayOxygenWarning_Hud {

    @Inject(
        method = "renderOxygenWarningOverlay",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/EntityRenderer;setupOverlayRendering()V",
            shift = At.Shift.AFTER,
            remap = true),
        cancellable = true)
    private static void dashboard$move(CallbackInfo ci) {
        if (!GalacticraftCompat.beginOverlay(GalacticraftCompat.OXYGEN_WARNING)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderOxygenWarningOverlay", at = @At("RETURN"))
    private static void dashboard$restore(CallbackInfo ci) {
        GalacticraftCompat.endOverlay();
    }
}
