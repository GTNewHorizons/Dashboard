package com.gtnewhorizons.dashboard.mixins.late.backhand;

import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.client.VanillaHudElements;
import com.gtnewhorizons.dashboard.compat.BackhandCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

/** Makes Backhand's off-hand slot its own element, though Backhand draws it during the hotbar's event. */
@Pseudo
@Mixin(targets = "xonin.backhand.client.ClientEventHandler", remap = false)
public class MixinClientEventHandler_Hud {

    /** Backhand skips the slot when the hotbar is hidden, but the player may have hidden only the hotbar. */
    @ModifyExpressionValue(
        method = "renderHotbarOverlay",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/event/RenderGameOverlayEvent$Pre;isCanceled()Z"))
    private static boolean dashboard$ignoreHiddenHotbar(boolean cancelled) {
        return cancelled && !VanillaHudElements.isHiddenByDashboard(ElementType.HOTBAR);
    }

    /** Right where Backhand starts drawing, after it checked there is a slot to show. */
    @Inject(
        method = "renderHotbar",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V", ordinal = 0),
        cancellable = true)
    private static void dashboard$move(int screenWidth, int screenHeight, float partialTicks, CallbackInfo ci) {
        if (!BackhandCompat.beginOffhandSlot(screenWidth, screenHeight)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At("RETURN"))
    private static void dashboard$restore(CallbackInfo ci) {
        BackhandCompat.endOffhandSlot();
    }
}
