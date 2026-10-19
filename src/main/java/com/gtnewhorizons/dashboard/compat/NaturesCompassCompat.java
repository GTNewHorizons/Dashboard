package com.gtnewhorizons.dashboard.compat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Nature's Compass biome search text, shown while holding the compass, in the corner its overlaySide config picks.
 * Every line is drawn on its own, so each one is moved the same way.
 */
@SideOnly(Side.CLIENT)
public final class NaturesCompassCompat {

    private static final String PACKAGE = "com.chaosthedude.naturescompass.";
    private static final int LINE_HEIGHT = 9;
    /** Wide enough for the coordinates line, which is the longest. */
    private static final int WIDTH = 130;
    /** The last line of each state's text, which is how the mod counts its lines. */
    private static final int SEARCHING_LAST_LINE = 4;
    private static final int NOT_FOUND_LAST_LINE = 7;
    private static final int FOUND_LAST_LINE = 10;
    private static final String LANG = "string.naturescompass.";
    private static final int LABEL_COLOR = 0xFFFFFF;
    private static final int VALUE_COLOR = 0xAAAAAA;
    private static final String DEFAULT_SIDE = "BOTTOMRIGHT";

    public static final HudElement BIOME_INFO = new HudElement(
        "naturescompass:biome_info",
        "dashboard.element.naturescompass.biome_info",
        NaturesCompassCompat::getBounds).setDescription("dashboard.element.naturescompass.biome_info.description")
            .setOwnSettings("dashboard.element.naturescompass.biome_info.own_settings");

    /** Plain reflection, since its type is the mod's own enum and GTNHLib's Fields wants the exact type. */
    private static Field overlaySide;
    private static Fields.ClassFields<Object>.Field<Integer> lineOffset;
    private static Fields.ClassFields<Object>.Field<Integer> lastLine;

    private NaturesCompassCompat() {}

    public static void register() {
        try {
            Class<?> configClass = Class.forName(PACKAGE + "config.ConfigHandler");
            overlaySide = configClass.getField("overlaySide");
            lineOffset = ModFields.intField(ModFields.of(configClass), "lineOffset");
            lastLine = ModFields.intField(ModFields.of(PACKAGE + "client.RenderTickHandler"), "maxLineNum");
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not read the Nature's Compass text position, assuming the default", e);
        }
        PreviewDrawing.setImitationInBounds(BIOME_INFO, NaturesCompassCompat::drawSample);
        HudEditor.register(BIOME_INFO);
    }

    /** Called by the mixin for every line. */
    public static void drawLine(Runnable draw) {
        HudEditor.render(BIOME_INFO, draw);
    }

    /**
     * Like Nature's Compass: lines start 2 pixels in from its spot, 9 pixels apart, pushed down by lineOffset lines.
     * At the bottom, the text ends above the hotbar's height, so it grows upward with more lines.
     */
    private static HudBounds getBounds(int screenWidth, int screenHeight) {
        int lines = getLastLine() + 1;
        String side = getSide();
        int x = side.endsWith("RIGHT") ? screenWidth - 8 - WIDTH : 7;
        int top = side.startsWith("TOP") ? -2 : screenHeight - (lines - 1) * LINE_HEIGHT - 26;
        return new HudBounds(x, top + ModFields.readInt(lineOffset) * LINE_HEIGHT, WIDTH, lines * LINE_HEIGHT);
    }

    private static String getSide() {
        try {
            Object side = overlaySide != null ? overlaySide.get(null) : null;
            return side instanceof Enum<?>enumSide ? enumSide.name() : DEFAULT_SIDE;
        } catch (IllegalAccessException e) {
            return DEFAULT_SIDE;
        }
    }

    /** The mod keeps the line count of the text it drew last, searching until then. */
    private static int getLastLine() {
        return Math.max(ModFields.readInt(lastLine), SEARCHING_LAST_LINE);
    }

    /** The text of the state the box is sized for, aligned to the side of the screen like the real one. */
    private static void drawSample(HudBounds bounds) {
        List<String> lines = getSampleLines();
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        boolean right = getSide().endsWith("RIGHT");
        for (int line = 0; line < lines.size(); line++) {
            String text = lines.get(line);
            int x = right ? bounds.getRight() - font.getStringWidth(text) : bounds.x;
            font.drawStringWithShadow(
                text,
                x,
                bounds.y + line * LINE_HEIGHT,
                line % 3 == 0 ? LABEL_COLOR : VALUE_COLOR);
        }
    }

    /** Searching, not found with the search radius, or found with where it is. Labels use the mod's translations. */
    private static List<String> getSampleLines() {
        int last = getLastLine();
        String status = last == FOUND_LAST_LINE ? "found" : last == NOT_FOUND_LAST_LINE ? "notFound" : "searching";
        List<String> lines = new ArrayList<>(
            Arrays.asList(translate("status"), translate(status), "", translate("biome"), "Plains"));
        if (last == NOT_FOUND_LAST_LINE) {
            lines.addAll(Arrays.asList("", translate("radius"), "2500"));
        } else if (last == FOUND_LAST_LINE) {
            lines.addAll(Arrays.asList("", translate("coordinates"), "120, -340", "", translate("distance"), "360"));
        }
        return lines;
    }

    private static String translate(String key) {
        return StatCollector.translateToLocal(LANG + key);
    }
}
