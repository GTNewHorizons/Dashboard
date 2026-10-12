package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Mouse;

import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;

import cpw.mods.fml.client.GuiScrollingList;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Lists the bars, with a button per bar that cycles through themes. */
@SideOnly(Side.CLIENT)
public class GuiBarCustomize extends GuiScreen {

    private static final int BUTTON_DONE = 0;
    private static final int BUTTON_ALL = 1;
    private static final int BUTTON_RESET_FLIPS = 2;
    private static final int BUTTON_STATES = 3;
    private static final int SCROLL_UP = 4;
    private static final int SCROLL_DOWN = 5;
    private static final int BUTTON_RESET = 6;
    private static final int BOTTOM_BUTTON_GAP = 4;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_HEIGHT = BarSlot.HEALTH.height + BarThemes.PADDING * 2 + 1;
    private static final int THEME_BUTTON_WIDTH = 120;
    private static final int FLIP_BUTTON_WIDTH = 20;
    private static final int BOTTOM_BUTTON_WIDTH = 100;
    private static final int COLUMN_GAP = 6;
    private static final int TITLE_Y = 8;
    private static final int ALL_ROW_Y = 20;
    private static final int LIST_TOP = 46;
    private static final int LIST_BOTTOM_MARGIN = 30;

    private static boolean showStates;

    private final GuiScreen parent;
    private final List<HudElement> bars = new ArrayList<>();
    private final List<BarSlot> slots = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final GuiButton scrollUp = new GuiButton(SCROLL_UP, 0, 0, "");
    private final GuiButton scrollDown = new GuiButton(SCROLL_DOWN, 0, 0, "");
    private final GuiButton themeButton = new GuiButton(0, 0, 0, THEME_BUTTON_WIDTH, BUTTON_HEIGHT, "");
    private final GuiButton flipButton = new GuiButton(0, 0, 0, FLIP_BUTTON_WIDTH, BUTTON_HEIGHT, "");
    private BarList list;
    private GuiButton allButton;
    private GuiButton resetFlipsButton;
    private GuiButton resetButton;
    private int nameWidth;
    private int contentWidth;

    public GuiBarCustomize(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        bars.clear();
        slots.clear();
        rows.clear();
        for (Map.Entry<HudElement, BarSlot> entry : VanillaHudElements.getThemedBars()
            .entrySet()) {
            int barIndex = bars.size();
            bars.add(entry.getKey());
            slots.add(entry.getValue());
            rows.add(
                new Row(
                    barIndex,
                    entry.getValue(),
                    entry.getKey()
                        .getDisplayName(),
                    false));
            if (showStates) {
                for (BarSlot state : entry.getValue()
                    .getStates()) {
                    rows.add(new Row(barIndex, state, getStateName(state), true));
                }
            }
        }

        // Name, button and preview columns
        nameWidth = fontRendererObj.getStringWidth(getAllBarsName());
        int previewWidth = 0;
        for (Row row : rows) {
            nameWidth = Math.max(nameWidth, fontRendererObj.getStringWidth(row.name));
            previewWidth = Math.max(previewWidth, row.slot.width);
        }
        contentWidth = nameWidth + COLUMN_GAP
            + THEME_BUTTON_WIDTH
            + COLUMN_GAP
            + FLIP_BUTTON_WIDTH
            + COLUMN_GAP
            + BarThemes.PADDING
            + previewWidth
            + BarThemes.PADDING;
        list = new BarList();
        list.registerScrollButtons(buttonList, SCROLL_UP, SCROLL_DOWN);

        int left = getContentLeft();
        allButton = new GuiButton(BUTTON_ALL, left + getButtonX(), ALL_ROW_Y, THEME_BUTTON_WIDTH, BUTTON_HEIGHT, "");
        buttonList.add(allButton);
        resetFlipsButton = new GuiButton(
            BUTTON_RESET_FLIPS,
            left + getFlipX(),
            ALL_ROW_Y,
            FLIP_BUTTON_WIDTH,
            BUTTON_HEIGHT,
            "R");
        buttonList.add(resetFlipsButton);

        // Show States, Reset and Done, centered
        int bottomY = height - LIST_BOTTOM_MARGIN + (LIST_BOTTOM_MARGIN - BUTTON_HEIGHT) / 2;
        int step = BOTTOM_BUTTON_WIDTH + BOTTOM_BUTTON_GAP;
        int bottomX = width / 2 - (3 * BOTTOM_BUTTON_WIDTH + 2 * BOTTOM_BUTTON_GAP) / 2;
        String states = StatCollector
            .translateToLocal(showStates ? "dashboard.customize.hide_states" : "dashboard.customize.show_states");
        buttonList.add(new GuiButton(BUTTON_STATES, bottomX, bottomY, BOTTOM_BUTTON_WIDTH, BUTTON_HEIGHT, states));
        String reset = StatCollector.translateToLocal("dashboard.customize.reset");
        resetButton = new GuiButton(BUTTON_RESET, bottomX + step, bottomY, BOTTOM_BUTTON_WIDTH, BUTTON_HEIGHT, reset);
        buttonList.add(resetButton);
        String done = StatCollector.translateToLocal("dashboard.editor.button.done");
        buttonList
            .add(new GuiButton(BUTTON_DONE, bottomX + 2 * step, bottomY, BOTTOM_BUTTON_WIDTH, BUTTON_HEIGHT, done));
        updateAllBarsRow();
    }

    /** Only changes after a click. */
    private void updateAllBarsRow() {
        allButton.displayString = allBarsMatch() ? getThemeName(HudLayout.getTheme(bars.get(0)))
            : StatCollector.translateToLocal("dashboard.customize.mixed");
        resetFlipsButton.enabled = anyBarFlipped();
        resetButton.enabled = resetFlipsButton.enabled || anyBarThemed();
    }

    private boolean anyBarThemed() {
        for (HudElement bar : bars) {
            if (HudLayout.getTheme(bar) != null) {
                return true;
            }
        }
        return false;
    }

    private int getContentLeft() {
        return (width - contentWidth) / 2;
    }

    private int getButtonX() {
        return nameWidth + COLUMN_GAP;
    }

    private int getFlipX() {
        return getButtonX() + THEME_BUTTON_WIDTH + COLUMN_GAP;
    }

    private int getPreviewX() {
        return getFlipX() + FLIP_BUTTON_WIDTH + COLUMN_GAP + BarThemes.PADDING;
    }

    private static String getAllBarsName() {
        return StatCollector.translateToLocal("dashboard.customize.all_bars");
    }

    private static String getStateName(BarSlot state) {
        return StatCollector.translateToLocal(
            "dashboard.customize.state." + state.name()
                .toLowerCase(Locale.ROOT));
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

    private boolean anyBarFlipped() {
        for (HudElement bar : bars) {
            if (HudLayout.isFlipped(bar)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        list.drawScreen(mouseX, mouseY, partialTicks);

        int textColor = ColorUtils.text.getColor();
        drawCenteredString(
            fontRendererObj,
            StatCollector.translateToLocal("dashboard.customize.title"),
            width / 2,
            TITLE_Y,
            textColor);
        drawRowName(getAllBarsName(), ALL_ROW_Y, textColor);
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
        int right = getContentLeft() + nameWidth;
        fontRendererObj.drawStringWithShadow(name, right - fontRendererObj.getStringWidth(name), y, color);
    }

    private void drawRow(Row row, int rowY, int mouseX, int mouseY) {
        HudElement bar = bars.get(row.barIndex);
        String theme = HudLayout.getTheme(bar);
        int left = getContentLeft();
        drawRowName(row.name, rowY, row.state ? ColorUtils.textState.getColor() : ColorUtils.text.getColor());

        if (!row.state) {
            placeRowButtons(row, rowY);
            themeButton.drawButton(mc, mouseX, mouseY);
            flipButton.drawButton(mc, mouseX, mouseY);
        }

        int x = left + getPreviewX();
        int y = rowY + (BUTTON_HEIGHT - row.slot.height) / 2;
        if (theme == null) {
            VanillaHudElements.drawVanillaSample(row.slot, x, y);
        } else {
            boolean fromRight = BarThemes.fillsFromRight(bar, slots.get(row.barIndex));
            BarThemes.drawSample(theme, row.slot, x, y, fromRight);
        }
        if (row.slot == BarSlot.EXPERIENCE) {
            HudBounds bounds = new HudBounds(x, y, row.slot.width, row.slot.height);
            VanillaHudElements.drawLevelNumber(bounds, VanillaHudElements.SAMPLE_LEVEL);
        }
    }

    private void placeRowButtons(Row row, int rowY) {
        HudElement bar = bars.get(row.barIndex);
        String theme = HudLayout.getTheme(bar);
        int left = getContentLeft();
        themeButton.xPosition = left + getButtonX();
        themeButton.yPosition = rowY;
        themeButton.displayString = getThemeName(theme);

        flipButton.xPosition = left + getFlipX();
        flipButton.yPosition = rowY;
        flipButton.displayString = BarThemes.fillsFromRight(bar, slots.get(row.barIndex)) ? "<" : ">";
        flipButton.enabled = theme != null;
    }

    private void clickRow(Row row, int rowY, int mouseX, int mouseY) {
        if (row.state) {
            return;
        }
        HudElement bar = bars.get(row.barIndex);
        placeRowButtons(row, rowY);
        if (themeButton.mousePressed(mc, mouseX, mouseY)) {
            themeButton.func_146113_a(mc.getSoundHandler());
            HudLayout.setTheme(bar, getNextTheme(HudLayout.getTheme(bar)));
        } else if (flipButton.mousePressed(mc, mouseX, mouseY)) {
            flipButton.func_146113_a(mc.getSoundHandler());
            HudLayout.setFlipped(bar, !HudLayout.isFlipped(bar));
        }
        updateAllBarsRow();
    }

    /** The Forge list misses wheel events. */
    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            list.actionPerformed(wheel > 0 ? scrollUp : scrollDown);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case BUTTON_DONE -> mc.displayGuiScreen(parent);
            case BUTTON_STATES -> {
                showStates = !showStates;
                initGui();
            }
            case BUTTON_ALL -> {
                // Mixed bars start over from vanilla
                String current = allBarsMatch() ? HudLayout.getTheme(bars.get(0)) : null;
                String theme = getNextTheme(current);
                for (HudElement bar : bars) {
                    HudLayout.setTheme(bar, theme);
                }
            }
            case BUTTON_RESET_FLIPS -> {
                for (HudElement bar : bars) {
                    HudLayout.setFlipped(bar, false);
                }
            }
            case BUTTON_RESET -> {
                for (HudElement bar : bars) {
                    HudLayout.setTheme(bar, null);
                    HudLayout.setFlipped(bar, false);
                }
            }
            default -> {}
        }
        updateAllBarsRow();
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

    private final class BarList extends GuiScrollingList {

        private BarList() {
            super(
                GuiBarCustomize.this.mc,
                GuiBarCustomize.this.width,
                GuiBarCustomize.this.height,
                LIST_TOP,
                GuiBarCustomize.this.height - LIST_BOTTOM_MARGIN,
                0,
                ROW_HEIGHT);
        }

        @Override
        protected int getSize() {
            return rows.size();
        }

        private final int[] rowYs = new int[rows.size()];

        @Override
        protected void elementClicked(int index, boolean doubleClick) {
            clickRow(rows.get(index), rowYs[index], mouseX, mouseY);
        }

        @Override
        protected boolean isSelected(int index) {
            return false;
        }

        @Override
        protected void drawBackground() {}

        @Override
        protected void drawGradientRect(int left, int top, int right, int bottom, int topColor, int bottomColor) {}

        @Override
        protected void drawSlot(int index, int right, int rowY, int slotHeight, Tessellator tessellator) {
            rowYs[index] = rowY;
            drawRow(rows.get(index), rowY, mouseX, mouseY);
        }
    }

    /** One line of the list: a bar, or one of its states. */
    private static final class Row {

        private final int barIndex;
        private final BarSlot slot;
        private final String name;
        private final boolean state;

        private Row(int barIndex, BarSlot slot, String name, boolean state) {
            this.barIndex = barIndex;
            this.slot = slot;
            this.name = name;
            this.state = state;
        }
    }
}
