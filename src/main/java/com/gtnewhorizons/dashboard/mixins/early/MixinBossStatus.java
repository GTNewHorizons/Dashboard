package com.gtnewhorizons.dashboard.mixins.early;

import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.boss.IBossDisplayData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.client.BarNumbers;

@Mixin(BossStatus.class)
public class MixinBossStatus {

    // Remember boss
    @Inject(method = "setBossStatus", at = @At("HEAD"))
    private static void dashboard$rememberBoss(IBossDisplayData boss, boolean hasColorModifier, CallbackInfo ci) {
        BarNumbers.setBoss(boss);
    }
}
