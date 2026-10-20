package com.gtnewhorizons.dashboard.compat;

import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.api.HudShift;
import com.gtnewhorizons.dashboard.client.HudLayout;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import thaumcraft.common.config.Config;

/**
 * Thaumic Tinkerer's Dislocation focus box, Kami tool mode text and soul hearts. The soul hearts sit by the food bar
 * but are drawn during the health bar's event, so Dashboard draws them itself, as an element of their own.
 */
@SideOnly(Side.CLIENT)
@EventBusSubscriber(side = Side.CLIENT)
public final class ThaumicTinkererCompat {

    private static final String PACKAGE = "thaumic.tinkerer.";
    private static final int ITEM_SIZE = 18;
    private static final int TEXT_PADDING = 4;
    private static final int TOOL_MODE_OFFSET = 72;
    private static final int SOUL_HEARTS_WIDTH = 81;
    private static final int SOUL_HEARTS_HEIGHT = 9;
    private static final int SAMPLE_SOUL_HP = 20;
    private static final int BOX_COLOR = 0x66000000;
    private static final int TEXT_COLOR = 0xFFAA00;

    public static final HudElement DISLOCATION_FOCUS = new HudElement(
        "thaumictinkerer:dislocation_focus",
        "dashboard.element.thaumictinkerer.dislocation_focus",
        ThaumicTinkererCompat::getDislocationBounds)
            .setOwnSettings("dashboard.element.thaumictinkerer.dislocation_focus.own_settings");

    public static final HudElement TOOL_MODE = new HudElement(
        "thaumictinkerer:tool_mode",
        "dashboard.element.thaumictinkerer.tool_mode",
        ThaumicTinkererCompat::getToolModeBounds);

    public static final HudElement SOUL_HEARTS = new HudElement(
        "thaumictinkerer:soul_hearts",
        "dashboard.element.thaumictinkerer.soul_hearts",
        ThaumicTinkererCompat::getSoulHeartsBounds)
            .setOwnSettings("dashboard.element.thaumictinkerer.soul_hearts.own_settings");

    private static Fields.ClassFields<Object>.Field<String> toolModeText;
    private static Fields.ClassFields<Object>.Field<Integer> soulHp;
    private static Fields.ClassFields<Object>.Field<Integer> soulHeartHeight;
    private static Method drawSoulHearts;
    private static final HudShift DISLOCATION_SHIFT = new HudShift();
    private static final HudShift TOOL_MODE_SHIFT = new HudShift();

    private ThaumicTinkererCompat() {}

    @EventBusSubscriber.Condition
    public static boolean isThaumicTinkererLoaded() {
        return Mods.THAUMIC_TINKERER.isModLoaded();
    }

    public static void register() {
        try {
            toolModeText = ModFields.field(
                ModFields.of(PACKAGE + "client.core.handler.kami.ToolModeHUDHandler"),
                "currentTooltip",
                String.class);
            Class<?> soulHearts = Class.forName(PACKAGE + "client.core.handler.kami.SoulHeartClientHandler");
            soulHp = ModFields.intField(ModFields.of(soulHearts), "clientPlayerHP");
            soulHeartHeight = ModFields
                .intField(ModFields.of(PACKAGE + "common.core.handler.ConfigHandler"), "soulHeartHeight");
            drawSoulHearts = soulHearts
                .getMethod("renderHUD", ScaledResolution.class, EntityPlayer.class, ItemStack.class);
            PreviewDrawing.setImitation(SOUL_HEARTS, (screenWidth, screenHeight) -> drawSoulHeartsPreview());
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG
                .warn("Could not read the Thaumic Tinkerer HUD, its HUD editor sizes and soul hearts are off", e);
        }
        PreviewDrawing.setImitationInBounds(DISLOCATION_FOCUS, ThaumicTinkererCompat::drawDislocationPreview);
        PreviewDrawing.setImitationInBounds(TOOL_MODE, ThaumicTinkererCompat::drawToolModePreview);
        for (HudElement element : new HudElement[] { DISLOCATION_FOCUS, TOOL_MODE, SOUL_HEARTS }) {
            HudRegistry.registerFallback(element);
        }
    }

    /** Called by the mixin right before the Dislocation box starts drawing. False means the player hid it. */
    public static boolean beginDislocationFocus() {
        return DISLOCATION_SHIFT.begin(DISLOCATION_FOCUS);
    }

    public static void endDislocationFocus() {
        DISLOCATION_SHIFT.end();
    }

    public static boolean beginToolMode() {
        return TOOL_MODE_SHIFT.begin(TOOL_MODE);
    }

    public static void endToolMode() {
        TOOL_MODE_SHIFT.end();
    }

    /** True when Dashboard draws the soul hearts itself, so Thaumic Tinkerer's own drawing is skipped. */
    public static boolean drawsSoulHearts() {
        return drawSoulHearts != null;
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || drawSoulHearts == null
            || soulHp.getValue(null) <= 0) {
            return;
        }
        HudRegistry.render(SOUL_HEARTS, () -> drawSoulHearts(event.resolution));
    }

    private static void drawSoulHearts(ScaledResolution resolution) {
        try {
            drawSoulHearts.invoke(null, resolution, Minecraft.getMinecraft().thePlayer, null);
        } catch (ReflectiveOperationException e) {
            drawSoulHearts = null;
            Dashboard.LOG
                .error("Could not draw the Thaumic Tinkerer soul hearts, Thaumic Tinkerer draws them again", e);
        }
    }

    /** Thaumic Tinkerer's own drawing, with sample soul hearts for the moment. */
    private static void drawSoulHeartsPreview() {
        int realHp = soulHp.getValue(null);
        soulHp.setValue(null, SAMPLE_SOUL_HP);
        try {
            drawSoulHearts(HudLayout.getResolution());
        } finally {
            soulHp.setValue(null, realHp);
        }
    }

    /** Next to Thaumcraft's wand dial: the block's icon, with the text in the small font beside it. */
    private static HudBounds getDislocationBounds(int screenWidth, int screenHeight) {
        int y = Config.dialBottom ? screenHeight - 70 : 50;
        int textWidth = getSmallTextWidth(StatCollector.translateToLocal("ttmisc.focusDislocation.tooltip"));
        return new HudBounds(TEXT_PADDING - 1, y - 1, ITEM_SIZE + 1 + textWidth + TEXT_PADDING, ITEM_SIZE + 1);
    }

    /** Centered above the hotbar, a little above the held item's name. */
    private static HudBounds getToolModeBounds(int screenWidth, int screenHeight) {
        int width = Minecraft.getMinecraft().fontRenderer.getStringWidth(getToolModeText());
        return new HudBounds((screenWidth - width) / 2, screenHeight - TOOL_MODE_OFFSET, width, 9);
    }

    /** Above the food bar, at Thaumic Tinkerer's soulHeart.height, higher while the air bubbles show. */
    private static HudBounds getSoulHeartsBounds(int screenWidth, int screenHeight) {
        int y = screenHeight - ModFields.readInt(soulHeartHeight, 49);
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player != null && player.getAir() < 300) {
            y -= 10;
        }
        return new HudBounds(screenWidth / 2 + 10, y, SOUL_HEARTS_WIDTH, SOUL_HEARTS_HEIGHT);
    }

    private static String getToolModeText() {
        String text = toolModeText != null ? toolModeText.getValue(null) : null;
        return text == null || text.isEmpty() ? StatCollector.translateToLocal("ttmisc.mode.pick.1") : text;
    }

    private static int getSmallTextWidth(String text) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        boolean unicode = font.getUnicodeFlag();
        font.setUnicodeFlag(true);
        int width = font.getStringWidth(text);
        font.setUnicodeFlag(unicode);
        return width;
    }

    /** Like Thaumic Tinkerer's: a chest in the box, the text in the small font beside it. */
    private static void drawDislocationPreview(HudBounds bounds) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String text = StatCollector.translateToLocal("ttmisc.focusDislocation.tooltip");
        int x = bounds.x + 1;
        int y = bounds.y + 1;
        Gui.drawRect(bounds.x, bounds.y, x + ITEM_SIZE, y + ITEM_SIZE, BOX_COLOR);
        Gui.drawRect(x + ITEM_SIZE, y, bounds.getRight(), y + 9, BOX_COLOR);
        boolean unicode = font.getUnicodeFlag();
        font.setUnicodeFlag(true);
        font.drawStringWithShadow(text, x + ITEM_SIZE + 2, y, TEXT_COLOR);
        font.setUnicodeFlag(unicode);
        PreviewDrawing.drawItem(new ItemStack(Blocks.chest), x, y);
    }

    private static void drawToolModePreview(HudBounds bounds) {
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(getToolModeText(), bounds.x, bounds.y, TEXT_COLOR);
    }
}
