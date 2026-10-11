package com.gtnewhorizon.dashboard.mixins.early;

import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.GuiIngameForge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.dashboard.api.HudEditor;
import com.gtnewhorizon.dashboard.api.HudElement;
import com.gtnewhorizon.dashboard.client.VanillaHudElements;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/** Held item name */
@Mixin(value = GuiIngameForge.class, remap = false)
public class MixinGuiIngameForge_HudText {

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
