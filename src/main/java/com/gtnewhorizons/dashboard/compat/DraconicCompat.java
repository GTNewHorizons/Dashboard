package com.gtnewhorizons.dashboard.compat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.api.HudShift;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draconic Evolution's info box, armor HUD and magnet icon. The info box and armor HUD are placed by Draconic's own HUD
 * settings, as thousandths of the screen, so their default bounds are worked out the same way.
 */
@SideOnly(Side.CLIENT)
public final class DraconicCompat {

    private static final String PACKAGE = "com.brandon3055.draconicevolution.";
    // Indexes into Draconic's hudSettings
    private static final int INFO_X = 0;
    private static final int INFO_Y = 1;
    private static final int ARMOR_X = 2;
    private static final int ARMOR_Y = 3;
    private static final int INFO_SCALE = 4;
    private static final int ARMOR_SCALE = 5;
    private static final int ARMOR_SIDEWAYS = 8;
    private static final int ARMOR_TEXT = 9;
    private static final int INFO_ENABLED = 10;
    private static final int ARMOR_ENABLED = 11;
    private static final int[] DEFAULT_SETTINGS = { 996, 825, 69, 907, 90, 100, 3, 0, 1, 1, 1, 1 };
    private static final int ITEM_SIZE = 16;
    private static final int MAGNET_X = 7;

    public static final HudElement INFO_BOX = new HudElement(
        "draconicevolution:info_box",
        "dashboard.element.draconicevolution.info_box",
        DraconicCompat::getInfoBoxBounds)
            .setOwnSettings("dashboard.element.draconicevolution.own_settings", "key.toolConfig")
            .setActiveCheck(() -> getSetting(INFO_ENABLED) == 1);

    public static final HudElement ARMOR_HUD = new HudElement(
        "draconicevolution:armor_hud",
        "dashboard.element.draconicevolution.armor_hud",
        DraconicCompat::getArmorHudBounds)
            .setOwnSettings("dashboard.element.draconicevolution.own_settings", "key.toolConfig")
            .setActiveCheck(() -> getSetting(ARMOR_ENABLED) == 1);

    public static final HudElement MAGNET_ICON = new HudElement(
        "draconicevolution:magnet_icon",
        "dashboard.element.draconicevolution.magnet_icon",
        (screenWidth, screenHeight) -> new HudBounds(MAGNET_X, (int) (screenHeight * 0.25f), ITEM_SIZE, ITEM_SIZE));

    private static Fields.ClassFields<Object>.Field<int[]> hudSettings;
    @SuppressWarnings("rawtypes")
    private static Fields.ClassFields<Object>.Field<List> infoLines;
    private static Fields.ClassFields<Object>.Field<Float> infoFade;
    private static Fields.ClassFields<Object>.Field<Float> armorFade;
    private static Fields.ClassFields<Object>.Field<Integer> shieldCharge;
    private static Fields.ClassFields<Object>.Field<Float> shieldPoints;
    private static Fields.ClassFields<Object>.Field<Float> maxShieldPoints;
    private static Fields.ClassFields<Object>.Field<Float> shieldEntropy;
    private static Fields.ClassFields<Object>.Field<Integer> energyCharge;
    private static Fields.ClassFields<Object>.Field<Long> energyTotal;
    private static Method drawInfoBox;
    private static Method drawArmorHud;
    private static Object hudHandler;
    private static final HudShift ARMOR_SHIFT = new HudShift();
    private static final HudShift MAGNET_SHIFT = new HudShift();
    private static boolean magnetIconShown;

    private DraconicCompat() {}

    public static void register() {
        try {
            hudSettings = ModFields
                .field(ModFields.of(PACKAGE + "common.handler.ConfigHandler"), "hudSettings", int[].class);
            Class<?> handler = Class.forName(PACKAGE + "client.handler.HudHandler");
            Fields.ClassFields<Object> fields = ModFields.of(handler);
            infoLines = ModFields.field(fields, "hudList", List.class);
            infoFade = ModFields.floatField(fields, "toolTipFadeOut");
            armorFade = ModFields.floatField(fields, "armorStatsFadeOut");
            shieldCharge = ModFields.intField(fields, "shieldPercentCharge");
            shieldPoints = ModFields.floatField(fields, "shieldPoints");
            maxShieldPoints = ModFields.floatField(fields, "maxShieldPoints");
            shieldEntropy = ModFields.floatField(fields, "shieldEntropy");
            energyCharge = ModFields.intField(fields, "rfCharge");
            energyTotal = ModFields.longField(fields, "rfTotal");
            drawInfoBox = Class.forName(PACKAGE + "client.utils.GuiHelper")
                .getMethod(
                    "drawHoveringText",
                    List.class,
                    int.class,
                    int.class,
                    FontRenderer.class,
                    float.class,
                    double.class,
                    int.class,
                    int.class);
            drawArmorHud = handler.getDeclaredMethod("drawArmorHUD", int.class, int.class, boolean.class, double.class);
            drawArmorHud.setAccessible(true);
            PreviewDrawing.setImitation(INFO_BOX, DraconicCompat::drawInfoBoxPreview);
            PreviewDrawing.setImitation(ARMOR_HUD, DraconicCompat::drawArmorHudPreview);
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG
                .warn("Could not read the Draconic Evolution HUD, its HUD editor sizes and previews are off", e);
        }
        Item magnet = GameRegistry.findItem("DraconicEvolution", "magnet");
        if (magnet != null) {
            ItemStack magnetStack = new ItemStack(magnet);
            PreviewDrawing
                .setImitationInBounds(MAGNET_ICON, bounds -> PreviewDrawing.drawItem(magnetStack, bounds.x, bounds.y));
        }
        HudRegistry.register(INFO_BOX);
        HudRegistry.register(ARMOR_HUD);
        HudRegistry.register(MAGNET_ICON);
    }

    /** Called by the mixin each frame, since the box lines and armor values live in Draconic's HUD handler. */
    public static void setHudHandler(Object handler) {
        hudHandler = handler;
    }

    public static boolean beginArmorHud() {
        return ARMOR_SHIFT.begin(ARMOR_HUD);
    }

    public static void endArmorHud() {
        ARMOR_SHIFT.end();
    }

    public static void beginMagnetIcon() {
        magnetIconShown = MAGNET_SHIFT.begin(MAGNET_ICON);
    }

    public static boolean isMagnetIconShown() {
        return magnetIconShown;
    }

    public static void endMagnetIcon() {
        MAGNET_SHIFT.end();
    }

    private static int getSetting(int index) {
        int[] settings = hudSettings != null ? hudSettings.getValue(null) : null;
        return settings != null && settings.length > index ? settings[index] : DEFAULT_SETTINGS[index];
    }

    /** Thousandths of the screen, like Draconic's position settings. */
    private static int getPosition(int index, int screenSize) {
        return (int) (getSetting(index) / 1000f * screenSize);
    }

    private static List<?> getInfoLines() {
        if (hudHandler != null && infoLines != null) {
            List<?> lines = infoLines.getValue(hudHandler);
            if (lines != null && !lines.isEmpty() && infoFade.getValue(hudHandler) > 0) {
                return lines;
            }
        }
        // Draconic's own sample, from its HUD settings screen
        return Arrays.asList(
            StatCollector.translateToLocal("info.de.hudDisplayConfigTxt1.txt"),
            "",
            StatCollector.translateToLocal("info.de.hudDisplayConfigTxt2.txt"),
            "",
            StatCollector.translateToLocal("info.de.hudDisplayConfigTxt3.txt"));
    }

    /** Like Minecraft's tooltip, which Draconic's box copies: next to its spot, kept on screen, in its scale. */
    private static HudBounds getInfoBoxBounds(int screenWidth, int screenHeight) {
        List<?> lines = getInfoLines();
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        double scale = getSetting(INFO_SCALE) / 100d;
        int scaledWidth = (int) (screenWidth / scale);
        int scaledHeight = (int) (screenHeight / scale);
        int textWidth = 0;
        for (Object line : lines) {
            textWidth = Math.max(textWidth, font.getStringWidth(String.valueOf(line)));
        }
        int textHeight = 8 + (lines.size() > 1 ? 2 + (lines.size() - 1) * 10 : 0);
        int textX = (int) (getPosition(INFO_X, screenWidth) / scale) + 12;
        int textY = (int) (getPosition(INFO_Y, screenHeight) / scale) - 12;
        if (textX + textWidth > scaledWidth) {
            textX -= 28 + textWidth;
        }
        if (textY + textHeight + 6 > scaledHeight) {
            textY = scaledHeight - textHeight - 6;
        }
        return new HudBounds(
            (int) Math.round((textX - 4) * scale),
            (int) Math.round((textY - 4) * scale),
            (int) Math.round((textWidth + 8) * scale),
            (int) Math.round((textHeight + 8) * scale));
    }

    /** Upright: the bar with its icon below and its text on the right. Sideways: the icon on the left, text below. */
    private static HudBounds getArmorHudBounds(int screenWidth, int screenHeight) {
        double scale = getSetting(ARMOR_SCALE) / 100d;
        boolean text = getSetting(ARMOR_TEXT) == 1;
        int left;
        int width;
        int height;
        if (getSetting(ARMOR_SIDEWAYS) == 1) {
            left = -15;
            width = 119;
            height = text ? 27 : 17;
        } else {
            left = 0;
            width = text ? Math.max(17, 18 + getArmorTextWidth()) : 17;
            height = 122;
        }
        return new HudBounds(
            getPosition(ARMOR_X, screenWidth) + (int) Math.round(left * scale),
            getPosition(ARMOR_Y, screenHeight),
            (int) Math.round(width * scale),
            (int) Math.round(height * scale));
    }

    /** Draconic's three text lines next to the upright bar. */
    private static int getArmorTextWidth() {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        float points = 1000;
        float maxPoints = 1000;
        float entropy = 0;
        long energy = 0;
        if (hudHandler != null && shieldPoints != null) {
            points = shieldPoints.getValue(hudHandler);
            maxPoints = maxShieldPoints.getValue(hudHandler);
            entropy = shieldEntropy.getValue(hudHandler);
            energy = energyTotal.getValue(hudHandler);
        }
        return Math.max(
            font.getStringWidth(Math.round(points) + "/" + (int) maxPoints),
            Math.max(
                font.getStringWidth("EN:" + (int) entropy + "%"),
                font.getStringWidth("RF:" + NumberFormatUtil.formatNumber(energy))));
    }

    private static void drawInfoBoxPreview(int screenWidth, int screenHeight) {
        try {
            drawInfoBox.invoke(
                null,
                getInfoLines(),
                getPosition(INFO_X, screenWidth),
                getPosition(INFO_Y, screenHeight),
                Minecraft.getMinecraft().fontRenderer,
                1f,
                getSetting(INFO_SCALE) / 100d,
                screenWidth,
                screenHeight);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not draw the Draconic Evolution info box", e);
        }
    }

    /** Draconic's own drawing, with sample values put in its handler for the moment. */
    private static void drawArmorHudPreview(int screenWidth, int screenHeight) {
        if (hudHandler == null) {
            return;
        }
        float oldFade = swap(armorFade, 1f);
        int oldCharge = swap(shieldCharge, 70);
        float oldPoints = swap(shieldPoints, 700f);
        float oldMaxPoints = swap(maxShieldPoints, 1000f);
        float oldEntropy = swap(shieldEntropy, 20f);
        int oldEnergyCharge = swap(energyCharge, 60);
        long oldEnergy = swap(energyTotal, 1_000_000L);
        try {
            drawArmorHud.invoke(
                hudHandler,
                getPosition(ARMOR_X, screenWidth),
                getPosition(ARMOR_Y, screenHeight),
                getSetting(ARMOR_SIDEWAYS) == 1,
                getSetting(ARMOR_SCALE) / 100d);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not draw the Draconic Evolution armor HUD", e);
        } finally {
            armorFade.setValue(hudHandler, oldFade);
            shieldCharge.setValue(hudHandler, oldCharge);
            shieldPoints.setValue(hudHandler, oldPoints);
            maxShieldPoints.setValue(hudHandler, oldMaxPoints);
            shieldEntropy.setValue(hudHandler, oldEntropy);
            energyCharge.setValue(hudHandler, oldEnergyCharge);
            energyTotal.setValue(hudHandler, oldEnergy);
        }
    }

    /** Puts the sample value in Draconic's handler and returns the value it replaced. */
    private static <F> F swap(Fields.ClassFields<Object>.Field<F> field, F sample) {
        F old = field.getValue(hudHandler);
        field.setValue(hudHandler, sample);
        return old;
    }
}
