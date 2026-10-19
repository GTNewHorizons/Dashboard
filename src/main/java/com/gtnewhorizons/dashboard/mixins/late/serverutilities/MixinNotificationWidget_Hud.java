package com.gtnewhorizons.dashboard.mixins.late.serverutilities;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.GtnhLibCompat;

/**
 * Moves or hides ServerUtilities' notifications with GTNHLib's text above the hotbar, which shows at almost the same
 * spot. Its title notifications already go through GTNHLib's titles.
 */
@Pseudo
@Mixin(targets = "serverutils.client.NotificationHandler$NotificationWidget", remap = false)
public class MixinNotificationWidget_Hud {

    /** The start of the drawing, once ServerUtilities checked the notification shows. */
    @Inject(
        method = "render",
        at = @At(value = "INVOKE", target = "Lserverutils/lib/client/GlStateManager;pushMatrix()V", ordinal = 0),
        cancellable = true)
    private void dashboard$begin(CallbackInfo ci) {
        if (!GtnhLibCompat.begin(GtnhLibCompat.ABOVE_HOTBAR_TEXT)) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void dashboard$end(CallbackInfo ci) {
        GtnhLibCompat.end();
    }
}
