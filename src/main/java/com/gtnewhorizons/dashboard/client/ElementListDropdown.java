package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The HUD editor's list of elements, under a heading per mod, for finding hidden, small or covered ones. */
@SideOnly(Side.CLIENT)
final class ElementListDropdown extends ScrollingDropdown {

    private static final int INDENT = 6;
    private static final int LOCK_GAP = 3;

    private static final Comparator<HudElement> ORDER = Comparator
        .comparingInt((HudElement element) -> EditorModFilter.getModRank(EditorModFilter.getModKey(element)))
        .thenComparing(HudElement::getDisplayModName, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(HudElement::getDisplayName, String.CASE_INSENSITIVE_ORDER);

    /** A mod heading where the element is null. */
    private final List<Row> rows = new ArrayList<>();
    private final Consumer<HudElement> grab;
    private int width;

    /** Grab is called with the element left-clicked in the list, to pick it up. */
    ElementListDropdown(FontRenderer font, Consumer<HudElement> grab) {
        super(font);
        this.grab = grab;
    }

    /** The element on the row under the mouse, null over headings or outside the list. */
    HudElement getElementAt(int mouseX, int mouseY) {
        int row = getRowAt(mouseX, mouseY);
        return row >= 0 ? rows.get(row).element : null;
    }

    /** The same elements as the editor shows. */
    @Override
    void onOpen() {
        List<HudElement> elements = new ArrayList<>();
        for (HudElement element : HudRegistry.getElements()) {
            if (EditorModFilter.isInEditor(element)) {
                elements.add(element);
            }
        }
        elements.sort(ORDER);
        rows.clear();
        int textWidth = 0;
        String modKey = null;
        for (HudElement element : elements) {
            if (!EditorModFilter.getModKey(element)
                .equals(modKey)) {
                modKey = EditorModFilter.getModKey(element);
                rows.add(new Row(null, element.getDisplayModName()));
                textWidth = Math.max(textWidth, font.getStringWidth(element.getDisplayModName()));
            }
            rows.add(new Row(element, element.getDisplayName()));
            textWidth = Math.max(textWidth, INDENT + font.getStringWidth(element.getDisplayName()));
        }
        width = PADDING * 2 + textWidth + LOCK_GAP + Icons.SMALL_SIZE;
    }

    @Override
    int getRowCount() {
        return rows.size();
    }

    @Override
    int getRowsWidth() {
        return width;
    }

    @Override
    boolean isClickable(int row) {
        return rows.get(row).element != null;
    }

    @Override
    void drawRow(int row, int rowX, int rowY, boolean hovered) {
        Row entry = rows.get(row);
        int textY = rowY + (ROW_HEIGHT - font.FONT_HEIGHT) / 2 + 1;
        if (entry.element == null) {
            Gui.drawRect(
                rowX - PADDING,
                rowY,
                rowX - PADDING + width,
                rowY + ROW_HEIGHT,
                ColorUtils.sectionHeader.getColor());
            font.drawStringWithShadow(entry.text, rowX, textY, ColorUtils.text.getColor());
            return;
        }
        int textColor = HudLayout.isVisible(entry.element) ? ColorUtils.text.getColor()
            : ColorUtils.textState.getColor();
        font.drawStringWithShadow(entry.text, rowX + INDENT, textY, textColor);
        if (HudLayout.isLocked(entry.element)) {
            Icons.drawSmall(
                Icons.SMALL_LOCK,
                rowX - PADDING + width - PADDING - Icons.SMALL_SIZE,
                rowY + 1,
                ColorUtils.text);
        }
    }

    /** Left-click picks the element up, right-click shows or hides it, like on its box. Locked ones stay. */
    @Override
    void clickRow(int row, int mouseButton) {
        HudElement element = rows.get(row).element;
        if (HudLayout.isLocked(element)) {
            return;
        }
        if (mouseButton == 0) {
            close();
            grab.accept(element);
        } else if (mouseButton == 1) {
            HudLayout.setVisible(element, !HudLayout.isVisible(element));
            GuiHelpers.playClickSound();
        }
    }

    private static final class Row {

        private final HudElement element;
        private final String text;

        private Row(HudElement element, String text) {
            this.element = element;
            this.text = text;
        }
    }
}
