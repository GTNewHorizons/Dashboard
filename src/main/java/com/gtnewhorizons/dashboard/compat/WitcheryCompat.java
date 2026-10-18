package com.gtnewhorizons.dashboard.compat;

import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Witchery's HUD: the vampire blood meter and its target's blood, drawn with the HUD, plus the infusion and creature
 * power bars and the brew and bark belt charges, drawn at the end of the frame. Parts drawn one after the other in the
 * same method are drawn invisibly when hidden, since the method can not be cut. The vampire power bar replaces the
 * hotbar, so it already moves with the Hotbar element. Its radial menus and glyph grid are left alone.
 */
@SideOnly(Side.CLIENT)
public final class WitcheryCompat {

    private static final String PACKAGE = "com.emoniph.witchery.";
    private static final int DROP_SIZE = 8;
    private static final int BLOOD_PER_DROP = 250;
    private static final int SAMPLE_DROPS = 4;
    private static final int TARGET_DROPS = 4;
    private static final ResourceLocation DROP = new ResourceLocation("witchery", "textures/gui/bdrop.png");
    private static final ResourceLocation DROP_EMPTY = new ResourceLocation("witchery", "textures/gui/bdropfaded.png");
    private static final ResourceLocation BARK = new ResourceLocation("witchery", "textures/gui/creatures.png");
    private static final int BAR_WIDTH = 8;
    private static final int BAR_HEIGHT = 41;
    private static final int ICON_SIZE = 16;
    private static final int BREW_HEIGHT = 18;
    private static final int BARK_SIZE = 8;
    private static final int BARK_MAX_CHARGES = 10;
    private static final int BARK_SAMPLE_CHARGES = 5;
    private static final String SIDE_SETTINGS = "dashboard.element.witchery.side.own_settings";

    public static final HudElement BLOOD_METER = new HudElement(
        "witchery:blood_meter",
        "dashboard.element.witchery.blood_meter",
        WitcheryCompat::getBloodMeterBounds).setOwnSettings(SIDE_SETTINGS);

    public static final HudElement TARGET_BLOOD = new HudElement(
        "witchery:target_blood",
        "dashboard.element.witchery.target_blood",
        (screenWidth, screenHeight) -> new HudBounds(
            screenWidth / 2 - TARGET_DROPS * DROP_SIZE / 2,
            screenHeight / 2 + 10,
            TARGET_DROPS * DROP_SIZE,
            DROP_SIZE));

    public static final HudElement INFUSION_ENERGY = barElement("infusion_energy", 20);
    public static final HudElement CREATURE_POWER = barElement("creature_power", 30);

    public static final HudElement INFUSED_BREW = new HudElement(
        "witchery:infused_brew",
        "dashboard.element.witchery.infused_brew",
        (screenWidth, screenHeight) -> new HudBounds(
            getSideX(17, 23, screenWidth),
            screenHeight / 2 + 26,
            ICON_SIZE,
            BREW_HEIGHT)).setOwnSettings(SIDE_SETTINGS);

    public static final HudElement BARK_BELT = new HudElement(
        "witchery:bark_belt",
        "dashboard.element.witchery.bark_belt",
        WitcheryCompat::getBarkBeltBounds);

    private static Object config;
    private static Fields.ClassFields<Object>.Field<Boolean> guiOnLeft;
    private static Method extendedPlayer;
    private static Method isVampire;
    private static Method getMaxBlood;
    private static Method drawBar;
    private static Object infusionBar;
    private static Object creatureBar;
    private static final ShiftTracker SHIFT = new ShiftTracker();

    private WitcheryCompat() {}

    public static void register() {
        try {
            Class<?> configClass = Class.forName(PACKAGE + "util.Config");
            config = configClass.getMethod("instance")
                .invoke(null);
            guiOnLeft = ModFields.booleanField(ModFields.of(configClass), "guiOnLeft");
            Class<?> playerClass = Class.forName(PACKAGE + "common.ExtendedPlayer");
            extendedPlayer = playerClass.getMethod("get", EntityPlayer.class);
            isVampire = playerClass.getMethod("isVampire");
            getMaxBlood = playerClass.getMethod("getMaxBloodPower");
            Class<?> barClass = Class.forName(PACKAGE + "client.renderer.RenderInfusionEnergyBar");
            infusionBar = barClass.getConstructor(boolean.class)
                .newInstance(true);
            creatureBar = barClass.getConstructor(boolean.class)
                .newInstance(false);
            drawBar = barClass
                .getMethod("draw", double.class, double.class, double.class, EntityPlayer.class, int.class);
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not read the Witchery HUD, its HUD editor sizes and previews are a guess", e);
        }
        PreviewDrawing.setImitationInBounds(BLOOD_METER, bounds -> drawDrops(bounds, false));
        PreviewDrawing.setImitationInBounds(TARGET_BLOOD, bounds -> drawDrops(bounds, true));
        PreviewDrawing.setImitationInBounds(INFUSION_ENERGY, bounds -> drawBar(bounds, true));
        PreviewDrawing.setImitationInBounds(CREATURE_POWER, bounds -> drawBar(bounds, false));
        PreviewDrawing.setImitationInBounds(INFUSED_BREW, WitcheryCompat::drawBrew);
        PreviewDrawing.setImitationInBounds(BARK_BELT, WitcheryCompat::drawBark);
        for (HudElement element : new HudElement[] { BLOOD_METER, TARGET_BLOOD, INFUSION_ENERGY, CREATURE_POWER,
            INFUSED_BREW, BARK_BELT }) {
            HudEditor.register(element);
        }
    }

    /** A glass bar a little above the middle of the screen, at the side Witchery's GUIOnLeft config picks. */
    private static HudElement barElement(String name, int sideOffset) {
        return new HudElement(
            "witchery:" + name,
            "dashboard.element.witchery." + name,
            (screenWidth, screenHeight) -> new HudBounds(
                getSideX(sideOffset, sideOffset, screenWidth),
                screenHeight / 2 - 16,
                BAR_WIDTH,
                BAR_HEIGHT)).setOwnSettings(SIDE_SETTINGS);
    }

    /** For a part drawn in the middle of a method: a hidden one is drawn invisibly, since the method can not be cut. */
    public static void beginOrHide(HudElement element) {
        SHIFT.beginOrHide(element);
    }

    /** For a part with a method of its own. False means hidden, so the method should be skipped. */
    public static boolean begin(HudElement element) {
        return SHIFT.begin(element);
    }

    public static void end() {
        SHIFT.end();
    }

    /**
     * One drop per 250 of the vampire's maximum blood, stacked up from the middle of the screen at the side Witchery's
     * GUIOnLeft config picks.
     */
    private static HudBounds getBloodMeterBounds(int screenWidth, int screenHeight) {
        int drops = getMaxDrops();
        int bottom = (int) (screenHeight * 0.5f + 16) + DROP_SIZE;
        return new HudBounds(getSideX(10, 10, screenWidth), bottom - drops * DROP_SIZE, DROP_SIZE, drops * DROP_SIZE);
    }

    /**
     * Above the health bar, like one more row of hearts. Witchery leaves room for every row of vanilla hearts, squeezed
     * together like vanilla does, or for the single row Tinkers' Construct draws.
     */
    private static HudBounds getBarkBeltBounds(int screenWidth, int screenHeight) {
        int heartsHeight = 10;
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player != null && !Mods.TINKERS.isModLoaded()) {
            int rows = MathHelper.ceiling_float_int((player.getMaxHealth() + player.getAbsorptionAmount()) / 2 / 10);
            heartsHeight += (rows - 1) * Math.max(10 - (rows - 2), 3);
        }
        return new HudBounds(
            screenWidth / 2 - 91,
            screenHeight - 39 - heartsHeight,
            BARK_MAX_CHARGES * BARK_SIZE,
            BARK_SIZE);
    }

    private static int getSideX(int leftX, int rightOffset, int screenWidth) {
        return isGuiOnLeft() ? leftX : screenWidth - rightOffset;
    }

    private static boolean isGuiOnLeft() {
        return guiOnLeft == null || guiOnLeft.getValue(config);
    }

    /** A few drops for players who are not vampires, so the box is not empty. */
    private static int getMaxDrops() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player != null && extendedPlayer != null) {
            try {
                Object vampire = extendedPlayer.invoke(null, player);
                if ((boolean) isVampire.invoke(vampire)) {
                    return Math.max(1, (int) getMaxBlood.invoke(vampire) / BLOOD_PER_DROP);
                }
            } catch (ReflectiveOperationException e) {
                extendedPlayer = null;
            }
        }
        return SAMPLE_DROPS;
    }

    /** Witchery's drops, half of them full: in a row for the target, stacked up for the meter. */
    private static void drawDrops(HudBounds bounds, boolean row) {
        int drops = row ? TARGET_DROPS : bounds.height / DROP_SIZE;
        for (int drop = 0; drop < drops; drop++) {
            int x = row ? bounds.x + drop * DROP_SIZE : bounds.x;
            int y = row ? bounds.y : bounds.getBottom() - (drop + 1) * DROP_SIZE;
            PreviewDrawing.bindTexture(drop < drops / 2 ? DROP : DROP_EMPTY);
            Gui.func_146110_a(x, y, 0, 0, DROP_SIZE, DROP_SIZE, DROP_SIZE, DROP_SIZE);
        }
    }

    /** Witchery's own bar, half full. */
    private static void drawBar(HudBounds bounds, boolean infusion) {
        if (drawBar == null) {
            return;
        }
        try {
            Object bar = infusion ? infusionBar : creatureBar;
            drawBar.invoke(bar, bounds.x, bounds.y, 0.5, Minecraft.getMinecraft().thePlayer, 1);
        } catch (ReflectiveOperationException e) {
            drawBar = null;
            Dashboard.LOG.warn("Could not draw the Witchery bar preview", e);
        }
    }

    /** A potion with its minutes left written over it, like Witchery does. */
    private static void drawBrew(HudBounds bounds) {
        PreviewDrawing.drawItem(new ItemStack(Items.potionitem), bounds.x, bounds.y);
        Minecraft.getMinecraft().fontRenderer
            .drawStringWithShadow("5", bounds.x + ICON_SIZE / 2 - 2, bounds.y + 10, 0xFFFFFF);
    }

    private static void drawBark(HudBounds bounds) {
        PreviewDrawing.bindTexture(BARK);
        for (int charge = 0; charge < BARK_SAMPLE_CHARGES; charge++) {
            PreviewDrawing.drawTexture(bounds.x + charge * BARK_SIZE, bounds.y, 0, 248, BARK_SIZE, BARK_SIZE);
        }
    }
}
