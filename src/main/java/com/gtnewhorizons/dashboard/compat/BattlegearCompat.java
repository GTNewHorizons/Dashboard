package com.gtnewhorizons.dashboard.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeHooks;

import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Battlegear's shield block bar above the hotbar, and the quiver slots at the top of the screen. Each is drawn by a
 * method of its own, so a hidden one is skipped.
 */
@SideOnly(Side.CLIENT)
public final class BattlegearCompat {

    private static final ResourceLocation SHIELD_BAR_TEXTURE = new ResourceLocation(
        "battlegear2",
        "textures/gui/Shield Bar.png");
    private static final ResourceLocation HOTBAR = new ResourceLocation("textures/gui/widgets.png");
    private static final int SHIELD_BAR_WIDTH = 182;
    private static final int SHIELD_BAR_HEIGHT = 9;
    private static final int QUIVER_SLOTS = 4;
    private static final int QUIVER_HALF_WIDTH = 1 + QUIVER_SLOTS * 10;
    private static Fields.ClassFields<Object>.Field<int[]> shieldOffset;
    private static Fields.ClassFields<Object>.Field<int[]> quiverOffset;

    public static final HudElement SHIELD_BAR = new HudElement(
        "battlegear2:shield_bar",
        "dashboard.element.battlegear2.shield_bar",
        BattlegearCompat::getShieldBarBounds).setDescription("dashboard.element.battlegear2.shield_bar.description")
            .setOwnSettings("dashboard.element.battlegear2.shield_bar.own_settings");

    /** One pixel wider and taller than the bar, for the selected slot's frame. */
    public static final HudElement QUIVER = new HudElement(
        "battlegear2:quiver",
        "dashboard.element.battlegear2.quiver",
        (screenWidth, screenHeight) -> new HudBounds(
            screenWidth / 2 + readOffset(quiverOffset, 0) - QUIVER_HALF_WIDTH - 1,
            readOffset(quiverOffset, 1) - 1,
            QUIVER_HALF_WIDTH * 2 + 2,
            23)).setDescription("dashboard.element.battlegear2.quiver.description")
                .setOwnSettings("dashboard.element.battlegear2.quiver.own_settings");

    private static final ShiftTracker SHIFT = new ShiftTracker();

    private BattlegearCompat() {}

    public static void register() {
        try {
            Fields.ClassFields<Object> config = ModFields.of("mods.battlegear2.utils.BattlegearConfig");
            shieldOffset = ModFields.field(config, "shieldBarOffset", int[].class);
            quiverOffset = ModFields.field(config, "quiverBarOffset", int[].class);
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not read the Battlegear bar positions, assuming the defaults", e);
        }
        PreviewDrawing.setImitationInBounds(SHIELD_BAR, BattlegearCompat::drawShieldBar);
        PreviewDrawing.setImitationInBounds(QUIVER, BattlegearCompat::drawQuiver);
        HudEditor.register(SHIELD_BAR);
        HudEditor.register(QUIVER);
    }

    /** Called by the mixin at the start of each bar's method. False means hidden, so the method should be skipped. */
    public static boolean begin(HudElement element) {
        return SHIFT.begin(element);
    }

    public static void end() {
        SHIFT.end();
    }

    /**
     * Above the hotbar, raised like Battlegear does: over the health bar in survival, and over the armor or air bar
     * too when either shows or the player rides a horse.
     */
    private static HudBounds getShieldBarBounds(int screenWidth, int screenHeight) {
        int y = screenHeight - 35 + readOffset(shieldOffset, 1);
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (player != null) {
            boolean ridingHorse = player.isRidingHorse();
            if (player.capabilities.isCreativeMode) {
                y -= ridingHorse ? 5 : 0;
            } else {
                y -= 16;
                if (ridingHorse || player.getAir() < 300 || ForgeHooks.getTotalArmorValue(player) > 0) {
                    y -= 10;
                }
            }
        }
        return new HudBounds(
            screenWidth / 2 - 91 + readOffset(shieldOffset, 0),
            y,
            SHIELD_BAR_WIDTH,
            SHIELD_BAR_HEIGHT);
    }

    /** Battlegear's config holds x, then y. */
    private static int readOffset(Fields.ClassFields<Object>.Field<int[]> offset, int index) {
        int[] values = offset != null ? offset.getValue(null) : null;
        return values != null && values.length > index ? values[index] : 0;
    }

    /** Battlegear's bar, two thirds full. */
    private static void drawShieldBar(HudBounds bounds) {
        PreviewDrawing.bindTexture(SHIELD_BAR_TEXTURE);
        PreviewDrawing.drawTexture(bounds.x, bounds.y, 0, 0, SHIELD_BAR_WIDTH, SHIELD_BAR_HEIGHT);
        PreviewDrawing
            .drawTexture(bounds.x, bounds.y, 0, SHIELD_BAR_HEIGHT, SHIELD_BAR_WIDTH * 2 / 3, SHIELD_BAR_HEIGHT);
    }

    /** The two ends of the hotbar texture with the first slot selected, holding arrows. */
    private static void drawQuiver(HudBounds bounds) {
        int centerX = bounds.x + 1 + QUIVER_HALF_WIDTH;
        int y = bounds.y + 1;
        PreviewDrawing.bindTexture(HOTBAR);
        PreviewDrawing.drawTexture(centerX - QUIVER_HALF_WIDTH, y, 0, 0, QUIVER_HALF_WIDTH, 22);
        PreviewDrawing.drawTexture(centerX, y, 182 - QUIVER_HALF_WIDTH, 0, QUIVER_HALF_WIDTH, 22);
        PreviewDrawing.drawTexture(bounds.x, bounds.y, 0, 22, 24, 22);
        PreviewDrawing.drawItem(new ItemStack(Items.arrow, 64), centerX - QUIVER_HALF_WIDTH + 3, y + 3);
    }
}
