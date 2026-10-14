package com.gtnewhorizons.dashboard.mixins.late.gravisuite;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngame;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.compat.GraviSuiteCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/**
 * Wraps each line GraviSuite draws, so the element only counts as showing while there is text.
 * <p>
 * GraviSuite Neo replaces {@code onTickRedner} with an {@code @Overwrite}. The higher priority applies this after that,
 * so it wraps the code that really runs.
 */
@Pseudo
@Mixin(targets = "gravisuite.client.ClientTickHandler", remap = false, priority = 1500)
public class MixinClientTickHandler_Hud {

    /** GraviSuite only draws while the game has focus, which it does not have while the HUD editor is open. */
    @ModifyExpressionValue(
        method = "onTickRedner",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;inGameHasFocus:Z", remap = true))
    private static boolean dashboard$drawInHudEditor(boolean inGameHasFocus) {
        return inGameHasFocus || HudEditor.isEditorOpen();
    }

    @WrapOperation(
        method = "onTickRedner",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiIngame;drawString(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;III)V",
            remap = true))
    private static void dashboard$moveStatusLine(GuiIngame gui, FontRenderer font, String text, int x, int y, int color,
        Operation<Void> original) {
        HudEditor.render(GraviSuiteCompat.STATUS, () -> original.call(gui, font, text, x, y, color));
    }
}
