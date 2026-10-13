package com.gtnewhorizons.dashboard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.DashboardConfig;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Crosshair shapes, one 16 x 16 cell each in textures/gui/crosshairs.png. Style 0 is vanilla's. How many cells are
 * filled comes from the "crosshairs" entry in themes.json, so resource packs can add their own.
 */
@SideOnly(Side.CLIENT)
@EventBusSubscriber(side = Side.CLIENT)
public final class Crosshairs {

    public static final int VANILLA = 0;
    /** A full 256 x 256 sheet of 16 x 16 cells. */
    public static final int MAX_STYLES = 256;
    public static final int MAX_SIZE = 5;
    public static final int CELL_SIZE = 16;
    /** Like vanilla's, pixel 7, 7 of a cell lands on the middle of the screen. */
    private static final int CENTER = 7;

    static final ResourceLocation TEXTURE = new ResourceLocation(Dashboard.MODID, "textures/gui/crosshairs.png");

    private static int styleCount;

    private Crosshairs() {}

    public static int getStyleCount() {
        return styleCount;
    }

    static void setStyleCount(int count) {
        styleCount = MathHelper.clamp_int(count, 0, MAX_STYLES);
    }

    /** The saved style, or vanilla if the current resource packs have fewer crosshairs. */
    public static int getStyle() {
        int style = DashboardConfig.crosshairStyle;
        return style > styleCount ? VANILLA : Math.max(VANILLA, style);
    }

    @SubscribeEvent
    public static void onPreRender(RenderGameOverlayEvent.Pre event) {
        if (event.type != ElementType.CROSSHAIRS) {
            return;
        }
        int style = getStyle();
        int size = MathHelper.clamp_int(DashboardConfig.crosshairSize, 1, MAX_SIZE);
        if (style == VANILLA && size == 1) {
            return;
        }
        event.setCanceled(true);
        draw(style, size, event.resolution.getScaledWidth() / 2, event.resolution.getScaledHeight() / 2);
    }

    /** Inverts what is behind it like vanilla, so its more visible on any background. */
    public static void draw(int style, int size, int centerX, int centerY) {
        PreviewDrawing.bindTexture(style == VANILLA ? Gui.icons : TEXTURE);
        int cell = style - 1;
        int u = style == VANILLA ? 0 : cell % 16 * CELL_SIZE;
        int v = style == VANILLA ? 0 : cell / 16 * CELL_SIZE;

        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0);
        GL11.glScalef(size, size, 1);
        OpenGlHelper.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR, 1, 0);
        PreviewDrawing.drawTexture(-CENTER, -CENTER, u, v, CELL_SIZE, CELL_SIZE);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(Gui.icons);
    }
}
