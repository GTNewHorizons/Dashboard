package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;

import org.lwjgl.input.Keyboard;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizon.gtnhlib.util.data.ItemId;
import com.gtnewhorizons.dashboard.DashboardConfig;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The hidden held items list of the Customize screen: a text box to add item ids, and a line per id to remove it. */
@SideOnly(Side.CLIENT)
final class RenderingSection {

    private static final int BUTTON_RESET = 401;
    private static final int BUTTON_ADD = 402;
    private static final int BUTTON_HELD = 403;
    /** Remove buttons count up from here, one per entry. */
    private static final int BUTTON_REMOVE = 410;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ADD_WIDTH = 40;
    private static final int HELD_WIDTH = 20;
    private static final int REMOVE_WIDTH = 20;
    private static final int GAP = 4;
    private static final int MAX_INPUT_WIDTH = 220;
    private static final int MAX_ID_LENGTH = 128;
    private static final int HINT_PADDING = 4;

    private final FontRenderer font;
    private final GuiTextField input;
    private final GuiButton addButton;
    private final GuiButton heldButton;
    private final GuiButton resetButton;
    private final GuiButton[] removeButtons;
    private final String[] entryTexts;
    private final boolean[] entryKnown;
    private final int inputX;
    private final int inputWidth;
    /** Shows both forms: every meta, and one meta. */
    private final String hint;
    private boolean inputDrawn;
    private boolean listChanged;

    /** Text and focus are carried over from the previous section, since adding an id rebuilds the screen. */
    RenderingSection(FontRenderer font, int centerX, int maxWidth, int resetWidth, String text, boolean focused) {
        this.font = font;
        int buttonsWidth = GAP + HELD_WIDTH + GAP + ADD_WIDTH;
        inputWidth = Math.min(MAX_INPUT_WIDTH, maxWidth - buttonsWidth);
        inputX = centerX - (inputWidth + buttonsWidth) / 2;
        int heldX = inputX + inputWidth + GAP;
        int buttonX = heldX + HELD_WIDTH + GAP;

        input = new GuiTextField(font, inputX, 0, inputWidth, BUTTON_HEIGHT);
        input.setMaxStringLength(MAX_ID_LENGTH);
        input.setText(text);
        input.setFocused(focused);
        updateInputColor();
        hint = font.trimStringToWidth(
            StatCollector.translateToLocal("dashboard.rendering.hint"),
            inputWidth - HINT_PADDING * 2);
        addButton = new GuiButton(
            BUTTON_ADD,
            buttonX,
            0,
            ADD_WIDTH,
            BUTTON_HEIGHT,
            StatCollector.translateToLocal("dashboard.rendering.add"));
        heldButton = new IconButton(BUTTON_HELD, heldX, 0, HELD_WIDTH, BUTTON_HEIGHT, Icons.HELD);
        heldButton.enabled = getHeldItem() != null;
        resetButton = new GuiButton(
            BUTTON_RESET,
            0,
            0,
            resetWidth,
            BUTTON_HEIGHT,
            StatCollector.translateToLocal("dashboard.customize.reset"));

        String[] entries = DashboardConfig.hiddenHeldItems;
        removeButtons = new GuiButton[entries.length];
        entryTexts = new String[entries.length];
        entryKnown = new boolean[entries.length];
        for (int i = 0; i < entries.length; i++) {
            removeButtons[i] = new IconButton(BUTTON_REMOVE + i, buttonX, 0, REMOVE_WIDTH, BUTTON_HEIGHT, Icons.REMOVE);
            ItemId id = HiddenHeldItems.parse(entries[i]);
            entryKnown[i] = id != null;
            String detail = id != null ? getItemName(id)
                : StatCollector.translateToLocal("dashboard.rendering.unknown");
            entryTexts[i] = font.trimStringToWidth(entries[i] + EnumChatFormatting.GRAY + "  " + detail, inputWidth);
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

    GuiButton getResetButton() {
        return resetButton;
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

    /** Null if the button is not one of this section's held or remove buttons. */
    List<String> getTooltip(GuiButton button) {
        if (button.id == BUTTON_HELD) {
            return Collections.singletonList(StatCollector.translateToLocal("dashboard.rendering.held.tooltip"));
        }
        if (button.id < BUTTON_REMOVE || button.id >= BUTTON_REMOVE + removeButtons.length) {
            return null;
        }
        return Collections.singletonList(StatCollector.translateToLocal("dashboard.rendering.remove"));
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
            int textY = y + (BUTTON_HEIGHT - font.FONT_HEIGHT) / 2 + 1;
            font.drawString(hint, inputX + HINT_PADDING, textY, ColorUtils.textHint.getColor());
        }
        inputDrawn = true;
    }

    void drawEntry(int index, int y) {
        int textY = y + (BUTTON_HEIGHT - font.FONT_HEIGHT) / 2 + 1;
        int color = entryKnown[index] ? ColorUtils.textInput.getColor() : ColorUtils.textUnknown.getColor();
        font.drawStringWithShadow(entryTexts[index], inputX, textY, color);
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

    /** False if the button is not one of this section's. */
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
