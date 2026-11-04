package com.gtnewhorizons.dashboard.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.MathHelper;

import org.lwjgl.input.Mouse;

import com.gtnewhorizons.dashboard.api.HudBounds;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A list that opens under a button in the HUD editor, with a scrollbar when it is too long to fit. */
@SideOnly(Side.CLIENT)
abstract class ScrollingDropdown {

    static final int ROW_HEIGHT = Icons.SMALL_SIZE;
    static final int PADDING = 3;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_GAP = 2;
    private static final int MIN_HANDLE_HEIGHT = 8;
    private static final int MAX_VISIBLE_ROWS = 12;

    final FontRenderer font;
    private boolean open;
    private int x;
    private int y;
    private int visibleRows;
    private int firstRow;
    private boolean draggingHandle;
    private int handleGrabOffset;

    ScrollingDropdown(FontRenderer font) {
        this.font = font;
    }

    abstract int getRowCount();

    /** Without the scrollbar. */
    abstract int getRowsWidth();

    abstract void drawRow(int row, int rowX, int rowY, boolean hovered);

    abstract void clickRow(int row);

    /** Rows that can not be hovered or clicked, like headings. */
    boolean isClickable(int row) {
        return true;
    }

    /** Called before the list opens, to fill it with what is there now. */
    void onOpen() {}

    boolean isOpen() {
        return open;
    }

    void toggle() {
        if (open) {
            close();
        } else {
            onOpen();
            open = true;
        }
    }

    void close() {
        open = false;
        draggingHandle = false;
    }

    /** Under the button, or above it when there is no room below. Long lists scroll. */
    void placeAt(GuiButton button, int screenWidth, int screenHeight) {
        int rowsThatFit = Math.max(1, (screenHeight - PADDING * 2) / ROW_HEIGHT);
        visibleRows = Math.min(getRowCount(), Math.min(rowsThatFit, MAX_VISIBLE_ROWS));
        firstRow = Math.max(0, Math.min(firstRow, getRowCount() - visibleRows));
        int height = getHeight();
        x = HudLayout.clampToScreen(button.xPosition, getWidth(), screenWidth);
        int below = button.yPosition + button.height;
        y = below + height <= screenHeight ? below
            : HudLayout.clampToScreen(button.yPosition - height, height, screenHeight);
    }

    boolean contains(int mouseX, int mouseY) {
        return open && new HudBounds(x, y, getWidth(), getHeight()).contains(mouseX, mouseY);
    }

    void draw(int mouseX, int mouseY) {
        Gui.drawRect(x, y, x + getWidth(), y + getHeight(), ColorUtils.menuBackground.getColor());
        int hovered = getRowAt(mouseX, mouseY);
        for (int row = firstRow; row < firstRow + visibleRows; row++) {
            int rowY = y + PADDING + (row - firstRow) * ROW_HEIGHT;
            if (row == hovered) {
                Gui.drawRect(x, rowY, x + getRowsWidth(), rowY + ROW_HEIGHT, ColorUtils.sectionHeader.getColor());
            }
            drawRow(row, x + PADDING, rowY, row == hovered);
        }
        if (hasScrollbar()) {
            drawScrollbar(mouseX, mouseY);
        }
    }

    private void drawScrollbar(int mouseX, int mouseY) {
        HudBounds track = getTrackBounds();
        HudBounds handle = getHandleBounds();
        Gui.drawRect(track.x, track.y, track.getRight(), track.getBottom(), ColorUtils.scrollbarTrack.getColor());
        boolean hovered = draggingHandle || handle.contains(mouseX, mouseY);
        Gui.drawRect(
            handle.x,
            handle.y,
            handle.getRight(),
            handle.getBottom(),
            (hovered ? ColorUtils.scrollbarHandleHovered : ColorUtils.scrollbarHandle).getColor());
    }

    /** Call every frame while open, so a dragged handle follows the mouse until the button is let go. */
    void updateHandleDrag(int mouseY) {
        if (!draggingHandle) {
            return;
        }
        if (!Mouse.isButtonDown(0)) {
            draggingHandle = false;
            return;
        }
        HudBounds track = getTrackBounds();
        int handleTravel = track.height - getHandleBounds().height;
        int lastFirstRow = getRowCount() - visibleRows;
        if (handleTravel > 0) {
            float fraction = (mouseY - handleGrabOffset - track.y) / (float) handleTravel;
            firstRow = MathHelper.clamp_int(Math.round(fraction * lastFirstRow), 0, lastFirstRow);
        }
    }

    /**
     * True if the click was on the list. Clicking the scrollbar grabs its handle, or moves it to the click first.
     */
    boolean click(int mouseX, int mouseY) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        if (hasScrollbar() && mouseX >= x + getRowsWidth()) {
            HudBounds handle = getHandleBounds();
            handleGrabOffset = handle.contains(mouseX, mouseY) ? mouseY - handle.y : handle.height / 2;
            draggingHandle = true;
            updateHandleDrag(mouseY);
            return true;
        }
        int row = getRowAt(mouseX, mouseY);
        if (row >= 0) {
            clickRow(row);
        }
        return true;
    }

    void scroll(int rows) {
        firstRow = Math.max(0, Math.min(firstRow + rows, getRowCount() - visibleRows));
    }

    /** -1 between rows, like on the padding, on the scrollbar and on rows that can not be clicked. */
    int getRowAt(int mouseX, int mouseY) {
        if (!contains(mouseX, mouseY) || mouseX >= x + getRowsWidth()) {
            return -1;
        }
        int offset = mouseY - y - PADDING;
        int row = firstRow + offset / ROW_HEIGHT;
        return offset >= 0 && row < firstRow + visibleRows && isClickable(row) ? row : -1;
    }

    private int getHeight() {
        return PADDING * 2 + visibleRows * ROW_HEIGHT;
    }

    /** Only when the list does not fit, so short lists stay as narrow as before. */
    private boolean hasScrollbar() {
        return getRowCount() > visibleRows;
    }

    private int getWidth() {
        return hasScrollbar() ? getRowsWidth() + SCROLLBAR_WIDTH + SCROLLBAR_GAP : getRowsWidth();
    }

    /** Next to the rows, as tall as them. */
    private HudBounds getTrackBounds() {
        return new HudBounds(x + getRowsWidth(), y + PADDING, SCROLLBAR_WIDTH, visibleRows * ROW_HEIGHT);
    }

    /** As big as the part of the list in view, and as far down as the list is scrolled. */
    private HudBounds getHandleBounds() {
        HudBounds track = getTrackBounds();
        int height = Math.max(MIN_HANDLE_HEIGHT, track.height * visibleRows / getRowCount());
        int lastFirstRow = getRowCount() - visibleRows;
        int top = track.y + (track.height - height) * firstRow / Math.max(1, lastFirstRow);
        return new HudBounds(track.x, top, track.width, height);
    }
}
