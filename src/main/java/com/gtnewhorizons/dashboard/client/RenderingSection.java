package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;

import org.lwjgl.input.Keyboard;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizon.gtnhlib.util.data.ItemId;
import com.gtnewhorizons.dashboard.DashboardConfig;
import com.gtnewhorizons.dashboard.api.HudBounds;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The hidden held items list in the Customize screen. */
@SideOnly(Side.CLIENT)
final class RenderingSection extends CustomizeSection {

    private static final int BUTTON_RESET = 401;
    private static final int BUTTON_ADD = 402;
    private static final int BUTTON_HELD = 403;
    /** Remove buttons count up from here, one per entry. */
    private static final int BUTTON_REMOVE = 410;
    private static final int ADD_WIDTH = 40;
    private static final int HELD_WIDTH = 20;
    private static final int REMOVE_WIDTH = 20;
    private static final int GAP = 4;
    private static final int MAX_INPUT_WIDTH = 220;
    private static final int MAX_ID_LENGTH = 128;
    private static final int HINT_PADDING = 4;
    private static final String ENTRY_SEPARATOR = "  ";

    private final FontRenderer font;
    private final GuiTextField input;
    private final GuiButton addButton;
    private final GuiButton heldButton;
    private final GuiButton[] removeButtons;
    private final String[] entryIds;
    private final String[] entryNames;
    private final boolean[] entryKnown;
    /** Uncut, so a cut entry can still be read in its tooltip. */
    private final String[] entryFullTexts;
    private final int inputX;
    private final int inputWidth;
    private final int entryWidth;
    /** Shows both forms: every meta, and one meta. */
    private final String hint;
    private boolean inputDrawn;
    private boolean listChanged;

    /** Text and focus are carried over from the previous section, since adding an id rebuilds the screen. */
    RenderingSection(FontRenderer font, int centerX, int maxWidth, int resetWidth, String text, boolean focused) {
        super(BUTTON_RESET, resetWidth);
        this.font = font;
        int buttonsWidth = GAP + HELD_WIDTH + GAP + ADD_WIDTH;
        inputWidth = Math.min(MAX_INPUT_WIDTH, maxWidth - buttonsWidth);
        inputX = centerX - (inputWidth + buttonsWidth) / 2;
        int heldX = inputX + inputWidth + GAP;
        int buttonX = heldX + HELD_WIDTH + GAP;
        entryWidth = buttonX - GAP - inputX;

        input = new GuiTextField(font, inputX, 0, inputWidth, GuiHelpers.BUTTON_HEIGHT);
        input.setMaxStringLength(MAX_ID_LENGTH);
        input.setText(text);
        input.setFocused(focused);
        updateInputColor();
        hint = font.trimStringToWidth(
            StatCollector.translateToLocal("dashboard.rendering.hint"),
            inputWidth - HINT_PADDING * 2);
        addButton = GuiHelpers
            .button(BUTTON_ADD, buttonX, ADD_WIDTH, StatCollector.translateToLocal("dashboard.rendering.add"));
        heldButton = GuiHelpers.iconButton(BUTTON_HELD, heldX, HELD_WIDTH, Icons.HELD);
        heldButton.enabled = getHeldItem() != null;

        String[] entries = DashboardConfig.hiddenHeldItems;
        removeButtons = new GuiButton[entries.length];
        entryIds = new String[entries.length];
        entryNames = new String[entries.length];
        entryFullTexts = new String[entries.length];
        entryKnown = new boolean[entries.length];
        for (int i = 0; i < entries.length; i++) {
            removeButtons[i] = GuiHelpers.iconButton(BUTTON_REMOVE + i, buttonX, REMOVE_WIDTH, Icons.REMOVE);
            ItemId id = HiddenHeldItems.parse(entries[i]);
            entryKnown[i] = id != null;
            String name = id != null ? getItemName(id) : StatCollector.translateToLocal("dashboard.rendering.unknown");
            entryIds[i] = GuiHelpers.fitToWidth(font, entries[i], entryWidth);
            entryNames[i] = GuiHelpers.fitToWidth(font, name, entryWidth);
            entryFullTexts[i] = entries[i] + ENTRY_SEPARATOR + name;
        }
        resetButton.enabled = entries.length > 0;
    }

    /** Some modded items throw for a meta they do not expect. */
    private static String getItemName(ItemId id) {
        int meta = id.getItemMeta() == OreDictionary.WILDCARD_VALUE ? 0 : id.getItemMeta();
        try {
            return new ItemStack(id.getItem(), 1, meta).getDisplayName();
        } catch (RuntimeException e) {
            return "";
        }
    }

    GuiButton getHeldButton() {
        return heldButton;
    }

    GuiButton getAddButton() {
        return addButton;
    }

    private static ItemStack getHeldItem() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        return player != null ? player.getHeldItem() : null;
    }

    GuiButton[] getRemoveButtons() {
        return removeButtons;
    }

    @Override
    List<String> getTooltip(GuiButton button) {
        if (button.id == BUTTON_HELD) {
            return GuiHelpers.getTooltip("dashboard.rendering.held.tooltip");
        }
        if (button.id < BUTTON_REMOVE || button.id >= BUTTON_REMOVE + removeButtons.length) {
            return null;
        }
        return GuiHelpers.getTooltip("dashboard.rendering.remove");
    }

    String getText() {
        return input.getText();
    }

    boolean isFocused() {
        return input.isFocused();
    }

    /** True once after the list changed, so the screen can rebuild its lines. */
    boolean takeListChanged() {
        boolean changed = listChanged;
        listChanged = false;
        return changed;
    }

    /** Call before the list draws, so a click can not focus the box while it is scrolled out of view. */
    void startFrame() {
        inputDrawn = false;
    }

    void drawInput(int y) {
        input.yPosition = y;
        input.drawTextBox();
        if (input.getText()
            .isEmpty() && !input.isFocused()) {
            int textY = GuiHelpers.getTextY(font, y);
            font.drawString(hint, inputX + HINT_PADDING, textY, ColorUtils.textHint.getColor());
        }
        inputDrawn = true;
    }

    /** The id above the item name, both inside the remove button's height. */
    void drawEntry(int index, int y) {
        boolean known = entryKnown[index];
        int idColor = known ? ColorUtils.textInput.getColor() : ColorUtils.textUnknown.getColor();
        int nameColor = known ? ColorUtils.textState.getColor() : ColorUtils.textUnknown.getColor();
        font.drawStringWithShadow(entryIds[index], inputX, y + 1, idColor);
        font.drawStringWithShadow(entryNames[index], inputX, y + 1 + font.FONT_HEIGHT + 1, nameColor);
    }

    /** Null unless the entry was cut and the mouse is over its text. */
    List<String> getEntryTooltip(int index, int y, int mouseX, int mouseY) {
        if (!new HudBounds(inputX, y, entryWidth, GuiHelpers.BUTTON_HEIGHT).contains(mouseX, mouseY)) {
            return null;
        }
        String shown = entryIds[index] + ENTRY_SEPARATOR + entryNames[index];
        return GuiHelpers.getCutTooltip(shown, entryFullTexts[index]);
    }

    void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (inputDrawn) {
            input.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    /** False if the box does not have focus, so the screen handles the key. Enter adds the id. */
    boolean keyTyped(char typedChar, int keyCode) {
        if (!input.isFocused()) {
            return false;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            add();
        } else {
            input.textboxKeyTyped(typedChar, keyCode);
            updateInputColor();
        }
        return true;
    }

    void updateCursor() {
        input.updateCursorCounter();
    }

    /** Red while the typed id matches no item. */
    private void updateInputColor() {
        String text = input.getText()
            .trim();
        boolean unknown = !text.isEmpty() && HiddenHeldItems.parse(text) == null;
        input.setTextColor(unknown ? ColorUtils.textUnknown.getColor() : ColorUtils.textInput.getColor());
    }

    @Override
    boolean actionPerformed(GuiButton button) {
        if (button.id == BUTTON_ADD) {
            add();
        } else if (button.id == BUTTON_HELD) {
            ItemStack held = getHeldItem();
            String entry = held != null ? HiddenHeldItems.toEntry(held) : null;
            if (entry != null) {
                input.setText(entry);
                input.setFocused(true);
                updateInputColor();
            }
        } else if (button.id == BUTTON_RESET) {
            setEntries(new ArrayList<>());
        } else if (button.id >= BUTTON_REMOVE && button.id < BUTTON_REMOVE + removeButtons.length) {
            List<String> entries = new ArrayList<>(Arrays.asList(DashboardConfig.hiddenHeldItems));
            entries.remove(button.id - BUTTON_REMOVE);
            setEntries(entries);
        } else {
            return false;
        }
        return true;
    }

    /** Unknown ids are added too, shown in red, in case the mod is only missing for now. */
    private void add() {
        String text = input.getText()
            .trim();
        if (text.isEmpty()) {
            return;
        }
        List<String> entries = new ArrayList<>(Arrays.asList(DashboardConfig.hiddenHeldItems));
        if (!entries.contains(text)) {
            entries.add(text);
            setEntries(entries);
        }
        input.setText("");
        updateInputColor();
    }

    /** A new list, so HiddenHeldItems sees the change. */
    private void setEntries(List<String> entries) {
        DashboardConfig.hiddenHeldItems = entries.toArray(new String[0]);
        ConfigurationManager.save(DashboardConfig.class);
        listChanged = true;
    }
}
