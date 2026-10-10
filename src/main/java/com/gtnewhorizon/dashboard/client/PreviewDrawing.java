package com.gtnewhorizon.dashboard.client;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.dashboard.api.HudBounds;
import com.gtnewhorizon.dashboard.api.HudEditor;
import com.gtnewhorizon.dashboard.api.HudElement;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Imitate an element, for elements that need game state to draw for real. */
@SideOnly(Side.CLIENT)
public final class PreviewDrawing {

    private static final Gui GUI = new Gui();

    private PreviewDrawing() {}

    /** Draws the imitation at the element's default position, moved like the real element. */
    public static void setImitation(HudElement element, HudElement.PreviewRenderer draw) {
        element.setPreview(
            (screenWidth, screenHeight) -> HudEditor
                .render(element, () -> draw.drawPreview(screenWidth, screenHeight)));
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

    /** Draws part of the bound texture. */
    public static void drawTexture(int x, int y, int u, int v, int width, int height) {
        GUI.drawTexturedModalRect(x, y, u, v, width, height);
    }
}
