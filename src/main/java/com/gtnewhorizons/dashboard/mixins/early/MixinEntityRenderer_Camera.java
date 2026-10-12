package com.gtnewhorizons.dashboard.mixins.early;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizons.dashboard.DashboardConfig;
import com.gtnewhorizons.dashboard.client.ScreenOverlays;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer_Camera {

    // Hurt camera tilt
    @ModifyArg(
        method = "hurtCameraEffect",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V", ordinal = 2, remap = false),
        index = 0,
        require = 1)
    private float dashboard$hurtShake(float angle) {
        return angle * ScreenOverlays.getHurtShakeStrength();
    }

    // Nausea and portal wobble
    @ModifyArg(
        method = "setupCameraTransform",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glScalef(FFF)V", ordinal = 1, remap = false),
        index = 0,
        require = 1)
    private float dashboard$nausea(float stretch) {
        return 1 + (stretch - 1) * ScreenOverlays.getNauseaStrength();
    }

    // Night vision ending flicker
    @Inject(method = "getNightVisionBrightness", at = @At("HEAD"), cancellable = true)
    private void dashboard$noNightVisionFlicker(EntityPlayer player, float partialTicks,
        CallbackInfoReturnable<Float> cir) {
        if (!DashboardConfig.nightVisionFlicker) {
            cir.setReturnValue(1.0f);
        }
    }
}
