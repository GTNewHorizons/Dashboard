package com.gtnewhorizons.dashboard.mixins.late.draconic;

import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.compat.DraconicCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/** Moves or hides Draconic Evolution's info box and armor HUD. */
@Pseudo
@Mixin(targets = "com.brandon3055.draconicevolution.client.handler.HudHandler", remap = false)
public class MixinHudHandler_Hud {

    /** Draconic only fills the info box while no screen is open, so the HUD editor counts as no screen here. */
    @ModifyExpressionValue(
        method = "clientTick",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;",
            remap = true))
    private GuiScreen dashboard$fillInHudEditor(GuiScreen currentScreen) {
        return HudEditor.isEditorOpen() ? null : currentScreen;
    }

    @Inject(method = "drawHUD", at = @At("HEAD"))
    private void dashboard$rememberHandler(CallbackInfo ci) {
        DraconicCompat.setHudHandler(this);
    }

    @WrapOperation(
        method = "drawHUD",
        at = @At(
            value = "INVOKE",
            target = "Lcom/brandon3055/draconicevolution/client/utils/GuiHelper;drawHoveringText(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;FDII)V"))
    private void dashboard$moveInfoBox(List<?> lines, int x, int y, FontRenderer font, float alpha, double scale,
        int screenWidth, int screenHeight, Operation<Void> original) {
        if (DraconicCompat.beginInfoBox()) {
            try {
                original.call(lines, x, y, font, alpha, scale, screenWidth, screenHeight);
            } finally {
                HudEditor.endRender();
            }
        }
    }

    /** The armor HUD is drawn last, so stopping here skips only it. */
    @Inject(
        method = "drawHUD",
        at = @At(
            value = "INVOKE",
            target = "Lcom/brandon3055/draconicevolution/client/handler/HudHandler;drawArmorHUD(IIZD)V"),
        cancellable = true)
    private void dashboard$moveArmorHud(CallbackInfo ci) {
        if (!DraconicCompat.beginArmorHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawHUD", at = @At("RETURN"))
    private void dashboard$restoreArmorHud(CallbackInfo ci) {
        DraconicCompat.endArmorHud();
    }
}
