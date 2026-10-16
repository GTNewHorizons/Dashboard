package com.gtnewhorizons.dashboard.mixins.late.draconic;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.compat.DraconicCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/**
 * Moves or hides the magnet icon Draconic Evolution shows after toggling its magnet. Only the item is skipped when
 * hidden, so Draconic still turns its lighting back off.
 */
@Pseudo
@Mixin(targets = "com.brandon3055.draconicevolution.client.handler.ItemDisplayManager", remap = false)
public class MixinItemDisplayManager_Hud {

    @Inject(
        method = "renderGameOverlayEvent",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPushMatrix()V", shift = At.Shift.AFTER))
    private void dashboard$moveIcon(CallbackInfo ci) {
        DraconicCompat.beginMagnetIcon();
    }

    @WrapOperation(
        method = "renderGameOverlayEvent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderItemAndEffectIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;II)V",
            remap = true))
    private void dashboard$skipHiddenIcon(RenderItem renderer, FontRenderer font, TextureManager textures,
        ItemStack stack, int x, int y, Operation<Void> original) {
        if (DraconicCompat.isMagnetIconShown()) {
            original.call(renderer, font, textures, stack, x, y);
        }
    }

    @Inject(
        method = "renderGameOverlayEvent",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPopMatrix()V"))
    private void dashboard$restoreIcon(CallbackInfo ci) {
        DraconicCompat.endMagnetIcon();
    }
}
