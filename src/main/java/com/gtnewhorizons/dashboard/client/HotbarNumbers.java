package com.gtnewhorizons.dashboard.client;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.dashboard.DashboardConfig;
import com.gtnewhorizons.dashboard.api.HudBounds;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The key bound to each hotbar slot, drawn on the slot so players can learn the shortcuts. */
@SideOnly(Side.CLIENT)
public final class HotbarNumbers {

    /** Top left, top, top right, left, middle, right, bottom left, bottom, bottom right. */
    public static final int POSITION_COUNT = 9;
    public static final int MIN_SIZE = 50;
    public static final int MAX_SIZE = 100;
    public static final int DEFAULT_SIZE = 75;
    /** The 16 chat colors, black to white. */
    public static final int COLOR_COUNT = 16;
    public static final int DEFAULT_COLOR = 15;

    private static final int SLOTS = 9;
    private static final int SLOT_STEP = 20;
    private static final int ITEM_OFFSET = 3;
    private static final int ITEM_SIZE = 16;

    /** Stands for "show the slot number" in the text cache. */
    private static final int NO_KEY = Integer.MIN_VALUE;

    private static final EnumChatFormatting[] COLORS = Arrays.copyOf(EnumChatFormatting.values(), COLOR_COUNT);

    private static final int[] keyCodes = new int[SLOTS];
    private static final String[] texts = new String[SLOTS];
    private static final int[] widths = new int[SLOTS];
    private static int textsSettings = -1;

    private HotbarNumbers() {}

    public static int getPosition() {
        return MathHelper.clamp_int(DashboardConfig.hotbarNumberPosition, 0, POSITION_COUNT - 1);
    }

    public static int getSize() {
        return MathHelper.clamp_int(DashboardConfig.hotbarNumberSize, MIN_SIZE, MAX_SIZE);
    }

    public static EnumChatFormatting getColor() {
        return COLORS[MathHelper.clamp_int(DashboardConfig.hotbarNumberColor, 0, COLOR_COUNT - 1)];
    }

    /** Called by the hotbar mixin, after the hotbar background and before the items. */
    public static void drawUnderItems(int screenWidth, int screenHeight) {
        if (DashboardConfig.hotbarNumberUnderItems) {
            draw(getHotbarBounds(screenWidth, screenHeight));
        }
    }

    static void drawOverItems(int screenWidth, int screenHeight) {
        if (!DashboardConfig.hotbarNumberUnderItems) {
            draw(getHotbarBounds(screenWidth, screenHeight));
        }
    }

    private static HudBounds getHotbarBounds(int screenWidth, int screenHeight) {
        return VanillaHudElements.getElement(ElementType.HOTBAR)
            .getDefaultBounds(screenWidth, screenHeight);
    }

    /** Uses the hotbar's default bounds, since its move and resize are already applied. */
    static void draw(HudBounds hotbar) {
        if (!DashboardConfig.hotbarNumbers) {
            return;
        }
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        float scale = getSize() / 100f;
        int position = getPosition();
        int column = position % 3;
        int row = position / 3;
        float height = (font.FONT_HEIGHT - 1) * scale;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        // Over whatever is drawn already, the items too when drawn after them
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        for (int slot = 0; slot < SLOTS; slot++) {
            String text = getText(font, slot, scale);
            float width = widths[slot] * scale;
            float x = hotbar.x + ITEM_OFFSET + slot * SLOT_STEP + (ITEM_SIZE - width) * column / 2;
            float y = hotbar.y + ITEM_OFFSET + (ITEM_SIZE - height) * row / 2;
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 0);
            GL11.glScalef(scale, scale, 1);
            font.drawStringWithShadow(text, 0, 0, 0xFFFFFF);
            GL11.glPopMatrix();
        }
        GL11.glPopAttrib();
    }

    /**
     * Made again only when the key or a setting changes, since it runs every frame. NUMPAD1 becomes N1, and long names
     * are cut to fit the slot.
     */
    private static String getText(FontRenderer font, int slot, float scale) {
        int keyCode = DashboardConfig.hotbarNumberBoundKey
            ? Minecraft.getMinecraft().gameSettings.keyBindsHotbar[slot].getKeyCode()
            : NO_KEY;
        EnumChatFormatting color = getColor();
        int settings = (getSize() * 2 + (DashboardConfig.hotbarNumberBoundKey ? 1 : 0)) * COLOR_COUNT + color.ordinal();
        if (textsSettings != settings) {
            textsSettings = settings;
            Arrays.fill(texts, null);
        }
        if (texts[slot] == null || keyCodes[slot] != keyCode) {
            keyCodes[slot] = keyCode;
            String name = keyCode == NO_KEY ? String.valueOf(slot + 1)
                : GameSettings.getKeyDisplayString(keyCode)
                    .replace("NUMPAD", "N");
            String fitted = font.trimStringToWidth(name, (int) (ITEM_SIZE / scale));
            texts[slot] = color + fitted;
            widths[slot] = font.getStringWidth(fitted);
        }
        return texts[slot];
    }
}
