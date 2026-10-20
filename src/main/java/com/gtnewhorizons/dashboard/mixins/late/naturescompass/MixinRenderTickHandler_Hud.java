package com.gtnewhorizons.dashboard.mixins.late.naturescompass;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.client.FrameEndHud;
import com.gtnewhorizons.dashboard.compat.NaturesCompassCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Moves or hides the Nature's Compass text. It is drawn at the end of the frame when no screen (or the chat) is open,
 * so the HUD editor counts as no screen, and in the editor the editor runs the drawing instead.
 */
@Pseudo
@Mixin(targets = "com.chaosthedude.naturescompass.client.RenderTickHandler", remap = false)
public abstract class MixinRenderTickHandler_Hud {

    @Shadow(remap = false)
    public abstract void onRenderTick(TickEvent.RenderTickEvent event);

    @Inject(method = "onRenderTick", at = @At("HEAD"), cancellable = true)
    private void dashboard$drawInEditor(TickEvent.RenderTickEvent event, CallbackInfo ci) {
        if (FrameEndHud.skip(this, this::onRenderTick, event)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(
        method = "onRenderTick",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;",
            remap = true))
    private GuiScreen dashboard$drawInHudEditor(GuiScreen currentScreen) {
        return HudRegistry.isLayoutScreenOpen() ? null : currentScreen;
    }

    @WrapOperation(
        method = "onRenderTick",
        at = @At(
            value = "INVOKE",
            target = "Lcom/chaosthedude/naturescompass/util/RenderUtils;drawLineOffsetStringOnHUD(Ljava/lang/String;IIII)V"))
    private void dashboard$moveLine(String text, int x, int y, int color, int line, Operation<Void> original) {
        NaturesCompassCompat.drawLine(() -> original.call(text, x, y, color, line));
    }
}
