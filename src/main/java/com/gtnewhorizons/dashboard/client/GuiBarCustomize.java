package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

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
    private static final int BUTTON_STATES = 3;
    private static final int SCROLL_UP = 4;
    private static final int SCROLL_DOWN = 5;
    private static final int BUTTON_CROSSHAIR_STYLE = 7;
    private static final int BUTTON_CROSSHAIR_SIZE = 8;
    private static final int BUTTON_RESET_CROSSHAIR = 14;
    private static final int BUTTON_TINKERS_CROSSHAIR = 15;
    private static final int BUTTON_AVOID_OVERLAP = 16;
    private static final int BUTTON_AVOID_OVERLAP_AUTO = 17;
    private static final int SWATCH_SIZE = 20;
    private static final int LINE_HEIGHT = BarSlot.HEALTH.height + BarThemes.PADDING * 2 + 1;
    private static final int THEME_BUTTON_WIDTH = 120;
    private static final int ICON_BUTTON_WIDTH = 20;
    private static final int SETTING_BUTTON_WIDTH = 52;
    private static final int SECTION_BUTTON_WIDTH = 80;
    private static final int ALL_BARS = -1;
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
    private final Set<Integer> open = new HashSet<>();
    private final List<HudElement> bars = new ArrayList<>();
    private final List<BarSlot> slots = new ArrayList<>();
    private final List<List<HudElement>> barNumbers = new ArrayList<>();
    private final List<Line> lines = new ArrayList<>();
    private final List<HudElement> numbers = new ArrayList<>();
    private final GuiButton scrollUp = new GuiButton(SCROLL_UP, 0, 0, "");
    private final GuiButton scrollDown = new GuiButton(SCROLL_DOWN, 0, 0, "");
    private final GuiButton themeButton = GuiHelpers.button(0, 0, THEME_BUTTON_WIDTH);
    private final IconButton moreSettingsButton = GuiHelpers.iconButton(0, 0, ICON_BUTTON_WIDTH, Icons.MORE_SETTINGS);
    private final IconButton barResetButton = GuiHelpers.iconButton(0, 0, ICON_BUTTON_WIDTH, Icons.RESET);
    private final IconButton flipButton = GuiHelpers.iconButton(0, 0, ICON_BUTTON_WIDTH, Icons.ARROW_RIGHT);
    private final GuiButton settingButton = GuiHelpers.button(0, 0, SETTING_BUTTON_WIDTH);
    private BarList list;
    private GuiButton crosshairStyleButton;
    private GuiButton crosshairSizeButton;
    private GuiButton resetCrosshairButton;
    private GuiButton tinkersCrosshairButton;
    private GuiButton avoidOverlapButton;
    private GuiButton avoidOverlapAutoButton;
    private StatusEffectsSection statusEffects;
    private ScreenOverlaysSection screenOverlays;
    private HotbarSection hotbar;
    private RenderingSection rendering;
    private final List<CustomizeSection> sections = new ArrayList<>();
    private int nameWidth;
    private int contentWidth;
    private int crosshairLeft;
    private int previewLines;
    private int numberSampleWidth;
    private boolean previewBeside;
    private boolean rebuildNeeded;
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
        barNumbers.clear();
        lines.clear();
        numbers.clear();
        for (Map.Entry<HudElement, BarSlot> entry : VanillaHudElements.getThemedBars()
            .entrySet()) {
            bars.add(entry.getKey());
            slots.add(entry.getValue());
            List<HudElement> own = new ArrayList<>();
            HudElement number = BarNumbers.getNumber(entry.getKey());
            if (number != null) {
                own.add(number);
            }
            if (entry.getValue() == BarSlot.FOOD) {
                own.add(BarNumbers.getSaturationNumber());
            }
            barNumbers.add(own);
            numbers.addAll(own);
        }
        measureColumns();

        String states = StatCollector
            .translateToLocal(showStates ? "dashboard.customize.hide_states" : "dashboard.customize.show_states");
        GuiButton statesButton = GuiHelpers.button(BUTTON_STATES, 0, SECTION_BUTTON_WIDTH, states);
        String reset = StatCollector.translateToLocal("dashboard.customize.reset");
        resetCrosshairButton = GuiHelpers.button(BUTTON_RESET_CROSSHAIR, 0, SECTION_BUTTON_WIDTH, reset);
        int crosshairNameWidth = fontRendererObj.getStringWidth(getCrosshairStyleName());
        int crosshairWidth = crosshairNameWidth + COLUMN_GAP
            + THEME_BUTTON_WIDTH
            + COLUMN_GAP
            + ICON_BUTTON_WIDTH
            + COLUMN_GAP
            + CROSSHAIR_BACKGROUNDS.length * SWATCH_SIZE;
        crosshairLeft = width / 2 - crosshairWidth / 2;
        crosshairStyleButton = GuiHelpers
            .button(BUTTON_CROSSHAIR_STYLE, crosshairLeft + crosshairNameWidth + COLUMN_GAP, THEME_BUTTON_WIDTH);
        crosshairSizeButton = GuiHelpers.button(
            BUTTON_CROSSHAIR_SIZE,
            crosshairStyleButton.xPosition + THEME_BUTTON_WIDTH + COLUMN_GAP,
            ICON_BUTTON_WIDTH);
        int columnWidth = Math.min(STATUS_COLUMN_WIDTH, (contentWidth - STATUS_COLUMN_GAP) / 2);
        tinkersCrosshairButton = Mods.TINKERS.isModLoaded()
            ? GuiHelpers.button(BUTTON_TINKERS_CROSSHAIR, 0, columnWidth)
            : null;
        updateCrosshairButtons();

        addHeader("dashboard.customize.section.bars");
        addBar(ALL_BARS);
        lines.add(new Line(null));
        for (int i = 0; i < bars.size(); i++) {
            addBar(i);
            if (showStates) {
                for (BarSlot state : slots.get(i)
                    .getStates()) {
                    lines.add(new StateLine(i, state));
                }
            }
        }
        addSectionEnd(statesButton);

        addHeader("dashboard.customize.crosshair");
        lines.add(new CrosshairLine());
        if (tinkersCrosshairButton != null) {
            lines.add(new CenteredLine(tinkersCrosshairButton));
        }
        addSectionEnd(resetCrosshairButton);

        int leftColumnX = width / 2 - columnWidth - STATUS_COLUMN_GAP / 2;
        int rightColumnX = width / 2 + STATUS_COLUMN_GAP / 2;
        hotbar = new HotbarSection(leftColumnX, rightColumnX, columnWidth, SECTION_BUTTON_WIDTH);
        addHeader("dashboard.customize.hotbar");
        for (GuiButton[] row : hotbar.getRows()) {
            lines.add(new Line(null, row));
        }
        lines.add(new HotbarPreviewLine());
        addSectionEnd(hotbar.getResetButton());

        statusEffects = new StatusEffectsSection(leftColumnX, rightColumnX, columnWidth, SECTION_BUTTON_WIDTH);
        addHeader("dashboard.customize.status_effects");
        for (GuiButton[] row : statusEffects.getRows()) {
            lines.add(new Line(null, row));
        }
        previewLines = (StatusEffects.getMaxSamplesHeight() + LINE_HEIGHT - 1) / LINE_HEIGHT;
        for (int i = 0; i < previewLines; i++) {
            lines.add(new StatusEffectsPreviewLine(i));
        }
        addSectionEnd(statusEffects.getResetButton());

        screenOverlays = new ScreenOverlaysSection(width / 2, contentWidth, SECTION_BUTTON_WIDTH);
        addHeader("dashboard.customize.screen_overlays");
        lines.add(new ColumnTitlesLine());
        for (GuiButton[] row : screenOverlays.getRows()) {
            lines.add(new Line(null, row));
        }
        addSectionEnd(screenOverlays.getResetButton());

        rendering = new RenderingSection(
            fontRendererObj,
            width / 2,
            contentWidth,
            SECTION_BUTTON_WIDTH,
            renderingText,
            renderingFocused);
        sections.addAll(Arrays.asList(hotbar, statusEffects, screenOverlays, rendering));
        addHeader("dashboard.customize.rendering");
        lines.add(new TextLine(StatCollector.translateToLocal("dashboard.rendering.hidden_held_items")));
        lines.add(new InputLine());
        for (int i = 0; i < rendering.getRemoveButtons().length; i++) {
            lines.add(new EntryLine(i));
        }
        addSectionEnd(rendering.getResetButton());

        avoidOverlapButton = GuiHelpers.button(BUTTON_AVOID_OVERLAP, 0, columnWidth);
        avoidOverlapAutoButton = GuiHelpers.button(BUTTON_AVOID_OVERLAP_AUTO, 0, columnWidth);
        updatePlacementButtons();
        addHeader("dashboard.customize.placement");
        addSectionEnd(avoidOverlapButton, avoidOverlapAutoButton);

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
    }

    /** A section's title, with a gap below. */
    private void addHeader(String langKey) {
        lines.add(new HeaderLine(StatCollector.translateToLocal(langKey)));
        lines.add(new Line(null));
    }

    /** A section's last buttons centered, usually its Reset, with a gap below. */
    private void addSectionEnd(GuiButton... buttons) {
        lines.add(new CenteredLine(buttons));
        lines.add(new Line(null));
    }

    /** Measured over every bar, setting and state, so the columns line up. */
    private void measureColumns() {
        nameWidth = fontRendererObj.getStringWidth(getAllBarsName());
        int previewWidth = 0;
        numberSampleWidth = BarThemes.PADDING + fontRendererObj.getStringWidth("000 / 000");
        for (int i = 0; i < bars.size(); i++) {
            nameWidth = Math.max(nameWidth, fontRendererObj.getStringWidth(getBarName(i)));
            previewWidth = Math.max(
                previewWidth,
                slots.get(i).width + numberSampleWidth * barNumbers.get(i)
                    .size());
            for (BarSlot state : slots.get(i)
                .getStates()) {
                nameWidth = Math.max(nameWidth, fontRendererObj.getStringWidth(getStateName(state)));
            }
        }
        int descriptionWidth = 0;
        for (Setting setting : Setting.values()) {
            nameWidth = Math.max(nameWidth, fontRendererObj.getStringWidth(setting.getName(false)));
            nameWidth = Math.max(nameWidth, fontRendererObj.getStringWidth(setting.getName(true)));
            descriptionWidth = Math.max(descriptionWidth, fontRendererObj.getStringWidth(setting.getDescription()));
        }
        int besideWidth = getBesidePreviewX() + previewWidth + BarThemes.PADDING;
        int settingWidth = getDescriptionX() + descriptionWidth;
        int belowWidth = getButtonX() + BarThemes.PADDING + previewWidth + BarThemes.PADDING;
        int maxWidth = width - SCREEN_MARGIN * 2;
        previewBeside = besideWidth <= maxWidth;
        contentWidth = Math.min(maxWidth, Math.max(previewBeside ? besideWidth : belowWidth, settingWidth));
    }

    /** The bar's line, its preview below it on narrow screens, then its settings while open. */
    private void addBar(int barIndex) {
        lines.add(new BarLine(barIndex));
        if (!previewBeside && barIndex != ALL_BARS) {
            lines.add(new PreviewLine(barIndex));
        }
        if (!open.contains(barIndex)) {
            return;
        }
        lines.add(new SettingLine(barIndex, Setting.FILL_DIRECTION, null));
        lines.add(new SettingLine(barIndex, Setting.DECORATIONS, null));
        List<HudElement> own = barIndex == ALL_BARS ? Collections.singletonList(null) : barNumbers.get(barIndex);
        for (HudElement number : own) {
            lines.add(new SettingLine(barIndex, Setting.NUMBER, number));
            lines.add(new SettingLine(barIndex, Setting.NUMBER_STYLE, number));
            lines.add(new SettingLine(barIndex, Setting.DECIMALS, number));
        }
    }

    private String getBarName(int barIndex) {
        return barIndex == ALL_BARS ? getAllBarsName()
            : bars.get(barIndex)
                .getDisplayName();
    }

    private List<HudElement> getBars(int barIndex) {
        return barIndex == ALL_BARS ? bars : Collections.singletonList(bars.get(barIndex));
    }

    /** All bars' numbers, or one bar's. */
    private List<HudElement> getNumbers(int barIndex) {
        return barIndex == ALL_BARS ? numbers : barNumbers.get(barIndex);
    }

    /** Theme, fill direction, decorations and its numbers back to how they start. */
    private void resetBars(int barIndex) {
        for (HudElement bar : getBars(barIndex)) {
            HudLayout.setTheme(bar, null);
            HudLayout.setFlipped(bar, false);
            HudLayout.setDecorationHidden(bar, false);
        }
        for (HudElement number : getNumbers(barIndex)) {
            HudLayout.setNumberOn(number, false);
            HudLayout.setNumberStyle(number, NumberStyle.SLASH);
            HudLayout.setNumberDecimals(number, null);
        }
    }

    private boolean isChanged(int barIndex) {
        for (HudElement bar : getBars(barIndex)) {
            if (HudLayout.getTheme(bar) != null || HudLayout.isFlipped(bar) || HudLayout.isDecorationHidden(bar)) {
                return true;
            }
        }
        for (HudElement number : getNumbers(barIndex)) {
            if (HudLayout.isNumberOn(number) || HudLayout.getNumberStyle(number) != NumberStyle.SLASH
                || HudLayout.getNumberDecimals(number) != null) {
                return true;
            }
        }
        return false;
    }

    /** Null if the numbers use different decimals. */
    private static Integer getCommonDecimals(List<HudElement> numbers) {
        int first = BarNumbers.getDecimals(numbers.get(0));
        for (HudElement number : numbers) {
            if (BarNumbers.getDecimals(number) != first) {
                return null;
            }
        }
        return first;
    }

    /** Ctrl goes back to each number's default. Mixed numbers start over from whole numbers. */
    private static void stepDecimals(List<HudElement> numbers) {
        Integer common = getCommonDecimals(numbers);
        int decimals = GuiHelpers
            .wrap((common != null ? common : 0) + GuiHelpers.getClickStep(), BarNumbers.MAX_DECIMALS + 1);
        for (HudElement number : numbers) {
            if (isCtrlKeyDown()) {
                HudLayout.setNumberDecimals(number, null);
            } else {
                BarNumbers.setDecimals(number, decimals);
            }
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

    private static int countNumbersOn(List<HudElement> numbers) {
        int count = 0;
        for (HudElement number : numbers) {
            if (HudLayout.isNumberOn(number)) {
                count++;
            }
        }
        return count;
    }

    /** Null if the numbers use different styles. */
    private static NumberStyle getCommonNumberStyle(List<HudElement> numbers) {
        NumberStyle first = HudLayout.getNumberStyle(numbers.get(0));
        for (HudElement number : numbers) {
            if (HudLayout.getNumberStyle(number) != first) {
                return null;
            }
        }
        return first;
    }

    /** All on, unless all are on already. Ctrl turns them off. */
    private static void toggleNumbers(List<HudElement> numbers) {
        boolean on = !isCtrlKeyDown() && countNumbersOn(numbers) < numbers.size();
        for (HudElement number : numbers) {
            HudLayout.setNumberOn(number, on);
        }
    }

    /** Mixed numbers start over from the first style. */
    private static void stepNumberStyles(List<HudElement> numbers) {
        NumberStyle common = getCommonNumberStyle(numbers);
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

    /** Auto only does something while Avoid Overlap is on. */
    private void updatePlacementButtons() {
        avoidOverlapButton.displayString = StatCollector.translateToLocal("dashboard.customize.avoid_overlap") + ": "
            + GuiHelpers.getOnOff(DashboardConfig.avoidOverlap);
        avoidOverlapAutoButton.displayString = StatCollector.translateToLocal("dashboard.customize.avoid_overlap_auto")
            + ": "
            + GuiHelpers.getOnOff(DashboardConfig.avoidOverlapAuto);
        avoidOverlapAutoButton.enabled = DashboardConfig.avoidOverlap;
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

    private static String getCrosshairStyleName() {
        return StatCollector.translateToLocal("dashboard.customize.crosshair_style");
    }

    private int getContentLeft() {
        return (width - contentWidth) / 2;
    }

    private int getButtonX() {
        return nameWidth + COLUMN_GAP;
    }

    private int getMoreSettingsX() {
        return getButtonX() + THEME_BUTTON_WIDTH + COLUMN_GAP;
    }

    private int getResetX() {
        return getMoreSettingsX() + ICON_BUTTON_WIDTH + COLUMN_GAP;
    }

    private int getPreviewX() {
        return previewBeside ? getBesidePreviewX() : getButtonX() + BarThemes.PADDING;
    }

    private int getBesidePreviewX() {
        return getResetX() + ICON_BUTTON_WIDTH + COLUMN_GAP + BarThemes.PADDING;
    }

    private int getDescriptionX() {
        return getButtonX() + SETTING_BUTTON_WIDTH + COLUMN_GAP;
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

    /** Mixed bars start over from vanilla. */
    private static void stepThemes(List<HudElement> bars) {
        String theme = stepTheme(themesMatch(bars) ? HudLayout.getTheme(bars.get(0)) : null);
        for (HudElement bar : bars) {
            HudLayout.setTheme(bar, theme);
        }
    }

    private static boolean themesMatch(List<HudElement> bars) {
        String first = HudLayout.getTheme(bars.get(0));
        for (HudElement bar : bars) {
            String theme = HudLayout.getTheme(bar);
            if (theme == null ? first != null : !theme.equals(first)) {
                return false;
            }
        }
        return true;
    }

    private static int count(List<HudElement> elements, Predicate<HudElement> check) {
        int count = 0;
        for (HudElement element : elements) {
            if (check.test(element)) {
                count++;
            }
        }
        return count;
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
        int listBottom = height - LIST_BOTTOM_MARGIN;
        // Left out while previewing, so the whole overlay shows
        if (!screenOverlays.isPreviewing()) {
            GuiHelpers.drawDirtStrip(width, 0, LIST_TOP);
            GuiHelpers.drawDirtStrip(width, listBottom, height);
        }

        drawCenteredString(fontRendererObj, title, width / 2, TITLE_Y, ColorUtils.text.getColor());
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Rows scrolled under the strips can still be hovered
        if (rowTooltip != null && mouseY >= LIST_TOP && mouseY < listBottom) {
            drawHoveringText(rowTooltip, mouseX, mouseY, fontRendererObj);
        }
    }

    /** Null for buttons without a tooltip. */
    private List<String> getButtonTooltip(GuiButton button) {
        return switch (button.id) {
            case BUTTON_CROSSHAIR_SIZE -> GuiHelpers.getTooltip("dashboard.customize.crosshair_size");
            case BUTTON_TINKERS_CROSSHAIR -> Arrays.asList(
                StatCollector.translateToLocal("dashboard.customize.tinkers_crosshair.on"),
                StatCollector.translateToLocal("dashboard.customize.tinkers_crosshair.off"));
            case BUTTON_AVOID_OVERLAP -> Arrays.asList(
                StatCollector.translateToLocal("dashboard.customize.avoid_overlap.on"),
                StatCollector.translateToLocal("dashboard.customize.avoid_overlap.off"));
            case BUTTON_AVOID_OVERLAP_AUTO -> Arrays.asList(
                StatCollector.translateToLocal("dashboard.customize.avoid_overlap_auto.on"),
                StatCollector.translateToLocal("dashboard.customize.avoid_overlap_auto.off"));
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

    /** The title, then the value in gray, Mixed when null. */
    private static List<String> getValueTooltip(String titleKey, String value) {
        String shown = value != null ? value : StatCollector.translateToLocal("dashboard.customize.mixed");
        return Arrays.asList(StatCollector.translateToLocal(titleKey), EnumChatFormatting.GRAY + shown);
    }

    private static String getShownHidden(boolean shown) {
        return StatCollector.translateToLocal(shown ? "dashboard.customize.shown" : "dashboard.customize.hidden");
    }

    private String fitToButton(String text, GuiButton button) {
        return GuiHelpers.fitToWidth(fontRendererObj, text, button.width - LABEL_MARGIN);
    }

    /** On a few backgrounds, to show the inverting. Always at x1, since x5 would not fit the row. */
    private void drawCrosshairPreview(int y) {
        int x = crosshairSizeButton.xPosition + ICON_BUTTON_WIDTH + COLUMN_GAP;
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

    /** The bar or one of its states, then the bar's number samples after it. */
    private void drawBarPreview(int barIndex, BarSlot slot, boolean withNumbers, int lineY) {
        HudElement bar = bars.get(barIndex);
        String theme = HudLayout.getTheme(bar);
        int x = getContentLeft() + getPreviewX();
        int y = lineY + (GuiHelpers.BUTTON_HEIGHT - slot.height) / 2;
        if (theme == null) {
            VanillaHudElements.drawVanillaSample(slot, x, y);
        } else {
            boolean fromRight = BarThemes.fillsFromRight(bar, slots.get(barIndex));
            BarThemes.drawSample(theme, slot, x, y, fromRight, !HudLayout.isDecorationHidden(bar));
        }
        if (slot == BarSlot.EXPERIENCE) {
            HudBounds bounds = new HudBounds(x, y - 6, slot.width, slot.height);
            VanillaHudElements.drawLevelNumber(bounds, VanillaHudElements.SAMPLE_LEVEL);
        }
        if (!withNumbers) {
            return;
        }
        int numberX = x + slot.width + BarThemes.PADDING;
        for (HudElement number : barNumbers.get(barIndex)) {
            drawNumberSample(number, numberX, lineY);
            numberX += numberSampleWidth;
        }
    }

    /** Places a shared button on the line being drawn. */
    private void place(GuiButton button, int x, int y) {
        button.xPosition = getContentLeft() + x;
        button.yPosition = y;
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
            case BUTTON_AVOID_OVERLAP -> {
                DashboardConfig.avoidOverlap = !DashboardConfig.avoidOverlap;
                ConfigurationManager.save(DashboardConfig.class);
                updatePlacementButtons();
            }
            case BUTTON_AVOID_OVERLAP_AUTO -> {
                DashboardConfig.avoidOverlapAuto = !DashboardConfig.avoidOverlapAuto;
                ConfigurationManager.save(DashboardConfig.class);
                updatePlacementButtons();
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

    /** A bar or All bars: its name, theme, More settings and reset, with the bar's preview beside it. */
    private final class BarLine extends Line {

        private final int barIndex;

        private BarLine(int barIndex) {
            super(null, themeButton, moreSettingsButton, barResetButton);
            this.barIndex = barIndex;
        }

        @Override
        protected void draw(int y, int mouseX, int mouseY) {
            drawRowName(getBarName(barIndex), y, ColorUtils.text.getColor());
            // The buttons are shared by every line, so they get this line's place and labels first
            placeButtons(y);
            super.draw(y, mouseX, mouseY);
            if (previewBeside && barIndex != ALL_BARS) {
                drawBarPreview(barIndex, slots.get(barIndex), true, y);
            }
        }

        private void placeButtons(int y) {
            place(themeButton, getButtonX(), y);
            themeButton.displayString = fitToButton(getThemeLabel(), themeButton);
            place(moreSettingsButton, getMoreSettingsX(), y);
            place(barResetButton, getResetX(), y);
            barResetButton.enabled = isChanged(barIndex);
        }

        private String getThemeLabel() {
            List<HudElement> targets = getBars(barIndex);
            return themesMatch(targets) ? getThemeName(HudLayout.getTheme(targets.get(0)))
                : StatCollector.translateToLocal("dashboard.customize.mixed");
        }

        @Override
        protected List<String> getTooltip(GuiButton button) {
            if (button == themeButton) {
                return GuiHelpers.getCutTooltip(themeButton.displayString, getThemeLabel());
            }
            if (button != moreSettingsButton) {
                return GuiHelpers.getTooltip("dashboard.customize.reset_bar");
            }
            String title = StatCollector.translateToLocal("dashboard.customize.more_settings");
            return barIndex == ALL_BARS ? Collections.singletonList(title)
                : Arrays.asList(
                    title,
                    EnumChatFormatting.GRAY + StatCollector.translateToLocal("dashboard.customize.more_settings.hint"));
        }

        @Override
        protected void click(int y, int mouseX, int mouseY) {
            placeButtons(y);
            super.click(y, mouseX, mouseY);
        }

        @Override
        protected void onPress(GuiButton button) {
            if (button == themeButton) {
                stepThemes(getBars(barIndex));
            } else if (button == moreSettingsButton) {
                if (!open.remove(barIndex)) {
                    open.add(barIndex);
                }
                rebuildNeeded = true;
            } else {
                resetBars(barIndex);
            }
        }
    }

    /** On narrow screens, a bar's preview below its line. */
    private final class PreviewLine extends Line {

        private final int barIndex;

        private PreviewLine(int barIndex) {
            super(null);
            this.barIndex = barIndex;
        }

        @Override
        protected void draw(int y, int mouseX, int mouseY) {
            drawBarPreview(barIndex, slots.get(barIndex), true, y);
        }
    }

    /** One of a bar's states, like poisoned, shown as a preview only. */
    private final class StateLine extends Line {

        private final int barIndex;
        private final BarSlot state;

        private StateLine(int barIndex, BarSlot state) {
            super(null);
            this.barIndex = barIndex;
            this.state = state;
        }

        @Override
        protected void draw(int y, int mouseX, int mouseY) {
            drawRowName(getStateName(state), y, ColorUtils.textState.getColor());
            drawBarPreview(barIndex, state, false, y);
        }
    }

    /** One setting of an open bar, or of every bar under All bars: its name, its button, then what it does. */
    private final class SettingLine extends Line {

        private final int barIndex;
        private final Setting setting;
        private final HudElement number;

        private SettingLine(int barIndex, Setting setting, HudElement number) {
            super(null, setting == Setting.FILL_DIRECTION ? flipButton : settingButton);
            this.barIndex = barIndex;
            this.setting = setting;
            this.number = number;
        }

        private boolean isSaturation() {
            return number != null && number == BarNumbers.getSaturationNumber();
        }

        private List<HudElement> getTargetNumbers() {
            return number != null ? Collections.singletonList(number) : numbers;
        }

        @Override
        protected void draw(int y, int mouseX, int mouseY) {
            // A whole line high, so the settings of a bar share one background
            int left = getContentLeft() - HEADER_OVERHANG;
            int top = y - (LINE_HEIGHT - GuiHelpers.BUTTON_HEIGHT) / 2;
            drawRect(
                left,
                top,
                left + contentWidth + HEADER_OVERHANG * 2,
                top + LINE_HEIGHT,
                ColorUtils.barSettings.getColor());
            drawRowName(setting.getName(isSaturation()), y, ColorUtils.textState.getColor());
            placeButton(y);
            super.draw(y, mouseX, mouseY);
            // Only under All bars
            if (barIndex != ALL_BARS) {
                return;
            }
            int x = getContentLeft() + getDescriptionX();
            String description = GuiHelpers
                .fitToWidth(fontRendererObj, setting.getDescription(), getContentLeft() + contentWidth - x);
            fontRendererObj.drawStringWithShadow(
                description,
                x,
                GuiHelpers.getTextY(fontRendererObj, y),
                ColorUtils.textState.getColor());
        }

        private void placeButton(int y) {
            List<HudElement> targets = getBars(barIndex);
            boolean themed = count(targets, bar -> HudLayout.getTheme(bar) != null) > 0;
            if (setting == Setting.FILL_DIRECTION) {
                place(flipButton, getButtonX(), y);
                boolean fromRight = barIndex == ALL_BARS ? count(targets, HudLayout::isFlipped) == targets.size()
                    : BarThemes.fillsFromRight(targets.get(0), slots.get(barIndex));
                flipButton.icon = fromRight ? Icons.ARROW_LEFT : Icons.ARROW_RIGHT;
                flipButton.enabled = themed;
                return;
            }
            place(settingButton, getButtonX(), y);
            String value = getValue();
            settingButton.displayString = value != null ? value
                : StatCollector.translateToLocal("dashboard.customize.mixed");
            settingButton.enabled = setting == Setting.DECORATIONS ? themed
                : setting == Setting.NUMBER || countNumbersOn(getTargetNumbers()) > 0;
        }

        /** Null when the bars differ. */
        private String getValue() {
            List<HudElement> numbers = getTargetNumbers();
            return switch (setting) {
                case DECORATIONS -> getAllOrNone(getBars(barIndex), bar -> !HudLayout.isDecorationHidden(bar), true);
                case NUMBER -> getAllOrNone(numbers, HudLayout::isNumberOn, true);
                case NUMBER_STYLE -> {
                    NumberStyle style = getCommonNumberStyle(numbers);
                    yield style != null ? getStyleSample(style) : null;
                }
                default -> {
                    Integer decimals = getCommonDecimals(numbers);
                    yield decimals != null ? getDecimalsLabel(decimals) : null;
                }
            };
        }

        /** On or Off on the button, Shown or Hidden in the tooltip, null when mixed. */
        private String getAllOrNone(List<HudElement> elements, Predicate<HudElement> check, boolean onOff) {
            int count = count(elements, check);
            if (count != 0 && count != elements.size()) {
                return null;
            }
            boolean all = count != 0;
            return onOff ? GuiHelpers.getOnOff(all) : getShownHidden(all);
        }

        @Override
        protected List<String> getTooltip(GuiButton button) {
            List<HudElement> targets = getBars(barIndex);
            String title = setting.getNameKey(isSaturation());
            return switch (setting) {
                case FILL_DIRECTION -> getValueTooltip(title, getFillDirection(targets));
                case DECORATIONS -> getValueTooltip(
                    title,
                    getAllOrNone(targets, bar -> !HudLayout.isDecorationHidden(bar), false));
                case NUMBER -> getValueTooltip(title, getAllOrNone(getTargetNumbers(), HudLayout::isNumberOn, false));
                case NUMBER_STYLE -> getStyleTooltip(getCommonNumberStyle(getTargetNumbers()));
                default -> getDecimalsTooltip(getCommonDecimals(getTargetNumbers()));
            };
        }

        /** From the left or right for a bar, flipped or not for All bars, null when mixed. */
        private String getFillDirection(List<HudElement> targets) {
            if (barIndex != ALL_BARS) {
                boolean fromRight = BarThemes.fillsFromRight(targets.get(0), slots.get(barIndex));
                return StatCollector.translateToLocal(
                    fromRight ? "dashboard.customize.fill_direction.right" : "dashboard.customize.fill_direction.left");
            }
            int flipped = count(targets, HudLayout::isFlipped);
            if (flipped != 0 && flipped != targets.size()) {
                return null;
            }
            return StatCollector.translateToLocal(
                flipped != 0 ? "dashboard.customize.fill_direction.flipped"
                    : "dashboard.customize.fill_direction.default");
        }

        @Override
        protected void click(int y, int mouseX, int mouseY) {
            placeButton(y);
            super.click(y, mouseX, mouseY);
        }

        /** Mixed bars all turn the same way. Ctrl goes back to how they start. */
        @Override
        protected void onPress(GuiButton button) {
            List<HudElement> targets = getBars(barIndex);
            switch (setting) {
                case FILL_DIRECTION -> {
                    boolean flip = !isCtrlKeyDown() && count(targets, HudLayout::isFlipped) < targets.size();
                    for (HudElement bar : targets) {
                        HudLayout.setFlipped(bar, flip);
                    }
                }
                case DECORATIONS -> {
                    boolean hide = !isCtrlKeyDown() && count(targets, HudLayout::isDecorationHidden) == 0;
                    for (HudElement bar : targets) {
                        HudLayout.setDecorationHidden(bar, hide);
                    }
                }
                case NUMBER -> toggleNumbers(getTargetNumbers());
                case NUMBER_STYLE -> stepNumberStyles(getTargetNumbers());
                default -> stepDecimals(getTargetNumbers());
            }
        }
    }

    /** The settings below an open bar. Number settings name the saturation number as such. */
    private enum Setting {

        FILL_DIRECTION("dashboard.customize.fill_direction", null),
        DECORATIONS("dashboard.customize.decorations", null),
        NUMBER("dashboard.customize.number", "dashboard.customize.saturation"),
        NUMBER_STYLE("dashboard.customize.number_style", "dashboard.customize.saturation_style"),
        DECIMALS("dashboard.customize.number_decimals", "dashboard.customize.saturation_decimals");

        private final String key;
        private final String saturationKey;

        Setting(String key, String saturationKey) {
            this.key = key;
            this.saturationKey = saturationKey;
        }

        private String getNameKey(boolean saturation) {
            return saturation && saturationKey != null ? saturationKey : key;
        }

        private String getName(boolean saturation) {
            return StatCollector.translateToLocal(getNameKey(saturation));
        }

        private String getDescription() {
            return StatCollector.translateToLocal(key + ".description");
        }
    }
}
