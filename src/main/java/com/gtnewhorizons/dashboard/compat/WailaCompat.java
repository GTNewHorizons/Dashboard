package com.gtnewhorizons.dashboard.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Waila's info box. Its size changes with what the player looks at, so the element takes the place and size of the box
 * Waila drew last, which also follows Waila's own position and scale settings.
 */
@SideOnly(Side.CLIENT)
public final class WailaCompat {

    private static final String OVERLAY = "mcp.mobius.waila.overlay.";
    private static final int BORDER = 1;
    private static final int FALLBACK_WIDTH = 100;
    private static final int FALLBACK_HEIGHT = 30;

    public static final HudElement INFO_BOX = new HudElement(
        "waila:info_box",
        "dashboard.element.waila.info_box",
        WailaCompat::getBoxBounds);

    private static Fields.ClassFields<Object>.Field<Integer> boxX;
    private static Fields.ClassFields<Object>.Field<Integer> boxY;
    private static Fields.ClassFields<Object>.Field<Integer> boxWidth;
    private static Fields.ClassFields<Object>.Field<Integer> boxHeight;
    private static Fields.ClassFields<Object>.Field<Float> scale;
    private static Constructor<?> newBox;
    private static Method drawBox;
    private static Object lastBox;

    private WailaCompat() {}

    public static void register() {
        try {
            Class<?> tooltip = Class.forName(OVERLAY + "Tooltip");
            Fields.ClassFields<Object> fields = ModFields.of(OVERLAY + "Tooltip");
            boxX = ModFields.intField(fields, "x");
            boxY = ModFields.intField(fields, "y");
            boxWidth = ModFields.intField(fields, "w");
            boxHeight = ModFields.intField(fields, "h");
            // Read last, so a found scale means every box field was found too
            scale = ModFields.floatField(ModFields.of(OVERLAY + "OverlayConfig"), "scale");
            newBox = tooltip.getConstructor(List.class, ItemStack.class);
            drawBox = Class.forName(OVERLAY + "OverlayRenderer")
                .getDeclaredMethod("doRenderOverlay", tooltip);
            drawBox.setAccessible(true);
            INFO_BOX.setPreview((screenWidth, screenHeight) -> drawSampleBox());
        } catch (ReflectiveOperationException | RuntimeException e) {
            Dashboard.LOG.warn("Could not read Waila's info box, its HUD editor size and preview are off", e);
        }
        HudEditor.register(INFO_BOX);
    }

    /** Called by the mixin when Waila starts drawing a box. False means the element is hidden. */
    public static boolean beginBox(Object box) {
        lastBox = box;
        return HudEditor.beginRender(INFO_BOX);
    }

    /** Waila's own drawing code, so the sample matches the player's Waila colors and scale. */
    private static void drawSampleBox() {
        try {
            drawBox.invoke(null, createSampleBox());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not draw the Waila sample box", e);
        }
    }

    /** Stone, like looking at a block, made again each time so it follows Waila's current settings. */
    private static Object createSampleBox() throws ReflectiveOperationException {
        ItemStack stone = new ItemStack(Blocks.stone);
        List<String> lines = Arrays.asList(
            EnumChatFormatting.WHITE + stone.getDisplayName(),
            EnumChatFormatting.BLUE.toString() + EnumChatFormatting.ITALIC + "Minecraft");
        return newBox.newInstance(lines, stone);
    }

    private static HudBounds getBoxBounds(int screenWidth, int screenHeight) {
        if (lastBox == null && newBox != null) {
            try {
                lastBox = createSampleBox();
            } catch (ReflectiveOperationException e) {
                newBox = null;
            }
        }
        if (lastBox == null || scale == null) {
            return new HudBounds(screenWidth / 2 - FALLBACK_WIDTH / 2, 0, FALLBACK_WIDTH, FALLBACK_HEIGHT);
        }
        float boxScale = scale.getValue(null);
        return new HudBounds(
            Math.round(boxX.getValue(lastBox) * boxScale),
            Math.round(boxY.getValue(lastBox) * boxScale),
            Math.round((boxWidth.getValue(lastBox) + BORDER) * boxScale),
            Math.round((boxHeight.getValue(lastBox) + BORDER) * boxScale));
    }
}
