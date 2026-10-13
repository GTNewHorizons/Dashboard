package com.gtnewhorizons.dashboard.mixins.early;

import net.minecraftforge.client.GuiIngameForge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.client.HotbarNumbers;

@Mixin(value = GuiIngameForge.class, remap = false)
public class MixinGuiIngameForge_HotbarNumbers {

    // Hotbar slot numbers under the items: after the hotbar background, before the items
    @Inject(
        method = "renderHotbar",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderHelper;enableGUIStandardItemLighting()V",
            remap = true),
        require = 1)
    private void dashboard$drawNumbersUnderItems(int width, int height, float partialTicks, CallbackInfo ci) {
        HotbarNumbers.drawUnderItems(width, height);
    }
}
