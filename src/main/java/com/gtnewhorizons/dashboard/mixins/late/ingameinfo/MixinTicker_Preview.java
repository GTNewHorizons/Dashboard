package com.gtnewhorizons.dashboard.mixins.late.ingameinfo;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

/**
 * InGameInfoXML only draws when no screen (or the chat) is open, so the HUD editor counts as no screen here. That way
 * players see the real text while moving it.
 */
@Pseudo
@Mixin(targets = "com.github.lunatrius.ingameinfo.handler.Ticker", remap = false)
public class MixinTicker_Preview {

    @ModifyExpressionValue(
        method = "isRunning",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;",
            remap = true))
    private GuiScreen dashboard$drawInHudEditor(GuiScreen currentScreen) {
        return HudRegistry.isLayoutScreenOpen() ? null : currentScreen;
    }
}
