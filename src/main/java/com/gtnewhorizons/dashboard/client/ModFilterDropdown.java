package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

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

    ModFilterDropdown(FontRenderer font) {
        this.font = font;
        Map<String, String> names = new LinkedHashMap<>();
        for (HudElement element : HudEditor.getElements()) {
            names.putIfAbsent(EditorModFilter.getModKey(element), element.getModName());
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
    }

    /** Under the button, or above it when there is no room below. Long lists scroll. */
    void placeAt(GuiButton button, int screenWidth, int screenHeight) {
        int rowsThatFit = Math.max(1, (screenHeight - PADDING * 2) / ROW_HEIGHT);
        visibleRows = Math.min(entries.size(), rowsThatFit);
        firstRow = Math.max(0, Math.min(firstRow, entries.size() - visibleRows));
        int height = getHeight();
        x = HudLayout.clampToScreen(button.xPosition, width, screenWidth);
        int below = button.yPosition + button.height;
        y = below + height <= screenHeight ? below
            : HudLayout.clampToScreen(button.yPosition - height, height, screenHeight);
    }

    boolean contains(int mouseX, int mouseY) {
        return open && new HudBounds(x, y, width, getHeight()).contains(mouseX, mouseY);
    }

    void draw(int mouseX, int mouseY) {
        Gui.drawRect(x, y, x + width, y + getHeight(), ColorUtils.menuBackground.getColor());
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
    }

    /** True if the click was on the list. Clicking a row ticks or unticks its mod. */
    boolean click(int mouseX, int mouseY) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        int row = getRowAt(mouseX, mouseY);
        if (row >= 0) {
            String key = entries.get(row).key;
            EditorModFilter.setShown(key, !EditorModFilter.isShown(key));
            GuiHelpers.playClickSound();
        }
        return true;
    }

    void scroll(int rows) {
        firstRow = Math.max(0, Math.min(firstRow + rows, entries.size() - visibleRows));
    }

    /** -1 between rows, like on the padding. */
    private int getRowAt(int mouseX, int mouseY) {
        if (!contains(mouseX, mouseY)) {
            return -1;
        }
        int offset = mouseY - y - PADDING;
        int row = firstRow + offset / ROW_HEIGHT;
        return offset >= 0 && row < firstRow + visibleRows ? row : -1;
    }

    private int getHeight() {
        return PADDING * 2 + visibleRows * ROW_HEIGHT;
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
