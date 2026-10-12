package com.gtnewhorizons.dashboard.mixins.early;

import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.GuiIngameForge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.client.VanillaHudElements;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(value = GuiIngameForge.class, remap = false)
public class MixinGuiIngameForge_HudText {

    // Held item name
    @WrapOperation(
        method = "renderToolHightlight",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I",
            remap = true))
    private int dashboard$moveItemName(FontRenderer font, String text, int x, int y, int color,
        Operation<Integer> original) {
        return dashboard$drawShifted(VanillaHudElements.ITEM_NAME, font, text, x, y, color, original);
    }

    // Action bar - music disc message
    @WrapOperation(
        method = "renderRecordOverlay",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I",
            remap = true))
    private int dashboard$moveActionBar(FontRenderer font, String text, int x, int y, int color,
        Operation<Integer> original) {
        return dashboard$drawShifted(VanillaHudElements.ACTION_BAR, font, text, x, y, color, original);
    }

    // XP level number
    @WrapOperation(
        method = "renderExperience",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I",
            remap = true))
    private int dashboard$skipLevelNumber(FontRenderer font, String text, int x, int y, int color,
        Operation<Integer> original) {
        return 0;
    }

    @Unique
    private static int dashboard$drawShifted(HudElement element, FontRenderer font, String text, int x, int y,
        int color, Operation<Integer> original) {
        if (!HudEditor.beginRender(element)) {
            return 0;
        }
        try {
            return original.call(font, text, x, y, color);
        } finally {
            HudEditor.endRender();
        }
    }
}
