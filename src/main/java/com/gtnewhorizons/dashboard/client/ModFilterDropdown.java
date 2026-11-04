package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The HUD editor's mod list, with a tick box per mod that shows or leaves out its elements in the editor. */
@SideOnly(Side.CLIENT)
final class ModFilterDropdown extends ScrollingDropdown {

    private static final int BOX_GAP = 3;

    private static final Comparator<Entry> ORDER = Comparator
        .comparingInt((Entry entry) -> EditorModFilter.getModRank(entry.key))
        .thenComparing(entry -> entry.name, String.CASE_INSENSITIVE_ORDER);

    private final List<Entry> entries = new ArrayList<>();
    private final int width;

    ModFilterDropdown(FontRenderer font) {
        super(font);
        Map<String, String> names = new LinkedHashMap<>();
        for (HudElement element : HudRegistry.getElements()) {
            names.putIfAbsent(EditorModFilter.getModKey(element), element.getDisplayModName());
        }
        int nameWidth = 0;
        for (Map.Entry<String, String> mod : names.entrySet()) {
            entries.add(new Entry(mod.getKey(), mod.getValue()));
            nameWidth = Math.max(nameWidth, font.getStringWidth(mod.getValue()));
        }
        entries.sort(ORDER);
        width = PADDING * 2 + Icons.SMALL_SIZE + BOX_GAP + nameWidth;
    }

    @Override
    int getRowCount() {
        return entries.size();
    }

    @Override
    int getRowsWidth() {
        return width;
    }

    @Override
    void drawRow(int row, int rowX, int rowY, boolean hovered) {
        Entry entry = entries.get(row);
        boolean shown = EditorModFilter.isShown(entry.key);
        Icons.drawSmall(
            shown ? Icons.SMALL_TICK_BOX_TICKED : Icons.SMALL_TICK_BOX,
            rowX,
            rowY,
            hovered ? ColorUtils.buttonTextHovered : ColorUtils.buttonText);
        int textColor = shown ? ColorUtils.text.getColor() : ColorUtils.textState.getColor();
        int textY = rowY + (ROW_HEIGHT - font.FONT_HEIGHT) / 2 + 1;
        font.drawStringWithShadow(entry.name, rowX + Icons.SMALL_SIZE + BOX_GAP, textY, textColor);
    }

    /** Ticks or unticks the mod, Ctrl+click all the others. Any mouse button. */
    @Override
    void clickRow(int row, int mouseButton) {
        String key = entries.get(row).key;
        if (GuiScreen.isCtrlKeyDown()) {
            setOthersOpposite(key);
        } else {
            EditorModFilter.setShown(Collections.singleton(key), !EditorModFilter.isShown(key));
        }
        GuiHelpers.playClickSound();
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

    private static final class Entry {

        private final String key;
        private final String name;

        private Entry(String key, String name) {
            this.key = key;
            this.name = name;
        }
    }
}
