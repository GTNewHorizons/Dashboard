package com.gtnewhorizons.dashboard.mixins.late.adventurebackpack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.compat.AdventureBackpackCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

/**
 * Splits the HUD into potion icons and tanks, which are drawn one after the other, each behind a config check:
 * <ol>
 * <li>Reading {@code statusOverlay} starts the potion icons. Returning false there hides them.</li>
 * <li>Reading {@code tanksOverlay} ends the potion icons and starts the tanks. Returning false hides the tanks.</li>
 * <li>{@code glPopAttrib} at the end of the method ends the tanks.</li>
 * </ol>
 * The boss bar drawn before these (when its {@code bossBarIndent} config is not 0) is left alone.
 */
@Pseudo
@Mixin(targets = "com.darkona.adventurebackpack.client.gui.GuiOverlay", remap = false)
public class MixinGuiOverlay_Hud {

    @Unique
    private boolean dashboard$potionIconsShifted;
    @Unique
    private boolean dashboard$tanksShifted;

    @ModifyExpressionValue(
        method = "onRenderExperienceBar",
        at = @At(value = "FIELD", target = "Lcom/darkona/adventurebackpack/config/ConfigHandler;statusOverlay:Z"))
    private boolean dashboard$beginPotionIcons(boolean enabled) {
        // Only counts as showing when there are effects to draw
        if (!enabled || !AdventureBackpackCompat.hasPotionEffects()) {
            return enabled;
        }
        dashboard$potionIconsShifted = HudEditor.beginRender(AdventureBackpackCompat.POTION_ICONS);
        return dashboard$potionIconsShifted;
    }

    @Inject(
        method = "onRenderExperienceBar",
        at = @At(value = "FIELD", target = "Lcom/darkona/adventurebackpack/config/ConfigHandler;tanksOverlay:Z"))
    private void dashboard$endPotionIcons(CallbackInfo ci) {
        if (dashboard$potionIconsShifted) {
            dashboard$potionIconsShifted = false;
            HudEditor.endRender();
        }
    }

    @ModifyExpressionValue(
        method = "onRenderExperienceBar",
        at = @At(value = "FIELD", target = "Lcom/darkona/adventurebackpack/config/ConfigHandler;tanksOverlay:Z"))
    private boolean dashboard$beginTanks(boolean enabled) {
        // Only counts as showing while wearing something with tanks
        if (!enabled || !AdventureBackpackCompat.isWearingTanks()) {
            return enabled;
        }
        dashboard$tanksShifted = HudEditor.beginRender(AdventureBackpackCompat.TANKS);
        return dashboard$tanksShifted;
    }

    @Inject(
        method = "onRenderExperienceBar",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPopAttrib()V"))
    private void dashboard$endTanks(CallbackInfo ci) {
        if (dashboard$tanksShifted) {
            dashboard$tanksShifted = false;
            HudEditor.endRender();
        }
    }
}
