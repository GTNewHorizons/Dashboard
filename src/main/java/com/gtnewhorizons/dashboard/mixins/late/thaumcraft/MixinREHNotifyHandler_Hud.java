package com.gtnewhorizons.dashboard.mixins.late.thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.compat.ThaumcraftCompat;

import thaumcraft.client.lib.REHNotifyHandler;

/**
 * Moves or hides Thaumcraft's research notifications, like {@link MixinClientTickEventsFML_Hud}. The flying aspect
 * icons are left alone, since they fly across the whole screen.
 */
@Mixin(value = REHNotifyHandler.class, remap = false)
public class MixinREHNotifyHandler_Hud {

    @Inject(method = "renderNotifyHUD", at = @At("HEAD"), cancellable = true)
    private void dashboard$hideNotifications(CallbackInfo ci) {
        if (!HudRegistry.shouldRender(ThaumcraftCompat.NOTIFICATIONS)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "renderNotifyHUD",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/opengl/GL11;glLoadIdentity()V",
            ordinal = 1,
            shift = At.Shift.AFTER))
    private void dashboard$moveNotifications(CallbackInfo ci) {
        HudRegistry.translateToPosition(ThaumcraftCompat.NOTIFICATIONS);
    }
}
