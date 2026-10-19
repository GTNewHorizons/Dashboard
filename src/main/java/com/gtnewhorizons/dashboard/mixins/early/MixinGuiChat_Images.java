package com.gtnewhorizons.dashboard.mixins.early;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.client.ChatImageControls;
import com.gtnewhorizons.dashboard.client.HudLayout;

/** Lets image elements take mouse input in chat, and saves what was changed when chat closes. */
@Mixin(GuiChat.class)
public abstract class MixinGuiChat_Images extends GuiScreen {

    @Inject(method = "handleMouseInput", at = @At("HEAD"), cancellable = true)
    private void dashboard$handleImages(CallbackInfo ci) {
        if (ChatImageControls.handleMouseInput(width, height)) {
            ci.cancel();
        }
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void dashboard$saveLayout(CallbackInfo ci) {
        HudLayout.save();
    }
}
