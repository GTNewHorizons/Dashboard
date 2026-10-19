package com.gtnewhorizons.dashboard.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * IC2's helmet HUD in the top left corner: the charge of each worn armor piece, and in its second mode the held item's
 * name and info below it. Both are drawn one after the other in the same method, so a hidden one is drawn invisibly.
 */
@SideOnly(Side.CLIENT)
public final class Ic2Compat {

    private static final ResourceLocation PANEL = new ResourceLocation("ic2", "textures/gui/GUIOverlay.png");
    private static final int PANEL_WIDTH = 71;
    private static final int PANEL_HEIGHT = 69;
    private static final int ROW_HEIGHT = 16;
    /** IC2's own key bindings, both pressed together switch the helmet HUD mode. */
    private static final String ALT_KEY = "ALT Key";
    private static final String HUD_KEY = "Hub Expand Key";
    private static final String[] SAMPLE_ARMOR_NAMES = { "quantumHelmet", "quantumBodyarmor", "quantumLeggings",
        "quantumBoots" };

    public static final HudElement ARMOR_CHARGE = new HudElement(
        "ic2:armor_charge",
        "dashboard.element.ic2.armor_charge",
        (screenWidth, screenHeight) -> new HudBounds(0, 0, PANEL_WIDTH, PANEL_HEIGHT))
            .setDescription("dashboard.element.ic2.armor_charge.description", ALT_KEY, HUD_KEY);

    /** Sized for the name and a few info lines, which differ per item. */
    public static final HudElement HELD_ITEM = new HudElement(
        "ic2:held_item",
        "dashboard.element.ic2.held_item",
        (screenWidth, screenHeight) -> new HudBounds(5, 74, 120, 46))
            .setDescription("dashboard.element.ic2.held_item.description", ALT_KEY, HUD_KEY);

    private static final ShiftTracker SHIFT = new ShiftTracker();
    /** A full Quantum suit, helmet first, for the previews. Read once, since IC2 makes them at startup. */
    private static final ItemStack[] SAMPLE_ARMOR = new ItemStack[SAMPLE_ARMOR_NAMES.length];

    private Ic2Compat() {}

    public static void register() {
        try {
            Fields.ClassFields<Object> items = ModFields.of("ic2.core.Ic2Items");
            for (int piece = 0; piece < SAMPLE_ARMOR.length; piece++) {
                SAMPLE_ARMOR[piece] = ModFields.field(items, SAMPLE_ARMOR_NAMES[piece], ItemStack.class)
                    .getValue(null);
            }
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not read the IC2 armor, the HUD editor previews show none", e);
        }
        PreviewDrawing.setImitationInBounds(ARMOR_CHARGE, Ic2Compat::drawArmorCharge);
        PreviewDrawing.setImitationInBounds(HELD_ITEM, Ic2Compat::drawHeldItem);
        HudEditor.register(ARMOR_CHARGE);
        HudEditor.register(HELD_ITEM);
    }

    /** Called by the mixin around each part. A hidden part is drawn invisibly, since the method can not be cut. */
    public static void beginOrHide(HudElement element) {
        SHIFT.beginOrHide(element);
    }

    public static void end() {
        SHIFT.end();
    }

    /** IC2's panel with a full Quantum suit. */
    private static void drawArmorCharge(HudBounds bounds) {
        PreviewDrawing.bindTexture(PANEL);
        PreviewDrawing.drawTexture(bounds.x, bounds.y, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        for (int row = 0; row < SAMPLE_ARMOR.length; row++) {
            int y = bounds.y + 4 + row * ROW_HEIGHT;
            if (SAMPLE_ARMOR[row] != null) {
                PreviewDrawing.drawItem(SAMPLE_ARMOR[row], bounds.x + 5, y);
            }
            font.drawString("100%", bounds.x + 25, y + 5, 0xFFFFFF);
        }
    }

    /** The held item, or a Quantum helmet when the hand is empty. */
    private static void drawHeldItem(HudBounds bounds) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        ItemStack held = player != null ? player.getHeldItem() : null;
        if (held == null) {
            held = SAMPLE_ARMOR[0];
        }
        if (held != null) {
            PreviewDrawing.drawItem(held, bounds.x, bounds.y);
            Minecraft.getMinecraft().fontRenderer
                .drawString(held.getDisplayName(), bounds.x + 25, bounds.y + 4, 0xFFFFFF);
        }
    }
}
