package com.gtnewhorizons.dashboard.compat;

import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.api.HudShift;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Tainted Magic's Katana charge runes and Thaumic Disassembler mode text. Both are drawn shrunk, at spots worked out
 * from the screen center in their shrunk coordinates, so the default bounds copy that math.
 */
@SideOnly(Side.CLIENT)
public final class TaintedMagicCompat {

    private static final String PACKAGE = "taintedmagic.common.items.tools.";
    private static final float KATANA_SCALE = 0.315f;
    private static final int RUNE_SIZE = 16;
    private static final int RUNE_COUNT = 16;
    private static final float DISASSEMBLER_SCALE = 0.8f;
    private static final int DISASSEMBLER_RAISE = 15;
    private static final ResourceLocation RUNES = new ResourceLocation("thaumcraft", "textures/misc/script.png");

    public static final HudElement KATANA_CHARGE = new HudElement(
        "taintedmagic:katana_charge",
        "dashboard.element.taintedmagic.katana_charge",
        TaintedMagicCompat::getKatanaBounds);

    public static final HudElement DISASSEMBLER_MODE = new HudElement(
        "taintedmagic:disassembler_mode",
        "dashboard.element.taintedmagic.disassembler_mode",
        TaintedMagicCompat::getDisassemblerBounds);

    private static Class<?> disassembler;
    private static Method getModeName;
    private static Method getEfficiency;
    private static final HudShift KATANA_SHIFT = new HudShift();
    private static final HudShift DISASSEMBLER_SHIFT = new HudShift();

    private TaintedMagicCompat() {}

    public static void register() {
        try {
            disassembler = Class.forName(PACKAGE + "ItemThaumicDisassembler");
            getModeName = disassembler.getMethod("getModeName", ItemStack.class);
            getEfficiency = disassembler.getMethod("getEfficiency", ItemStack.class);
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not read the Thaumic Disassembler mode, its HUD editor size is a guess", e);
        }
        PreviewDrawing.setImitationInBounds(KATANA_CHARGE, TaintedMagicCompat::drawKatanaPreview);
        PreviewDrawing.setImitationInBounds(DISASSEMBLER_MODE, TaintedMagicCompat::drawDisassemblerPreview);
        HudRegistry.registerFallback(KATANA_CHARGE);
        HudRegistry.registerFallback(DISASSEMBLER_MODE);
    }

    /** Called by the mixins right before the drawing shrinks itself. False means the player hid it. */
    public static boolean beginKatanaCharge() {
        return KATANA_SHIFT.begin(KATANA_CHARGE);
    }

    public static void endKatanaCharge() {
        KATANA_SHIFT.end();
    }

    public static boolean beginDisassemblerMode() {
        return DISASSEMBLER_SHIFT.begin(DISASSEMBLER_MODE);
    }

    public static void endDisassemblerMode() {
        DISASSEMBLER_SHIFT.end();
    }

    /** A row of 16 runes, lower in creative, where Tainted Magic leaves room for the missing hearts. */
    private static HudBounds getKatanaBounds(int screenWidth, int screenHeight) {
        int x = Math.round((screenWidth / 2 + 725) * KATANA_SCALE);
        int y = Math.round((screenHeight / 2 + (isCreative() ? 805 : 755)) * KATANA_SCALE);
        return new HudBounds(
            x,
            y,
            Math.round(RUNE_COUNT * RUNE_SIZE * KATANA_SCALE),
            Math.round(RUNE_SIZE * KATANA_SCALE) + 1);
    }

    private static HudBounds getDisassemblerBounds(int screenWidth, int screenHeight) {
        int x = Math.round((screenWidth / 2 + 100) * DISASSEMBLER_SCALE);
        int y = Math.round((screenHeight / 2 + (isCreative() ? 230 : 212)) * DISASSEMBLER_SCALE) - DISASSEMBLER_RAISE;
        int width = Minecraft.getMinecraft().fontRenderer.getStringWidth(getDisassemblerText());
        return new HudBounds(x, y, Math.round(width * DISASSEMBLER_SCALE), Math.round(9 * DISASSEMBLER_SCALE));
    }

    private static boolean isCreative() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        return player != null && player.capabilities.isCreativeMode;
    }

    /** Like Tainted Magic's, from the held Disassembler, or a sample. */
    private static String getDisassemblerText() {
        String mode = StatCollector.translateToLocal("text.mode") + ": ";
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        ItemStack held = player != null ? player.getCurrentEquippedItem() : null;
        if (held != null && disassembler != null && disassembler.isInstance(held.getItem())) {
            try {
                return mode + getModeName.invoke(null, held) + " (" + getEfficiency.invoke(null, held) + ")";
            } catch (ReflectiveOperationException e) {
                disassembler = null;
            }
        }
        return mode + "Default (3)";
    }

    /** Like Tainted Magic's runes, the first half charged. */
    private static void drawKatanaPreview(HudBounds bounds) {
        PreviewDrawing.bindTexture(RUNES);
        GL11.glPushMatrix();
        GL11.glTranslatef(bounds.x, bounds.y, 0);
        GL11.glScalef(KATANA_SCALE, KATANA_SCALE, 1);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        for (int rune = 0; rune < RUNE_COUNT; rune++) {
            if (rune < RUNE_COUNT / 2) {
                tessellator.setColorRGBA_F(1 - rune / 16f, rune / 16f, 0, 0.9f);
            } else {
                tessellator.setColorRGBA_F(0.8f, 0.7f, 0.4f, 0.7f);
            }
            float u = rune / 16f;
            int x = rune * RUNE_SIZE;
            tessellator.addVertexWithUV(x, RUNE_SIZE, 0, u, 1);
            tessellator.addVertexWithUV(x + RUNE_SIZE, RUNE_SIZE, 0, u + 1 / 16f, 1);
            tessellator.addVertexWithUV(x + RUNE_SIZE, 0, 0, u + 1 / 16f, 0);
            tessellator.addVertexWithUV(x, 0, 0, u, 0);
        }
        tessellator.draw();
        GL11.glPopMatrix();
    }

    private static void drawDisassemblerPreview(HudBounds bounds) {
        PreviewDrawing.drawScaledString(getDisassemblerText(), bounds.x, bounds.y, DISASSEMBLER_SCALE, 0xFFFFFF);
    }
}
