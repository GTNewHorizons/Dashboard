package com.gtnewhorizons.dashboard.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudElement;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class BarThemes {

    public static final List<String> THEMES = Collections.unmodifiableList(Arrays.asList("test"));
    public static final float PREVIEW_FILL = 2 / 3f;

    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private static final Gui GUI = new Gui();

    private BarThemes() {}

    public static String getDisplayName(String theme) {
        return StatCollector.translateToLocal("dashboard.theme." + theme);
    }

    /** The slot's vanilla side. */
    public static boolean fillsFromRight(HudElement element, BarSlot slot) {
        return slot.fromRight != HudLayout.isFlipped(element);
    }

    public static void drawBar(String theme, BarSlot slot, int x, int y, float fraction, boolean fromRight) {
        bindTexture(theme);
        GUI.drawTexturedModalRect(x, y, slot.emptyU, slot.emptyV, slot.width, slot.height);
        drawFill(slot, x, y, fraction, fromRight);
    }

    public static void drawFillOnly(String theme, BarSlot slot, int x, int y, float fraction, boolean fromRight) {
        bindTexture(theme);
        drawFill(slot, x, y, fraction, fromRight);
    }

    private static void drawFill(BarSlot slot, int x, int y, float fraction, boolean fromRight) {
        int fillWidth = Math.round(Math.max(0, Math.min(1, fraction)) * slot.width);
        if (fillWidth <= 0) {
            return;
        }
        int offset = fromRight ? slot.width - fillWidth : 0;
        GUI.drawTexturedModalRect(x + offset, y, slot.fullU + offset, slot.fullV, fillWidth, slot.height);
    }

    private static void bindTexture(String theme) {
        ResourceLocation texture = TEXTURES
            .computeIfAbsent(theme, id -> new ResourceLocation(Dashboard.MODID, "textures/gui/bars/" + id + ".png"));
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1, 1, 1, 1);
    }
}
