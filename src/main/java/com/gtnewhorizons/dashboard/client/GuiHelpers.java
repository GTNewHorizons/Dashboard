package com.gtnewhorizons.dashboard.client;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Small pieces the editor screens share, so each screen does not keep its own copy. */
@SideOnly(Side.CLIENT)
final class GuiHelpers {

    static final int BUTTON_HEIGHT = 20;
    private static final String ELLIPSIS = "...";
    private static final ResourceLocation CLICK_SOUND = new ResourceLocation("gui.button.press");
    private static final int DIRT_COLOR = 0x404040;
    private static final float DIRT_TILE = 32;

    private GuiHelpers() {}

    /** A button of the usual height, whose row places it later. Its label is set when it updates. */
    static GuiButton button(int id, int x, int width) {
        return button(id, x, width, "");
    }

    static GuiButton button(int id, int x, int width, String label) {
        return new GuiButton(id, x, 0, width, BUTTON_HEIGHT, label);
    }

    static IconButton iconButton(int id, int x, int width, int icon) {
        return new IconButton(id, x, 0, width, BUTTON_HEIGHT, icon);
    }

    static String getOnOff(boolean on) {
        return StatCollector.translateToLocal(on ? "dashboard.customize.number.on" : "dashboard.customize.number.off");
    }

    /** Forward, or back with Shift. */
    static int getClickStep() {
        return GuiScreen.isShiftKeyDown() ? -1 : 1;
    }

    /** Wraps around, also below zero, where Java's % would stay negative. */
    static int wrap(int value, int count) {
        return (value % count + count) % count;
    }

    /** Text centered in a line, like on a button. */
    static int getTextY(FontRenderer font, int lineY) {
        return lineY + (BUTTON_HEIGHT - font.FONT_HEIGHT) / 2 + 1;
    }

    /** Cut by pixel width with "..." at the end, since letters differ in width. */
    static String fitToWidth(FontRenderer font, String text, int maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) {
            return text;
        }
        return font.trimStringToWidth(text, maxWidth - font.getStringWidth(ELLIPSIS)) + ELLIPSIS;
    }

    static List<String> getTooltip(String langKey) {
        return Collections.singletonList(StatCollector.translateToLocal(langKey));
    }

    /** The full text as a tooltip, for text that had to be cut to fit. Null when nothing was cut. */
    static List<String> getCutTooltip(String shown, String full) {
        return shown.equals(full) ? null : Collections.singletonList(full);
    }

    /** Vanilla's button click, for clicks that are not on a button. */
    static void playClickSound() {
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(PositionedSoundRecord.func_147674_a(CLICK_SOUND, 1.0F));
    }

    /** Plays the click sound when the button was hit, like vanilla. */
    static boolean press(GuiButton button, int mouseX, int mouseY) {
        if (!button.mousePressed(Minecraft.getMinecraft(), mouseX, mouseY)) {
            return false;
        }
        playClickSound();
        return true;
    }

    /**
     * Vanilla's darkened options dirt across the screen, like above and below menu lists. Forge's list only draws it
     * outside a world, and it hides rows scrolled past the list's edges.
     */
    static void drawDirtStrip(int screenWidth, int top, int bottom) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(Gui.optionsBackground);
        GL11.glColor4f(1, 1, 1, 1);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorOpaque_I(DIRT_COLOR);
        tessellator.addVertexWithUV(0, bottom, 0, 0, bottom / DIRT_TILE);
        tessellator.addVertexWithUV(screenWidth, bottom, 0, screenWidth / DIRT_TILE, bottom / DIRT_TILE);
        tessellator.addVertexWithUV(screenWidth, top, 0, screenWidth / DIRT_TILE, top / DIRT_TILE);
        tessellator.addVertexWithUV(0, top, 0, 0, top / DIRT_TILE);
        tessellator.draw();
    }

    /** The mouse position of the current mouse event, in GUI pixels. */
    static int getEventMouseX(int screenWidth) {
        return Mouse.getEventX() * screenWidth / Minecraft.getMinecraft().displayWidth;
    }

    /** Counted from the top, unlike the mouse event's own value. */
    static int getEventMouseY(int screenHeight) {
        return screenHeight - Mouse.getEventY() * screenHeight / Minecraft.getMinecraft().displayHeight - 1;
    }

    /** For buttons that stay clickable while greyed out, since vanilla ignores clicks on disabled buttons. */
    static boolean isMouseOver(GuiButton button, int mouseX, int mouseY) {
        return button.visible && mouseX >= button.xPosition
            && mouseY >= button.yPosition
            && mouseX < button.xPosition + button.width
            && mouseY < button.yPosition + button.height;
    }
}
