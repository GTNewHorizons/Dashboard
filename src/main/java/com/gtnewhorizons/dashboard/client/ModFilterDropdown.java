package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.MathHelper;

import org.lwjgl.input.Mouse;

import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The HUD editor's mod list, with a tick box per mod that shows or leaves out its elements in the editor. */
@SideOnly(Side.CLIENT)
final class ModFilterDropdown {

    private static final int ROW_HEIGHT = Icons.SMALL_SIZE;
    private static final int PADDING = 3;
    private static final int BOX_GAP = 3;
    private static final String MINECRAFT = "minecraft";
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_GAP = 2;
    private static final int MIN_HANDLE_HEIGHT = 8;
    private static final int MAX_VISIBLE_ROWS = 12;

    /** Minecraft and Dashboard first, since most players look for those, then the rest by name. */
    private static final Comparator<Entry> ORDER = Comparator.comparingInt(Entry::getRank)
        .thenComparing(entry -> entry.name, String.CASE_INSENSITIVE_ORDER);

    private final FontRenderer font;
    private final List<Entry> entries = new ArrayList<>();
    private boolean open;
    private int x;
    private int y;
    private int width;
    private int visibleRows;
    private int firstRow;
    private boolean draggingHandle;
    private int handleGrabOffset;

    ModFilterDropdown(FontRenderer font) {
        this.font = font;
        Map<String, String> names = new LinkedHashMap<>();
        for (HudElement element : HudEditor.getElements()) {
            names.putIfAbsent(EditorModFilter.getModKey(element), element.getEditorModName());
        }
        int nameWidth = 0;
        for (Map.Entry<String, String> mod : names.entrySet()) {
            entries.add(new Entry(mod.getKey(), mod.getValue()));
            nameWidth = Math.max(nameWidth, font.getStringWidth(mod.getValue()));
        }
        entries.sort(ORDER);
        width = PADDING * 2 + Icons.SMALL_SIZE + BOX_GAP + nameWidth;
    }

    boolean isOpen() {
        return open;
    }

    void toggle() {
        open = !open;
    }

    void close() {
        open = false;
        draggingHandle = false;
    }

    /** Under the button, or above it when there is no room below. Long lists scroll. */
    void placeAt(GuiButton button, int screenWidth, int screenHeight) {
        int rowsThatFit = Math.max(1, (screenHeight - PADDING * 2) / ROW_HEIGHT);
        visibleRows = Math.min(entries.size(), Math.min(rowsThatFit, MAX_VISIBLE_ROWS));
        firstRow = Math.max(0, Math.min(firstRow, entries.size() - visibleRows));
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
            Entry entry = entries.get(row);
            int rowY = y + PADDING + (row - firstRow) * ROW_HEIGHT;
            if (row == hovered) {
                Gui.drawRect(x, rowY, x + width, rowY + ROW_HEIGHT, ColorUtils.sectionHeader.getColor());
            }
            boolean shown = EditorModFilter.isShown(entry.key);
            Icons.drawSmall(
                shown ? Icons.SMALL_TICK_BOX_TICKED : Icons.SMALL_TICK_BOX,
                x + PADDING,
                rowY,
                row == hovered ? ColorUtils.buttonTextHovered : ColorUtils.buttonText);
            int textColor = shown ? ColorUtils.text.getColor() : ColorUtils.textState.getColor();
            int textY = rowY + (ROW_HEIGHT - font.FONT_HEIGHT) / 2 + 1;
            font.drawStringWithShadow(entry.name, x + PADDING + Icons.SMALL_SIZE + BOX_GAP, textY, textColor);
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
        int lastFirstRow = entries.size() - visibleRows;
        if (handleTravel > 0) {
            float fraction = (mouseY - handleGrabOffset - track.y) / (float) handleTravel;
            firstRow = MathHelper.clamp_int(Math.round(fraction * lastFirstRow), 0, lastFirstRow);
        }
    }

    /**
     * True if the click was on the list. Clicking a row ticks or unticks its mod, Ctrl+click all the others. Clicking
     * the scrollbar grabs its handle, or moves it to the click first.
     */
    boolean click(int mouseX, int mouseY) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        if (hasScrollbar() && mouseX >= x + width) {
            HudBounds handle = getHandleBounds();
            handleGrabOffset = handle.contains(mouseX, mouseY) ? mouseY - handle.y : handle.height / 2;
            draggingHandle = true;
            updateHandleDrag(mouseY);
            return true;
        }
        int row = getRowAt(mouseX, mouseY);
        if (row >= 0) {
            String key = entries.get(row).key;
            if (GuiScreen.isCtrlKeyDown()) {
                setOthersOpposite(key);
            } else {
                EditorModFilter.setShown(Collections.singleton(key), !EditorModFilter.isShown(key));
            }
            GuiHelpers.playClickSound();
        }
        return true;
    }

    /**
     * Gives every other mod the opposite of this mod's state, like showing only this one. If they all have it already,
     * gives them this mod's state back, so a second Ctrl+click undoes the first.
     */
    private void setOthersOpposite(String key) {
        boolean shown = EditorModFilter.isShown(key);
        List<String> others = new ArrayList<>();
        boolean allOpposite = true;
        for (Entry entry : entries) {
            if (!entry.key.equals(key)) {
                others.add(entry.key);
                allOpposite &= EditorModFilter.isShown(entry.key) != shown;
            }
        }
        EditorModFilter.setShown(others, allOpposite ? shown : !shown);
    }

    void scroll(int rows) {
        firstRow = Math.max(0, Math.min(firstRow + rows, entries.size() - visibleRows));
    }

    /** -1 between rows, like on the padding, and on the scrollbar. */
    private int getRowAt(int mouseX, int mouseY) {
        if (!contains(mouseX, mouseY) || mouseX >= x + width) {
            return -1;
        }
        int offset = mouseY - y - PADDING;
        int row = firstRow + offset / ROW_HEIGHT;
        return offset >= 0 && row < firstRow + visibleRows ? row : -1;
    }

    private int getHeight() {
        return PADDING * 2 + visibleRows * ROW_HEIGHT;
    }

    /** Only when the list does not fit, so short lists stay as narrow as before. */
    private boolean hasScrollbar() {
        return entries.size() > visibleRows;
    }

    private int getWidth() {
        return hasScrollbar() ? width + SCROLLBAR_WIDTH + SCROLLBAR_GAP : width;
    }

    /** Next to the rows, as tall as them. */
    private HudBounds getTrackBounds() {
        return new HudBounds(x + width, y + PADDING, SCROLLBAR_WIDTH, visibleRows * ROW_HEIGHT);
    }

    /** As big as the part of the list in view, and as far down as the list is scrolled. */
    private HudBounds getHandleBounds() {
        HudBounds track = getTrackBounds();
        int height = Math.max(MIN_HANDLE_HEIGHT, track.height * visibleRows / entries.size());
        int lastFirstRow = entries.size() - visibleRows;
        int top = track.y + (track.height - height) * firstRow / Math.max(1, lastFirstRow);
        return new HudBounds(track.x, top, track.width, height);
    }

    private static final class Entry {

        private final String key;
        private final String name;

        private Entry(String key, String name) {
            this.key = key;
            this.name = name;
        }

        private int getRank() {
            if (key.equals(MINECRAFT)) {
                return 0;
            }
            return key.equals(Dashboard.MODID) ? 1 : 2;
        }
    }
}
