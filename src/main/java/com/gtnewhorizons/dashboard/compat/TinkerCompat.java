package com.gtnewhorizons.dashboard.compat;

import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.dashboard.Dashboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Tinkers' Construct parts Dashboard works with. Its classes are found by name, so Tinkers is not needed. */
@SideOnly(Side.CLIENT)
public final class TinkerCompat {

    /** Bows, crossbows, javelins and throwing knives, which draw their own crosshair. */
    private static final String RANGED_WEAPON = "tconstruct.library.weaponry.IWindup";
    private static final String BACKHAND_UTILS = "xonin.backhand.api.core.BackhandUtils";

    private static Class<?> rangedWeapon;
    private static Method getOffhandItem;
    private static boolean looked;

    private TinkerCompat() {}

    /**
     * Like Tinkers itself, only counts weapons with their tool data, since only those get its crosshair. Also the
     * Backhand off-hand, which Backhand draws the Tinkers crosshair for.
     */
    public static boolean isHoldingRangedWeapon() {
        Class<?> weaponClass = getRangedWeaponClass();
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (weaponClass == null || player == null) {
            return false;
        }
        return isRangedWeapon(weaponClass, player.getCurrentEquippedItem())
            || isRangedWeapon(weaponClass, getOffhandItem(player));
    }

    private static boolean isRangedWeapon(Class<?> weaponClass, ItemStack stack) {
        return stack != null && stack.getTagCompound() != null && weaponClass.isInstance(stack.getItem());
    }

    /** Null without Backhand or without an off-hand item. */
    private static ItemStack getOffhandItem(EntityPlayer player) {
        if (getOffhandItem == null) {
            return null;
        }
        try {
            return (ItemStack) getOffhandItem.invoke(null, player);
        } catch (ReflectiveOperationException e) {
            getOffhandItem = null;
            Dashboard.LOG.warn("Could not read the Backhand off-hand item, its Tinkers crosshair is not checked", e);
            return null;
        }
    }

    private static void findOffhandItemMethod() {
        try {
            getOffhandItem = Class.forName(BACKHAND_UTILS)
                .getMethod("getOffhandItem", EntityPlayer.class);
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not find the Backhand off-hand item, its Tinkers crosshair is not checked", e);
        }
    }

    private static Class<?> getRangedWeaponClass() {
        if (!looked) {
            looked = true;
            if (Mods.TINKERS.isModLoaded()) {
                try {
                    rangedWeapon = Class.forName(RANGED_WEAPON);
                } catch (ClassNotFoundException e) {
                    Dashboard.LOG.warn("Tinkers' ranged weapons were not found, its crosshair setting does nothing");
                }
                if (Mods.BACKHAND.isModLoaded()) {
                    findOffhandItemMethod();
                }
            }
        }
        return rangedWeapon;
    }
}
