package com.gtnewhorizons.dashboard.mixins.late.galacticraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.GalacticraftCompat;

/**
 * Moved right after {@code setupOverlayRendering}, since that resets the matrix. Rockets without a GUI texture never
 * reach it, so they are not moved.
 */
@Pseudo
@Mixin(targets = "micdoodle8.mods.galacticraft.core.client.gui.overlay.OverlayRocket", remap = false)
public class MixinOverlayRocket_Hud {

    @Inject(
        method = "renderSpaceshipOverlay",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/EntityRenderer;setupOverlayRendering()V",
            shift = At.Shift.AFTER,
            remap = true),
        cancellable = true)
    private static void dashboard$move(CallbackInfo ci) {
        if (!GalacticraftCompat.beginOverlay(GalacticraftCompat.ROCKET_ALTITUDE)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSpaceshipOverlay", at = @At("RETURN"))
    private static void dashboard$restore(CallbackInfo ci) {
        GalacticraftCompat.endOverlay();
    }
}
