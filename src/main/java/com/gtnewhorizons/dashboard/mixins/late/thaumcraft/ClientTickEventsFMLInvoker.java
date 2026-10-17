package com.gtnewhorizons.dashboard.mixins.late.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import thaumcraft.client.lib.ClientTickEventsFML;

/** Calls Thaumcraft's private HUD drawing, for the HUD editor previews with sample items. */
@Mixin(value = ClientTickEventsFML.class, remap = false)
public interface ClientTickEventsFMLInvoker {

    @Invoker("renderCastingWandHud")
    void dashboard$renderCastingWandHud(Float partialTicks, EntityPlayer player, long time, ItemStack wand);

    @Invoker("renderSanityHud")
    void dashboard$renderSanityHud(Float partialTicks, EntityPlayer player, long time);
}
