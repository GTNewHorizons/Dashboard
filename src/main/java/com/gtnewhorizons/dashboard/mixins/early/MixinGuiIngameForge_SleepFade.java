package com.gtnewhorizons.dashboard.mixins.early;

import net.minecraftforge.client.GuiIngameForge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.gtnewhorizons.dashboard.client.ScreenOverlays;

/** Twice, since the compiled call can name either class as the owner of drawRect. */
@Mixin(value = GuiIngameForge.class, remap = false)
public class MixinGuiIngameForge_SleepFade {

    // Sleep fade strength
    @ModifyArg(
        method = "renderSleepFade",
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/GuiIngameForge;drawRect(IIIII)V", remap = true),
        index = 4,
        require = 0,
        expect = 0)
    private int dashboard$sleepFade(int color) {
        return ScreenOverlays.scaleAlpha(color, ScreenOverlays.getSleepFadeStrength());
    }

    @ModifyArg(
        method = "renderSleepFade",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;drawRect(IIIII)V", remap = true),
        index = 4,
        require = 0,
        expect = 0)
    private int dashboard$sleepFadeGui(int color) {
        return ScreenOverlays.scaleAlpha(color, ScreenOverlays.getSleepFadeStrength());
    }
}
