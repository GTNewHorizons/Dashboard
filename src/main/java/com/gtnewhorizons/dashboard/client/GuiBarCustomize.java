package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Mouse;

import com.gtnewhorizon.gtnhlib.color.ColorResource;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizons.dashboard.DashboardConfig;
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
    private static final int BUTTON_CROSSHAIR_STYLE = 7;
    private static final int BUTTON_CROSSHAIR_SIZE = 8;
    private static final int BUTTON_ALL_NUMBERS = 9;
    private static final int BUTTON_ALL_NUMBER_STYLES = 10;
    private static final int BUTTON_ALL_NUMBER_DECIMALS = 11;
    private static final int SWATCH_SIZE = 20;
    private static final int BOTTOM_BUTTON_GAP = 4;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_HEIGHT = BarSlot.HEALTH.height + BarThemes.PADDING * 2 + 1;
    private static final int THEME_BUTTON_WIDTH = 120;
    private static final int FLIP_BUTTON_WIDTH = 20;
    private static final int NUMBER_BUTTON_WIDTH = 40;
    private static final int NUMBER_STYLE_BUTTON_WIDTH = 50;
    private static final int DECIMALS_BUTTON_WIDTH = 30;
    private static final int BOTTOM_BUTTON_WIDTH = 100;
    private static final int COLUMN_GAP = 6;
    private static final int TITLE_Y = 8;
    private static final int CROSSHAIR_ROW_Y = 20;
    private static final int ALL_ROW_Y = 46;
    private static final int LIST_TOP = 72;
    private static final int LIST_BOTTOM_MARGIN = 30;
    private static final int LABEL_MARGIN = 8;
    private static final int SCREEN_MARGIN = 10;
    private static final String ELLIPSIS = "...";

    private static boolean showStates;

    private final GuiScreen parent;
    private final List<HudElement> bars = new ArrayList<>();
    private final List<BarSlot> slots = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final List<HudElement> numbers = new ArrayList<>();
    private final GuiButton scrollUp = new GuiButton(SCROLL_UP, 0, 0, "");
    private final GuiButton scrollDown = new GuiButton(SCROLL_DOWN, 0, 0, "");
    private final GuiButton themeButton = new GuiButton(0, 0, 0, THEME_BUTTON_WIDTH, BUTTON_HEIGHT, "");
    private final GuiButton flipButton = new GuiButton(0, 0, 0, FLIP_BUTTON_WIDTH, BUTTON_HEIGHT, "");
    private final GuiButton numberButton = new GuiButton(0, 0, 0, NUMBER_BUTTON_WIDTH, BUTTON_HEIGHT, "");
    private final GuiButton numberStyleButton = new GuiButton(0, 0, 0, NUMBER_STYLE_BUTTON_WIDTH, BUTTON_HEIGHT, "");
    private final GuiButton decimalsButton = new GuiButton(0, 0, 0, DECIMALS_BUTTON_WIDTH, BUTTON_HEIGHT, "");
    private BarList list;
    private GuiButton allButton;
    private GuiButton resetFlipsButton;
    private GuiButton allNumbersButton;
    private GuiButton allNumberStylesButton;
    private GuiButton allDecimalsButton;
    private GuiButton resetButton;
    private GuiButton crosshairStyleButton;
    private GuiButton crosshairSizeButton;
    private int nameWidth;
    private int contentWidth;
    private boolean twoLines;
    private String allBarsLabel;
    private List<String> rowTooltip;

    public GuiBarCustomize(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        bars.clear();
        slots.clear();
        rows.clear();
        numbers.clear();
        for (Map.Entry<HudElement, BarSlot> entry : VanillaHudElements.getThemedBars()
            .entrySet()) {
            int barIndex = bars.size();
            bars.add(entry.getKey());
            slots.add(entry.getValue());
            HudElement number = BarNumbers.getNumber(entry.getKey());
            addRow(
                new Row(
                    barIndex,
                    entry.getValue(),
                    entry.getKey()
                        .getDisplayName(),
                    false,
                    number,
                    false));
            if (entry.getValue() == BarSlot.FOOD) {
                String saturation = StatCollector.translateToLocal("dashboard.customize.saturation");
                addRow(new Row(barIndex, BarSlot.FOOD, saturation, false, BarNumbers.getSaturationNumber(), true));
            }
            if (showStates) {
                for (BarSlot state : entry.getValue()
                    .getStates()) {
                    addRow(new Row(barIndex, state, getStateName(state), true, null, false));
                }
            }
        }

        // Name, button and preview columns
        nameWidth = Math
            .max(fontRendererObj.getStringWidth(getAllBarsName()), fontRendererObj.getStringWidth(getCrosshairName()));
        int previewWidth = 0;
        int numberSampleWidth = BarThemes.PADDING + fontRendererObj.getStringWidth("000 / 000");
        for (Row row : rows) {
            nameWidth = Math.max(nameWidth, fontRendererObj.getStringWidth(row.name));
            int rowWidth = row.number != null && !row.numberOnly ? row.slot.width + numberSampleWidth : row.slot.width;
            previewWidth = Math.max(previewWidth, rowWidth);
        }
        int themeWidth = THEME_BUTTON_WIDTH + COLUMN_GAP + FLIP_BUTTON_WIDTH;
        int numbersWidth = NUMBER_BUTTON_WIDTH + COLUMN_GAP
            + NUMBER_STYLE_BUTTON_WIDTH
            + COLUMN_GAP
            + DECIMALS_BUTTON_WIDTH
            + COLUMN_GAP
            + BarThemes.PADDING
            + previewWidth
            + BarThemes.PADDING;
        int oneLineWidth = nameWidth + COLUMN_GAP + themeWidth + COLUMN_GAP + numbersWidth;
        twoLines = oneLineWidth > width - SCREEN_MARGIN * 2;
        contentWidth = twoLines ? nameWidth + COLUMN_GAP + Math.max(themeWidth, numbersWidth) : oneLineWidth;
        list = new BarList();
        list.registerScrollButtons(buttonList, SCROLL_UP, SCROLL_DOWN);

        int left = getContentLeft();
        crosshairStyleButton = new GuiButton(
            BUTTON_CROSSHAIR_STYLE,
            left + getButtonX(),
            CROSSHAIR_ROW_Y,
            THEME_BUTTON_WIDTH,
            BUTTON_HEIGHT,
            "");
        buttonList.add(crosshairStyleButton);
        crosshairSizeButton = new GuiButton(
            BUTTON_CROSSHAIR_SIZE,
            left + getFlipX(),
            CROSSHAIR_ROW_Y,
            FLIP_BUTTON_WIDTH,
            BUTTON_HEIGHT,
            "");
        buttonList.add(crosshairSizeButton);
        updateCrosshairButtons();

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
        int allNumbersY = ALL_ROW_Y + getSecondLineOffset();
        allNumbersButton = new GuiButton(
            BUTTON_ALL_NUMBERS,
            left + getNumberX(),
            allNumbersY,
            NUMBER_BUTTON_WIDTH,
            BUTTON_HEIGHT,
            "");
        buttonList.add(allNumbersButton);
        allNumberStylesButton = new GuiButton(
            BUTTON_ALL_NUMBER_STYLES,
            left + getNumberStyleX(),
            allNumbersY,
            NUMBER_STYLE_BUTTON_WIDTH,
            BUTTON_HEIGHT,
            "");
        buttonList.add(allNumberStylesButton);
        allDecimalsButton = new GuiButton(
            BUTTON_ALL_NUMBER_DECIMALS,
            left + getDecimalsX(),
            allNumbersY,
            DECIMALS_BUTTON_WIDTH,
            BUTTON_HEIGHT,
            "");
        buttonList.add(allDecimalsButton);

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

    private void addRow(Row row) {
        rows.add(row);
        if (row.number != null) {
            numbers.add(row.number);
        }
    }

    /** Only changes after a click. */
    private void updateAllBarsRow() {
        String mixed = StatCollector.translateToLocal("dashboard.customize.mixed");
        allBarsLabel = allBarsMatch() ? getThemeName(HudLayout.getTheme(bars.get(0))) : mixed;
        allButton.displayString = fitToButton(allBarsLabel, allButton);
        resetFlipsButton.enabled = anyBarFlipped();

        int numbersOn = countNumbersOn();
        allNumbersButton.displayString = numbersOn == numbers.size() ? getNumberToggleName(true)
            : numbersOn == 0 ? getNumberToggleName(false) : mixed;
        NumberStyle style = getCommonNumberStyle();
        allNumberStylesButton.displayString = style != null ? getStyleSample(style) : mixed;
        allNumberStylesButton.enabled = numbersOn > 0;
        Integer decimals = getCommonDecimals();
        allDecimalsButton.displayString = decimals != null ? getDecimalsLabel(decimals) : mixed;
        allDecimalsButton.enabled = numbersOn > 0;

        resetButton.enabled = resetFlipsButton.enabled || anyBarThemed()
            || numbersOn > 0
            || style != NumberStyle.SLASH
            || anyDecimalsChanged();
    }

    /** Null if the numbers use different decimals. */
    private Integer getCommonDecimals() {
        int first = BarNumbers.getDecimals(numbers.get(0));
        for (HudElement number : numbers) {
            if (BarNumbers.getDecimals(number) != first) {
                return null;
            }
        }
        return first;
    }

    private boolean anyDecimalsChanged() {
        for (HudElement number : numbers) {
            if (HudLayout.getNumberDecimals(number) != null) {
                return true;
            }
        }
        return false;
    }

    /** Ctrl goes back to the number's default. */
    private static void stepDecimals(HudElement number, int decimals) {
        if (isCtrlKeyDown()) {
            HudLayout.setNumberDecimals(number, null);
        } else {
            BarNumbers.setDecimals(number, wrap(decimals + getClickStep(), BarNumbers.MAX_DECIMALS + 1));
        }
    }

    private void stepAllDecimals() {
        Integer common = getCommonDecimals();
        int decimals = common != null ? common : 0;
        for (HudElement number : numbers) {
            stepDecimals(number, decimals);
        }
    }

    private static String getDecimalsLabel(int decimals) {
        return switch (decimals) {
            case 0 -> "#";
            case 1 -> "#.#";
            default -> "#.##";
        };
    }

    /** Null decimals means mixed. */
    private static List<String> getDecimalsTooltip(Integer decimals) {
        String name = decimals != null
            ? StatCollector.translateToLocal("dashboard.customize.number_decimals." + decimals)
            : StatCollector.translateToLocal("dashboard.customize.mixed");
        return Arrays.asList(
            StatCollector.translateToLocal("dashboard.customize.number_decimals"),
            EnumChatFormatting.GRAY + name);
    }

    private int countNumbersOn() {
        int count = 0;
        for (HudElement number : numbers) {
            if (HudLayout.isNumberOn(number)) {
                count++;
            }
        }
        return count;
    }

    /** Null if the numbers use different styles. */
    private NumberStyle getCommonNumberStyle() {
        NumberStyle first = HudLayout.getNumberStyle(numbers.get(0));
        for (HudElement number : numbers) {
            if (HudLayout.getNumberStyle(number) != first) {
                return null;
            }
        }
        return first;
    }

    /** All on, unless all are on already. */
    private void toggleAllNumbers() {
        boolean on = !isCtrlKeyDown() && countNumbersOn() < numbers.size();
        for (HudElement number : numbers) {
            HudLayout.setNumberOn(number, on);
        }
    }

    private void stepAllNumberStyles() {
        NumberStyle common = getCommonNumberStyle();
        NumberStyle style = stepNumberStyle(common != null ? common : NumberStyle.SLASH);
        for (HudElement number : numbers) {
            HudLayout.setNumberStyle(number, style);
        }
    }

    /** Ctrl goes back to the first style. */
    private static NumberStyle stepNumberStyle(NumberStyle style) {
        if (isCtrlKeyDown()) {
            return NumberStyle.SLASH;
        }
        NumberStyle[] styles = NumberStyle.values();
        return styles[wrap(style.ordinal() + getClickStep(), styles.length)];
    }

    private static String getNumberToggleName(boolean on) {
        return StatCollector.translateToLocal(on ? "dashboard.customize.number.on" : "dashboard.customize.number.off");
    }

    private static String getStyleSample(NumberStyle style) {
        return style.format("13", "20", "65");
    }

    private void updateCrosshairButtons() {
        int style = DashboardConfig.crosshairStyle;
        crosshairStyleButton.displayString = style == Crosshairs.VANILLA
            ? StatCollector.translateToLocal("dashboard.theme.vanilla")
            : style + " / " + Crosshairs.STYLE_COUNT;
        crosshairSizeButton.displayString = "x" + DashboardConfig.crosshairSize;
    }

    /** Ctrl goes back to vanilla. */
    private void changeCrosshairStyle() {
        DashboardConfig.crosshairStyle = isCtrlKeyDown() ? Crosshairs.VANILLA
            : wrap(DashboardConfig.crosshairStyle + getClickStep(), Crosshairs.STYLE_COUNT + 1);
        ConfigurationManager.save(DashboardConfig.class);
        updateCrosshairButtons();
    }

    private void changeCrosshairSize() {
        DashboardConfig.crosshairSize = isCtrlKeyDown() ? 1
            : wrap(DashboardConfig.crosshairSize - 1 + getClickStep(), Crosshairs.MAX_SIZE) + 1;
        ConfigurationManager.save(DashboardConfig.class);
        updateCrosshairButtons();
    }

    /** Forward, or back with Shift. */
    private static int getClickStep() {
        return isShiftKeyDown() ? -1 : 1;
    }

    /** Wraps around in both directions. */
    private static int wrap(int value, int count) {
        return (value % count + count) % count;
    }

    private static String getCrosshairName() {
        return StatCollector.translateToLocal("dashboard.customize.crosshair");
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

    private int getNumberX() {
        return twoLines ? getButtonX() : getFlipX() + FLIP_BUTTON_WIDTH + COLUMN_GAP;
    }

    private int getSecondLineOffset() {
        return twoLines ? ROW_HEIGHT : 0;
    }

    private int getRowHeight() {
        return ROW_HEIGHT + getSecondLineOffset();
    }

    private int getListTop() {
        return LIST_TOP + getSecondLineOffset();
    }

    private int getNumberStyleX() {
        return getNumberX() + NUMBER_BUTTON_WIDTH + COLUMN_GAP;
    }

    private int getDecimalsX() {
        return getNumberStyleX() + NUMBER_STYLE_BUTTON_WIDTH + COLUMN_GAP;
    }

    private int getPreviewX() {
        return getDecimalsX() + DECIMALS_BUTTON_WIDTH + COLUMN_GAP + BarThemes.PADDING;
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

    /** Vanilla, then each theme in order, either way. Ctrl goes back to vanilla. */
    private static String stepTheme(String theme) {
        if (isCtrlKeyDown()) {
            return null;
        }
        List<String> themes = BarThemes.getThemes();
        int index = wrap(themes.indexOf(theme) + 1 + getClickStep(), themes.size() + 1);
        return index == 0 ? null : themes.get(index - 1);
    }

    private void stepAllBars() {
        // Mixed bars start over from vanilla
        String current = allBarsMatch() ? HudLayout.getTheme(bars.get(0)) : null;
        String theme = stepTheme(current);
        for (HudElement bar : bars) {
            HudLayout.setTheme(bar, theme);
        }
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
        rowTooltip = null;
        list.drawScreen(mouseX, mouseY, partialTicks);

        int textColor = ColorUtils.text.getColor();
        drawCenteredString(
            fontRendererObj,
            StatCollector.translateToLocal("dashboard.customize.title"),
            width / 2,
            TITLE_Y,
            textColor);
        drawRowName(getAllBarsName(), ALL_ROW_Y, textColor);
        drawRowName(getCrosshairName(), CROSSHAIR_ROW_Y, textColor);
        drawCrosshairPreview();
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (resetFlipsButton.func_146115_a()) {
            drawTooltip(StatCollector.translateToLocal("dashboard.customize.reset_flips"), mouseX, mouseY);
        } else if (crosshairSizeButton.func_146115_a()) {
            drawTooltip(StatCollector.translateToLocal("dashboard.customize.crosshair_size"), mouseX, mouseY);
        } else if (allButton.func_146115_a() && !allButton.displayString.equals(allBarsLabel)) {
            drawTooltip(allBarsLabel, mouseX, mouseY);
        } else if (allNumbersButton.func_146115_a()) {
            drawTooltip(StatCollector.translateToLocal("dashboard.customize.number"), mouseX, mouseY);
        } else if (allNumberStylesButton.func_146115_a()) {
            drawTooltip(getStyleTooltip(getCommonNumberStyle()), mouseX, mouseY);
        } else if (allDecimalsButton.func_146115_a()) {
            drawTooltip(getDecimalsTooltip(getCommonDecimals()), mouseX, mouseY);
        } else if (rowTooltip != null) {
            drawTooltip(rowTooltip, mouseX, mouseY);
        }
    }

    private void drawTooltip(String text, int mouseX, int mouseY) {
        drawTooltip(Collections.singletonList(text), mouseX, mouseY);
    }

    private void drawTooltip(List<String> lines, int mouseX, int mouseY) {
        drawHoveringText(lines, mouseX, mouseY, fontRendererObj);
    }

    /** Null style means mixed. */
    private static List<String> getStyleTooltip(NumberStyle style) {
        String name = style != null ? style.getDisplayName()
            : StatCollector.translateToLocal("dashboard.customize.mixed");
        return Arrays
            .asList(StatCollector.translateToLocal("dashboard.customize.number_style"), EnumChatFormatting.GRAY + name);
    }

    private String fitToButton(String text, GuiButton button) {
        int maxWidth = button.width - LABEL_MARGIN;
        if (fontRendererObj.getStringWidth(text) <= maxWidth) {
            return text;
        }
        return fontRendererObj.trimStringToWidth(text, maxWidth - fontRendererObj.getStringWidth(ELLIPSIS)) + ELLIPSIS;
    }

    /** On a few backgrounds, to show the inverting. Always at x1, since x5 would not fit the row. */
    private void drawCrosshairPreview() {
        ColorResource[] backgrounds = { ColorUtils.crosshairBackground1, ColorUtils.crosshairBackground2,
            ColorUtils.crosshairBackground3, ColorUtils.crosshairBackground4, ColorUtils.crosshairBackground5 };
        int x = getContentLeft() + getFlipX() + FLIP_BUTTON_WIDTH + COLUMN_GAP;
        int y = CROSSHAIR_ROW_Y;
        for (ColorResource background : backgrounds) {
            drawRect(x, y, x + SWATCH_SIZE, y + SWATCH_SIZE, background.getColor());
            Crosshairs.draw(DashboardConfig.crosshairStyle, 1, x + SWATCH_SIZE / 2, y + SWATCH_SIZE / 2);
            x += SWATCH_SIZE;
        }
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
        }
        if (!row.state && !row.numberOnly) {
            themeButton.drawButton(mc, mouseX, mouseY);
            flipButton.drawButton(mc, mouseX, mouseY);
            String themeName = getThemeName(theme);
            if (themeButton.func_146115_a() && !themeButton.displayString.equals(themeName)) {
                rowTooltip = Collections.singletonList(themeName);
            } else if (flipButton.func_146115_a()) {
                rowTooltip = Collections
                    .singletonList(StatCollector.translateToLocal("dashboard.customize.fill_direction"));
            }
        }
        if (row.number != null) {
            numberButton.drawButton(mc, mouseX, mouseY);
            numberStyleButton.drawButton(mc, mouseX, mouseY);
            decimalsButton.drawButton(mc, mouseX, mouseY);
            if (numberButton.func_146115_a()) {
                rowTooltip = Collections.singletonList(StatCollector.translateToLocal("dashboard.customize.number"));
            } else if (numberStyleButton.func_146115_a()) {
                rowTooltip = getStyleTooltip(HudLayout.getNumberStyle(row.number));
            } else if (decimalsButton.func_146115_a()) {
                rowTooltip = getDecimalsTooltip(BarNumbers.getDecimals(row.number));
            }
        }
        int x = left + getPreviewX();
        int lineY = rowY + getSecondLineOffset();
        if (row.numberOnly) {
            drawNumberSample(row.number, x, lineY);
            return;
        }
        if (row.number != null) {
            drawNumberSample(row.number, x + row.slot.width + BarThemes.PADDING, lineY);
        }

        int y = lineY + (BUTTON_HEIGHT - row.slot.height) / 2;
        if (theme == null) {
            VanillaHudElements.drawVanillaSample(row.slot, x, y);
        } else {
            boolean fromRight = BarThemes.fillsFromRight(bar, slots.get(row.barIndex));
            BarThemes.drawSample(theme, row.slot, x, y, fromRight);
        }
        if (row.slot == BarSlot.EXPERIENCE) {
            HudBounds bounds = new HudBounds(x, y - 6, row.slot.width, row.slot.height);
            VanillaHudElements.drawLevelNumber(bounds, VanillaHudElements.SAMPLE_LEVEL);
        }
    }

    private void drawNumberSample(HudElement number, int x, int rowY) {
        if (!HudLayout.isNumberOn(number)) {
            return;
        }
        int y = rowY + (BUTTON_HEIGHT - fontRendererObj.FONT_HEIGHT) / 2 + 1;
        fontRendererObj.drawStringWithShadow(BarNumbers.getSampleText(number), x, y, 0xFFFFFF);
    }

    private void placeRowButtons(Row row, int rowY) {
        HudElement bar = bars.get(row.barIndex);
        String theme = HudLayout.getTheme(bar);
        int left = getContentLeft();
        themeButton.xPosition = left + getButtonX();
        themeButton.yPosition = rowY;
        themeButton.displayString = fitToButton(getThemeName(theme), themeButton);

        flipButton.xPosition = left + getFlipX();
        flipButton.yPosition = rowY;
        flipButton.displayString = BarThemes.fillsFromRight(bar, slots.get(row.barIndex)) ? "<" : ">";
        flipButton.enabled = theme != null;

        if (row.number != null) {
            boolean on = HudLayout.isNumberOn(row.number);
            int lineY = rowY + getSecondLineOffset();
            numberButton.xPosition = left + getNumberX();
            numberButton.yPosition = lineY;
            numberButton.displayString = getNumberToggleName(on);
            numberStyleButton.xPosition = left + getNumberStyleX();
            numberStyleButton.yPosition = lineY;
            numberStyleButton.displayString = getStyleSample(HudLayout.getNumberStyle(row.number));
            numberStyleButton.enabled = on;
            decimalsButton.xPosition = left + getDecimalsX();
            decimalsButton.yPosition = lineY;
            decimalsButton.displayString = getDecimalsLabel(BarNumbers.getDecimals(row.number));
            decimalsButton.enabled = on;
        }
    }

    private void clickRow(Row row, int rowY, int mouseX, int mouseY) {
        if (row.state) {
            return;
        }
        HudElement bar = bars.get(row.barIndex);
        placeRowButtons(row, rowY);
        if (!row.numberOnly && themeButton.mousePressed(mc, mouseX, mouseY)) {
            themeButton.func_146113_a(mc.getSoundHandler());
            HudLayout.setTheme(bar, stepTheme(HudLayout.getTheme(bar)));
        } else if (!row.numberOnly && flipButton.mousePressed(mc, mouseX, mouseY)) {
            flipButton.func_146113_a(mc.getSoundHandler());
            HudLayout.setFlipped(bar, !isCtrlKeyDown() && !HudLayout.isFlipped(bar));
        } else if (row.number != null && numberButton.mousePressed(mc, mouseX, mouseY)) {
            numberButton.func_146113_a(mc.getSoundHandler());
            HudLayout.setNumberOn(row.number, !isCtrlKeyDown() && !HudLayout.isNumberOn(row.number));
        } else if (row.number != null && numberStyleButton.mousePressed(mc, mouseX, mouseY)) {
            numberStyleButton.func_146113_a(mc.getSoundHandler());
            HudLayout.setNumberStyle(row.number, stepNumberStyle(HudLayout.getNumberStyle(row.number)));
        } else if (row.number != null && decimalsButton.mousePressed(mc, mouseX, mouseY)) {
            decimalsButton.func_146113_a(mc.getSoundHandler());
            stepDecimals(row.number, BarNumbers.getDecimals(row.number));
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
            case BUTTON_ALL -> stepAllBars();
            case BUTTON_RESET_FLIPS -> {
                for (HudElement bar : bars) {
                    HudLayout.setFlipped(bar, false);
                }
            }
            case BUTTON_ALL_NUMBERS -> toggleAllNumbers();
            case BUTTON_ALL_NUMBER_STYLES -> stepAllNumberStyles();
            case BUTTON_ALL_NUMBER_DECIMALS -> stepAllDecimals();
            case BUTTON_CROSSHAIR_STYLE -> changeCrosshairStyle();
            case BUTTON_CROSSHAIR_SIZE -> changeCrosshairSize();
            case BUTTON_RESET -> {
                for (HudElement bar : bars) {
                    HudLayout.setTheme(bar, null);
                    HudLayout.setFlipped(bar, false);
                }
                for (HudElement number : numbers) {
                    HudLayout.setNumberOn(number, false);
                    HudLayout.setNumberStyle(number, NumberStyle.SLASH);
                    HudLayout.setNumberDecimals(number, null);
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
                GuiBarCustomize.this.getListTop(),
                GuiBarCustomize.this.height - LIST_BOTTOM_MARGIN,
                0,
                GuiBarCustomize.this.getRowHeight());
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

    /** One line of the list: a bar, one of its states, or a number without a bar (saturation). */
    private static final class Row {

        private final int barIndex;
        private final BarSlot slot;
        private final String name;
        private final boolean state;
        private final HudElement number;
        private final boolean numberOnly;

        private Row(int barIndex, BarSlot slot, String name, boolean state, HudElement number, boolean numberOnly) {
            this.barIndex = barIndex;
            this.slot = slot;
            this.name = name;
            this.state = state;
            this.number = number;
            this.numberOnly = numberOnly;
        }
    }
}
