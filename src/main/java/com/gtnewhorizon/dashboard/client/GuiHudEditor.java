package com.gtnewhorizon.dashboard.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.dashboard.DashboardConfig;
import com.gtnewhorizon.dashboard.api.HudBounds;
import com.gtnewhorizon.dashboard.api.HudEditor;
import com.gtnewhorizon.dashboard.api.HudElement;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Shows a box around every registered HUD element.
 */
@SideOnly(Side.CLIENT)
public class GuiHudEditor extends GuiScreen {

    private static final int BUTTON_DONE = 0;
    private static final int BUTTON_RESET_ALL = 1;
    private static final int BUTTON_GRID = 2;
    private static final int BUTTON_LABELS = 3;
    private static final int BUTTON_POSITIONS = 4;
    private static final int[] TOGGLE_BUTTON_IDS = { BUTTON_GRID, BUTTON_LABELS, BUTTON_POSITIONS };
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 3;
    private static final int BUTTON_TEXT_PADDING = 12;
    private static final int SNAP_DISTANCE = 4;
    private static final int CENTER_MARK_LENGTH = 3;
    private static final float POSITION_TEXT_SCALE = 0.75f;
    private static final String[] HELP_LINE_KEYS = { "dashboard.editor.help_tooltip.move",
        "dashboard.editor.help_tooltip.toggle", "dashboard.editor.help_tooltip.reset" };
    private static final int HELP_ICON_SIZE = 12;
    private static final int TITLE_ROW_HEIGHT = HELP_ICON_SIZE;
    private static final int TITLE_ROW_GAP = 4;
    private static final int MENU_PADDING = 6;
    private static final int GRID_SIZE = 20;

    /**
     * Where boxes overlap, elements that are showing come first, then smaller boxes, so small elements on top of big
     * ones can still be grabbed.
     */
    private static final Comparator<ElementBox> PICK_PRIORITY = Comparator.comparing((ElementBox box) -> !box.showing)
        .thenComparingInt(box -> box.bounds.getArea());

    /** Every element's box for the current frame, most important first. */
    private final List<ElementBox> boxes = new ArrayList<>();
    /** The button rows of the panel, from top to bottom. */
    private final List<List<GuiButton>> buttonRows = new ArrayList<>();
    private final List<ToggleButton> toggleButtons = new ArrayList<>();
    private String title;
    private int menuWidth;
    private int menuHeight;
    private HudElement draggedElement;
    private int grabOffsetX;
    private int grabOffsetY;
    /** How far the menu was dragged from its default spot. Not saved. */
    private int menuOffsetX;
    private int menuOffsetY;
    private boolean draggingMenu;
    private int menuGrabOffsetX;
    private int menuGrabOffsetY;

    @Override
    public void initGui() {
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
        actionButtons
            .add(createButton(BUTTON_RESET_ALL, StatCollector.translateToLocal("dashboard.editor.button.reset_all")));
        buttonRows.add(actionButtons);

        for (List<GuiButton> row : buttonRows) {
            buttonList.addAll(row);
        }
        title = StatCollector.translateToLocal("dashboard.editor.title");
        menuWidth = calculateMenuWidth();
        menuHeight = calculateMenuHeight();
        updateToggleStates();
        positionButtons();
    }

    private GuiButton createButton(int id, String text) {
        return new GuiButton(id, 0, 0, getButtonWidth(text), BUTTON_HEIGHT, text);
    }

    private int getButtonWidth(String text) {
        return Math.max(BUTTON_HEIGHT, fontRendererObj.getStringWidth(text) + BUTTON_TEXT_PADDING);
    }

    private static String getToggleName(int buttonId) {
        return switch (buttonId) {
            case BUTTON_GRID -> "grid";
            case BUTTON_LABELS -> "labels";
            case BUTTON_POSITIONS -> "positions";
            default -> throw new IllegalArgumentException("Not a toggle button: " + buttonId);
        };
    }

    private static boolean isToggleOn(int buttonId) {
        return switch (buttonId) {
            case BUTTON_GRID -> DashboardConfig.showGrid;
            case BUTTON_LABELS -> DashboardConfig.showLabels;
            case BUTTON_POSITIONS -> DashboardConfig.showPositions;
            default -> false;
        };
    }

    private void toggle(int buttonId) {
        switch (buttonId) {
            case BUTTON_GRID -> DashboardConfig.showGrid = !DashboardConfig.showGrid;
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
            super(id, 0, 0, width, BUTTON_HEIGHT, text);
        }

        private void setOn(boolean on) {
            enabled = on;
        }

        /** Vanilla ignores clicks on disabled buttons. */
        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            return visible && mouseX >= xPosition
                && mouseY >= yPosition
                && mouseX < xPosition + width
                && mouseY < yPosition + height;
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
            y += BUTTON_HEIGHT + BUTTON_GAP;
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
        int x = clampToScreen(getMenuDefaultX() + menuOffsetX, menuWidth, width);
        int y = clampToScreen(getMenuDefaultY() + menuOffsetY, menuHeight, height);
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
        int rowsHeight = buttonRows.size() * (BUTTON_HEIGHT + BUTTON_GAP) - BUTTON_GAP;
        return MENU_PADDING * 2 + TITLE_ROW_HEIGHT + TITLE_ROW_GAP + rowsHeight;
    }

    private int getMenuDefaultX() {
        return width / 2 - menuWidth / 2;
    }

    private int getMenuDefaultY() {
        return (height - menuHeight) / 2;
    }

    private void dragMenuTo(int mouseX, int mouseY) {
        menuOffsetX = clampToScreen(mouseX - menuGrabOffsetX, menuWidth, width) - getMenuDefaultX();
        menuOffsetY = clampToScreen(mouseY - menuGrabOffsetY, menuHeight, height) - getMenuDefaultY();
        positionButtons();
    }

    private static int clampToScreen(int position, int size, int screenSize) {
        return Math.max(0, Math.min(position, screenSize - size));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (draggedElement != null) {
            if (Mouse.isButtonDown(0)) {
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
        boolean overMenu = draggingMenu || getMenuBounds().contains(mouseX, mouseY);
        HudElement highlighted = draggedElement;
        if (highlighted == null && !overMenu) {
            ElementBox hovered = findBoxAt(mouseX, mouseY);
            highlighted = hovered != null ? hovered.element : null;
        }

        if (DashboardConfig.showGrid) {
            drawGrid();
        }

        // The most important boxes are drawn last, so they end up on top
        for (int i = boxes.size() - 1; i >= 0; i--) {
            ElementBox box = boxes.get(i);
            drawElementBox(box, box.element == highlighted);
        }

        drawMenuPanel(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (highlighted != null && draggedElement == null) {
            drawHoveringText(getTooltip(highlighted), mouseX, mouseY, fontRendererObj);
        } else if (!draggingMenu) {
            List<String> menuTooltip = getMenuTooltip(mouseX, mouseY);
            if (!menuTooltip.isEmpty()) {
                drawHoveringText(menuTooltip, mouseX, mouseY, fontRendererObj);
            }
        }
    }

    /** Lines start from the screen center. */
    private void drawGrid() {
        int centerX = width / 2;
        int centerY = height / 2;
        for (int x = centerX % GRID_SIZE; x < width; x += GRID_SIZE) {
            int color = x == centerX ? ColorUtils.gridCenter.getColor() : ColorUtils.grid.getColor();
            drawRect(x, 0, x + 1, height, color);
        }
        for (int y = centerY % GRID_SIZE; y < height; y += GRID_SIZE) {
            int color = y == centerY ? ColorUtils.gridCenter.getColor() : ColorUtils.grid.getColor();
            drawRect(0, y, width, y + 1, color);
        }
    }

    private void drawMenuPanel(int mouseX, int mouseY) {
        HudBounds menu = getMenuBounds();
        drawRect(menu.x, menu.y, menu.getRight(), menu.getBottom(), ColorUtils.menuBackground.getColor());
        int titleY = menu.y + MENU_PADDING + (TITLE_ROW_HEIGHT - fontRendererObj.FONT_HEIGHT) / 2 + 1;
        fontRendererObj.drawStringWithShadow(title, menu.x + MENU_PADDING, titleY, ColorUtils.text.getColor());

        HudBounds help = getHelpIconBounds(menu);
        boolean hovered = help.contains(mouseX, mouseY) && !draggingMenu;
        drawRect(
            help.x,
            help.y,
            help.getRight(),
            help.getBottom(),
            hovered ? ColorUtils.helpIconHovered.getColor() : ColorUtils.helpIcon.getColor());
        drawCenteredString(fontRendererObj, "?", help.x + help.width / 2, help.y + 2, ColorUtils.text.getColor());
    }

    private static HudBounds getHelpIconBounds(HudBounds menu) {
        return new HudBounds(
            menu.getRight() - MENU_PADDING - HELP_ICON_SIZE,
            menu.y + MENU_PADDING,
            HELP_ICON_SIZE,
            HELP_ICON_SIZE);
    }

    private List<String> getMenuTooltip(int mouseX, int mouseY) {
        List<String> lines = new ArrayList<>();
        if (getHelpIconBounds(getMenuBounds()).contains(mouseX, mouseY)) {
            for (String key : HELP_LINE_KEYS) {
                lines.add(StatCollector.translateToLocal(key));
            }
        }
        for (ToggleButton button : toggleButtons) {
            if (button.mousePressed(mc, mouseX, mouseY)) {
                lines.add(getToggleStateText(button.id));
            }
        }
        return lines;
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
        if (DashboardConfig.showLabels) {
            drawElementName(element.getDisplayName(), bounds);
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
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(POSITION_TEXT_SCALE, POSITION_TEXT_SCALE, 1);
        fontRendererObj.drawStringWithShadow(text, 0, 0, ColorUtils.textPosition.getColor());
        GL11.glPopMatrix();
    }

    private static void drawCenterMark(HudBounds bounds, int color) {
        int centerX = bounds.x + bounds.width / 2;
        drawRect(centerX, bounds.y, centerX + 1, bounds.y + CENTER_MARK_LENGTH, color);
    }

    /**
     * Centered inside the box, wrapped onto several lines if it is too wide. If the lines do not fit either, the name
     * is shrunk onto one line.
     */
    private void drawElementName(String name, HudBounds bounds) {
        int maxWidth = bounds.width - 2;
        // Hodge: MixinFontRenderer / Without this, j8 crashes
        if (maxWidth < fontRendererObj.getCharWidth('W')) {
            return;
        }
        int lineHeight = fontRendererObj.FONT_HEIGHT;
        int centerX = bounds.x + bounds.width / 2;
        List<String> lines = fontRendererObj.listFormattedStringToWidth(name, maxWidth);

        if (lines.size() == 1 || lines.size() * lineHeight <= bounds.height) {
            int y = bounds.y + (bounds.height - lines.size() * lineHeight) / 2 + 1;
            for (String line : lines) {
                drawCenteredString(fontRendererObj, line, centerX, y, ColorUtils.text.getColor());
                y += lineHeight;
            }
            return;
        }

        float scale = (float) maxWidth / fontRendererObj.getStringWidth(name);
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, bounds.y + bounds.height / 2f, 0);
        GL11.glScalef(scale, scale, 1);
        drawCenteredString(fontRendererObj, name, 0, -lineHeight / 2 + 1, ColorUtils.text.getColor());
        GL11.glPopMatrix();
    }

    private void drawOutline(HudBounds bounds, int color) {
        drawRect(bounds.x - 1, bounds.y - 1, bounds.getRight() + 1, bounds.y, color);
        drawRect(bounds.x - 1, bounds.getBottom(), bounds.getRight() + 1, bounds.getBottom() + 1, color);
        drawRect(bounds.x - 1, bounds.y, bounds.x, bounds.getBottom(), color);
        drawRect(bounds.getRight(), bounds.y, bounds.getRight() + 1, bounds.getBottom(), color);
    }

    private List<String> getTooltip(HudElement element) {
        List<String> lines = new ArrayList<>();
        lines.add(element.getDisplayName());
        if (!HudLayout.isVisible(element)) {
            lines.add(
                EnumChatFormatting.RED + StatCollector.translateToLocal("dashboard.editor.element_tooltip.hidden"));
        }
        if (!element.isCurrentlyShowing()) {
            lines.add(
                EnumChatFormatting.GRAY
                    + StatCollector.translateToLocal("dashboard.editor.element_tooltip.not_showing"));
        }
        lines.add(EnumChatFormatting.BLUE.toString() + EnumChatFormatting.ITALIC + element.getModName());
        return lines;
    }

    private void updateBoxes() {
        boxes.clear();
        for (HudElement element : HudEditor.getElements()) {
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
        if (isOverButton(mouseX, mouseY)) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        HudBounds menu = getMenuBounds();
        if (menu.contains(mouseX, mouseY)) {
            if (mouseButton == 0) {
                draggingMenu = true;
                menuGrabOffsetX = mouseX - menu.x;
                menuGrabOffsetY = mouseY - menu.y;
            }
            return;
        }

        ElementBox box = findBoxAt(mouseX, mouseY);
        if (box == null) {
            return;
        }

        HudElement element = box.element;
        if (mouseButton == 0 && isCtrlKeyDown()) {
            HudLayout.resetPosition(element);
            playClickSound();
        } else if (mouseButton == 0) {
            draggedElement = element;
            grabOffsetX = mouseX - box.bounds.x;
            grabOffsetY = mouseY - box.bounds.y;
        } else if (mouseButton == 1) {
            HudLayout.setVisible(element, !HudLayout.isVisible(element));
            playClickSound();
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggedElement != null) {
            dragTo(mouseX, mouseY);
        }
    }

    private void dragTo(int mouseX, int mouseY) {
        HudBounds bounds = HudLayout.getBounds(draggedElement, width, height);
        int x = mouseX - grabOffsetX;
        int y = mouseY - grabOffsetY;
        if (!isShiftKeyDown()) {
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
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        super.mouseMovedOrUp(mouseX, mouseY, state);
        // -1 means the mouse only moved, otherwise it is the released button
        if (state == 0) {
            draggedElement = null;
            draggingMenu = false;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        int editorKey = HudEditorKeybind.OPEN_EDITOR.getKeyCode();
        if (editorKey != 0 && keyCode == editorKey) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case BUTTON_DONE -> mc.displayGuiScreen(null);
            case BUTTON_RESET_ALL -> HudLayout.resetAll();
            case BUTTON_GRID, BUTTON_LABELS, BUTTON_POSITIONS -> toggle(button.id);
            default -> {}
        }
    }

    @Override
    public void onGuiClosed() {
        HudLayout.save();
    }

    private void playClickSound() {
        mc.getSoundHandler()
            .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
    }
}
