package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizons.dashboard.DashboardOtherConfig;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Items the player picked up, one row each with the newest at the bottom. More of the same item adds to its row and
 * keeps it showing longer.
 */
@SideOnly(Side.CLIENT)
@EventBusSubscriber(side = Side.CLIENT)
public final class PickupFeed {

    public static final int DEFAULT_SECONDS = 3;
    public static final int MAX_SECONDS = 30;
    public static final int DEFAULT_SLIDE_OUT_MILLIS = 300;
    public static final int MAX_SLIDE_OUT_MILLIS = 2000;

    private static final int MAX_ROWS = 8;
    private static final int ITEM_SIZE = 16;
    private static final int ROW_HEIGHT = ITEM_SIZE + 2;
    private static final int TEXT_GAP = 4;
    private static final int WIDTH = 120;
    private static final int MARGIN = 2;
    private static final int BELOW_MIDDLE = 10;
    private static final ItemStack[] SAMPLE_ITEMS = { new ItemStack(Blocks.cobblestone), new ItemStack(Items.coal),
        new ItemStack(Items.diamond) };
    private static final int[] SAMPLE_COUNTS = { 32, 5, 1 };

    public static final HudElement PICKUP_FEED = new HudElement(
        "dashboard:pickup_feed",
        "dashboard.element.pickup_feed",
        (screenWidth, screenHeight) -> new HudBounds(
            screenWidth - WIDTH - MARGIN,
            screenHeight / 2 + BELOW_MIDDLE,
            WIDTH,
            MAX_ROWS * ROW_HEIGHT)).setHiddenByDefault();

    private static final List<Row> ROWS = new ArrayList<>();

    private PickupFeed() {}

    public static void register() {
        PreviewDrawing.setImitationInBounds(PICKUP_FEED, PickupFeed::drawSample);
        HudRegistry.register(PICKUP_FEED);
    }

    /** Called by the mixin when the player picks up an item. Ignored while hidden, so nothing piles up. */
    public static void add(ItemStack stack) {
        if (!HudLayout.isVisible(PICKUP_FEED)) {
            return;
        }
        long now = Minecraft.getSystemTime();
        int count = stack.stackSize;
        for (Iterator<Row> iterator = ROWS.iterator(); iterator.hasNext();) {
            Row row = iterator.next();
            if (row.stack.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(row.stack, stack)) {
                iterator.remove();
                count += row.count;
                break;
            }
        }
        ItemStack icon = stack.copy();
        icon.stackSize = 1;
        ROWS.add(new Row(icon, count, now));
        if (ROWS.size() > MAX_ROWS) {
            ROWS.remove(0);
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || ROWS.isEmpty()) {
            return;
        }
        long now = Minecraft.getSystemTime();
        long showMillis = DashboardOtherConfig.pickupFeedSeconds * 1000L;
        int slideOutMillis = DashboardOtherConfig.pickupFeedSlideOutMillis;
        ROWS.removeIf(row -> now - row.start >= showMillis + slideOutMillis);
        if (ROWS.isEmpty()) {
            return;
        }
        HudRegistry.render(PICKUP_FEED, () -> {
            HudBounds bounds = PICKUP_FEED
                .getDefaultBounds(event.resolution.getScaledWidth(), event.resolution.getScaledHeight());
            for (int i = 0; i < ROWS.size(); i++) {
                Row row = ROWS.get(i);
                long slidingFor = Math.max(0, now - row.start - showMillis);
                int slideX = slideOutMillis == 0 ? 0 : (int) (slidingFor * WIDTH / slideOutMillis);
                drawRow(bounds, ROWS.size() - 1 - i, row.stack, row.count, slideX);
            }
        });
    }

    private static void drawSample(HudBounds bounds) {
        for (int i = 0; i < SAMPLE_ITEMS.length; i++) {
            drawRow(bounds, SAMPLE_ITEMS.length - 1 - i, SAMPLE_ITEMS[i], SAMPLE_COUNTS[i], 0);
        }
    }

    /**
     * Right aligned: the item at the right edge, its count and name left of it. Both move right together by
     * {@code slideX}, since items can not fade like text.
     */
    private static void drawRow(HudBounds bounds, int rowFromBottom, ItemStack stack, int count, int slideX) {
        int y = bounds.getBottom() - (rowFromBottom + 1) * ROW_HEIGHT + 1;
        int itemX = bounds.getRight() - ITEM_SIZE + slideX;
        PreviewDrawing.drawItem(stack, itemX, y);
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String text = "+" + count + " " + stack.getDisplayName();
        font.drawStringWithShadow(
            text,
            itemX - TEXT_GAP - font.getStringWidth(text),
            y + (ITEM_SIZE - font.FONT_HEIGHT) / 2 + 1,
            0xFFFFFF);
    }

    private static final class Row {

        private final ItemStack stack;
        private final int count;
        private final long start;

        private Row(ItemStack stack, int count, long start) {
            this.stack = stack;
            this.count = count;
            this.start = start;
        }
    }
}
