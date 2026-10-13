package com.gtnewhorizons.dashboard.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import squeek.applecore.ModConfig;
import squeek.applecore.api.AppleCoreAPI;
import squeek.applecore.api.food.FoodValues;

/** AppleCore's food bar extras, for themed food bars. Its classes are only touched after checking it is loaded. */
@SideOnly(Side.CLIENT)
public final class AppleCoreCompat {

    private AppleCoreCompat() {}

    /** Follows AppleCore's own setting, so players who turned it off there do not get it back. */
    public static boolean showsSaturation() {
        return Mods.APPLECORE.isModLoaded() && ModConfig.SHOW_SATURATION_OVERLAY;
    }

    public static boolean showsExhaustion() {
        return Mods.APPLECORE.isModLoaded() && ModConfig.SHOW_FOOD_EXHAUSTION_UNDERLAY;
    }

    /** From AppleCore, since it can change how much exhaustion costs a food point. */
    public static float getExhaustionFill(EntityPlayer player) {
        return AppleCoreAPI.accessor.getExhaustion(player) / AppleCoreAPI.accessor.getMaxExhaustion(player);
    }

    public static boolean showsHeldFood() {
        return Mods.APPLECORE.isModLoaded() && ModConfig.SHOW_FOOD_VALUES_OVERLAY;
    }

    /** AppleCore knows foods changed by other mods. Null when not holding food. */
    public static HeldFood getHeldFood(EntityPlayer player) {
        ItemStack held = player.getHeldItem();
        if (held == null || !AppleCoreAPI.accessor.isFood(held)) {
            return null;
        }
        FoodValues values = FoodValues.get(held, player);
        return new HeldFood(values.hunger, values.getSaturationIncrement());
    }

    /** Our own holder, so AppleCore's classes stay inside this one. */
    public static final class HeldFood {

        public final int hunger;
        public final float saturation;

        private HeldFood(int hunger, float saturation) {
            this.hunger = hunger;
            this.saturation = saturation;
        }
    }
}
