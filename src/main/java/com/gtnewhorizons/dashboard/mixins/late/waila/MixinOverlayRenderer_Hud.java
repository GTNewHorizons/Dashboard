package com.gtnewhorizons.dashboard.mixins.late.waila;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.compat.WailaCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Pseudo
@Mixin(targets = "mcp.mobius.waila.overlay.OverlayRenderer", remap = false)
public class MixinOverlayRenderer_Hud {

    /** Waila stops looking at blocks while a screen is open, so the HUD editor counts as no screen here. */
    @ModifyExpressionValue(
        method = "isOverlayVisible",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;",
            remap = true))
    private static GuiScreen dashboard$lookInHudEditor(GuiScreen currentScreen) {
        return HudEditor.isEditorOpen() ? null : currentScreen;
    }

    @Inject(method = "doRenderOverlay", at = @At("HEAD"), cancellable = true)
    private static void dashboard$move(@Coerce Object box, CallbackInfo ci) {
        if (!WailaCompat.beginBox(box)) {
            ci.cancel();
        }
    }

    @Inject(method = "doRenderOverlay", at = @At("RETURN"))
    private static void dashboard$restore(CallbackInfo ci) {
        HudEditor.endRender();
    }
}
