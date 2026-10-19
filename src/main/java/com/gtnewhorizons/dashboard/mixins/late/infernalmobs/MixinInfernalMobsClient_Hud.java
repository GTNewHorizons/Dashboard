package com.gtnewhorizons.dashboard.mixins.late.infernalmobs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.InfernalMobsCompat;

/** Moves or hides the Infernal Mobs health bar, name and modifiers. */
@Pseudo
@Mixin(targets = "atomicstryker.infernalmobs.client.InfernalMobsClient", remap = false)
public class MixinInfernalMobsClient_Hud {

    /** The start of the drawing, once Infernal Mobs found an elite mob to show. */
    @Inject(
        method = "onPreRenderGameOverlay",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V", ordinal = 0),
        cancellable = true)
    private void dashboard$begin(CallbackInfo ci) {
        if (!InfernalMobsCompat.begin()) {
            ci.cancel();
        }
    }

    @Inject(method = "onPreRenderGameOverlay", at = @At("RETURN"))
    private void dashboard$end(CallbackInfo ci) {
        InfernalMobsCompat.end();
    }
}
