package com.gtnewhorizons.dashboard.client;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Small pieces the editor screens share, so each screen does not keep its own copy. */
@SideOnly(Side.CLIENT)
final class GuiHelpers {

    static final int BUTTON_HEIGHT = 20;
    private static final String ELLIPSIS = "...";

    private GuiHelpers() {}

    static String getOnOff(boolean on) {
        return StatCollector.translateToLocal(on ? "dashboard.customize.number.on" : "dashboard.customize.number.off");
    }

    /** Forward, or back with Shift. */
    static int getClickStep() {
        return GuiScreen.isShiftKeyDown() ? -1 : 1;
    }

    /** Wraps around, also below zero, where Java's % would stay negative. */
    static int wrap(int value, int count) {
        return (value % count + count) % count;
    }

    /** Text centered in a line, like on a button. */
    static int getTextY(FontRenderer font, int lineY) {
        return lineY + (BUTTON_HEIGHT - font.FONT_HEIGHT) / 2 + 1;
    }

    /** Cut by pixel width with "..." at the end, since letters differ in width. */
    static String fitToWidth(FontRenderer font, String text, int maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) {
            return text;
        }
        return font.trimStringToWidth(text, maxWidth - font.getStringWidth(ELLIPSIS)) + ELLIPSIS;
    }

    static List<String> getTooltip(String langKey) {
        return Collections.singletonList(StatCollector.translateToLocal(langKey));
    }

    /** The full text as a tooltip, for text that had to be cut to fit. Null when nothing was cut. */
    static List<String> getCutTooltip(String shown, String full) {
        return shown.equals(full) ? null : Collections.singletonList(full);
    }

    /** For buttons that stay clickable while greyed out, since vanilla ignores clicks on disabled buttons. */
    static boolean isMouseOver(GuiButton button, int mouseX, int mouseY) {
        return button.visible && mouseX >= button.xPosition
            && mouseY >= button.yPosition
            && mouseX < button.xPosition + button.width
            && mouseY < button.yPosition + button.height;
    }
}
