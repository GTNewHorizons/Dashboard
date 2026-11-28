package com.gtnewhorizons.dashboard.mixins.early;

import java.util.List;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiNewChat.class)
public interface GuiNewChatAccessor {

    /** The wrapped lines on screen, newest first. */
    @Accessor("field_146253_i")
    List<ChatLine> dashboard$getDrawnLines();
}
