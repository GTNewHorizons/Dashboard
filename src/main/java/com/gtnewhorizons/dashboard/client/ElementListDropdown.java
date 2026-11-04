package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.StatCollector;

import com.gtnewhorizons.dashboard.api.HudBounds;
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

    private static final int SEARCH_HEIGHT = 14;
    private static final int SEARCH_GAP = 2;
    private static final int MIN_WIDTH = 100;

    private final List<HudElement> elements = new ArrayList<>();
    /** The elements matching the search, a mod heading where the element is null. */
    private final List<Row> rows = new ArrayList<>();
    private final Consumer<HudElement> grab;
    private final GuiTextField search;
    private int width;

    /** Grab is called with the element left-clicked in the list, to pick it up. */
    ElementListDropdown(FontRenderer font, Consumer<HudElement> grab) {
        super(font);
        this.grab = grab;
        search = new GuiTextField(font, 0, 0, 0, SEARCH_HEIGHT);
        search.setMaxStringLength(50);
    }

    /** After the search box was clicked. Every key goes to it then. */
    boolean isTyping() {
        return isOpen() && search.isFocused();
    }

    void keyTyped(char typedChar, int keyCode) {
        if (search.textboxKeyTyped(typedChar, keyCode)) {
            updateRows();
        }
    }

    /** Call every tick, so the typing cursor blinks. */
    void updateCursor() {
        search.updateCursorCounter();
    }

    /** The element on the row under the mouse, null over headings or outside the list. */
    HudElement getElementAt(int mouseX, int mouseY) {
        int row = getRowAt(mouseX, mouseY);
        return row >= 0 ? rows.get(row).element : null;
    }

    /** The same elements as the editor shows, with an empty search. As wide as all of them, so typing keeps it. */
    @Override
    void onOpen() {
        elements.clear();
        int textWidth = 0;
        for (HudElement element : HudRegistry.getElements()) {
            if (EditorModFilter.isInEditor(element)) {
                elements.add(element);
                textWidth = Math.max(textWidth, font.getStringWidth(element.getDisplayModName()));
                textWidth = Math.max(textWidth, INDENT + font.getStringWidth(element.getDisplayName()));
            }
        }
        elements.sort(ORDER);
        width = Math.max(MIN_WIDTH, PADDING * 2 + textWidth + LOCK_GAP + Icons.SMALL_SIZE);
        search.width = width - PADDING * 2;
        search.setText("");
        search.setFocused(false);
        updateRows();
    }

    /** Matches the element's name or its mod's name, ignoring case. Starts at the top, where the best matches are. */
    private void updateRows() {
        String query = search.getText()
            .trim()
            .toLowerCase(Locale.ROOT);
        rows.clear();
        scrollToTop();
        String lastModKey = null;
        for (HudElement element : elements) {
            if (!query.isEmpty() && !contains(element.getDisplayName(), query)
                && !contains(element.getDisplayModName(), query)) {
                continue;
            }
            String modKey = EditorModFilter.getModKey(element);
            if (!modKey.equals(lastModKey)) {
                lastModKey = modKey;
                rows.add(new Row(null, element.getDisplayModName()));
            }
            rows.add(new Row(element, element.getDisplayName()));
        }
    }

    private static boolean contains(String text, String lowerCaseQuery) {
        return text.toLowerCase(Locale.ROOT)
            .contains(lowerCaseQuery);
    }

    /** Clicking the search box starts typing, right-clicking it also empties it. Clicking elsewhere stops typing. */
    @Override
    void clickTop(int mouseX, int mouseY, int mouseButton) {
        boolean overSearch = new HudBounds(search.xPosition, search.yPosition, search.width, search.height)
            .contains(mouseX, mouseY);
        if (mouseButton == 1 && overSearch) {
            search.setText("");
            updateRows();
        }
        search.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    int getTopHeight() {
        return rows.isEmpty() ? SEARCH_HEIGHT : SEARCH_HEIGHT + SEARCH_GAP;
    }

    @Override
    void drawTop(int topX, int topY) {
        search.xPosition = topX;
        search.yPosition = topY;
        search.drawTextBox();
        if (!search.isFocused() && search.getText()
            .isEmpty()) {
            String hint = StatCollector.translateToLocal("dashboard.editor.elements_search.hint");
            font.drawStringWithShadow(
                hint,
                topX + 4,
                topY + (SEARCH_HEIGHT - font.FONT_HEIGHT) / 2 + 1,
                ColorUtils.textState.getColor());
        }
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
