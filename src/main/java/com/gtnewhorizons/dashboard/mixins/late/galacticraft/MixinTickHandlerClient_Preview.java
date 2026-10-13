package com.gtnewhorizons.dashboard.mixins.late.galacticraft;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizons.dashboard.api.HudEditor;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

/**
 * Galacticraft only draws its overlays when no screen (or the chat) is open, so the HUD editor counts as no screen
 * here. That way players see the real elements while moving them.
 */
@Pseudo
@Mixin(targets = "micdoodle8.mods.galacticraft.core.tick.TickHandlerClient", remap = false)
public class MixinTickHandlerClient_Preview {

    @ModifyExpressionValue(
        method = "onRenderTick",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;",
            remap = true))
    private GuiScreen dashboard$drawInHudEditor(GuiScreen currentScreen) {
        return HudEditor.isEditorOpen() ? null : currentScreen;
    }
}
