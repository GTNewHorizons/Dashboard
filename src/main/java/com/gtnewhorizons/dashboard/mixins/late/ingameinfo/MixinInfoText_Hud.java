package com.gtnewhorizons.dashboard.mixins.late.ingameinfo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.InGameInfoCompat;

/** Moves or hides each line of InGameInfoXML text, with the icons inside it, as part of its screen area. */
@Pseudo
@Mixin(targets = "com.github.lunatrius.ingameinfo.client.gui.InfoText", remap = false)
public class MixinInfoText_Hud {

    @Inject(method = "drawInfo", at = @At("HEAD"), cancellable = true)
    private void dashboard$beginLine(CallbackInfo ci) {
        if (!InGameInfoCompat.beginLine(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "drawInfo", at = @At("RETURN"))
    private void dashboard$endLine(CallbackInfo ci) {
        InGameInfoCompat.endLine();
    }
}
