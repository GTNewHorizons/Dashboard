package com.gtnewhorizons.dashboard.mixins.late.waila;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.client.FrameEndHud;

import cpw.mods.fml.common.gameevent.TickEvent;

/** Waila draws its info box at the end of the frame, so in the HUD editor the editor draws it instead. */
@Pseudo
@Mixin(targets = "mcp.mobius.waila.overlay.WailaTickHandler", remap = false)
public abstract class MixinWailaTickHandler_Hud {

    @Shadow(remap = false)
    public abstract void tickRender(TickEvent.RenderTickEvent event);

    @Inject(method = "tickRender", at = @At("HEAD"), cancellable = true)
    private void dashboard$drawInEditor(TickEvent.RenderTickEvent event, CallbackInfo ci) {
        if (FrameEndHud.skip(this, this::tickRender, event)) {
            ci.cancel();
        }
    }
}
