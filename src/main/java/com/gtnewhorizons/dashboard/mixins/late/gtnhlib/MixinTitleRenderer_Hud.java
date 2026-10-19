package com.gtnewhorizons.dashboard.mixins.late.gtnhlib;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.client.title.TitleRenderer;
import com.gtnewhorizons.dashboard.compat.GtnhLibCompat;

/**
 * Moves or hides GTNHLib's titles. Starts right before the particles, so they, the effects, the icon and the text all
 * move together.
 */
@Mixin(value = TitleRenderer.class, remap = false)
public class MixinTitleRenderer_Hud {

    @Inject(
        method = "render",
        at = @At(value = "INVOKE", target = "Lcom/gtnewhorizon/gtnhlib/client/title/TitleParticleSystem;render(F)V"),
        cancellable = true)
    private static void dashboard$begin(CallbackInfo ci) {
        if (!GtnhLibCompat.begin(GtnhLibCompat.TITLE)) {
            // GTNHLib closes its profiler section at the end, which skipping would miss
            Minecraft.getMinecraft().mcProfiler.endSection();
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private static void dashboard$end(CallbackInfo ci) {
        GtnhLibCompat.end();
    }
}
