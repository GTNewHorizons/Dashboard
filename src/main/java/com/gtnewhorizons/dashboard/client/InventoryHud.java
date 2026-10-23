package com.gtnewhorizons.dashboard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The 27 storage slots of the player's inventory on the HUD, on the inventory screen's own background, so resource
 * packs change it too. See-through with Ctrl+scroll in the editor.
 */
@SideOnly(Side.CLIENT)
@EventBusSubscriber(side = Side.CLIENT)
public final class InventoryHud {

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/inventory.png");
    /** The storage rows in the inventory texture, with a 1 pixel border around them. */
    private static final int TEXTURE_X = 6;
    private static final int TEXTURE_Y = 82;
    private static final int WIDTH = 164;
    private static final int HEIGHT = 56;
    private static final int FIRST_ITEM = 2;
    private static final int SLOT_SIZE = 18;
    private static final int COLUMNS = 9;
    private static final int SLOTS = 27;
    /** The storage slots come after the 9 hotbar slots. */
    private static final int FIRST_SLOT = 9;
    private static final int DEFAULT_OPACITY = 30;
    private static final int BELOW_MIDDLE = 129;

    public static final HudElement INVENTORY = new HudElement(
        "dashboard:inventory",
        "dashboard.element.inventory",
        (screenWidth, screenHeight) -> new HudBounds(
            screenWidth / 2 - WIDTH / 2,
            screenHeight / 2 + BELOW_MIDDLE - HEIGHT / 2,
            WIDTH,
            HEIGHT)).setHiddenByDefault();

    private InventoryHud() {}

    public static void register() {
        ElementOpacity.allow(INVENTORY, DEFAULT_OPACITY);
        PreviewDrawing.setImitationInBounds(INVENTORY, InventoryHud::draw);
        HudRegistry.register(INVENTORY);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || Minecraft.getMinecraft().thePlayer == null) {
            return;
        }
        HudRegistry.render(
            INVENTORY,
            () -> draw(
                INVENTORY.getDefaultBounds(event.resolution.getScaledWidth(), event.resolution.getScaledHeight())));
    }

    private static void draw(HudBounds bounds) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) {
            return;
        }
        PreviewDrawing.bindTexture(TEXTURE);
        PreviewDrawing
            .drawTexture(bounds.x, bounds.y, TEXTURE_X, TEXTURE_Y, WIDTH, HEIGHT, ElementOpacity.get(INVENTORY));
        ItemStack[] items = player.inventory.mainInventory;
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = items[FIRST_SLOT + slot];
            if (stack != null) {
                PreviewDrawing.drawItem(
                    stack,
                    bounds.x + FIRST_ITEM + slot % COLUMNS * SLOT_SIZE,
                    bounds.y + FIRST_ITEM + slot / COLUMNS * SLOT_SIZE);
            }
        }
    }
}
