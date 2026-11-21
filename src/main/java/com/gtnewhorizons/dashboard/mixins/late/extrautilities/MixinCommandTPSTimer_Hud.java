package com.gtnewhorizons.dashboard.mixins.late.extrautilities;

import net.minecraft.client.gui.FontRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.client.FrameEndHud;
import com.gtnewhorizons.dashboard.compat.ExtraUtilitiesCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Moves or hides Extra Utilities' TPS line. Its own run at the end of the frame is skipped while Dashboard runs it with
 * the HUD, see {@link ExtraUtilitiesCompat}. Otherwise the HUD editor runs it, so the line does not cover the editor.
 */
@Pseudo
@Mixin(targets = "com.rwtema.extrautils.CommandTPSTimer", remap = false)
public abstract class MixinCommandTPSTimer_Hud {

    @Shadow(remap = false)
    public abstract void onDraw(TickEvent.RenderTickEvent event);

    @Inject(method = "onDraw", at = @At("HEAD"), cancellable = true)
    private void dashboard$drawWithHud(TickEvent.RenderTickEvent event, CallbackInfo ci) {
        boolean skip = ExtraUtilitiesCompat.isDrawnWithHud() ? !ExtraUtilitiesCompat.isDrawingInHud()
            : FrameEndHud.skip(this, this::onDraw, event);
        if (skip) {
            ci.cancel();
        }
    }

    @WrapOperation(
        method = "onDraw",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;IIIZ)I",
            remap = true))
    private int dashboard$moveTpsLine(FontRenderer font, String text, int x, int y, int color, boolean shadow,
        Operation<Integer> original) {
        if (!HudRegistry.beginRender(ExtraUtilitiesCompat.TPS)) {
            return 0;
        }
        try {
            return original.call(font, text, x, y, color, shadow);
        } finally {
            HudRegistry.endRender();
        }
    }
}
