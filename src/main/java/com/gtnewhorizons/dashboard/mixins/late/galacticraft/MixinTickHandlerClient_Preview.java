package com.gtnewhorizons.dashboard.mixins.late.galacticraft;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.client.FrameEndHud;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Galacticraft and GalaxySpace only draw their overlays when no screen (or the chat) is open, so the HUD editor counts
 * as no screen here. That way players see the real elements while moving them. They draw at the end of the frame, so in
 * the editor the editor runs them instead.
 */
@Pseudo
@Mixin(
    targets = { "micdoodle8.mods.galacticraft.core.tick.TickHandlerClient",
        "galaxyspace.core.handler.GSClientEventHandler" },
    remap = false)
public abstract class MixinTickHandlerClient_Preview {

    @Shadow(remap = false)
    public abstract void onRenderTick(TickEvent.RenderTickEvent event);

    @Inject(method = "onRenderTick", at = @At("HEAD"), cancellable = true)
    private void dashboard$drawInEditor(TickEvent.RenderTickEvent event, CallbackInfo ci) {
        if (FrameEndHud.skip(this, this::onRenderTick, event)) {
            ci.cancel();
        }
    }

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
