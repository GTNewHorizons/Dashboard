package com.gtnewhorizons.dashboard.compat;

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

    private static Class<?> rangedWeapon;
    private static boolean looked;

    private TinkerCompat() {}

    /** Like Tinkers itself, only counts weapons with their tool data, since only those get its crosshair. */
    public static boolean isHoldingRangedWeapon() {
        Class<?> weaponClass = getRangedWeaponClass();
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (weaponClass == null || player == null) {
            return false;
        }
        ItemStack held = player.getCurrentEquippedItem();
        return held != null && held.getTagCompound() != null && weaponClass.isInstance(held.getItem());
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
            }
        }
        return rangedWeapon;
    }
}
