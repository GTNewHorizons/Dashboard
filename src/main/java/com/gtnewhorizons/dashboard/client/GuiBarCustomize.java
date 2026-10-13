package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.color.ColorResource;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizons.dashboard.DashboardConfig;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.compat.Mods;

import cpw.mods.fml.client.GuiScrollingList;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The Customize screen: one scrolling list with a section per HUD feature, so every setting is in one place. */
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
    private static final int BUTTON_RESET_CROSSHAIR = 14;
    private static final int BUTTON_TINKERS_CROSSHAIR = 15;
    private static final int SWATCH_SIZE = 20;
    private static final int LINE_HEIGHT = BarSlot.HEALTH.height + BarThemes.PADDING * 2 + 1;
    private static final int THEME_BUTTON_WIDTH = 120;
    private static final int FLIP_BUTTON_WIDTH = 20;
    private static final int NUMBER_BUTTON_WIDTH = 40;
    private static final int NUMBER_STYLE_BUTTON_WIDTH = 52;
    private static final int DECIMALS_BUTTON_WIDTH = 40;
    private static final int SECTION_BUTTON_WIDTH = 80;
    private static final int BOTTOM_BUTTON_WIDTH = 100;
    private static final int COLUMN_GAP = 6;
    private static final int TITLE_Y = 8;
    private static final int LIST_TOP = 20;
    private static final int HEADER_OVERHANG = 6;
    private static final int LIST_BOTTOM_MARGIN = 30;
    private static final int LABEL_MARGIN = 8;
    private static final int SCREEN_MARGIN = 10;
    private static final int STATUS_COLUMN_WIDTH = 150;
    private static final int STATUS_COLUMN_GAP = 4;

    /** Forge's list scrolls while the mouse is held on a row, which would fight a dragged slider. */
    private static final Fields.ClassFields<GuiScrollingList>.Field<Float> LIST_DRAG_START = Fields
        .ofClass(GuiScrollingList.class)
        .getFloatField(Fields.LookupType.DECLARED, "initialMouseClickY");
    private static final float NOT_DRAGGING = -2;
    /** Kept when the screen rebuilds, so adding or removing a line does not jump back to the top. */
    private static final Fields.ClassFields<GuiScrollingList>.Field<Float> LIST_SCROLL = Fields
        .ofClass(GuiScrollingList.class)
        .getFloatField(Fields.LookupType.DECLARED, "scrollDistance");

    /** Light, dark, sky, grass and sand, to show how the crosshair inverts what is behind it. */
    private static final ColorResource[] CROSSHAIR_BACKGROUNDS = { ColorUtils.crosshairBackground1,
        ColorUtils.crosshairBackground2, ColorUtils.crosshairBackground3, ColorUtils.crosshairBackground4,
        ColorUtils.crosshairBackground5 };

    private static boolean showStates;

    private final GuiScreen parent;
    private final List<HudElement> bars = new ArrayList<>();
    private final List<BarSlot> slots = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final List<Line> lines = new ArrayList<>();
    private final List<HudElement> numbers = new ArrayList<>();
    private final GuiButton scrollUp = new GuiButton(SCROLL_UP, 0, 0, "");
    private final GuiButton scrollDown = new GuiButton(SCROLL_DOWN, 0, 0, "");
    private final GuiButton themeButton = new GuiButton(0, 0, 0, THEME_BUTTON_WIDTH, GuiHelpers.BUTTON_HEIGHT, "");
    private final IconButton flipButton = new IconButton(
        0,
        0,
        0,
        FLIP_BUTTON_WIDTH,
        GuiHelpers.BUTTON_HEIGHT,
        Icons.ARROW_RIGHT);
    private final GuiButton numberButton = new GuiButton(0, 0, 0, NUMBER_BUTTON_WIDTH, GuiHelpers.BUTTON_HEIGHT, "");
    private final GuiButton numberStyleButton = new GuiButton(
        0,
        0,
        0,
        NUMBER_STYLE_BUTTON_WIDTH,
        GuiHelpers.BUTTON_HEIGHT,
        "");
    private final GuiButton decimalsButton = new GuiButton(
        0,
        0,
        0,
        DECIMALS_BUTTON_WIDTH,
        GuiHelpers.BUTTON_HEIGHT,
        "");
    private BarList list;
    private GuiButton allButton;
    private GuiButton resetFlipsButton;
    private GuiButton allNumbersButton;
    private GuiButton allNumberStylesButton;
    private GuiButton allDecimalsButton;
    private GuiButton resetButton;
    private GuiButton crosshairStyleButton;
    private GuiButton crosshairSizeButton;
    private GuiButton resetCrosshairButton;
    private GuiButton tinkersCrosshairButton;
    private StatusEffectsSection statusEffects;
    private ScreenOverlaysSection screenOverlays;
    private HotbarSection hotbar;
    private RenderingSection rendering;
    private final List<CustomizeSection> sections = new ArrayList<>();
    private int nameWidth;
    private int contentWidth;
    private int crosshairLeft;
    private int previewLines;
    private boolean twoLines;
    private boolean rebuildNeeded;
    private String allBarsLabel;
    private List<String> rowTooltip;
    private String title;

    public GuiBarCustomize(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        // A rebuild makes a new section, which would forget unsaved slider changes
        for (CustomizeSection section : sections) {
            section.save();
        }
        sections.clear();
        float scroll = list != null ? LIST_SCROLL.getValue(list) : 0;
        String renderingText = rendering != null ? rendering.getText() : "";
        boolean renderingFocused = rendering != null && rendering.isFocused();
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        title = StatCollector.translateToLocal("dashboard.customize.title");
        bars.clear();
        slots.clear();
        rows.clear();
        lines.clear();
        numbers.clear();
        int stateNameWidth = 0;
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
            for (BarSlot state : entry.getValue()
                .getStates()) {
                stateNameWidth = Math.max(stateNameWidth, fontRendererObj.getStringWidth(getStateName(state)));
                if (showStates) {
                    addRow(new Row(barIndex, state, getStateName(state), true, null, false));
                }
            }
        }

        // Measured over every row, so the columns line up
        nameWidth = Math.max(stateNameWidth, fontRendererObj.getStringWidth(getAllBarsName()));
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

        // Buttons inside the list get their height while the list draws
        int left = getContentLeft();
        String states = StatCollector
            .translateToLocal(showStates ? "dashboard.customize.hide_states" : "dashboard.customize.show_states");
        GuiButton statesButton = new GuiButton(
            BUTTON_STATES,
            0,
            0,
            SECTION_BUTTON_WIDTH,
            GuiHelpers.BUTTON_HEIGHT,
            states);
        String reset = StatCollector.translateToLocal("dashboard.customize.reset");
        resetButton = new GuiButton(BUTTON_RESET, 0, 0, SECTION_BUTTON_WIDTH, GuiHelpers.BUTTON_HEIGHT, reset);
        allButton = new GuiButton(BUTTON_ALL, left + getButtonX(), 0, THEME_BUTTON_WIDTH, GuiHelpers.BUTTON_HEIGHT, "");
        resetFlipsButton = new IconButton(
            BUTTON_RESET_FLIPS,
            left + getFlipX(),
            0,
            FLIP_BUTTON_WIDTH,
            GuiHelpers.BUTTON_HEIGHT,
            Icons.RESET);
        allNumbersButton = new GuiButton(
            BUTTON_ALL_NUMBERS,
            left + getNumberX(),
            0,
            NUMBER_BUTTON_WIDTH,
            GuiHelpers.BUTTON_HEIGHT,
            "");
        allNumberStylesButton = new GuiButton(
            BUTTON_ALL_NUMBER_STYLES,
            left + getNumberStyleX(),
            0,
            NUMBER_STYLE_BUTTON_WIDTH,
            GuiHelpers.BUTTON_HEIGHT,
            "");
        allDecimalsButton = new GuiButton(
            BUTTON_ALL_NUMBER_DECIMALS,
            left + getDecimalsX(),
            0,
            DECIMALS_BUTTON_WIDTH,
            GuiHelpers.BUTTON_HEIGHT,
            "");
        resetCrosshairButton = new GuiButton(
            BUTTON_RESET_CROSSHAIR,
            0,
            0,
            SECTION_BUTTON_WIDTH,
            GuiHelpers.BUTTON_HEIGHT,
            reset);
        int crosshairNameWidth = fontRendererObj.getStringWidth(getCrosshairStyleName());
        int crosshairWidth = crosshairNameWidth + COLUMN_GAP
            + THEME_BUTTON_WIDTH
            + COLUMN_GAP
            + FLIP_BUTTON_WIDTH
            + COLUMN_GAP
            + CROSSHAIR_BACKGROUNDS.length * SWATCH_SIZE;
        crosshairLeft = width / 2 - crosshairWidth / 2;
        crosshairStyleButton = new GuiButton(
            BUTTON_CROSSHAIR_STYLE,
            crosshairLeft + crosshairNameWidth + COLUMN_GAP,
            0,
            THEME_BUTTON_WIDTH,
            GuiHelpers.BUTTON_HEIGHT,
            "");
        crosshairSizeButton = new GuiButton(
            BUTTON_CROSSHAIR_SIZE,
            crosshairStyleButton.xPosition + THEME_BUTTON_WIDTH + COLUMN_GAP,
            0,
            FLIP_BUTTON_WIDTH,
            GuiHelpers.BUTTON_HEIGHT,
            "");
        int columnWidth = Math.min(STATUS_COLUMN_WIDTH, (contentWidth - STATUS_COLUMN_GAP) / 2);
        tinkersCrosshairButton = Mods.TINKERS.isModLoaded()
            ? new GuiButton(BUTTON_TINKERS_CROSSHAIR, 0, 0, columnWidth, GuiHelpers.BUTTON_HEIGHT, "")
            : null;
        updateCrosshairButtons();

        lines.add(new HeaderLine(StatCollector.translateToLocal("dashboard.customize.section.bars")));
        lines.add(new Line(null));
        if (twoLines) {
            lines.add(new Line(getAllBarsName(), allButton, resetFlipsButton));
            lines.add(new Line(null, allNumbersButton, allNumberStylesButton, allDecimalsButton));
        } else {
            lines.add(
                new Line(
                    getAllBarsName(),
                    allButton,
                    resetFlipsButton,
                    allNumbersButton,
                    allNumberStylesButton,
                    allDecimalsButton));
        }
        lines.add(new Line(null));
        for (Row row : rows) {
            if (twoLines && row.number != null && !row.numberOnly) {
                lines.add(new BarLine(row, true, false));
                lines.add(new BarLine(row, false, true));
            } else {
                lines.add(new BarLine(row, true, true));
            }
        }
        lines.add(new CenteredLine(statesButton, resetButton));
        lines.add(new Line(null));

        lines.add(new HeaderLine(getCrosshairName()));
        lines.add(new Line(null));
        lines.add(new CrosshairLine());
        if (tinkersCrosshairButton != null) {
            lines.add(new CenteredLine(tinkersCrosshairButton));
        }
        lines.add(new CenteredLine(resetCrosshairButton));
        lines.add(new Line(null));

        int leftColumnX = width / 2 - columnWidth - STATUS_COLUMN_GAP / 2;
        int rightColumnX = width / 2 + STATUS_COLUMN_GAP / 2;
        hotbar = new HotbarSection(leftColumnX, rightColumnX, columnWidth, SECTION_BUTTON_WIDTH);
        lines.add(new HeaderLine(StatCollector.translateToLocal("dashboard.customize.hotbar")));
        lines.add(new Line(null));
        for (GuiButton[] row : hotbar.getRows()) {
            lines.add(new Line(null, row));
        }
        lines.add(new HotbarPreviewLine());
        lines.add(new CenteredLine(hotbar.getResetButton()));
        lines.add(new Line(null));

        statusEffects = new StatusEffectsSection(leftColumnX, rightColumnX, columnWidth, SECTION_BUTTON_WIDTH);
        lines.add(new HeaderLine(StatCollector.translateToLocal("dashboard.customize.status_effects")));
        lines.add(new Line(null));
        for (GuiButton[] row : statusEffects.getRows()) {
            lines.add(new Line(null, row));
        }
        previewLines = (StatusEffects.getMaxSamplesHeight() + LINE_HEIGHT - 1) / LINE_HEIGHT;
        for (int i = 0; i < previewLines; i++) {
            lines.add(new StatusEffectsPreviewLine(i));
        }
        lines.add(new CenteredLine(statusEffects.getResetButton()));
        lines.add(new Line(null));

        screenOverlays = new ScreenOverlaysSection(width / 2, contentWidth, SECTION_BUTTON_WIDTH);
        lines.add(new HeaderLine(StatCollector.translateToLocal("dashboard.customize.screen_overlays")));
        lines.add(new Line(null));
        lines.add(new ColumnTitlesLine());
        for (GuiButton[] row : screenOverlays.getRows()) {
            lines.add(new Line(null, row));
        }
        lines.add(new CenteredLine(screenOverlays.getResetButton()));
        lines.add(new Line(null));

        rendering = new RenderingSection(
            fontRendererObj,
            width / 2,
            contentWidth,
            SECTION_BUTTON_WIDTH,
            renderingText,
            renderingFocused);
        sections.addAll(Arrays.asList(hotbar, statusEffects, screenOverlays, rendering));
        lines.add(new HeaderLine(StatCollector.translateToLocal("dashboard.customize.rendering")));
        lines.add(new Line(null));
        lines.add(new TextLine(StatCollector.translateToLocal("dashboard.rendering.hidden_held_items")));
        lines.add(new InputLine());
        for (int i = 0; i < rendering.getRemoveButtons().length; i++) {
            lines.add(new EntryLine(i));
        }
        lines.add(new CenteredLine(rendering.getResetButton()));
        lines.add(new Line(null));

        list = new BarList();
        list.registerScrollButtons(buttonList, SCROLL_UP, SCROLL_DOWN);
        LIST_SCROLL.setValue(list, scroll);

        int bottomY = height - LIST_BOTTOM_MARGIN + (LIST_BOTTOM_MARGIN - GuiHelpers.BUTTON_HEIGHT) / 2;
        String done = StatCollector.translateToLocal("dashboard.editor.button.done");
        buttonList.add(
            new GuiButton(
                BUTTON_DONE,
                width / 2 - BOTTOM_BUTTON_WIDTH / 2,
                bottomY,
                BOTTOM_BUTTON_WIDTH,
                GuiHelpers.BUTTON_HEIGHT,
                done));
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
        allNumbersButton.displayString = numbersOn == numbers.size() ? GuiHelpers.getOnOff(true)
            : numbersOn == 0 ? GuiHelpers.getOnOff(false) : mixed;
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
            BarNumbers.setDecimals(
                number,
                GuiHelpers.wrap(decimals + GuiHelpers.getClickStep(), BarNumbers.MAX_DECIMALS + 1));
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

    private static List<String> getDecimalsTooltip(Integer decimals) {
        String name = decimals != null
            ? StatCollector.translateToLocal("dashboard.customize.number_decimals." + decimals)
            : null;
        return getValueTooltip("dashboard.customize.number_decimals", name);
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
        return styles[GuiHelpers.wrap(style.ordinal() + GuiHelpers.getClickStep(), styles.length)];
    }

    private static String getStyleSample(NumberStyle style) {
        return style.format("13", "20", "65");
    }

    private void updateCrosshairButtons() {
        int style = Crosshairs.getStyle();
        crosshairStyleButton.displayString = style == Crosshairs.VANILLA ? getThemeName(null)
            : style + " / " + Crosshairs.getStyleCount();
        crosshairSizeButton.displayString = "x" + DashboardConfig.crosshairSize;
        resetCrosshairButton.enabled = style != Crosshairs.VANILLA || DashboardConfig.crosshairSize != 1
            || tinkersCrosshairButton != null && !DashboardConfig.tinkersCrosshair;
        if (tinkersCrosshairButton != null) {
            tinkersCrosshairButton.displayString = StatCollector.translateToLocal(
                "dashboard.customize.tinkers_crosshair") + ": " + GuiHelpers.getOnOff(DashboardConfig.tinkersCrosshair);
        }
    }

    /** Ctrl goes back to vanilla. */
    private void changeCrosshairStyle() {
        DashboardConfig.crosshairStyle = isCtrlKeyDown() ? Crosshairs.VANILLA
            : GuiHelpers.wrap(Crosshairs.getStyle() + GuiHelpers.getClickStep(), Crosshairs.getStyleCount() + 1);
        ConfigurationManager.save(DashboardConfig.class);
        updateCrosshairButtons();
    }

    private void changeCrosshairSize() {
        DashboardConfig.crosshairSize = isCtrlKeyDown() ? 1
            : GuiHelpers.wrap(DashboardConfig.crosshairSize - 1 + GuiHelpers.getClickStep(), Crosshairs.MAX_SIZE) + 1;
        ConfigurationManager.save(DashboardConfig.class);
        updateCrosshairButtons();
    }

    private static String getCrosshairName() {
        return StatCollector.translateToLocal("dashboard.customize.crosshair");
    }

    private static String getCrosshairStyleName() {
        return StatCollector.translateToLocal("dashboard.customize.crosshair_style");
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
        int index = GuiHelpers.wrap(themes.indexOf(theme) + 1 + GuiHelpers.getClickStep(), themes.size() + 1);
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
        // Rebuilt here, since a click inside the list happens while the list is drawing
        if (rebuildNeeded) {
            rebuildNeeded = false;
            initGui();
        }
        if (screenOverlays.isPreviewing()) {
            screenOverlays.drawPreview(mc, width, height);
        } else {
            drawDefaultBackground();
        }
        rowTooltip = null;
        rendering.startFrame();
        // Checked every frame, since the Forge list can swallow the mouse release event
        if (isDraggingSlider()) {
            if (!Mouse.isButtonDown(0)) {
                for (CustomizeSection section : sections) {
                    section.releaseSliders(mouseX, mouseY);
                }
            } else {
                LIST_DRAG_START.setValue(list, NOT_DRAGGING);
            }
        }
        list.drawScreen(mouseX, mouseY, partialTicks);

        drawCenteredString(fontRendererObj, title, width / 2, TITLE_Y, ColorUtils.text.getColor());
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (rowTooltip != null) {
            drawHoveringText(rowTooltip, mouseX, mouseY, fontRendererObj);
        }
    }

    /** Null for buttons without a tooltip. */
    private List<String> getButtonTooltip(GuiButton button) {
        return switch (button.id) {
            case BUTTON_RESET_FLIPS -> GuiHelpers.getTooltip("dashboard.customize.reset_flips");
            case BUTTON_ALL -> GuiHelpers.getCutTooltip(button.displayString, allBarsLabel);
            case BUTTON_ALL_NUMBERS -> GuiHelpers.getTooltip("dashboard.customize.number");
            case BUTTON_ALL_NUMBER_STYLES -> getStyleTooltip(getCommonNumberStyle());
            case BUTTON_ALL_NUMBER_DECIMALS -> getDecimalsTooltip(getCommonDecimals());
            case BUTTON_CROSSHAIR_SIZE -> GuiHelpers.getTooltip("dashboard.customize.crosshair_size");
            case BUTTON_TINKERS_CROSSHAIR -> Arrays.asList(
                StatCollector.translateToLocal("dashboard.customize.tinkers_crosshair.on"),
                StatCollector.translateToLocal("dashboard.customize.tinkers_crosshair.off"));
            default -> getSectionTooltip(button);
        };
    }

    private List<String> getSectionTooltip(GuiButton button) {
        for (CustomizeSection section : sections) {
            List<String> tooltip = section.getTooltip(button);
            if (tooltip != null) {
                return tooltip;
            }
        }
        return null;
    }

    private boolean isDraggingSlider() {
        for (CustomizeSection section : sections) {
            if (section.isDragging()) {
                return true;
            }
        }
        return false;
    }

    private static List<String> getStyleTooltip(NumberStyle style) {
        return getValueTooltip("dashboard.customize.number_style", style != null ? style.getDisplayName() : null);
    }

    private static List<String> getValueTooltip(String titleKey, String value) {
        String shown = value != null ? value : StatCollector.translateToLocal("dashboard.customize.mixed");
        return Arrays.asList(StatCollector.translateToLocal(titleKey), EnumChatFormatting.GRAY + shown);
    }

    private String fitToButton(String text, GuiButton button) {
        return GuiHelpers.fitToWidth(fontRendererObj, text, button.width - LABEL_MARGIN);
    }

    /** On a few backgrounds, to show the inverting. Always at x1, since x5 would not fit the row. */
    private void drawCrosshairPreview(int y) {
        int x = crosshairSizeButton.xPosition + FLIP_BUTTON_WIDTH + COLUMN_GAP;
        for (ColorResource background : CROSSHAIR_BACKGROUNDS) {
            drawRect(x, y, x + SWATCH_SIZE, y + SWATCH_SIZE, background.getColor());
            Crosshairs.draw(Crosshairs.getStyle(), 1, x + SWATCH_SIZE / 2, y + SWATCH_SIZE / 2);
            x += SWATCH_SIZE;
        }
    }

    private void drawRowName(String name, int rowY, int color) {
        int y = GuiHelpers.getTextY(fontRendererObj, rowY);
        int right = getContentLeft() + nameWidth;
        fontRendererObj.drawStringWithShadow(name, right - fontRendererObj.getStringWidth(name), y, color);
    }

    private void drawNumberSample(HudElement number, int x, int rowY) {
        if (!HudLayout.isNumberOn(number)) {
            return;
        }
        int y = GuiHelpers.getTextY(fontRendererObj, rowY);
        fontRendererObj.drawStringWithShadow(BarNumbers.getSampleText(number), x, y, 0xFFFFFF);
    }

    /** Theme and flip on a bar's first line, the number buttons on its last. States get none. */
    private GuiButton[] getRowButtons(Row row, boolean first, boolean last) {
        List<GuiButton> rowButtons = new ArrayList<>();
        if (first && !row.state && !row.numberOnly) {
            rowButtons.add(themeButton);
            rowButtons.add(flipButton);
        }
        if (last && row.number != null) {
            rowButtons.add(numberButton);
            rowButtons.add(numberStyleButton);
            rowButtons.add(decimalsButton);
        }
        return rowButtons.toArray(new GuiButton[0]);
    }

    private void placeRowButtons(Row row, int y) {
        HudElement bar = bars.get(row.barIndex);
        String theme = HudLayout.getTheme(bar);
        int left = getContentLeft();
        themeButton.xPosition = left + getButtonX();
        themeButton.yPosition = y;
        themeButton.displayString = fitToButton(getThemeName(theme), themeButton);

        flipButton.xPosition = left + getFlipX();
        flipButton.yPosition = y;
        flipButton.icon = BarThemes.fillsFromRight(bar, slots.get(row.barIndex)) ? Icons.ARROW_LEFT : Icons.ARROW_RIGHT;
        flipButton.enabled = theme != null;

        if (row.number != null) {
            boolean on = HudLayout.isNumberOn(row.number);
            numberButton.xPosition = left + getNumberX();
            numberButton.yPosition = y;
            numberButton.displayString = GuiHelpers.getOnOff(on);
            numberStyleButton.xPosition = left + getNumberStyleX();
            numberStyleButton.yPosition = y;
            numberStyleButton.displayString = getStyleSample(HudLayout.getNumberStyle(row.number));
            numberStyleButton.enabled = on;
            decimalsButton.xPosition = left + getDecimalsX();
            decimalsButton.yPosition = y;
            decimalsButton.displayString = getDecimalsLabel(BarNumbers.getDecimals(row.number));
            decimalsButton.enabled = on;
        }
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
                rebuildNeeded = true;
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
            case BUTTON_RESET_CROSSHAIR -> {
                DashboardConfig.crosshairStyle = Crosshairs.VANILLA;
                DashboardConfig.crosshairSize = 1;
                DashboardConfig.tinkersCrosshair = true;
                ConfigurationManager.save(DashboardConfig.class);
                updateCrosshairButtons();
            }
            case BUTTON_TINKERS_CROSSHAIR -> {
                DashboardConfig.tinkersCrosshair = !isCtrlKeyDown() && !DashboardConfig.tinkersCrosshair;
                ConfigurationManager.save(DashboardConfig.class);
                updateCrosshairButtons();
            }
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
            default -> {
                for (CustomizeSection section : sections) {
                    if (section.actionPerformed(button)) {
                        break;
                    }
                }
            }
        }
        if (rendering.takeListChanged()) {
            rebuildNeeded = true;
        }
        updateAllBarsRow();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
        } else if (rendering.keyTyped(typedChar, keyCode) && rendering.takeListChanged()) {
            rebuildNeeded = true;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        rendering.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        rendering.updateCursor();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        HudLayout.save();
        for (CustomizeSection section : sections) {
            section.save();
        }
    }

    private final class BarList extends GuiScrollingList {

        private final int[] lineYs = new int[lines.size()];

        private BarList() {
            super(
                GuiBarCustomize.this.mc,
                GuiBarCustomize.this.width,
                GuiBarCustomize.this.height,
                LIST_TOP,
                GuiBarCustomize.this.height - LIST_BOTTOM_MARGIN,
                0,
                LINE_HEIGHT);
        }

        @Override
        protected int getSize() {
            return lines.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick) {
            lines.get(index)
                .click(lineYs[index], mouseX, mouseY);
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
        protected void drawSlot(int index, int right, int y, int slotHeight, Tessellator tessellator) {
            lineYs[index] = y;
            lines.get(index)
                .draw(y, mouseX, mouseY);
        }
    }

    /** Forge's list only has rows of one height, so every section is built from these lines. */
    private class Line {

        private final String name;
        private final GuiButton[] buttons;

        private Line(String name, GuiButton... buttons) {
            this.name = name;
            this.buttons = buttons;
        }

        protected void draw(int y, int mouseX, int mouseY) {
            if (name != null) {
                drawName(name, y);
            }
            for (GuiButton button : buttons) {
                button.yPosition = y;
                button.drawButton(mc, mouseX, mouseY);
                if (button.func_146115_a()) {
                    rowTooltip = getTooltip(button);
                }
            }
        }

        protected void drawName(String name, int y) {
            drawRowName(name, y, ColorUtils.text.getColor());
        }

        protected List<String> getTooltip(GuiButton button) {
            return getButtonTooltip(button);
        }

        protected void click(int y, int mouseX, int mouseY) {
            for (GuiButton button : buttons) {
                button.yPosition = y;
                if (GuiHelpers.press(button, mouseX, mouseY)) {
                    onPress(button);
                    return;
                }
            }
        }

        protected void onPress(GuiButton button) {
            actionPerformed(button);
        }
    }

    /** The first line of a section: its title, centered on a gray band. */
    private final class HeaderLine extends Line {

        private HeaderLine(String title) {
            super(title);
        }

        @Override
        protected void draw(int y, int mouseX, int mouseY) {
            int left = getContentLeft() - HEADER_OVERHANG;
            drawRect(
                left,
                y - HEADER_OVERHANG / 2,
                left + contentWidth + HEADER_OVERHANG * 2,
                y + GuiHelpers.BUTTON_HEIGHT + HEADER_OVERHANG / 2,
                ColorUtils.sectionHeader.getColor());
            super.draw(y, mouseX, mouseY);
        }

        @Override
        protected void drawName(String title, int y) {
            int textY = GuiHelpers.getTextY(fontRendererObj, y);
            int textX = width / 2 - fontRendererObj.getStringWidth(title) / 2;
            fontRendererObj.drawStringWithShadow(title, textX, textY, ColorUtils.text.getColor());
        }
    }

    /** The last line of a section: its buttons, centered. */
    private final class CenteredLine extends Line {

        private CenteredLine(GuiButton... buttons) {
            super(null, buttons);
            int rowWidth = -COLUMN_GAP;
            for (GuiButton button : buttons) {
                rowWidth += button.width + COLUMN_GAP;
            }
            int x = width / 2 - rowWidth / 2;
            for (GuiButton button : buttons) {
                button.xPosition = x;
                x += button.width + COLUMN_GAP;
            }
        }
    }

    private final class CrosshairLine extends Line {

        private CrosshairLine() {
            super(getCrosshairStyleName(), crosshairStyleButton, crosshairSizeButton);
        }

        @Override
        protected void draw(int y, int mouseX, int mouseY) {
            super.draw(y, mouseX, mouseY);
            drawCrosshairPreview(y);
        }

        @Override
        protected void drawName(String name, int y) {
            int textY = GuiHelpers.getTextY(fontRendererObj, y);
            fontRendererObj.drawStringWithShadow(name, crosshairLeft, textY, ColorUtils.text.getColor());
        }
    }

    private final class TextLine extends Line {

        private final String text;

        private TextLine(String text) {
            super(null);
            this.text = text;
        }

        @Override
        protected void draw(int y, int mouseX, int mouseY) {
            int textY = GuiHelpers.getTextY(fontRendererObj, y);
            drawCenteredString(fontRendererObj, text, width / 2, textY, ColorUtils.text.getColor());
        }
    }

    private final class InputLine extends Line {

        private InputLine() {
            super(null, rendering.getHeldButton(), rendering.getAddButton());
        }

        @Override
        protected void draw(int y, int mouseX, int mouseY) {
            rendering.drawInput(y);
            super.draw(y, mouseX, mouseY);
        }
    }

    private final class EntryLine extends Line {

        private final int index;

        private EntryLine(int index) {
            super(null, rendering.getRemoveButtons()[index]);
            this.index = index;
        }

        @Override
        protected void draw(int y, int mouseX, int mouseY) {
            rendering.drawEntry(index, y);
            super.draw(y, mouseX, mouseY);
            List<String> entryTooltip = rendering.getEntryTooltip(index, y, mouseX, mouseY);
            if (entryTooltip != null) {
                rowTooltip = entryTooltip;
            }
        }
    }

    private final class HotbarPreviewLine extends Line {

        private HotbarPreviewLine() {
            super(null);
        }

        @Override
        protected void draw(int y, int mouseX, int mouseY) {
            hotbar.drawPreview(width / 2, y);
        }
    }

    private final class ColumnTitlesLine extends Line {

        private ColumnTitlesLine() {
            super(null);
        }

        @Override
        protected void draw(int y, int mouseX, int mouseY) {
            screenOverlays.drawColumnTitles(fontRendererObj, GuiHelpers.getTextY(fontRendererObj, y));
        }
    }

    /** One slice of the status effect samples, which span several lines and always take the same room. */
    private final class StatusEffectsPreviewLine extends Line {

        private final int index;

        private StatusEffectsPreviewLine(int index) {
            super(null);
            this.index = index;
        }

        @Override
        protected void draw(int y, int mouseX, int mouseY) {
            int scale = HudLayout.getGameGuiScale();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(0, mc.displayHeight - (y + LINE_HEIGHT) * scale, mc.displayWidth, LINE_HEIGHT * scale);
            int areaTop = y - index * LINE_HEIGHT;
            int centeredY = areaTop + (previewLines * LINE_HEIGHT - StatusEffects.getSamplesHeight()) / 2;
            StatusEffects.drawSamples(width / 2 - StatusEffects.getSamplesWidth() / 2, centeredY);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    /** A bar, one of its states or a number without a bar. On narrow screens a bar's numbers get a second line. */
    private final class BarLine extends Line {

        private final Row row;
        private final boolean first;
        private final boolean last;

        private BarLine(Row row, boolean first, boolean last) {
            super(null, getRowButtons(row, first, last));
            this.row = row;
            this.first = first;
            this.last = last;
        }

        @Override
        protected void draw(int y, int mouseX, int mouseY) {
            if (first) {
                drawRowName(row.name, y, row.state ? ColorUtils.textState.getColor() : ColorUtils.text.getColor());
            }
            // The buttons are shared by every bar row, so they get this row's place and labels first
            placeRowButtons(row, y);
            super.draw(y, mouseX, mouseY);
            if (last) {
                HudElement bar = bars.get(row.barIndex);
                drawPreview(bar, HudLayout.getTheme(bar), y);
            }
        }

        @Override
        protected List<String> getTooltip(GuiButton button) {
            if (button == themeButton) {
                return GuiHelpers
                    .getCutTooltip(themeButton.displayString, getThemeName(HudLayout.getTheme(bars.get(row.barIndex))));
            }
            if (button == flipButton) {
                return GuiHelpers.getTooltip("dashboard.customize.fill_direction");
            }
            if (button == numberButton) {
                return GuiHelpers.getTooltip("dashboard.customize.number");
            }
            if (button == numberStyleButton) {
                return getStyleTooltip(HudLayout.getNumberStyle(row.number));
            }
            return getDecimalsTooltip(BarNumbers.getDecimals(row.number));
        }

        private void drawPreview(HudElement bar, String theme, int lineY) {
            int x = getContentLeft() + getPreviewX();
            if (row.numberOnly) {
                drawNumberSample(row.number, x, lineY);
                return;
            }
            if (row.number != null) {
                drawNumberSample(row.number, x + row.slot.width + BarThemes.PADDING, lineY);
            }

            int y = lineY + (GuiHelpers.BUTTON_HEIGHT - row.slot.height) / 2;
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

        @Override
        protected void click(int y, int mouseX, int mouseY) {
            placeRowButtons(row, y);
            super.click(y, mouseX, mouseY);
        }

        @Override
        protected void onPress(GuiButton button) {
            HudElement bar = bars.get(row.barIndex);
            if (button == themeButton) {
                HudLayout.setTheme(bar, stepTheme(HudLayout.getTheme(bar)));
            } else if (button == flipButton) {
                HudLayout.setFlipped(bar, !isCtrlKeyDown() && !HudLayout.isFlipped(bar));
            } else if (button == numberButton) {
                HudLayout.setNumberOn(row.number, !isCtrlKeyDown() && !HudLayout.isNumberOn(row.number));
            } else if (button == numberStyleButton) {
                HudLayout.setNumberStyle(row.number, stepNumberStyle(HudLayout.getNumberStyle(row.number)));
            } else {
                stepDecimals(row.number, BarNumbers.getDecimals(row.number));
            }
            updateAllBarsRow();
        }
    }

    /** A bar, one of its states, or a number without a bar (saturation). */
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
