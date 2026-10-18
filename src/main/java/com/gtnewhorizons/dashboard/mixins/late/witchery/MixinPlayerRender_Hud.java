package com.gtnewhorizons.dashboard.mixins.late.witchery;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.client.FrameEndHud;
import com.gtnewhorizons.dashboard.compat.WitcheryCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Moves or hides Witchery's power bars and charges, drawn at the end of the frame when no screen is open. The HUD
 * editor counts as no screen, and in the editor the editor runs the drawing instead.
 */
@Pseudo
@Mixin(targets = "com.emoniph.witchery.client.PlayerRender", remap = false)
public abstract class MixinPlayerRender_Hud {

    private static final String DRAW_BAR = "Lcom/emoniph/witchery/client/renderer/RenderInfusionEnergyBar;draw(DDDLnet/minecraft/entity/player/EntityPlayer;I)V";

    @Shadow(remap = false)
    public abstract void onRenderTick(TickEvent.RenderTickEvent event);

    @Inject(method = "onRenderTick", at = @At("HEAD"), cancellable = true)
    private void dashboard$drawInEditor(TickEvent.RenderTickEvent event, CallbackInfo ci) {
        if (FrameEndHud.skip(this, this::onRenderTick, event)) {
            ci.cancel();
        }
    }

    /** The first check is for the remote viewing camera, the second for the HUD. */
    @ModifyExpressionValue(
        method = "onRenderTick",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;",
            ordinal = 1,
            remap = true))
    private GuiScreen dashboard$drawInHudEditor(GuiScreen currentScreen) {
        return HudEditor.isEditorOpen() ? null : currentScreen;
    }

    @Inject(method = "onRenderTick", at = @At(value = "INVOKE", target = DRAW_BAR, ordinal = 0))
    private void dashboard$beginInfusionEnergy(CallbackInfo ci) {
        WitcheryCompat.beginOrHide(WitcheryCompat.INFUSION_ENERGY);
    }

    @Inject(method = "onRenderTick", at = @At(value = "INVOKE", target = DRAW_BAR, ordinal = 1))
    private void dashboard$beginCreaturePower(CallbackInfo ci) {
        WitcheryCompat.beginOrHide(WitcheryCompat.CREATURE_POWER);
    }

    @Inject(method = "onRenderTick", at = @At(value = "INVOKE", target = DRAW_BAR, shift = At.Shift.AFTER))
    private void dashboard$endBar(CallbackInfo ci) {
        WitcheryCompat.end();
    }

    @Inject(method = "drawInfusedBrews", at = @At("HEAD"), cancellable = true)
    private void dashboard$beginInfusedBrew(CallbackInfo ci) {
        if (!WitcheryCompat.begin(WitcheryCompat.INFUSED_BREW)) {
            ci.cancel();
        }
    }

    @Inject(method = "drawBarkBeltCharges", at = @At("HEAD"), cancellable = true)
    private void dashboard$beginBarkBelt(CallbackInfo ci) {
        if (!WitcheryCompat.begin(WitcheryCompat.BARK_BELT)) {
            ci.cancel();
        }
    }

    @Inject(method = { "drawInfusedBrews", "drawBarkBeltCharges" }, at = @At("RETURN"))
    private void dashboard$endCharges(CallbackInfo ci) {
        WitcheryCompat.end();
    }
}
