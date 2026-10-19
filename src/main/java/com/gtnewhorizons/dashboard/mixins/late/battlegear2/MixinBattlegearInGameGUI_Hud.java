package com.gtnewhorizons.dashboard.mixins.late.battlegear2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.BattlegearCompat;

/** Moves or hides Battlegear's shield block bar and quiver slots. */
@Pseudo
@Mixin(targets = "mods.battlegear2.client.gui.BattlegearInGameGUI", remap = false)
public class MixinBattlegearInGameGUI_Hud {

    @Inject(method = "renderBlockBar", at = @At("HEAD"), cancellable = true)
    private void dashboard$beginShieldBar(CallbackInfo ci) {
        if (!BattlegearCompat.begin(BattlegearCompat.SHIELD_BAR)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderQuiverBar", at = @At("HEAD"), cancellable = true)
    private void dashboard$beginQuiver(CallbackInfo ci) {
        if (!BattlegearCompat.begin(BattlegearCompat.QUIVER)) {
            ci.cancel();
        }
    }

    @Inject(method = { "renderBlockBar", "renderQuiverBar" }, at = @At("RETURN"))
    private void dashboard$end(CallbackInfo ci) {
        BattlegearCompat.end();
    }
}
