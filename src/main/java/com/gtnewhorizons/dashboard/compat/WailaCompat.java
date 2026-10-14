package com.gtnewhorizons.dashboard.compat;

import java.awt.Dimension;
import java.awt.Point;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.opengl.GL11;

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
    private static final long SAMPLE_REUSE_MILLIS = 1000;

    public static final HudElement INFO_BOX = new HudElement(
        "waila:info_box",
        "dashboard.element.waila.info_box",
        WailaCompat::getBoxBounds);

    private static Fields.ClassFields<Object>.Field<Integer> boxX;
    private static Fields.ClassFields<Object>.Field<Integer> boxY;
    private static Fields.ClassFields<Object>.Field<Integer> boxWidth;
    private static Fields.ClassFields<Object>.Field<Integer> boxHeight;
    private static Fields.ClassFields<Object>.Field<Float> scale;
    private static Fields.ClassFields<Object>.Field<?> boxPosition;
    private static Fields.ClassFields<Object>.Field<?> chromaticBox;
    private static Object chromaticSampleBox;
    private static Method chromaticBoxSize;
    private static boolean warnedChromatic;
    private static Constructor<?> newBox;
    private static Method drawBox;
    private static Object lastBox;
    private static Object sampleBox;
    private static long lastSampleTime;

    private WailaCompat() {}

    public static void register() {
        try {
            Class<?> tooltip = Class.forName(OVERLAY + "Tooltip");
            Class<?> renderer = Class.forName(OVERLAY + "OverlayRenderer");
            Fields.ClassFields<Object> fields = ModFields.of(tooltip);
            boxX = ModFields.intField(fields, "x");
            boxY = ModFields.intField(fields, "y");
            boxWidth = ModFields.intField(fields, "w");
            boxHeight = ModFields.intField(fields, "h");
            // Read last, so a found scale means every box field was found too
            scale = ModFields.floatField(ModFields.of(OVERLAY + "OverlayConfig"), "scale");
            newBox = tooltip.getConstructor(List.class, ItemStack.class);
            drawBox = renderer.getDeclaredMethod("doRenderOverlay", tooltip);
            drawBox.setAccessible(true);
            boxPosition = fields.getUntypedField(Fields.LookupType.DECLARED, "pos");
            chromaticBox = findChromaticBox(renderer);
            if (chromaticBox != null) {
                INFO_BOX.setModNameAddon("ChromaticTooltips");
            }
            Dashboard.LOG.info("Waila info box is drawn by {}", chromaticBox != null ? "ChromaticTooltips" : "Waila");
            INFO_BOX.setPreview((screenWidth, screenHeight) -> drawSampleBox());
        } catch (ReflectiveOperationException | RuntimeException e) {
            Dashboard.LOG.warn("Could not read Waila's info box, its HUD editor size and preview are off", e);
        }
        HudEditor.register(INFO_BOX);
    }

    /** Found by its type, since mixins may rename the fields they add. Null without ChromaticTooltips Compat. */
    private static Fields.ClassFields<Object>.Field<?> findChromaticBox(Class<?> renderer) {
        for (Field field : renderer.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType()
                .getName()
                .endsWith(".TooltipContext")) {
                return ModFields.of(renderer)
                    .getUntypedField(Fields.LookupType.DECLARED, field.getName());
            }
        }
        return null;
    }

    /** Called by the mixin when Waila starts drawing a box. False means the element is hidden. */
    public static boolean beginBox(Object box) {
        lastBox = box;
        return HudEditor.beginRender(INFO_BOX);
    }

    /**
     * ChromaticTooltips builds its box after {@link #beginBox}, so the box placed there is the one from the frame
     * before. Placed again once the new one is built, right before it is scaled and drawn.
     */
    public static void placeChromaticBox() {
        if (chromaticBox == null) {
            // Waila's own drawing pushes the matrix before this point, so it is left alone
            return;
        }
        GL11.glPopMatrix();
        GL11.glPushMatrix();
        HudEditor.translateToPosition(INFO_BOX);
    }

    /**
     * Waila's own drawing code, so the sample matches the player's Waila colors and scale. The same sample is kept
     * while the editor is open, since ChromaticTooltips starts its box over for every new one. A new one is made after
     * a break, so it follows changed Waila settings.
     */
    private static void drawSampleBox() {
        long now = System.currentTimeMillis();
        try {
            if (sampleBox == null || now - lastSampleTime > SAMPLE_REUSE_MILLIS) {
                sampleBox = createSampleBox();
            }
            lastSampleTime = now;
            drawBox.invoke(null, sampleBox);
            if (chromaticBox != null) {
                chromaticSampleBox = chromaticBox.getValue(null);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not draw the Waila sample box", e);
        }
    }

    /** Stone, like looking at a block. */
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
        if (chromaticBox != null) {
            HudBounds chromatic = getChromaticBounds(screenWidth, screenHeight, boxScale);
            if (chromatic != null) {
                return chromatic;
            }
        }
        return new HudBounds(
            Math.round(boxX.getValue(lastBox) * boxScale),
            Math.round(boxY.getValue(lastBox) * boxScale),
            Math.round((boxWidth.getValue(lastBox) + BORDER) * boxScale),
            Math.round((boxHeight.getValue(lastBox) + BORDER) * boxScale));
    }

    /**
     * ChromaticTooltips Compat draws its own box, placed from Waila's position setting with its own size, and leaves
     * Waila's box at 0, 0. Worked out like it does, with the size of the box it drew last. Null before it drew one.
     */
    private static HudBounds getChromaticBounds(int screenWidth, int screenHeight, float boxScale) {
        Object box = chromaticBox.getValue(null);
        if (box == null && lastBox == sampleBox) {
            box = chromaticSampleBox;
        }
        Object position = boxPosition != null ? boxPosition.getValue(lastBox) : null;
        if (box == null || !(position instanceof Point point)) {
            warnOnce(box == null ? "it has not drawn a box yet" : "the box has no position");
            return null;
        }
        try {
            if (chromaticBoxSize == null) {
                chromaticBoxSize = box.getClass()
                    .getMethod("getTooltipSize");
            }
            Dimension size = (Dimension) chromaticBoxSize.invoke(box);
            if (size == null) {
                warnOnce("the box has no size");
                return null;
            }
            int x = ((int) (screenWidth / boxScale) - size.width - 1) * point.x / 10000;
            int y = ((int) (screenHeight / boxScale) - size.height - 1) * point.y / 10000;
            return new HudBounds(
                Math.round(x * boxScale),
                Math.round(y * boxScale),
                Math.round(size.width * boxScale),
                Math.round(size.height * boxScale));
        } catch (ReflectiveOperationException | ClassCastException e) {
            chromaticBox = null;
            Dashboard.LOG.warn("Could not read the ChromaticTooltips Waila box, using Waila's size instead", e);
            return null;
        }
    }

    /** Some of these happen for a frame or two, so only the first one is logged. */
    private static void warnOnce(String reason) {
        if (!warnedChromatic) {
            warnedChromatic = true;
            Dashboard.LOG.warn("Using Waila's size for the ChromaticTooltips Waila box, since {}", reason);
        }
    }
}
