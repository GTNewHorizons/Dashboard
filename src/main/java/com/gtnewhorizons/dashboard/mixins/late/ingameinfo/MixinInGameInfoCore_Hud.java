package com.gtnewhorizons.dashboard.mixins.late.ingameinfo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.InGameInfoCompat;

/** Tells when all InGameInfoXML text of a frame is drawn, so the measured area sizes are complete. */
@Pseudo
@Mixin(targets = "com.github.lunatrius.ingameinfo.InGameInfoCore", remap = false)
public class MixinInGameInfoCore_Hud {

    @Inject(method = "onTickRender", at = @At("RETURN"))
    private void dashboard$endFrame(CallbackInfo ci) {
        InGameInfoCompat.endFrame();
    }
}
