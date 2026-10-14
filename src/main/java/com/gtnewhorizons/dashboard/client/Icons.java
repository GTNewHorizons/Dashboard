package com.gtnewhorizons.dashboard.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.color.ColorResource;
import com.gtnewhorizons.dashboard.Dashboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Icons in textures/gui/icons.png: two rows of 16 x 16 cells for buttons, a 1 pixel gap, then rows of 12 x 12 cells
 * for smaller ones. Painted white with a vanilla text shadow, then tinted with the button text color.
 */
@SideOnly(Side.CLIENT)
public final class Icons {

    public static final int PREVIEW = 0;
    public static final int ARROW_RIGHT = 1;
    public static final int ARROW_LEFT = 2;
    public static final int RESET = 3;
    public static final int REMOVE = 4;
    public static final int HELD = 5;
    public static final int HELP = 6;

    public static final int SMALL_TICK_BOX = 0;
    public static final int SMALL_TICK_BOX_TICKED = 1;

    public static final int SMALL_SIZE = 12;
    private static final int CELL_SIZE = 16;
    private static final int CELLS_PER_ROW = 16;
    private static final int SMALL_TOP = CELL_SIZE * 2 + 1;
    private static final int SMALL_PER_ROW = 256 / SMALL_SIZE;
    private static final ResourceLocation TEXTURE = new ResourceLocation(Dashboard.MODID, "textures/gui/icons.png");

    private Icons() {}

    public static void drawOnButton(GuiButton button, int icon) {
        if (!button.visible) {
            return;
        }
        ColorResource tint = !button.enabled ? ColorUtils.buttonTextDisabled
            : button.func_146115_a() ? ColorUtils.buttonTextHovered : ColorUtils.buttonText;
        drawCentered(icon, button.xPosition + button.width / 2, button.yPosition + button.height / 2, tint);
    }

    public static void drawCentered(int icon, int centerX, int centerY, ColorResource tint) {
        draw(
            centerX - CELL_SIZE / 2,
            centerY - CELL_SIZE / 2,
            icon % CELLS_PER_ROW * CELL_SIZE,
            icon / CELLS_PER_ROW * CELL_SIZE,
            CELL_SIZE,
            tint);
    }

    /** Drawn at its top left corner, since small icons sit in rows of the same height. */
    public static void drawSmall(int icon, int x, int y, ColorResource tint) {
        draw(x, y, icon % SMALL_PER_ROW * SMALL_SIZE, SMALL_TOP + icon / SMALL_PER_ROW * SMALL_SIZE, SMALL_SIZE, tint);
    }

    private static void draw(int x, int y, int u, int v, int size, ColorResource tint) {
        int color = tint.getColor();
        PreviewDrawing.bindTexture(TEXTURE);
        GL11.glColor4f((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f, 1);
        PreviewDrawing.drawTexture(x, y, u, v, size, size);
        GL11.glColor4f(1, 1, 1, 1);
    }
}
