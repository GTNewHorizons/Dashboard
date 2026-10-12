package com.gtnewhorizons.dashboard.mixins.early;

import net.minecraft.client.gui.GuiIngame;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.gtnewhorizons.dashboard.client.ScreenOverlays;

@Mixin(GuiIngame.class)
public class MixinGuiIngame_Overlays {

    // Pumpkin blur opacity
    @ModifyArg(
        method = "renderPumpkinBlur",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V", ordinal = 0, remap = false),
        index = 3,
        require = 1)
    private float dashboard$pumpkinOpacity(float alpha) {
        return alpha * ScreenOverlays.getPumpkinOpacity();
    }

    // Portal swirl opacity
    @ModifyArg(
        method = "func_130015_b",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V", ordinal = 0, remap = false),
        index = 3,
        require = 1)
    private float dashboard$portalOpacity(float alpha) {
        return alpha * ScreenOverlays.getPortalOpacity();
    }

    // Vignette strength, its color is how much it darkens
    @ModifyArgs(
        method = "renderVignette",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V", ordinal = 0, remap = false),
        require = 1)
    private void dashboard$vignetteStrength(Args args) {
        dashboard$scaleColor(args, ScreenOverlays.getVignetteStrength());
    }

    @Unique
    private static void dashboard$scaleColor(Args args, float scale) {
        for (int i = 0; i < 3; i++) {
            args.set(i, (float) args.get(i) * scale);
        }
    }
}
