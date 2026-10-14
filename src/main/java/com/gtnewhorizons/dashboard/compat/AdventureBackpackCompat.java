package com.gtnewhorizons.dashboard.compat;

import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
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
 * Adventure Backpack's potion icons and tank gauges, drawn in one method that the mixin splits into two elements.
 * Positions come from Adventure Backpack's own config.
 */
@SideOnly(Side.CLIENT)
public final class AdventureBackpackCompat {

    private static final String PACKAGE = "com.darkona.adventurebackpack.";

    private static final int ICON_SIZE = 18;
    private static final int ICON_STEP = 20;
    /** How far Adventure Backpack moves the icons right to make room for the Thaumcraft wand dial. */
    private static final int THAUMCRAFT_DIAL_SPACE = 50;
    private static final int TANK_WIDTH = 10;
    private static final int TANK_HEIGHT = 23;
    private static final int TANKS_WIDTH = TANK_WIDTH * 3 + 1;
    private static final ResourceLocation INVENTORY_TEXTURE = new ResourceLocation(
        "textures/gui/container/inventory.png");
    private static final ResourceLocation TANKS_TEXTURE = new ResourceLocation(
        Mods.ADVENTURE_BACKPACK.getResourceLocation(),
        "textures/gui/overlay.png");

    public static final HudElement POTION_ICONS = new HudElement(
        "adventurebackpack:potion_icons",
        "dashboard.element.adventurebackpack.potion_icons",
        AdventureBackpackCompat::getPotionIconsBounds);

    public static final HudElement TANKS = new HudElement(
        "adventurebackpack:tanks",
        "dashboard.element.adventurebackpack.tanks",
        AdventureBackpackCompat::getTanksBounds);

    private static Fields.ClassFields<Object>.Field<Boolean> statusLeft;
    private static Fields.ClassFields<Object>.Field<Boolean> statusTop;
    private static Fields.ClassFields<Object>.Field<Integer> statusIndentH;
    private static Fields.ClassFields<Object>.Field<Integer> statusIndentV;
    private static Fields.ClassFields<Object>.Field<Boolean> statusThaumcraft;
    private static Fields.ClassFields<Object>.Field<Boolean> tanksRight;
    private static Fields.ClassFields<Object>.Field<Boolean> tanksBottom;
    private static Fields.ClassFields<Object>.Field<Integer> tanksIndentH;
    private static Fields.ClassFields<Object>.Field<Integer> tanksIndentV;
    private static Fields.ClassFields<Object>.Field<Boolean> thaumcraftDialBottom;
    private static Method isThaumcraftTool;
    private static Method getWearingTanks;
    private static final ShiftTracker POTION_ICONS_SHIFT = new ShiftTracker();
    private static final ShiftTracker TANKS_SHIFT = new ShiftTracker();

    private AdventureBackpackCompat() {}

    public static void register() {
        try {
            Fields.ClassFields<Object> config = ModFields.of(PACKAGE + "config.ConfigHandler");
            statusLeft = ModFields.booleanField(config, "statusOverlayLeft");
            statusTop = ModFields.booleanField(config, "statusOverlayTop");
            statusIndentH = ModFields.intField(config, "statusOverlayIndentH");
            statusIndentV = ModFields.intField(config, "statusOverlayIndentV");
            statusThaumcraft = ModFields.booleanField(config, "statusOverlayThaumcraft");
            tanksRight = ModFields.booleanField(config, "tanksOverlayRight");
            tanksBottom = ModFields.booleanField(config, "tanksOverlayBottom");
            tanksIndentH = ModFields.intField(config, "tanksOverlayIndentH");
            tanksIndentV = ModFields.intField(config, "tanksOverlayIndentV");
            thaumcraftDialBottom = ModFields
                .booleanField(ModFields.of(PACKAGE + "util.ThaumcraftUtils"), "DIAL_BOTTOM");
            isThaumcraftTool = Class.forName(PACKAGE + "util.ThaumcraftUtils")
                .getMethod("isTool", ItemStack.class);
            getWearingTanks = Class.forName(PACKAGE + "util.Wearing")
                .getMethod("getWearingWearableInv", EntityPlayer.class);
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not read the Adventure Backpack HUD settings, positions may be off", e);
        }
        PreviewDrawing.setImitationInBounds(POTION_ICONS, AdventureBackpackCompat::drawPotionIconsPreview);
        PreviewDrawing.setImitationInBounds(TANKS, AdventureBackpackCompat::drawTanksPreview);
        HudEditor.register(POTION_ICONS);
        HudEditor.register(TANKS);
    }

    /** As many sample icons as fit the box, from the vanilla inventory texture like Adventure Backpack. */
    private static void drawPotionIconsPreview(HudBounds bounds) {
        PreviewDrawing.bindTexture(INVENTORY_TEXTURE);
        int iconCount = (bounds.width + ICON_STEP - ICON_SIZE) / ICON_STEP;
        for (int i = 0; i < iconCount; i++) {
            PreviewDrawing.drawTexture(bounds.x + i * ICON_STEP, bounds.y, i * ICON_SIZE, 198, ICON_SIZE, ICON_SIZE);
        }
    }

    /** Two empty tank frames, right of where the tool icons go. */
    private static void drawTanksPreview(HudBounds bounds) {
        PreviewDrawing.bindTexture(TANKS_TEXTURE);
        int tanksX = bounds.x + TANK_WIDTH;
        PreviewDrawing.drawTexture(tanksX, bounds.y, 10, 0, TANK_WIDTH, TANK_HEIGHT);
        PreviewDrawing.drawTexture(tanksX + TANK_WIDTH + 1, bounds.y, 10, 0, TANK_WIDTH, TANK_HEIGHT);
    }

    /**
     * Called where Adventure Backpack reads its potion icons setting, with that setting. False hides them. Only counts
     * as showing when there are effects to draw.
     */
    public static boolean beginPotionIcons(boolean enabled) {
        if (!enabled || !hasPotionEffects()) {
            return enabled;
        }
        return POTION_ICONS_SHIFT.begin(POTION_ICONS);
    }

    public static void endPotionIcons() {
        POTION_ICONS_SHIFT.end();
    }

    /** Like {@link #beginPotionIcons}, only counting as showing while wearing something with tanks. */
    public static boolean beginTanks(boolean enabled) {
        if (!enabled || !isWearingTanks()) {
            return enabled;
        }
        return TANKS_SHIFT.begin(TANKS);
    }

    public static void endTanks() {
        TANKS_SHIFT.end();
    }

    private static boolean hasPotionEffects() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        return player != null && !player.getActivePotionEffects()
            .isEmpty();
    }

    /** A backpack or another item with tanks, which Adventure Backpack draws the gauges for. */
    private static boolean isWearingTanks() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null || getWearingTanks == null) {
            return false;
        }
        try {
            return getWearingTanks.invoke(null, player) != null;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /** One icon per effect, also effects without one, growing away from the configured screen edge. */
    private static HudBounds getPotionIconsBounds(int screenWidth, int screenHeight) {
        boolean left = ModFields.readBoolean(statusLeft, true);
        boolean top = ModFields.readBoolean(statusTop, true);
        int indentH = ModFields.readInt(statusIndentH);
        int indentV = ModFields.readInt(statusIndentV);

        int firstIconX = left ? indentH : screenWidth - ICON_SIZE - indentH;
        if (left && top == !ModFields.readBoolean(thaumcraftDialBottom, false)
            && ModFields.readBoolean(statusThaumcraft, false)
            && isHoldingThaumcraftTool()) {
            firstIconX += THAUMCRAFT_DIAL_SPACE;
        }
        int y = top ? indentV : screenHeight - ICON_SIZE - indentV;

        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        int iconCount = player == null ? 1
            : Math.max(
                1,
                player.getActivePotionEffects()
                    .size());
        int width = ICON_STEP * iconCount - (ICON_STEP - ICON_SIZE);
        int x = left ? firstIconX : firstIconX + ICON_SIZE - width;
        return new HudBounds(x, y, width, ICON_SIZE);
    }

    private static HudBounds getTanksBounds(int screenWidth, int screenHeight) {
        int indentH = ModFields.readInt(tanksIndentH);
        int indentV = ModFields.readInt(tanksIndentV);
        int tanksX = ModFields.readBoolean(tanksRight, true) ? screenWidth - TANK_WIDTH * 2 - indentH : indentH;
        int y = ModFields.readBoolean(tanksBottom, true) ? screenHeight - TANK_HEIGHT - indentV : indentV;
        return new HudBounds(tanksX - TANK_WIDTH, y, TANKS_WIDTH, TANK_HEIGHT);
    }

    private static boolean isHoldingThaumcraftTool() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null || isThaumcraftTool == null) {
            return false;
        }
        try {
            return (boolean) isThaumcraftTool.invoke(null, player.getHeldItem());
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
