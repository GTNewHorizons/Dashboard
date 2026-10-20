package com.gtnewhorizons.dashboard.mixins.early;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraftforge.client.GuiIngameForge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;
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

    /**
     * Action bar, like the music disc message. Forge keeps drawing it down to opacity 1, but the font draws text below
     * opacity 4 fully opaque, so the faded text would flash at the end.
     */
    @WrapOperation(
        method = "renderRecordOverlay",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I",
            remap = true))
    private int dashboard$moveActionBar(FontRenderer font, String text, int x, int y, int color,
        Operation<Integer> original) {
        if (color >>> 24 < 4) {
            return 0;
        }
        Operation<Integer> draw = VanillaHudElements.isShadowedActionBarText(text)
            ? args -> font.drawStringWithShadow(text, x, y, color)
            : original;
        return dashboard$drawShifted(VanillaHudElements.ACTION_BAR, font, text, x, y, color, draw);
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

    // Scoreboard sidebar
    @WrapOperation(
        method = { "renderGameOverlay", "func_73830_a" },
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/GuiIngameForge;func_96136_a(Lnet/minecraft/scoreboard/ScoreObjective;IILnet/minecraft/client/gui/FontRenderer;)V",
            remap = false))
    private void dashboard$moveScoreboard(GuiIngameForge gui, ScoreObjective objective, int height, int width,
        FontRenderer font, Operation<Void> original) {
        if (!HudRegistry.beginRender(VanillaHudElements.SCOREBOARD, width, height)) {
            return;
        }
        try {
            original.call(gui, objective, height, width, font);
        } finally {
            HudRegistry.endRender();
        }
    }

    @Unique
    private static int dashboard$drawShifted(HudElement element, FontRenderer font, String text, int x, int y,
        int color, Operation<Integer> original) {
        if (!HudRegistry.beginRender(element)) {
            return 0;
        }
        try {
            return original.call(font, text, x, y, color);
        } finally {
            HudRegistry.endRender();
        }
    }
}
