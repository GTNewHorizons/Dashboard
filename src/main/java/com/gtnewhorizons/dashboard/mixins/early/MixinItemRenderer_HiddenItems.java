package com.gtnewhorizons.dashboard.mixins.early;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizons.dashboard.client.HiddenHeldItems;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer_HiddenItems {

    // First person held item, read once at the start; no item draws the empty hand
    @ModifyExpressionValue(
        method = "renderItemInFirstPerson",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/ItemRenderer;itemToRender:Lnet/minecraft/item/ItemStack;",
            opcode = Opcodes.GETFIELD),
        require = 1)
    private ItemStack dashboard$hideHeldItem(ItemStack held) {
        return HiddenHeldItems.isHidden(held) ? null : held;
    }
}
