package com.gtnewhorizons.dashboard.compat;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;

import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.api.HudShift;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;
import com.gtnewhorizons.dashboard.client.VanillaHudElements;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Bogosorter's usage ticker, the held item and armor popping up left of the hotbar when their count changes, as its
 * own element. Its hotbar swap column sits above the selected hotbar slot, so it moves with the hotbar instead.
 */
@SideOnly(Side.CLIENT)
public final class BogosorterCompat {

    private static final int ITEM_SIZE = 16;
    /** From Bogosorter: the row reaches 219 pixels left of the screen middle, its right end 97. */
    private static final int ROW_LEFT = 219;
    private static final int ROW_WIDTH = ROW_LEFT - 97;
    /** Raised this high once fully slid in. */
    private static final int RAISED = 19;
    /** Bogosorter moves the row this far left when Backhand is loaded, to make room for the off-hand slot. */
    private static final int BACKHAND_SHIFT = 30;
    private static final int SAMPLE_HELD_X = 104;
    private static final int SAMPLE_HELMET_X = 66;

    public static final HudElement USAGE_TICKER = new HudElement(
        "bogosorter:usage_ticker",
        "dashboard.element.bogosorter.usage_ticker",
        (screenWidth, screenHeight) -> new HudBounds(
            screenWidth / 2 - (Mods.BACKHAND.isModLoaded() ? BACKHAND_SHIFT : 0) - ROW_LEFT,
            screenHeight - RAISED,
            ROW_WIDTH,
            ITEM_SIZE)).setDescription("dashboard.element.bogosorter.usage_ticker.description");

    private static final HudShift SHIFT = new HudShift();
    private static final ItemStack SAMPLE_HELD = new ItemStack(Blocks.cobblestone, 48);
    private static final ItemStack SAMPLE_HELMET = new ItemStack(Items.iron_helmet);

    private BogosorterCompat() {}

    public static void register() {
        PreviewDrawing.setImitationInBounds(USAGE_TICKER, BogosorterCompat::drawUsageTicker);
        HudRegistry.register(USAGE_TICKER);
    }

    /** Called by the mixins right before drawing. False means hidden, so the drawing should be skipped. */
    public static boolean beginUsageTicker() {
        return SHIFT.begin(USAGE_TICKER);
    }

    public static boolean beginHotbarSwap() {
        return SHIFT.begin(VanillaHudElements.getElement(ElementType.HOTBAR));
    }

    public static void end() {
        SHIFT.end();
    }

    private static void drawUsageTicker(HudBounds bounds) {
        PreviewDrawing.drawItem(SAMPLE_HELMET, bounds.x + SAMPLE_HELMET_X, bounds.y);
        PreviewDrawing.drawItem(SAMPLE_HELD, bounds.x + SAMPLE_HELD_X, bounds.y);
    }
}
