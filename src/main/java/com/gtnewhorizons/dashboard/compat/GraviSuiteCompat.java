package com.gtnewhorizons.dashboard.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The GraviSuite jetpack / chestplate status text: one or two lines in the corner picked by its {@code hudPosition}
 * config (1 top left, 2 top right, 3 bottom left, 4 bottom right).
 */
@SideOnly(Side.CLIENT)
public final class GraviSuiteCompat {

    /** Wide enough for the usual "Energy Level: 100%" and mode lines. The text itself can be a bit wider. */
    private static final int STATUS_WIDTH = 120;
    private static final int EDGE_MARGIN = 2;
    /** GraviSuite puts up to 3 pixels between the two lines. */
    private static final int LINE_GAP = 3;

    private static final int TOP_LEFT = 1;
    private static final int TOP_RIGHT = 2;
    private static final int BOTTOM_RIGHT = 4;

    public static final HudElement STATUS = new HudElement(
        "gravisuite:status",
        "dashboard.element.gravisuite.status",
        GraviSuiteCompat::getStatusBounds).setOwnSettings("dashboard.element.gravisuite.status.own_settings");

    private static Fields.ClassFields<Object>.Field<Integer> hudPosition;

    private GraviSuiteCompat() {}

    public static void register() {
        try {
            hudPosition = ModFields.intField(ModFields.of("gravisuite.GraviSuite"), "hudPos");
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not read the GraviSuite HUD position, assuming top left", e);
        }
        PreviewDrawing.setImitation(STATUS, GraviSuiteCompat::drawStatusPreview);
        HudRegistry.register(STATUS);
    }

    /** Right aligned in the right corners, like the real text. */
    private static void drawStatusPreview(int screenWidth, int screenHeight) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        HudBounds bounds = getStatusBounds(screenWidth, screenHeight);
        boolean right = bounds.x > screenWidth / 2;
        String[] lines = { StatCollector.translateToLocal("dashboard.preview.gravisuite.mode"),
            StatCollector.translateToLocal("dashboard.preview.gravisuite.energy") };
        int y = bounds.y;
        for (String line : lines) {
            int x = right ? bounds.getRight() - font.getStringWidth(line) : bounds.x;
            font.drawStringWithShadow(line, x, y, 0xFFFFFF);
            y += font.FONT_HEIGHT + LINE_GAP;
        }
    }

    private static HudBounds getStatusBounds(int screenWidth, int screenHeight) {
        int height = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT * 2 + LINE_GAP;
        int corner = ModFields.readInt(hudPosition, TOP_LEFT);
        boolean right = corner == TOP_RIGHT || corner == BOTTOM_RIGHT;
        boolean top = corner == TOP_LEFT || corner == TOP_RIGHT;
        int x = right ? screenWidth - STATUS_WIDTH - EDGE_MARGIN : EDGE_MARGIN;
        int y = top ? EDGE_MARGIN : screenHeight - height - EDGE_MARGIN;
        return new HudBounds(x, y, STATUS_WIDTH, height);
    }
}
