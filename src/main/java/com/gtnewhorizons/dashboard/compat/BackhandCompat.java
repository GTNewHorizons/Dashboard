package com.gtnewhorizons.dashboard.compat;

import java.lang.reflect.Method;

import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;

import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;
import com.gtnewhorizons.dashboard.client.VanillaHudElements;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Backhand's off-hand slot next to the hotbar. Backhand draws it during the hotbar's event, so on its own it would
 * move with the hotbar. The mixin makes it a separate element.
 */
@SideOnly(Side.CLIENT)
public final class BackhandCompat {

    private static final int SLOT_SIZE = 22;
    /** Offset from the screen center. */
    private static final int SLOT_OFFSET = 125;
    private static final int SLOT_OFFSET_DOUBLE_WIDE = 212;
    private static final ResourceLocation SLOT_TEXTURE = new ResourceLocation(
        Mods.BACKHAND.getResourceLocation(),
        "textures/gui/offhand_slot.png");

    public static final HudElement OFFHAND_SLOT = new HudElement(
        "backhand:offhand_slot",
        "dashboard.element.backhand.offhand_slot",
        BackhandCompat::getOffhandSlotBounds);

    private static Fields.ClassFields<Object>.Field<Integer> slotOffsetX;
    private static Fields.ClassFields<Object>.Field<Integer> slotOffsetY;
    private static boolean doubleWideHotbar;
    private static Method getOffhandItem;
    private static boolean slotShifted;

    private BackhandCompat() {}

    public static void register() {
        try {
            Fields.ClassFields<Object> config = ModFields.of("xonin.backhand.utils.BackhandConfigClient");
            slotOffsetX = ModFields.intField(config, "offhandHotbarSlotXOffset");
            slotOffsetY = ModFields.intField(config, "offhandHotbarSlotYOffset");
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not read the Backhand off-hand slot position, assuming the default", e);
        }
        try {
            getOffhandItem = Class.forName("xonin.backhand.api.core.BackhandUtils")
                .getMethod("getOffhandItem", EntityPlayer.class);
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not find the Backhand off-hand item, its Tinkers crosshair is not checked", e);
        }
        doubleWideHotbar = Loader.isModLoaded("dws");
        PreviewDrawing.setImitationInBounds(OFFHAND_SLOT, BackhandCompat::drawOffhandSlotPreview);
        HudRegistry.registerFallback(OFFHAND_SLOT);
    }

    /** Called by the mixin right where Backhand starts drawing the slot. False means the slot is hidden. */
    public static boolean beginOffhandSlot(int screenWidth, int screenHeight) {
        slotShifted = VanillaHudElements
            .beginSeparateElement(ElementType.HOTBAR, OFFHAND_SLOT, screenWidth, screenHeight);
        return slotShifted;
    }

    /** Safe to call also when the slot was not drawn. */
    public static void endOffhandSlot() {
        if (slotShifted) {
            slotShifted = false;
            VanillaHudElements.endSeparateElement(ElementType.HOTBAR);
        }
    }

    /** Null without an off-hand item, or if Backhand's method was not found. */
    public static ItemStack getOffhandItem(EntityPlayer player) {
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

    /** Left of the hotbar, moved by Backhand's slot offset configs. */
    private static HudBounds getOffhandSlotBounds(int screenWidth, int screenHeight) {
        int offsetX = (doubleWideHotbar ? SLOT_OFFSET_DOUBLE_WIDE : SLOT_OFFSET) - ModFields.readInt(slotOffsetX);
        int offsetY = ModFields.readInt(slotOffsetY);
        return new HudBounds(screenWidth / 2 - offsetX, screenHeight - SLOT_SIZE - offsetY, SLOT_SIZE, SLOT_SIZE);
    }

    private static void drawOffhandSlotPreview(HudBounds bounds) {
        PreviewDrawing.bindTexture(SLOT_TEXTURE);
        Gui.func_146110_a(bounds.x, bounds.y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
    }
}
