package com.gtnewhorizons.dashboard.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;
import com.gtnewhorizons.dashboard.mixins.late.thaumcraft.ClientTickEventsFMLInvoker;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.lib.ClientTickEventsFML;
import thaumcraft.common.config.Config;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.wands.ItemWandCasting;

/** Thaumcraft's HUD. It resets the matrix before drawing each element, so the mixins move them right after that. */
@SideOnly(Side.CLIENT)
public final class ThaumcraftCompat {

    private static final int WAND_DIAL_SIZE = 50;
    private static final int WARP_METER_WIDTH = 20;
    private static final int WARP_METER_HEIGHT = 76;
    private static final int RUNIC_SHIELD_WIDTH = 81;
    private static final int RUNIC_SHIELD_HEIGHT = 9;
    private static final int HOVER_HARNESS_WIDTH = 16;
    private static final int HOVER_HARNESS_HEIGHT = 71;
    /** Notification text is drawn at half size, right aligned. Longer lines stick out to the left. */
    private static final int NOTIFICATIONS_WIDTH = 120;
    private static final int NOTIFICATION_LINE_HEIGHT = 8;

    public static final HudElement WAND_DIAL = new HudElement(
        "thaumcraft:wand_dial",
        "dashboard.element.thaumcraft.wand_dial",
        ThaumcraftCompat::getWandDialBounds).setOwnSettings("dashboard.element.thaumcraft.wand_dial.own_settings");

    public static final HudElement WARP_METER = new HudElement(
        "thaumcraft:warp_meter",
        "dashboard.element.thaumcraft.warp_meter",
        (screenWidth, screenHeight) -> new HudBounds(1, 1, WARP_METER_WIDTH, WARP_METER_HEIGHT));

    /** Drawn where the hearts are, but on its own, so moving Health does not move it. */
    public static final HudElement RUNIC_SHIELD = new HudElement(
        "thaumcraft:runic_shield",
        "dashboard.element.thaumcraft.runic_shield",
        (screenWidth, screenHeight) -> new HudBounds(
            screenWidth / 2 - 91,
            screenHeight - 39,
            RUNIC_SHIELD_WIDTH,
            RUNIC_SHIELD_HEIGHT));

    public static final HudElement HOVER_HARNESS = new HudElement(
        "thaumcraft:hover_harness",
        "dashboard.element.thaumcraft.hover_harness",
        (screenWidth,
            screenHeight) -> new HudBounds(2, screenHeight / 2 - 43, HOVER_HARNESS_WIDTH, HOVER_HARNESS_HEIGHT));

    public static final HudElement NOTIFICATIONS = new HudElement(
        "thaumcraft:notifications",
        "dashboard.element.thaumcraft.notifications",
        ThaumcraftCompat::getNotificationsBounds)
            .setOwnSettings("dashboard.element.thaumcraft.notifications.own_settings");

    private static ClientTickEventsFML tickEventsHandler;
    private static ItemStack sampleWand;
    private static ItemStack sampleHarness;

    private ThaumcraftCompat() {}

    /** Called by the mixin when Thaumcraft creates its HUD handler, since Thaumcraft keeps no reference to it. */
    public static void setTickEventsHandler(ClientTickEventsFML handler) {
        tickEventsHandler = handler;
    }

    public static void register() {
        setPreviews();
        for (HudElement element : new HudElement[] { WAND_DIAL, WARP_METER, RUNIC_SHIELD, HOVER_HARNESS,
            NOTIFICATIONS }) {
            HudEditor.register(element);
        }
    }

    /**
     * The wand dial, warp meter and hover harness use Thaumcraft's own drawing code with sample items, which the
     * mixins move like the real thing. Runic shielding and notifications need player state, so they are imitated.
     */
    private static void setPreviews() {
        WAND_DIAL.setPreview(
            (screenWidth, screenHeight) -> getInvoker()
                .dashboard$renderCastingWandHud(0f, getPlayer(), System.currentTimeMillis(), getSampleWand()));
        WARP_METER.setPreview(
            (screenWidth, screenHeight) -> getInvoker()
                .dashboard$renderSanityHud(0f, getPlayer(), System.currentTimeMillis()));
        HOVER_HARNESS.setPreview((screenWidth, screenHeight) -> {
            if (sampleHarness == null) {
                sampleHarness = new ItemStack(ConfigItems.itemHoverHarness);
            }
            getHandler().renderHoverHUD(0f, getPlayer(), System.currentTimeMillis(), sampleHarness);
        });
        PreviewDrawing.setImitationInBounds(RUNIC_SHIELD, ThaumcraftCompat::drawRunicShieldPreview);
        PreviewDrawing.setImitationInBounds(NOTIFICATIONS, ThaumcraftCompat::drawNotificationsPreview);
    }

    private static ClientTickEventsFML getHandler() {
        if (tickEventsHandler == null) {
            throw new IllegalStateException("Thaumcraft's HUD handler was not created");
        }
        return tickEventsHandler;
    }

    private static ClientTickEventsFMLInvoker getInvoker() {
        return (ClientTickEventsFMLInvoker) getHandler();
    }

    private static EntityPlayer getPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }

    /** An iron capped wooden wand, half full of every primal aspect. */
    private static ItemStack getSampleWand() {
        if (sampleWand == null) {
            sampleWand = new ItemStack(ConfigItems.itemWandCasting);
            ItemWandCasting wand = (ItemWandCasting) sampleWand.getItem();
            wand.setCap(sampleWand, ConfigItems.WAND_CAP_IRON);
            wand.setRod(sampleWand, ConfigItems.WAND_ROD_WOOD);
            for (Aspect aspect : Aspect.getPrimalAspects()) {
                wand.storeVis(sampleWand, aspect, wand.getMaxVis(sampleWand) / 2);
            }
        }
        return sampleWand;
    }

    /** Ten runic shield icons, from the same spot on Thaumcraft's texture as {@code renderRunicArmorBar}. */
    private static void drawRunicShieldPreview(HudBounds bounds) {
        PreviewDrawing.bindTexture(ParticleEngine.particleTexture);
        for (int i = 0; i < 10; i++) {
            PreviewDrawing.drawTexture(bounds.x + i * 8, bounds.y, 160, 16, 9, 9);
        }
    }

    /** A few half size sample lines in the bottom right corner. */
    private static void drawNotificationsPreview(HudBounds bounds) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String text = StatCollector.translateToLocal("dashboard.preview.thaumcraft.notification");
        int lines = Math.min(3, bounds.height / NOTIFICATION_LINE_HEIGHT);
        for (int line = 0; line < lines; line++) {
            GL11.glPushMatrix();
            GL11.glTranslatef(
                bounds.getRight() - 10 - font.getStringWidth(text) / 2f,
                bounds.getBottom() - NOTIFICATION_LINE_HEIGHT * (line + 1),
                0);
            GL11.glScalef(0.5f, 0.5f, 1);
            font.drawString(text, 0, 0, 0xFFFFFF);
            GL11.glPopMatrix();
        }
    }

    /** Top left, or bottom left with Thaumcraft's {@code wand_dial_bottom} config. */
    private static HudBounds getWandDialBounds(int screenWidth, int screenHeight) {
        int y = Config.dialBottom ? screenHeight - WAND_DIAL_SIZE : 0;
        return new HudBounds(0, y, WAND_DIAL_SIZE, WAND_DIAL_SIZE);
    }

    /** One 8 pixel line each, stacked up from the bottom right corner, up to Thaumcraft's {@code notification_max}. */
    private static HudBounds getNotificationsBounds(int screenWidth, int screenHeight) {
        int height = Math.max(1, Config.notificationMax) * NOTIFICATION_LINE_HEIGHT;
        return new HudBounds(
            screenWidth - NOTIFICATIONS_WIDTH - 1,
            screenHeight - 6 - height,
            NOTIFICATIONS_WIDTH,
            height);
    }
}
