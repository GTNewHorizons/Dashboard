package com.gtnewhorizons.dashboard.client;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Imitate an element, for elements that need game state to draw for real. */
@SideOnly(Side.CLIENT)
public final class PreviewDrawing {

    public static final int HOTBAR_WIDTH = 182;
    public static final int HOTBAR_HEIGHT = 22;
    private static final ResourceLocation HOTBAR_TEXTURE = new ResourceLocation("textures/gui/widgets.png");
    private static final Gui GUI = new Gui();
    private static final RenderItem ITEM_RENDERER = new RenderItem();

    private PreviewDrawing() {}

    /** Draws the imitation at the element's default position, moved like the real element. */
    public static void setImitation(HudElement element, HudElement.PreviewRenderer draw) {
        element.setPreview((screenWidth, screenHeight) -> {
            if (HudRegistry.beginRender(element, screenWidth, screenHeight)) {
                try {
                    draw.drawPreview(screenWidth, screenHeight);
                } finally {
                    HudRegistry.endRender();
                }
            }
        });
    }

    /** Like {@link #setImitation}, for imitations that only need the element's default bounds. */
    public static void setImitationInBounds(HudElement element, Consumer<HudBounds> draw) {
        setImitation(
            element,
            (screenWidth, screenHeight) -> draw.accept(element.getDefaultBounds(screenWidth, screenHeight)));
    }

    public static void bindTexture(ResourceLocation texture) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1, 1, 1, 1);
    }

    /** Vanilla's version is not static, hence the shared Gui. */
    public static void drawTexture(int x, int y, int u, int v, int width, int height) {
        GUI.drawTexturedModalRect(x, y, u, v, width, height);
    }

    /** Text with a shadow, its top left corner at x, y, drawn at the given size. */
    public static void drawScaledString(String text, float x, float y, float scale, int color) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(scale, scale, 1);
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text, 0, 0, color);
        GL11.glPopMatrix();
    }

    /** The vanilla hotbar without its selection. Leaves the hotbar texture bound, which also holds the selection. */
    public static void drawHotbarBackground(int x, int y) {
        bindTexture(HOTBAR_TEXTURE);
        drawTexture(x, y, 0, 0, HOTBAR_WIDTH, HOTBAR_HEIGHT);
    }

    /** Items in hotbar slots from the left, null for an empty slot. */
    public static void drawHotbarItems(int x, int y, ItemStack[] items) {
        for (int slot = 0; slot < items.length; slot++) {
            if (items[slot] != null) {
                drawItem(items[slot], x + 3 + slot * 20, y + 3);
            }
        }
    }

    /** Off draws invisibly, for code that can not be skipped but should not show, like a hidden element. */
    public static void setColorWrites(boolean enabled) {
        GL11.glColorMask(enabled, enabled, enabled, enabled);
        GL11.glDepthMask(enabled);
    }

    /** With its stack count, lit like in inventories, and the lighting turned off again after. */
    public static void drawItem(ItemStack stack, int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        ITEM_RENDERER.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
        ITEM_RENDERER.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
    }
}
