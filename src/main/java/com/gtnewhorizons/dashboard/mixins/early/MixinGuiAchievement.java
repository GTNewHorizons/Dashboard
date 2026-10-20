package com.gtnewhorizons.dashboard.mixins.early;

import net.minecraft.client.gui.achievement.GuiAchievement;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.client.VanillaHudElements;

@Mixin(GuiAchievement.class)
public class MixinGuiAchievement {

    // Hide achievement popup
    @Inject(
        method = "func_146254_a",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/achievement/GuiAchievement;func_146258_c()V"),
        cancellable = true)
    private void dashboard$hideAchievement(CallbackInfo ci) {
        if (!HudRegistry.shouldRender(VanillaHudElements.ACHIEVEMENT)) {
            ci.cancel();
        }
    }

    // Move achievement popup
    @Inject(
        method = "func_146254_a",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/achievement/GuiAchievement;func_146258_c()V",
            shift = At.Shift.AFTER))
    private void dashboard$moveAchievement(CallbackInfo ci) {
        HudRegistry.translateToPosition(VanillaHudElements.ACHIEVEMENT);
    }
}
