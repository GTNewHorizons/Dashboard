package com.gtnewhorizon.dashboard.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.dashboard.api.HudElement;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Lists the bars, with a button per bar that cycles through themes. */
@SideOnly(Side.CLIENT)
public class GuiBarCustomize extends GuiScreen {

    private static final int BUTTON_DONE = 0;
    private static final int BUTTON_ALL = 1;
    private static final int BUTTON_RESET_FLIPS = 2;
    private static final int BUTTON_FIRST_BAR = 3;
    private static final int BUTTON_FIRST_FLIP = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_HEIGHT = 21;
    private static final int THEME_BUTTON_WIDTH = 120;
    private static final int FLIP_BUTTON_WIDTH = 20;
    private static final int COLUMN_GAP = 6;
    private static final int TITLE_Y = 12;
    private static final int LIST_TOP = 28;
    private static final int ALL_ROW_GAP = 4;

    private final GuiScreen parent;
    private final List<HudElement> bars = new ArrayList<>();
    private final List<BarSlot> slots = new ArrayList<>();
    private final List<GuiButton> barButtons = new ArrayList<>();
    private final List<GuiButton> flipButtons = new ArrayList<>();
    private GuiButton allButton;
    private GuiButton resetFlipsButton;
    private int nameRight;
    private int previewX;

    public GuiBarCustomize(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        bars.clear();
        slots.clear();
        barButtons.clear();
        flipButtons.clear();
        for (Map.Entry<HudElement, BarSlot> entry : VanillaHudElements.getThemedBars()
            .entrySet()) {
            bars.add(entry.getKey());
            slots.add(entry.getValue());
        }

        // Name, button and preview columns
        int nameWidth = fontRendererObj.getStringWidth(getAllBarsName());
        int previewWidth = 0;
        for (int i = 0; i < bars.size(); i++) {
            nameWidth = Math.max(
                nameWidth,
                fontRendererObj.getStringWidth(
                    bars.get(i)
                        .getDisplayName()));
            previewWidth = Math.max(previewWidth, slots.get(i).width);
        }
        int totalWidth = nameWidth + COLUMN_GAP
            + THEME_BUTTON_WIDTH
            + COLUMN_GAP
            + FLIP_BUTTON_WIDTH
            + COLUMN_GAP
            + previewWidth;
        nameRight = (width - totalWidth) / 2 + nameWidth;
        int buttonX = nameRight + COLUMN_GAP;
        int flipX = buttonX + THEME_BUTTON_WIDTH + COLUMN_GAP;
        previewX = flipX + FLIP_BUTTON_WIDTH + COLUMN_GAP;

        allButton = new GuiButton(BUTTON_ALL, buttonX, LIST_TOP, THEME_BUTTON_WIDTH, BUTTON_HEIGHT, "");
        buttonList.add(allButton);
        resetFlipsButton = new GuiButton(BUTTON_RESET_FLIPS, flipX, LIST_TOP, FLIP_BUTTON_WIDTH, BUTTON_HEIGHT, "R");
        buttonList.add(resetFlipsButton);
        for (int i = 0; i < bars.size(); i++) {
            GuiButton button = new GuiButton(
                BUTTON_FIRST_BAR + i,
                buttonX,
                getBarRowY(i),
                THEME_BUTTON_WIDTH,
                BUTTON_HEIGHT,
                "");
            barButtons.add(button);
            buttonList.add(button);

            GuiButton flip = new GuiButton(
                BUTTON_FIRST_FLIP + i,
                flipX,
                getBarRowY(i),
                FLIP_BUTTON_WIDTH,
                BUTTON_HEIGHT,
                "");
            flipButtons.add(flip);
            buttonList.add(flip);
        }

        String done = StatCollector.translateToLocal("dashboard.editor.button.done");
        int doneY = getBarRowY(bars.size()) + ALL_ROW_GAP;
        buttonList.add(new GuiButton(BUTTON_DONE, width / 2 - 50, doneY, 100, BUTTON_HEIGHT, done));
        updateButtonTexts();
    }

    private static int getBarRowY(int index) {
        return LIST_TOP + ROW_HEIGHT + ALL_ROW_GAP + index * ROW_HEIGHT;
    }

    private static String getAllBarsName() {
        return StatCollector.translateToLocal("dashboard.customize.all_bars");
    }

    private static String getThemeName(String theme) {
        return theme == null ? StatCollector.translateToLocal("dashboard.theme.vanilla")
            : BarThemes.getDisplayName(theme);
    }

    /** Vanilla, then each theme in order, then back to vanilla. */
    private static String getNextTheme(String theme) {
        int index = BarThemes.THEMES.indexOf(theme);
        return index + 1 < BarThemes.THEMES.size() ? BarThemes.THEMES.get(index + 1) : null;
    }

    private boolean allBarsMatch() {
        String first = HudLayout.getTheme(bars.get(0));
        for (HudElement bar : bars) {
            String theme = HudLayout.getTheme(bar);
            if (theme == null ? first != null : !theme.equals(first)) {
                return false;
            }
        }
        return true;
    }

    private void updateButtonTexts() {
        boolean anyFlipped = false;
        for (int i = 0; i < bars.size(); i++) {
            HudElement bar = bars.get(i);
            String theme = HudLayout.getTheme(bar);
            barButtons.get(i).displayString = getThemeName(theme);
            GuiButton flip = flipButtons.get(i);
            flip.displayString = BarThemes.fillsFromRight(bar, slots.get(i)) ? "<" : ">";
            flip.enabled = theme != null;
            anyFlipped |= HudLayout.isFlipped(bar);
        }
        resetFlipsButton.enabled = anyFlipped;
        allButton.displayString = allBarsMatch() ? getThemeName(HudLayout.getTheme(bars.get(0)))
            : StatCollector.translateToLocal("dashboard.customize.mixed");
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int textColor = ColorUtils.text.getColor();
        drawCenteredString(
            fontRendererObj,
            StatCollector.translateToLocal("dashboard.customize.title"),
            width / 2,
            TITLE_Y,
            textColor);

        drawRowName(getAllBarsName(), LIST_TOP, textColor);
        for (int i = 0; i < bars.size(); i++) {
            int rowY = getBarRowY(i);
            drawRowName(
                bars.get(i)
                    .getDisplayName(),
                rowY,
                textColor);
            drawPreview(i, rowY);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (isMouseOver(resetFlipsButton, mouseX, mouseY)) {
            drawHoveringText(
                Collections.singletonList(StatCollector.translateToLocal("dashboard.customize.reset_flips")),
                mouseX,
                mouseY,
                fontRendererObj);
        }
    }

    private static boolean isMouseOver(GuiButton button, int mouseX, int mouseY) {
        return mouseX >= button.xPosition && mouseY >= button.yPosition
            && mouseX < button.xPosition + button.width
            && mouseY < button.yPosition + button.height;
    }

    private void drawRowName(String name, int rowY, int color) {
        int y = rowY + (BUTTON_HEIGHT - fontRendererObj.FONT_HEIGHT) / 2 + 1;
        fontRendererObj.drawStringWithShadow(name, nameRight - fontRendererObj.getStringWidth(name), y, color);
    }

    /** The bar two thirds full in its picked theme, or its vanilla look. */
    private void drawPreview(int index, int rowY) {
        BarSlot slot = slots.get(index);
        int y = rowY + (BUTTON_HEIGHT - slot.height) / 2;
        String theme = HudLayout.getTheme(bars.get(index));
        if (theme == null) {
            VanillaHudElements.drawVanillaSample(slot, previewX, y);
        } else {
            HudElement bar = bars.get(index);
            BarThemes.drawBar(theme, slot, previewX, y, BarThemes.PREVIEW_FILL, BarThemes.fillsFromRight(bar, slot));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BUTTON_DONE) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id == BUTTON_ALL) {
            // Mixed bars start over from vanilla
            String current = allBarsMatch() ? HudLayout.getTheme(bars.get(0)) : null;
            String theme = getNextTheme(current);
            for (HudElement bar : bars) {
                HudLayout.setTheme(bar, theme);
            }
        } else if (button.id == BUTTON_RESET_FLIPS) {
            for (HudElement bar : bars) {
                HudLayout.setFlipped(bar, false);
            }
        } else if (button.id >= BUTTON_FIRST_FLIP) {
            HudElement bar = bars.get(button.id - BUTTON_FIRST_FLIP);
            HudLayout.setFlipped(bar, !HudLayout.isFlipped(bar));
        } else {
            HudElement bar = bars.get(button.id - BUTTON_FIRST_BAR);
            HudLayout.setTheme(bar, getNextTheme(HudLayout.getTheme(bar)));
        }
        updateButtonTexts();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    public void onGuiClosed() {
        HudLayout.save();
    }
}
