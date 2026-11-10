package com.gtnewhorizons.dashboard.mixins.late.hungeroverhaul;

import java.util.ArrayList;

import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizons.dashboard.compat.HungerOverhaulCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

/**
 * Draws each status line itself instead of adding it to Forge's text list. An empty line stays in the list, so lines
 * other mods add after it keep their place. Not null, which other mods reading the list may not expect.
 */
@Pseudo
@Mixin(targets = "iguanaman.hungeroverhaul.util.IguanaEventHook", remap = false)
public class MixinIguanaEventHook_Hud {

    @WrapOperation(
        method = "onRenderGameOverlay",
        at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"),
        require = 0,
        expect = 0)
    private boolean dashboard$drawStatusLine(ArrayList<String> list, Object line, Operation<Boolean> original,
        @Local(argsOnly = true) RenderGameOverlayEvent.Text event) {
        HungerOverhaulCompat.drawLine(list == event.right, (String) line, list.size());
        return original.call(list, "");
    }
}
