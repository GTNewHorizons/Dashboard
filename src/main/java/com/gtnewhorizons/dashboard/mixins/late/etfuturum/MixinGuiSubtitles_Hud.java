package com.gtnewhorizons.dashboard.mixins.late.etfuturum;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.EtFuturumCompat;

/** Moves or hides Et Futurum's sound captions. */
@Pseudo
@Mixin(targets = "ganymedes01.etfuturum.client.subtitle.GuiSubtitles", remap = false)
public class MixinGuiSubtitles_Hud {

    /** The start of the drawing, once Et Futurum checked there are captions. */
    @Inject(
        method = "renderSubtitles",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPushMatrix()V", ordinal = 0),
        cancellable = true)
    private void dashboard$begin(CallbackInfo ci) {
        if (!EtFuturumCompat.begin()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSubtitles", at = @At("RETURN"))
    private void dashboard$end(CallbackInfo ci) {
        EtFuturumCompat.end();
    }
}
