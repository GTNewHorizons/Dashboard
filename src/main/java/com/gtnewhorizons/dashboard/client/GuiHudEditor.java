package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizons.dashboard.DashboardConfig;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.compat.Mods;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The HUD editor: a box per element, so players can see and grab elements even while they are not showing. */
@SideOnly(Side.CLIENT)
public class GuiHudEditor extends GuiScreen {

    private static final int BUTTON_DONE = 0;
    private static final int BUTTON_RESET = 1;
    private static final int BUTTON_GRID = 2;
    private static final int BUTTON_LABELS = 3;
    private static final int BUTTON_POSITIONS = 4;
    private static final int BUTTON_PREVIEW = 5;
    private static final int BUTTON_CUSTOMIZE = 6;
    private static final int BUTTON_MODS = 7;
    private static final int BUTTON_ELEMENTS = 8;
    private static final int[] TOGGLE_BUTTON_IDS = { BUTTON_GRID, BUTTON_PREVIEW, BUTTON_LABELS, BUTTON_POSITIONS };
    private static final int BUTTON_GAP = 3;
    private static final int BUTTON_TEXT_PADDING = 12;
    private static final int SNAP_DISTANCE = 4;
    private static final int CENTER_MARK_LENGTH = 3;
    private static final float POSITION_TEXT_SCALE = 0.75f;
    private static final int OUTSIDE_NAME_PADDING = 1;
    private static final int HELP_ICON_SIZE = 12;
    private static final int HELP_CLICKS_TO_GIVE_UP = 10;
    private static final int TITLE_ROW_HEIGHT = HELP_ICON_SIZE;
    private static final int TITLE_ROW_GAP = 4;
    private static final int MENU_PADDING = 6;
    private static final int GRID_SIZE = 20;
    private static final int SAME_SPOT_OVERLAP_PERCENT = 75;

    /**
     * Where boxes overlap, elements that are showing come first, then smaller boxes, so small elements on top of big
     * ones can still be grabbed.
     */
    private static final Comparator<ElementBox> PICK_PRIORITY = Comparator.comparing((ElementBox box) -> !box.showing)
        .thenComparingInt(box -> box.bounds.getArea());

    /** Every element's box for the current frame, most important first. */
    private final List<ElementBox> boxes = new ArrayList<>();
    private final List<List<GuiButton>> buttonRows = new ArrayList<>();
    private final List<ToggleButton> toggleButtons = new ArrayList<>();
    private GuiButton customizeButton;
    private GuiButton modsButton;
    private ModFilterDropdown modFilter;
    private GuiButton elementsButton;
    private ElementListDropdown elementList;
    private String title;
    private int menuWidth;
    private int menuHeight;
    private HudElement draggedElement;
    private boolean carryingElement;
    private int grabOffsetX;
    private int grabOffsetY;
    /** How far the menu was dragged from its default spot. Not saved. */
    private int menuOffsetX;
    private int menuOffsetY;
    private boolean draggingMenu;
    private boolean menuHidden;
    private int helpClicks;
    private int menuGrabOffsetX;
    private int menuGrabOffsetY;
    private final ImagePan imagePan = new ImagePan();

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        buttonRows.clear();
        toggleButtons.clear();

        for (int id : TOGGLE_BUTTON_IDS) {
            String text = StatCollector.translateToLocal("dashboard.editor.button." + getToggleName(id));
            toggleButtons.add(new ToggleButton(id, getButtonWidth(text), text));
        }
        buttonRows.add(new ArrayList<>(toggleButtons));

        List<GuiButton> actionButtons = new ArrayList<>();
        actionButtons.add(createButton(BUTTON_DONE, StatCollector.translateToLocal("dashboard.editor.button.done")));
        actionButtons.add(createButton(BUTTON_RESET, StatCollector.translateToLocal("dashboard.editor.button.reset")));
        customizeButton = createButton(
            BUTTON_CUSTOMIZE,
            StatCollector.translateToLocal("dashboard.editor.button.customize"));
        actionButtons.add(customizeButton);
        elementsButton = createButton(
            BUTTON_ELEMENTS,
            StatCollector.translateToLocal("dashboard.editor.button.elements"));
        actionButtons.add(elementsButton);
        modsButton = createButton(BUTTON_MODS, StatCollector.translateToLocal("dashboard.editor.button.mods"));
        actionButtons.add(modsButton);
        buttonRows.add(actionButtons);
        modFilter = new ModFilterDropdown(fontRendererObj);
        elementList = new ElementListDropdown(fontRendererObj, this::carry);

        for (List<GuiButton> row : buttonRows) {
            buttonList.addAll(row);
        }
        title = StatCollector.translateToLocal("dashboard.editor.title");
        menuWidth = calculateMenuWidth();
        menuHeight = calculateMenuHeight();
        updateToggleStates();
        positionButtons();
        setMenuHidden(menuHidden);
    }

    private void setMenuHidden(boolean hidden) {
        menuHidden = hidden;
        draggingMenu = false;
        modFilter.close();
        elementList.close();
        for (Object button : buttonList) {
            ((GuiButton) button).visible = !hidden;
        }
    }

    /** The menu and its lists, which take clicks and scrolling before the elements under them. */
    private boolean isOverMenu(int mouseX, int mouseY) {
        return !menuHidden && (getMenuBounds().contains(mouseX, mouseY) || modFilter.contains(mouseX, mouseY)
            || elementList.contains(mouseX, mouseY));
    }

    private GuiButton createButton(int id, String text) {
        return GuiHelpers.button(id, 0, getButtonWidth(text), text);
    }

    private int getButtonWidth(String text) {
        return Math.max(GuiHelpers.BUTTON_HEIGHT, fontRendererObj.getStringWidth(text) + BUTTON_TEXT_PADDING);
    }

    private static String getToggleName(int buttonId) {
        return switch (buttonId) {
            case BUTTON_GRID -> "grid";
            case BUTTON_PREVIEW -> "preview";
            case BUTTON_LABELS -> "labels";
            case BUTTON_POSITIONS -> "positions";
            default -> throw new IllegalArgumentException("Not a toggle button: " + buttonId);
        };
    }

    private static boolean isToggleOn(int buttonId) {
        return switch (buttonId) {
            case BUTTON_GRID -> DashboardConfig.showGrid;
            case BUTTON_PREVIEW -> DashboardConfig.showPreview;
            case BUTTON_LABELS -> DashboardConfig.showLabels;
            case BUTTON_POSITIONS -> DashboardConfig.showPositions;
            default -> false;
        };
    }

    private void toggle(int buttonId) {
        switch (buttonId) {
            case BUTTON_GRID -> DashboardConfig.showGrid = !DashboardConfig.showGrid;
            case BUTTON_PREVIEW -> DashboardConfig.showPreview = !DashboardConfig.showPreview;
            case BUTTON_LABELS -> DashboardConfig.showLabels = !DashboardConfig.showLabels;
            case BUTTON_POSITIONS -> DashboardConfig.showPositions = !DashboardConfig.showPositions;
            default -> {}
        }
        ConfigurationManager.save(DashboardConfig.class);
        updateToggleStates();
    }

    private void updateToggleStates() {
        for (ToggleButton button : toggleButtons) {
            button.setOn(isToggleOn(button.id));
        }
    }

    private static String getToggleStateText(int buttonId) {
        String state = isToggleOn(buttonId) ? ".on" : ".off";
        return StatCollector.translateToLocal("dashboard.editor.button." + getToggleName(buttonId) + state);
    }

    private static final class ToggleButton extends GuiButton {

        private ToggleButton(int id, int width, String text) {
            super(id, 0, 0, width, GuiHelpers.BUTTON_HEIGHT, text);
        }

        private void setOn(boolean on) {
            enabled = on;
        }

        /** Vanilla ignores clicks on disabled buttons. */
        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            return GuiHelpers.isMouseOver(this, mouseX, mouseY);
        }
    }

    private void positionButtons() {
        HudBounds menu = getMenuBounds();
        int centerX = menu.x + menu.width / 2;
        int y = menu.y + MENU_PADDING + TITLE_ROW_HEIGHT + TITLE_ROW_GAP;
        for (List<GuiButton> row : buttonRows) {
            int x = centerX - getRowWidth(row) / 2;
            for (GuiButton button : row) {
                button.xPosition = x;
                button.yPosition = y;
                x += button.width + BUTTON_GAP;
            }
            y += GuiHelpers.BUTTON_HEIGHT + BUTTON_GAP;
        }
    }

    private static int getRowWidth(List<GuiButton> row) {
        int rowWidth = -BUTTON_GAP;
        for (GuiButton button : row) {
            rowWidth += button.width + BUTTON_GAP;
        }
        return rowWidth;
    }

    /** Starts in the middle of the screen and can be dragged from there. */
    private HudBounds getMenuBounds() {
        int x = HudLayout.clampToScreen(getMenuDefaultX() + menuOffsetX, menuWidth, width);
        int y = HudLayout.clampToScreen(getMenuDefaultY() + menuOffsetY, menuHeight, height);
        return new HudBounds(x, y, menuWidth, menuHeight);
    }

    private int calculateMenuWidth() {
        int contentWidth = fontRendererObj.getStringWidth(title) + BUTTON_GAP * 2 + HELP_ICON_SIZE;
        for (List<GuiButton> row : buttonRows) {
            contentWidth = Math.max(contentWidth, getRowWidth(row));
        }
        return contentWidth + MENU_PADDING * 2;
    }

    private int calculateMenuHeight() {
        int rowsHeight = buttonRows.size() * (GuiHelpers.BUTTON_HEIGHT + BUTTON_GAP) - BUTTON_GAP;
        return MENU_PADDING * 2 + TITLE_ROW_HEIGHT + TITLE_ROW_GAP + rowsHeight;
    }

    private int getMenuDefaultX() {
        return width / 2 - menuWidth / 2;
    }

    private int getMenuDefaultY() {
        return (height - menuHeight) / 2;
    }

    private void dragMenuTo(int mouseX, int mouseY) {
        menuOffsetX = HudLayout.clampToScreen(mouseX - menuGrabOffsetX, menuWidth, width) - getMenuDefaultX();
        menuOffsetY = HudLayout.clampToScreen(mouseY - menuGrabOffsetY, menuHeight, height) - getMenuDefaultY();
        positionButtons();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (imagePan.isActive()) {
            imagePan.update(mouseX, mouseY, width, height);
        }
        if (draggedElement != null) {
            if (carryingElement || Mouse.isButtonDown(0)) {
                dragTo(mouseX, mouseY);
            } else {
                draggedElement = null;
            }
        }
        if (draggingMenu) {
            if (Mouse.isButtonDown(0)) {
                dragMenuTo(mouseX, mouseY);
            } else {
                draggingMenu = false;
            }
        }

        updateBoxes();
        if (modFilter.isOpen()) {
            modFilter.placeAt(modsButton, width, height);
            modFilter.updateHandleDrag(mouseY);
        }
        if (elementList.isOpen()) {
            elementList.placeAt(elementsButton, width, height);
            elementList.updateHandleDrag(mouseY);
        }
        boolean overMenu = draggingMenu || isOverMenu(mouseX, mouseY);
        HudElement highlighted = draggedElement;
        if (highlighted == null && !overMenu) {
            ElementBox hovered = findBoxAt(mouseX, mouseY);
            highlighted = hovered != null ? hovered.element : null;
        }
        // Only the box lights up, its tooltip would cover the list
        HudElement hoveredInList = elementList.getElementAt(mouseX, mouseY);

        FrameEndHud.drawInEditor(partialTicks);
        if (DashboardConfig.showGrid) {
            drawGrid();
        }
        if (DashboardConfig.showPreview) {
            drawPreviews();
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        }

        drawElementBoxes(hoveredInList != null ? hoveredInList : highlighted);
        if (!menuHidden) {
            drawMenuPanel(mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        if (modFilter.isOpen()) {
            modFilter.draw(mouseX, mouseY);
        }
        if (elementList.isOpen()) {
            elementList.draw(mouseX, mouseY);
        }

        if (highlighted != null && draggedElement == null) {
            drawHoveringText(getTooltip(highlighted), mouseX, mouseY, fontRendererObj);
        } else if (!draggingMenu) {
            List<String> menuTooltip = getMenuTooltip(mouseX, mouseY);
            if (!menuTooltip.isEmpty()) {
                drawHoveringText(menuTooltip, mouseX, mouseY, fontRendererObj);
            }
        }
    }

    /** Samples for elements that are not showing, so players see what they are moving. */
    private void drawPreviews() {
        List<HudBounds> taken = new ArrayList<>();
        for (ElementBox box : boxes) {
            if (box.showing) {
                taken.add(box.bounds);
            }
        }
        List<HudElement> onTop = new ArrayList<>();
        for (ElementBox box : boxes) {
            if (box.showing || box.element.getPreview() == null || !HudLayout.isVisible(box.element)) {
                continue;
            }
            if (box.element.isPreviewOnTop()) {
                onTop.add(box.element);
            } else if (taken.stream()
                .noneMatch(other -> isSameSpot(box.bounds, other))) {
                    HudRegistry.drawPreview(box.element, width, height);
                    taken.add(box.bounds);
                }
        }
        for (HudElement element : onTop) {
            HudRegistry.drawPreview(element, width, height);
        }
    }

    /** Measured against the bigger box. */
    private static boolean isSameSpot(HudBounds a, HudBounds b) {
        int biggerArea = Math.max(a.getArea(), b.getArea());
        return biggerArea > 0 && a.getOverlapArea(b) >= biggerArea * SAME_SPOT_OVERLAP_PERCENT / 100;
    }

    /** Lines start from the screen center, so centered elements sit on a line. */
    private void drawGrid() {
        int centerX = width / 2;
        int centerY = height / 2;
        int gridColor = ColorUtils.grid.getColor();
        int centerColor = ColorUtils.gridCenter.getColor();
        for (int x = centerX % GRID_SIZE; x < width; x += GRID_SIZE) {
            drawRect(x, 0, x + 1, height, x == centerX ? centerColor : gridColor);
        }
        for (int y = centerY % GRID_SIZE; y < height; y += GRID_SIZE) {
            drawRect(0, y, width, y + 1, y == centerY ? centerColor : gridColor);
        }
    }

    private void drawMenuPanel(int mouseX, int mouseY) {
        HudBounds menu = getMenuBounds();
        drawRect(menu.x, menu.y, menu.getRight(), menu.getBottom(), ColorUtils.menuBackground.getColor());
        int titleY = menu.y + MENU_PADDING + (TITLE_ROW_HEIGHT - fontRendererObj.FONT_HEIGHT) / 2 + 1;
        fontRendererObj.drawStringWithShadow(title, menu.x + MENU_PADDING, titleY, ColorUtils.text.getColor());

        HudBounds help = getHelpIconBounds(menu);
        boolean hovered = help.contains(mouseX, mouseY) && !draggingMenu;
        Icons.drawCentered(
            Icons.HELP,
            help.x + help.width / 2,
            help.y + help.height / 2,
            hovered ? ColorUtils.buttonTextHovered : ColorUtils.text);
    }

    private static HudBounds getHelpIconBounds(HudBounds menu) {
        return new HudBounds(
            menu.getRight() - MENU_PADDING - HELP_ICON_SIZE,
            menu.y + MENU_PADDING,
            HELP_ICON_SIZE,
            HELP_ICON_SIZE);
    }

    /** The first line is the title. The keys are the ones bound in Controls, so the text follows rebinding. */
    private void addHelpLines(List<String> lines) {
        String reset = HudEditorKeybind.getKeyName(HudEditorKeybind.RESET);
        String size = HudEditorKeybind.getKeyName(HudEditorKeybind.SIZE_MODIFIER);
        String opacity = HudEditorKeybind.getKeyName(HudEditorKeybind.OPACITY_MODIFIER);
        String freeMove = HudEditorKeybind.getKeyName(HudEditorKeybind.FREE_MOVE_MODIFIER);
        lines.add(translate(getHelpTitleKey()));
        lines.add(noteLine("help_tooltip.point"));
        lines.add(helpLine("move", freeMove));
        lines.add(helpLine("nudge"));
        lines.add(helpLine("toggle"));
        lines.add(helpLine("reset", reset));
        lines.add(helpLine("resize", size));
        lines.add(helpLine("reset_size", size, reset));
        lines.add(helpLine("opacity", opacity));
        lines.add(helpLine("reset_opacity", opacity, reset));
        lines.add(helpLine("lock", HudEditorKeybind.getKeyName(HudEditorKeybind.LOCK)));
    }

    private String getHelpTitleKey() {
        if (helpClicks >= 7) return "help_tooltip.title_7";
        if (helpClicks >= 5) return "help_tooltip.title_5";
        if (helpClicks >= 3) return "help_tooltip.title_3";
        return "help_tooltip.title";
    }

    /** One control in the help tooltip's bullet list. */
    private static String helpLine(String name, Object... keys) {
        return EnumChatFormatting.GRAY + " - " + translate("help_tooltip." + name, keys);
    }

    /** A gray line under a tooltip's first line. */
    private static String noteLine(String key, Object... keys) {
        return EnumChatFormatting.GRAY + translate(key, keys);
    }

    /** For consistency. */
    private static String translate(String key, Object... keys) {
        return StatCollector.translateToLocalFormatted("dashboard.editor." + key, keys);
    }

    /** Turns the empty lines into a line across the tooltip. Dashes, since Angelica skips empty spaces. */
    private void fillSeparators(List<String> lines) {
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, fontRendererObj.getStringWidth(line));
        }
        char[] dashes = new char[width / Math.max(1, fontRendererObj.getCharWidth('-'))];
        Arrays.fill(dashes, '-');
        String separator = EnumChatFormatting.DARK_GRAY.toString() + EnumChatFormatting.STRIKETHROUGH
            + new String(dashes);
        lines.replaceAll(line -> line.isEmpty() ? separator : line);
    }

    private List<String> getMenuTooltip(int mouseX, int mouseY) {
        List<String> lines = new ArrayList<>();
        if (!menuHidden && getHelpIconBounds(getMenuBounds()).contains(mouseX, mouseY)) {
            if (helpClicks >= HELP_CLICKS_TO_GIVE_UP) {
                lines.add(translate("help_tooltip.give_up"));
                return lines;
            }
            addHelpLines(lines);
            lines.add("");
            lines.add(noteLine("help_tooltip.menu", HudEditorKeybind.getKeyName(HudEditorKeybind.HIDE_MENU)));
            lines.add(noteLine("help_tooltip.change_keys"));
            lines.add(
                noteLine(
                    "help_tooltip.layout",
                    HudLayout.getActiveLayout(),
                    HudLayout.LAYOUT_COUNT,
                    HudEditorKeybind.getKeyName(HudEditorKeybind.NEXT_LAYOUT)));
            // These only show under some conditions, which is easy to miss
            boolean inGameInfo = Mods.INGAMEINFO.isModLoaded();
            // TODO: JourneyMap release with MiniMapRenderEvent
            // boolean journeyMap = HudRegistry.getElement("journeymap:minimap") != null;
            if (inGameInfo /* || journeyMap */) {
                lines.add("");
            }
            if (inGameInfo) {
                lines.add(noteLine("help_tooltip.ingameinfo"));
            }
            // if (journeyMap) {
            // lines.add(noteLine("help_tooltip.journeymap"));
            // }
            fillSeparators(lines);
        }
        for (ToggleButton button : toggleButtons) {
            if (button.mousePressed(mc, mouseX, mouseY)) {
                lines.add(getToggleStateText(button.id));
            }
        }
        if (customizeButton.mousePressed(mc, mouseX, mouseY)) {
            lines.add(translate("customize_tooltip"));
            lines.add(noteLine("customize_tooltip.bars"));
            lines.add(noteLine("customize_tooltip.sections"));
        }
        if (!elementList.isOpen() && elementsButton.mousePressed(mc, mouseX, mouseY)) {
            lines.add(translate("elements_tooltip"));
            lines.add(noteLine("elements_tooltip.hover"));
            lines.add(noteLine("elements_tooltip.grab"));
            lines.add(noteLine("elements_tooltip.toggle"));
        }
        if (!modFilter.isOpen() && modsButton.mousePressed(mc, mouseX, mouseY)) {
            lines.add(translate("mods_tooltip"));
            lines.add(noteLine("mods_tooltip.game"));
            lines.add(noteLine("mods_tooltip.ctrl"));
        }
        return lines;
    }

    /**
     * The most important boxes are drawn last, so they end up on top. Names that do not fit their box go outside it,
     * after every box so no box covers them, and the highlighted one last.
     */
    private void drawElementBoxes(HudElement highlighted) {
        float smallScale = getSmallTextScale();
        List<ElementBox> namesOutside = new ArrayList<>();
        ElementBox highlightedNameOutside = null;
        for (int i = boxes.size() - 1; i >= 0; i--) {
            ElementBox box = boxes.get(i);
            boolean isHighlighted = box.element == highlighted;
            drawElementBox(box, isHighlighted);
            if (DashboardConfig.showLabels && !drawNameInside(box.element.getDisplayName(), box.bounds, smallScale)) {
                if (isHighlighted) {
                    highlightedNameOutside = box;
                } else {
                    namesOutside.add(box);
                }
            }
        }
        if (highlightedNameOutside != null) {
            namesOutside.add(highlightedNameOutside);
        }
        for (ElementBox box : namesOutside) {
            drawNameOutside(box.element.getDisplayName(), box.bounds, smallScale);
        }
    }

    /** About half size, but only at sizes where each font pixel is whole screen pixels, so the text stays sharp. */
    private static float getSmallTextScale() {
        int guiScale = HudLayout.getGameGuiScale();
        return (float) ((guiScale + 1) / 2) / guiScale;
    }

    private void drawElementBox(ElementBox box, boolean highlighted) {
        HudElement element = box.element;
        HudBounds bounds = box.bounds;

        int fillColor;
        int borderColor;
        if (!HudLayout.isVisible(element)) {
            fillColor = ColorUtils.hiddenFill.getColor();
            borderColor = ColorUtils.hiddenBorder.getColor();
        } else if (box.showing) {
            fillColor = ColorUtils.showingFill.getColor();
            borderColor = ColorUtils.showingBorder.getColor();
        } else {
            fillColor = ColorUtils.notShowingFill.getColor();
            borderColor = ColorUtils.notShowingBorder.getColor();
        }
        if (highlighted) {
            borderColor = ColorUtils.highlightBorder.getColor();
        }

        drawRect(bounds.x, bounds.y, bounds.getRight(), bounds.getBottom(), fillColor);
        drawOutline(bounds, borderColor);
        drawCenterMark(bounds, borderColor);
        if (HudLayout.isLocked(element)) {
            Icons.drawSmall(Icons.SMALL_LOCK, bounds.getRight() - Icons.SMALL_SIZE, bounds.y, ColorUtils.text);
        }
        if (DashboardConfig.showPositions) {
            drawPosition(bounds);
        }
    }

    /** The element's middle, measured from the middle of the screen. */
    private void drawPosition(HudBounds bounds) {
        int fromCenterX = bounds.x + bounds.width / 2 - width / 2;
        int fromCenterY = height / 2 - (bounds.y + bounds.height / 2);
        String text = fromCenterX + ", " + fromCenterY;
        float textWidth = fontRendererObj.getStringWidth(text) * POSITION_TEXT_SCALE;
        float textHeight = fontRendererObj.FONT_HEIGHT * POSITION_TEXT_SCALE;
        float x = bounds.getRight() - textWidth;
        float y = bounds.y - textHeight - 1;
        if (y < 0) {
            y = bounds.y + 1;
        }
        PreviewDrawing.drawScaledString(text, x, y, POSITION_TEXT_SCALE, ColorUtils.textPosition.getColor());
    }

    private static void drawCenterMark(HudBounds bounds, int color) {
        int centerX = bounds.x + bounds.width / 2;
        drawRect(centerX, bounds.y, centerX + 1, bounds.y + CENTER_MARK_LENGTH, color);
    }

    /**
     * Centered inside the box, trying full size, then sideways for tall boxes, then both smaller. Wrapped onto several
     * lines if it is too long. False when it does not fit any way.
     */
    private boolean drawNameInside(String name, HudBounds bounds, float smallScale) {
        boolean tall = bounds.height > bounds.width;
        boolean canShrink = smallScale < 1;
        return drawNameInside(name, bounds, 1, false) || tall && drawNameInside(name, bounds, 1, true)
            || canShrink && drawNameInside(name, bounds, smallScale, false)
            || tall && canShrink && drawNameInside(name, bounds, smallScale, true);
    }

    private boolean drawNameInside(String name, HudBounds bounds, float scale, boolean sideways) {
        int roomAlongLines = sideways ? bounds.height : bounds.width;
        int roomForLines = sideways ? bounds.width : bounds.height;
        int maxWidth = (int) ((roomAlongLines - 2) / scale);
        int lineHeight = fontRendererObj.FONT_HEIGHT;
        // The width check also keeps Hodgepodge's MixinFontRenderer from crashing on Java 8
        if (maxWidth < fontRendererObj.getCharWidth('W') || (lineHeight - 2) * scale > roomForLines) {
            return false;
        }
        List<String> lines = fontRendererObj.listFormattedStringToWidth(name, maxWidth);
        // The last line's gap and shadow may touch the border
        if ((lines.size() * lineHeight - 2) * scale > roomForLines) {
            return false;
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2, 0);
        if (sideways) {
            GL11.glRotatef(-90, 0, 0, 1);
        }
        GL11.glScalef(scale, scale, 1);
        int y = -lines.size() * lineHeight / 2 + 1;
        for (String line : lines) {
            drawCenteredString(fontRendererObj, line, 0, y, ColorUtils.text.getColor());
            y += lineHeight;
        }
        GL11.glPopMatrix();
        return true;
    }

    /** Smaller, on a dark backing above the box (and its position), or under it at the top of the screen. */
    private void drawNameOutside(String name, HudBounds bounds, float scale) {
        int textWidth = (int) Math.ceil(fontRendererObj.getStringWidth(name) * scale);
        int textHeight = (int) Math.ceil((fontRendererObj.FONT_HEIGHT - 1) * scale);
        int backingWidth = textWidth + OUTSIDE_NAME_PADDING * 2;
        int backingHeight = textHeight + OUTSIDE_NAME_PADDING * 2;
        int x = HudLayout.clampToScreen(bounds.x + (bounds.width - backingWidth) / 2, backingWidth, width);
        int y = bounds.y - 2 - backingHeight;
        if (DashboardConfig.showPositions) {
            y -= (int) Math.ceil(fontRendererObj.FONT_HEIGHT * POSITION_TEXT_SCALE) + 1;
        }
        if (y < 0) {
            y = Math.min(bounds.getBottom() + 2, height - backingHeight);
        }
        drawRect(x, y, x + backingWidth, y + backingHeight, ColorUtils.menuBackground.getColor());
        PreviewDrawing.drawScaledString(
            name,
            x + OUTSIDE_NAME_PADDING,
            y + OUTSIDE_NAME_PADDING,
            scale,
            ColorUtils.text.getColor());
    }

    static void drawOutline(HudBounds bounds, int color) {
        drawRect(bounds.x - 1, bounds.y - 1, bounds.getRight() + 1, bounds.y, color);
        drawRect(bounds.x - 1, bounds.getBottom(), bounds.getRight() + 1, bounds.getBottom() + 1, color);
        drawRect(bounds.x - 1, bounds.y, bounds.x, bounds.getBottom(), color);
        drawRect(bounds.getRight(), bounds.y, bounds.getRight() + 1, bounds.getBottom(), color);
    }

    /** The name and the element's state first, then what it is and where its own settings are, then its mod. */
    private List<String> getTooltip(HudElement element) {
        List<String> lines = new ArrayList<>();
        lines.add(element.getDisplayName());
        if (!HudLayout.isVisible(element)) {
            lines.add(
                EnumChatFormatting.RED + StatCollector.translateToLocal("dashboard.editor.element_tooltip.hidden"));
        }
        if (HudLayout.isLocked(element)) {
            lines.add(
                EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(
                    "dashboard.editor.element_tooltip.locked",
                    HudEditorKeybind.getKeyName(HudEditorKeybind.LOCK)));
        }
        lines.add(EnumChatFormatting.GRAY + getSizeText(element));
        if (ElementOpacity.isAllowed(element)) {
            lines.add(
                EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(
                    "dashboard.editor.element_tooltip.background",
                    ElementOpacity.getPercent(element)));
        }
        if (!element.isCurrentlyShowing()) {
            lines.add(
                EnumChatFormatting.GRAY
                    + StatCollector.translateToLocal("dashboard.editor.element_tooltip.not_showing"));
        }
        String description = element.getDescription();
        String ownSettings = element.getOwnSettings();
        if (description != null || ownSettings != null) {
            lines.add("");
        }
        if (description != null) {
            for (String line : description.split("\\\\n")) {
                lines.add(EnumChatFormatting.GRAY + line);
            }
        }
        if (ownSettings != null) {
            lines.add(
                EnumChatFormatting.GRAY + StatCollector
                    .translateToLocalFormatted("dashboard.editor.element_tooltip.own_settings", ownSettings));
            if (HudLayout.isMoved(element)) {
                lines.add(
                    EnumChatFormatting.GRAY
                        + StatCollector.translateToLocal("dashboard.editor.element_tooltip.own_settings_moved"));
            }
        }
        lines.add(EnumChatFormatting.BLUE.toString() + EnumChatFormatting.ITALIC + element.getDisplayModName());
        return lines;
    }

    private static String getSizeText(HudElement element) {
        int step = HudLayout.getSizeStep(element);
        int guiScale = HudLayout.getElementGuiScale(element);
        String key = "dashboard.editor.element_tooltip.size";
        if (guiScale == 1) {
            key += "_smallest";
        } else if (step == HudLayout.MAX_SIZE_STEP) {
            key += "_largest";
        }
        String stepText = step > 0 ? "+" + step : String.valueOf(step);
        return StatCollector.translateToLocalFormatted(key, stepText, guiScale);
    }

    private void updateBoxes() {
        boxes.clear();
        for (HudElement element : HudRegistry.getElements()) {
            if (!EditorModFilter.isInEditor(element)) {
                continue;
            }
            boxes.add(
                new ElementBox(element, HudLayout.getBounds(element, width, height), element.isCurrentlyShowing()));
        }
        boxes.sort(PICK_PRIORITY);
    }

    /** The most important box under the mouse. */
    private ElementBox findBoxAt(int mouseX, int mouseY) {
        for (ElementBox box : boxes) {
            if (box.bounds.contains(mouseX, mouseY)) {
                return box;
            }
        }
        return null;
    }

    private static final class ElementBox {

        private final HudElement element;
        private final HudBounds bounds;
        private final boolean showing;

        private ElementBox(HudElement element, HudBounds bounds, boolean showing) {
            this.element = element;
            this.bounds = bounds;
            this.showing = showing;
        }
    }

    private boolean isOverButton(int mouseX, int mouseY) {
        for (Object button : buttonList) {
            if (((GuiButton) button).mousePressed(mc, mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (carryingElement) {
            carryingElement = false;
            draggedElement = null;
            return;
        }
        if (modFilter.click(mouseX, mouseY, mouseButton) || elementList.click(mouseX, mouseY, mouseButton)) {
            return;
        }
        // A click anywhere else closes the lists, except on their own button, which toggles them
        if (!modsButton.mousePressed(mc, mouseX, mouseY)) {
            modFilter.close();
        }
        if (!elementsButton.mousePressed(mc, mouseX, mouseY)) {
            elementList.close();
        }
        if (isOverButton(mouseX, mouseY)) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        HudBounds menu = getMenuBounds();
        if (!menuHidden && mouseButton == 0 && getHelpIconBounds(menu).contains(mouseX, mouseY)) {
            clickHelp();
            return;
        }
        if (!menuHidden && menu.contains(mouseX, mouseY)) {
            if (mouseButton == 0) {
                draggingMenu = true;
                menuGrabOffsetX = mouseX - menu.x;
                menuGrabOffsetY = mouseY - menu.y;
            }
            return;
        }

        ElementBox box = findBoxAt(mouseX, mouseY);
        if (box == null || HudLayout.isLocked(box.element)) {
            return;
        }

        HudElement element = box.element;
        if (mouseButton == 0) {
            draggedElement = element;
            grabOffsetX = mouseX - box.bounds.x;
            grabOffsetY = mouseY - box.bounds.y;
        } else if (mouseButton == 1) {
            HudLayout.setVisible(element, !HudLayout.isVisible(element));
            GuiHelpers.playClickSound();
        } else if (mouseButton == 2 && ImageElements.isImage(element)) {
            imagePan.start(element, ImagePan.MIDDLE_BUTTON, mouseX, mouseY);
        }
    }

    /** Angernoisemaker machine 3000, patent pending */
    private void clickHelp() {
        // Gives up for as many clicks as it took, then starts over
        helpClicks = (helpClicks + 1) % (HELP_CLICKS_TO_GIVE_UP * 2);
        float pitch = Math.min(2, 0.8f + helpClicks * 0.12f);
        mc.getSoundHandler()
            .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("mob.villager.idle"), pitch));
    }

    /** Held by its middle, since it may be far from the mouse. */
    private void carry(HudElement element) {
        HudBounds bounds = HudLayout.getBounds(element, width, height);
        draggedElement = element;
        carryingElement = true;
        grabOffsetX = bounds.width / 2;
        grabOffsetY = bounds.height / 2;
    }

    private void dragTo(int mouseX, int mouseY) {
        HudBounds bounds = HudLayout.getBounds(draggedElement, width, height);
        int x = mouseX - grabOffsetX;
        int y = mouseY - grabOffsetY;
        if (!HudEditorKeybind.isHeld(HudEditorKeybind.FREE_MOVE_MODIFIER)) {
            x = snap(x, bounds.width, width);
            y = snap(y, bounds.height, height);
        }
        HudLayout.moveTo(draggedElement, x, y, width, height);
    }

    /**
     * Pulls the position onto the screen edges or the center line when it is close to them. Otherwise, if the grid is
     * on, pulls either edge of the element onto a close grid line.
     */
    private static int snap(int position, int elementSize, int screenSize) {
        int centered = (screenSize - elementSize) / 2;
        int farEdge = screenSize - elementSize;
        if (Math.abs(position) <= SNAP_DISTANCE) return 0;
        if (Math.abs(position - farEdge) <= SNAP_DISTANCE) return farEdge;
        if (Math.abs(position - centered) <= SNAP_DISTANCE) return centered;
        if (!DashboardConfig.showGrid) return position;

        int startOnGrid = nearestGridLine(position, screenSize);
        int endOnGrid = nearestGridLine(position + elementSize, screenSize) - elementSize;
        int startDistance = Math.abs(position - startOnGrid);
        int endDistance = Math.abs(position - endOnGrid);
        if (startDistance <= endDistance && startDistance <= SNAP_DISTANCE) return startOnGrid;
        if (endDistance <= SNAP_DISTANCE) return endOnGrid;
        return position;
    }

    /** Grid lines start from the screen center, like in drawGrid. */
    private static int nearestGridLine(int position, int screenSize) {
        int center = screenSize / 2;
        return center + Math.round((position - center) / (float) GRID_SIZE) * GRID_SIZE;
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mouseX = GuiHelpers.getEventMouseX(width);
        int mouseY = GuiHelpers.getEventMouseY(height);
        if (modFilter.contains(mouseX, mouseY)) {
            modFilter.scroll(wheel > 0 ? -1 : 1);
            return;
        }
        if (elementList.contains(mouseX, mouseY)) {
            elementList.scroll(wheel > 0 ? -1 : 1);
            return;
        }
        if (isOverMenu(mouseX, mouseY)) {
            return;
        }
        ElementBox box = findBoxAt(mouseX, mouseY);
        if (box != null && !HudLayout.isLocked(box.element)) {
            scrollOver(box.element, box.bounds, mouseX, mouseY, wheel);
        }
    }

    /**
     * Resize key + scroll resizes, see-through key + scroll changes the background opacity, plain scroll zooms an
     * image. Also used in chat.
     */
    static void scrollOver(HudElement element, HudBounds bounds, int mouseX, int mouseY, int wheel) {
        int steps = wheel > 0 ? 1 : -1;
        if (HudEditorKeybind.isHeld(HudEditorKeybind.SIZE_MODIFIER)) {
            resize(element, steps);
        } else if (HudEditorKeybind.isHeld(HudEditorKeybind.OPACITY_MODIFIER)) {
            if (ElementOpacity.isAllowed(element)) {
                ElementOpacity.change(element, steps);
            }
        } else if (ImageElements.isImage(element)) {
            ImageElements.zoom(element, bounds, mouseX, mouseY, steps);
        }
    }

    /** Stops shrinking at GUI scale 1, since nothing smaller can be drawn. */
    private static void resize(HudElement element, int change) {
        if (change < 0 && HudLayout.getElementGuiScale(element) <= 1) {
            return;
        }
        HudLayout.setSizeStep(element, HudLayout.getSizeStep(element) + change);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        super.mouseMovedOrUp(mouseX, mouseY, state);
        // -1 means the mouse only moved, otherwise it is the released button
        if (state == 0) {
            if (!carryingElement) {
                draggedElement = null;
            }
            draggingMenu = false;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        // Esc closes the open list instead of the editor, and while typing in its search box every key goes there
        if (elementList.isOpen() && keyCode == Keyboard.KEY_ESCAPE) {
            elementList.close();
            return;
        }
        if (elementList.isTyping()) {
            elementList.keyTyped(typedChar, keyCode);
            return;
        }
        int editorKey = HudEditorKeybind.OPEN_EDITOR.getKeyCode();
        if (editorKey != 0 && keyCode == editorKey) {
            mc.displayGuiScreen(null);
            return;
        }
        int resetKey = HudEditorKeybind.RESET.getKeyCode();
        int lockKey = HudEditorKeybind.LOCK.getKeyCode();
        if (draggedElement == null && keyCode != 0
            && (keyCode == resetKey || keyCode == lockKey)
            && !Keyboard.isRepeatEvent()) {
            HudElement element = findElementUnderMouse();
            if (element == null) {
                return;
            }
            if (keyCode == resetKey) {
                if (!HudLayout.isLocked(element)) {
                    reset(element);
                }
            } else {
                HudLayout.setLocked(element, !HudLayout.isLocked(element));
                GuiHelpers.playClickSound();
            }
            return;
        }
        int nudgeX = keyCode == Keyboard.KEY_LEFT ? -1 : keyCode == Keyboard.KEY_RIGHT ? 1 : 0;
        int nudgeY = keyCode == Keyboard.KEY_UP ? -1 : keyCode == Keyboard.KEY_DOWN ? 1 : 0;
        if (draggedElement == null && (nudgeX != 0 || nudgeY != 0)) {
            HudElement element = findElementUnderMouse();
            if (element != null && !HudLayout.isLocked(element)) {
                HudBounds bounds = HudLayout.getBounds(element, width, height);
                HudLayout.moveTo(element, bounds.x + nudgeX, bounds.y + nudgeY, width, height);
            }
            return;
        }
        int hideKey = HudEditorKeybind.HIDE_MENU.getKeyCode();
        if (hideKey != 0 && keyCode == hideKey && !Keyboard.isRepeatEvent()) {
            setMenuHidden(!menuHidden);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    /** For key presses, which come without a mouse position. Null over the menu or empty space. */
    private HudElement findElementUnderMouse() {
        int mouseX = Mouse.getX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getY() * height / mc.displayHeight - 1;
        if (isOverMenu(mouseX, mouseY)) {
            return null;
        }
        ElementBox box = findBoxAt(mouseX, mouseY);
        return box != null ? box.element : null;
    }

    /** The size, the see-through background, or without a modifier key the position. */
    private static void reset(HudElement element) {
        if (HudEditorKeybind.isHeld(HudEditorKeybind.SIZE_MODIFIER)) {
            HudLayout.setSizeStep(element, 0);
        } else if (HudEditorKeybind.isHeld(HudEditorKeybind.OPACITY_MODIFIER)) {
            if (!ElementOpacity.isAllowed(element)) {
                return;
            }
            ElementOpacity.reset(element);
        } else {
            HudLayout.resetPosition(element);
        }
        GuiHelpers.playClickSound();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case BUTTON_DONE -> mc.displayGuiScreen(null);
            case BUTTON_RESET -> resetShownMods();
            case BUTTON_CUSTOMIZE -> mc.displayGuiScreen(new GuiBarCustomize(this));
            case BUTTON_MODS -> modFilter.toggle();
            case BUTTON_ELEMENTS -> elementList.toggle();
            case BUTTON_GRID, BUTTON_PREVIEW, BUTTON_LABELS, BUTTON_POSITIONS -> toggle(button.id);
            default -> {}
        }
    }

    /** Locked elements and mods left out of the editor are left alone, so they can not be reset by accident. */
    private static void resetShownMods() {
        List<HudElement> shown = new ArrayList<>();
        for (HudElement element : HudRegistry.getElements()) {
            if (EditorModFilter.isShown(element) && !HudLayout.isLocked(element)) {
                shown.add(element);
            }
        }
        HudLayout.reset(shown);
    }

    @Override
    public void updateScreen() {
        elementList.updateCursor();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        HudLayout.save();
    }

}
