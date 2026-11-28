package com.gtnewhorizons.dashboard.mixins.early;

import net.minecraft.client.gui.GuiNewChat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.gtnewhorizons.dashboard.client.VanillaHudElements;

/** Clicks and hover tooltips find the chat line under the mouse where the chat is moved to. */
@Mixin(GuiNewChat.class)
public class MixinGuiNewChat_Clicks {

    @ModifyVariable(method = "func_146236_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int dashboard$moveMouseX(int mouseX) {
        return VanillaHudElements.toVanillaChatMouse(mouseX, false);
    }

    @ModifyVariable(method = "func_146236_a", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private int dashboard$moveMouseY(int mouseY) {
        return VanillaHudElements.toVanillaChatMouse(mouseY, true);
    }
}
