package com.gtnewhorizons.dashboard.mixins.late.bogosorter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.BogosorterCompat;

/** Moves or hides each item of Bogosorter's usage ticker, only while it pops up. */
@Pseudo
@Mixin(targets = "com.cleanroommc.bogosorter.client.usageticker.UsageTicker$Element", remap = false)
public class MixinUsageTickerElement_Hud {

    /** After Bogosorter checked the item is popping up right now. */
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/cleanroommc/bogosorter/client/usageticker/UsageTicker$Element;getRenderedStack(Lnet/minecraft/client/entity/EntityClientPlayerMP;)Lnet/minecraft/item/ItemStack;"),
        cancellable = true)
    private void dashboard$begin(CallbackInfo ci) {
        if (!BogosorterCompat.beginUsageTicker()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void dashboard$end(CallbackInfo ci) {
        BogosorterCompat.end();
    }
}
