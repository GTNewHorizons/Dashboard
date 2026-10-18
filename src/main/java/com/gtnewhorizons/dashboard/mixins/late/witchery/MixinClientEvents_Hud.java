package com.gtnewhorizons.dashboard.mixins.late.witchery;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.WitcheryCompat;

/** Moves or hides Witchery's blood meter and its target's blood, which are drawn one after the other. */
@Pseudo
@Mixin(targets = "com.emoniph.witchery.client.ClientEvents", remap = false)
public class MixinClientEvents_Hud {

    @Inject(
        method = "onRenderGameOverlay",
        at = @At(value = "INVOKE", target = "Lcom/emoniph/witchery/common/ExtendedPlayer;getMaxBloodPower()I"))
    private void dashboard$beginBloodMeter(CallbackInfo ci) {
        WitcheryCompat.beginOrHide(WitcheryCompat.BLOOD_METER);
    }

    /** Right before Witchery looks for a creature to show the blood of. */
    @Inject(
        method = "onRenderGameOverlay",
        at = @At(
            value = "INVOKE",
            target = "Lcom/emoniph/witchery/infusion/infusions/InfusionOtherwhere;raytraceEntities(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;ZD)Lnet/minecraft/util/MovingObjectPosition;"))
    private void dashboard$endBloodMeter(CallbackInfo ci) {
        WitcheryCompat.end();
    }

    @Inject(
        method = "onRenderGameOverlay",
        at = @At(
            value = "FIELD",
            target = "Lcom/emoniph/witchery/client/ClientEvents;BLOODDROP_BG:Lnet/minecraft/util/ResourceLocation;",
            ordinal = 1))
    private void dashboard$beginTargetBlood(CallbackInfo ci) {
        WitcheryCompat.beginOrHide(WitcheryCompat.TARGET_BLOOD);
    }

    @Inject(method = "onRenderGameOverlay", at = @At("RETURN"))
    private void dashboard$endTargetBlood(CallbackInfo ci) {
        WitcheryCompat.end();
    }
}
