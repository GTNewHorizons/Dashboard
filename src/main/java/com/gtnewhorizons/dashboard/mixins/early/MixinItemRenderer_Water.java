package com.gtnewhorizons.dashboard.mixins.early;

import net.minecraft.client.renderer.ItemRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.gtnewhorizons.dashboard.client.ScreenOverlays;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer_Water {

    // Underwater overlay opacity
    @ModifyArg(
        method = "renderWarpedTextureOverlay",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V", ordinal = 0, remap = false),
        index = 3,
        require = 1)
    private float dashboard$waterOpacity(float alpha) {
        return alpha * ScreenOverlays.getWaterOpacity();
    }
}
