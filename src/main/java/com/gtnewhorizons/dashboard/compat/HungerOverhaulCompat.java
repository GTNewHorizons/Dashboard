package com.gtnewhorizons.dashboard.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
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
 * Hunger Overhaul's status words, which it adds to the text lists Forge draws in the top corners: Hurt, Injured or
 * Dying on the left, Peckish, Hungry or Starving on the right. The mixin draws them here instead, in the row Forge
 * would have used, so they start where they always were.
 */
@SideOnly(Side.CLIENT)
public final class HungerOverhaulCompat {

    /** How Forge draws the lists. */
    private static final int EDGE_MARGIN = 2;
    private static final int RIGHT_MARGIN = 10;
    private static final int ROW_HEIGHT = 10;

    private static final StatusLine HEALTH = new StatusLine(
        "hungeroverhaul:health_status",
        false,
        "hungeroverhaul.injured");
    private static final StatusLine HUNGER = new StatusLine(
        "hungeroverhaul:hunger_status",
        true,
        "hungeroverhaul.hungry");

    private static Fields.ClassFields<Object>.Field<Boolean> addGuiText;

    private HungerOverhaulCompat() {}

    public static void register() {
        try {
            addGuiText = ModFields
                .booleanField(ModFields.of("iguanaman.hungeroverhaul.config.IguanaConfig"), "addGuiText");
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not read the Hunger Overhaul addGuiText setting, assuming it is on", e);
        }
        for (StatusLine line : new StatusLine[] { HEALTH, HUNGER }) {
            PreviewDrawing.setImitationInBounds(line.element, line::drawPreview);
            HudRegistry.registerFallback(line.element);
        }
    }

    /** Called by the mixin instead of adding the line to Forge's list, with the row it would have taken there. */
    public static void drawLine(boolean right, String line, int row) {
        (right ? HUNGER : HEALTH).draw(line, row);
    }

    private static boolean isGuiTextOn() {
        return addGuiText == null || addGuiText.getValue(null);
    }

    private static final class StatusLine {

        private final HudElement element;
        private final boolean right;
        private final String sampleKey;
        /** The last line and row drawn, so the box keeps its size and place while nothing shows. */
        private String text;
        private int row;

        StatusLine(String id, boolean right, String sampleKey) {
            this.element = new HudElement(id, this::getBounds).setActiveCheck(HungerOverhaulCompat::isGuiTextOn);
            this.right = right;
            this.sampleKey = sampleKey;
        }

        void draw(String line, int row) {
            this.text = line;
            this.row = row;
            ScaledResolution resolution = HudRegistry.getResolution();
            if (HudRegistry.beginRender(element, resolution)) {
                HudBounds bounds = getBounds(resolution.getScaledWidth(), resolution.getScaledHeight());
                Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(line, bounds.x, bounds.y, 0xFFFFFF);
                HudRegistry.endRender();
            }
        }

        void drawPreview(HudBounds bounds) {
            Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(getShownText(), bounds.x, bounds.y, 0xFFFFFF);
        }

        /** The last line drawn, or Hunger Overhaul's own yellow word before any showed. */
        private String getShownText() {
            return text != null ? text : EnumChatFormatting.YELLOW + StatCollector.translateToLocal(sampleKey);
        }

        HudBounds getBounds(int screenWidth, int screenHeight) {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            int width = font.getStringWidth(getShownText());
            int x = right ? screenWidth - width - RIGHT_MARGIN : EDGE_MARGIN;
            return new HudBounds(x, EDGE_MARGIN + row * ROW_HEIGHT, width, font.FONT_HEIGHT);
        }
    }
}
