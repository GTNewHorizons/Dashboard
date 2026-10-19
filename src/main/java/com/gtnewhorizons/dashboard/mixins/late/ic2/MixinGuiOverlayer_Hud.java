package com.gtnewhorizons.dashboard.mixins.late.ic2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.Ic2Compat;

/** Moves or hides IC2's armor charge panel and held item info, which are drawn one after the other. */
@Pseudo
@Mixin(targets = "ic2.core.GuiOverlayer", remap = false)
public class MixinGuiOverlayer_Hud {

    /** Right before the panel background. */
    @Inject(
        method = "onRenderHotBar",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V",
            remap = true))
    private void dashboard$beginArmorCharge(CallbackInfo ci) {
        Ic2Compat.beginOrHide(Ic2Compat.ARMOR_CHARGE);
    }

    /** Only reached in the helmet's second HUD mode. */
    @Inject(
        method = "onRenderHotBar",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;getCurrentEquippedItem()Lnet/minecraft/item/ItemStack;",
            remap = true))
    private void dashboard$beginHeldItem(CallbackInfo ci) {
        Ic2Compat.end();
        Ic2Compat.beginOrHide(Ic2Compat.HELD_ITEM);
    }

    @Inject(method = "onRenderHotBar", at = @At("RETURN"))
    private void dashboard$end(CallbackInfo ci) {
        Ic2Compat.end();
    }
}
