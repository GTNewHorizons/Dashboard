package com.gtnewhorizons.dashboard.mixins.late.gtnhlib;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.util.AboveHotbarHUD;
import com.gtnewhorizons.dashboard.compat.GtnhLibCompat;

/** Moves or hides GTNHLib's text above the hotbar. */
@Mixin(value = AboveHotbarHUD.class, remap = false)
public class MixinAboveHotbarHUD_Hud {

    /** The start of the drawing, once GTNHLib checked the text shows. */
    @Inject(
        method = "renderTextAboveHotbar(Lnet/minecraftforge/client/event/RenderGameOverlayEvent$Post;)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPushMatrix()V", ordinal = 0),
        cancellable = true)
    private void dashboard$begin(CallbackInfo ci) {
        if (!GtnhLibCompat.begin(GtnhLibCompat.ABOVE_HOTBAR_TEXT)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "renderTextAboveHotbar(Lnet/minecraftforge/client/event/RenderGameOverlayEvent$Post;)V",
        at = @At("RETURN"))
    private void dashboard$end(CallbackInfo ci) {
        GtnhLibCompat.end();
    }
}
