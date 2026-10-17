package com.gtnewhorizons.dashboard.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizons.dashboard.DashboardConfig;
import com.gtnewhorizons.dashboard.api.HudBounds;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The hotbar slot number settings in the Customize screen. */
@SideOnly(Side.CLIENT)
final class HotbarSection extends CustomizeSection {

    private static final int BUTTON_RESET = 301;
    private static final int BUTTON_NUMBERS = 302;
    private static final int BUTTON_TEXT = 303;
    private static final int BUTTON_POSITION = 304;
    private static final int BUTTON_SIZE = 305;
    private static final int BUTTON_COLOR = 306;
    private static final int BUTTON_LAYER = 307;
    private static final int SIZE_STEP = 25;
    private static final int HOTBAR_WIDTH = 182;
    private static final int HOTBAR_HEIGHT = 22;
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
    private static final ItemStack[] SAMPLE_ITEMS = { new ItemStack(Items.diamond_sword),
        new ItemStack(Items.iron_pickaxe), new ItemStack(Items.bread, 16), new ItemStack(Blocks.torch, 64),
        new ItemStack(Blocks.cobblestone, 32) };

    private final GuiButton[][] rows;

    HotbarSection(int leftX, int rightX, int buttonWidth, int resetWidth) {
        super(BUTTON_RESET, resetWidth);
        rows = new GuiButton[][] {
            { GuiHelpers.button(BUTTON_NUMBERS, leftX, buttonWidth),
                GuiHelpers.button(BUTTON_TEXT, rightX, buttonWidth) },
            { GuiHelpers.button(BUTTON_POSITION, leftX, buttonWidth),
                GuiHelpers.button(BUTTON_SIZE, rightX, buttonWidth) },
            { GuiHelpers.button(BUTTON_COLOR, leftX, buttonWidth),
                GuiHelpers.button(BUTTON_LAYER, rightX, buttonWidth) } };
        updateButtons();
    }

    /** Each row holds a left and a right button. */
    GuiButton[][] getRows() {
        return rows;
    }

    private void updateButtons() {
        boolean on = DashboardConfig.hotbarNumbers;
        for (GuiButton[] row : rows) {
            for (GuiButton button : row) {
                button.displayString = getName(button.id) + ": " + getValueName(button.id);
                button.enabled = on || button.id == BUTTON_NUMBERS;
            }
        }
        resetButton.enabled = !isDefault();
    }

    private static String getName(int buttonId) {
        String key = switch (buttonId) {
            case BUTTON_NUMBERS -> "numbers";
            case BUTTON_TEXT -> "text";
            case BUTTON_POSITION -> "position";
            case BUTTON_SIZE -> "size";
            case BUTTON_LAYER -> "layer";
            default -> "color";
        };
        return StatCollector.translateToLocal("dashboard.hotbar." + key);
    }

    private static String getValueName(int buttonId) {
        return switch (buttonId) {
            case BUTTON_NUMBERS -> GuiHelpers.getOnOff(DashboardConfig.hotbarNumbers);
            case BUTTON_TEXT -> StatCollector.translateToLocal(
                DashboardConfig.hotbarNumberBoundKey ? "dashboard.hotbar.text.bound_key"
                    : "dashboard.hotbar.text.numbers");
            case BUTTON_POSITION -> StatCollector
                .translateToLocal("dashboard.hotbar.position." + HotbarNumbers.getPosition());
            case BUTTON_SIZE -> HotbarNumbers.getSize() + "%";
            case BUTTON_LAYER -> StatCollector.translateToLocal(
                DashboardConfig.hotbarNumberUnderItems ? "dashboard.hotbar.layer.under_items"
                    : "dashboard.hotbar.layer.over_items");
            default -> HotbarNumbers.getColor() + StatCollector.translateToLocal(
                "dashboard.hotbar.color." + HotbarNumbers.getColor()
                    .ordinal());
        };
    }

    private static boolean isDefault() {
        return !DashboardConfig.hotbarNumbers && DashboardConfig.hotbarNumberBoundKey
            && !DashboardConfig.hotbarNumberUnderItems
            && DashboardConfig.hotbarNumberPosition == 0
            && DashboardConfig.hotbarNumberSize == HotbarNumbers.DEFAULT_SIZE
            && DashboardConfig.hotbarNumberColor == HotbarNumbers.DEFAULT_COLOR;
    }

    /** Shift goes back, Ctrl goes to the default. */
    @Override
    boolean actionPerformed(GuiButton button) {
        boolean reset = GuiScreen.isCtrlKeyDown();
        int step = GuiHelpers.getClickStep();
        switch (button.id) {
            case BUTTON_NUMBERS -> DashboardConfig.hotbarNumbers = !reset && !DashboardConfig.hotbarNumbers;
            case BUTTON_TEXT -> DashboardConfig.hotbarNumberBoundKey = reset || !DashboardConfig.hotbarNumberBoundKey;
            case BUTTON_LAYER -> DashboardConfig.hotbarNumberUnderItems = !reset
                && !DashboardConfig.hotbarNumberUnderItems;
            case BUTTON_POSITION -> DashboardConfig.hotbarNumberPosition = reset ? 0
                : GuiHelpers.wrap(HotbarNumbers.getPosition() + step, HotbarNumbers.POSITION_COUNT);
            case BUTTON_SIZE -> {
                int sizes = (HotbarNumbers.MAX_SIZE - HotbarNumbers.MIN_SIZE) / SIZE_STEP + 1;
                int index = (HotbarNumbers.getSize() - HotbarNumbers.MIN_SIZE) / SIZE_STEP;
                DashboardConfig.hotbarNumberSize = reset ? HotbarNumbers.DEFAULT_SIZE
                    : HotbarNumbers.MIN_SIZE + GuiHelpers.wrap(index + step, sizes) * SIZE_STEP;
            }
            case BUTTON_COLOR -> DashboardConfig.hotbarNumberColor = reset ? HotbarNumbers.DEFAULT_COLOR
                : GuiHelpers.wrap(
                    HotbarNumbers.getColor()
                        .ordinal() + step,
                    HotbarNumbers.COLOR_COUNT);
            case BUTTON_RESET -> {
                DashboardConfig.hotbarNumbers = false;
                DashboardConfig.hotbarNumberBoundKey = true;
                DashboardConfig.hotbarNumberUnderItems = false;
                DashboardConfig.hotbarNumberPosition = 0;
                DashboardConfig.hotbarNumberSize = HotbarNumbers.DEFAULT_SIZE;
                DashboardConfig.hotbarNumberColor = HotbarNumbers.DEFAULT_COLOR;
            }
            default -> {
                return false;
            }
        }
        ConfigurationManager.save(DashboardConfig.class);
        updateButtons();
        return true;
    }

    /** The keys are drawn by the same code as in game, so the preview always matches. */
    void drawPreview(int centerX, int y) {
        int x = centerX - HOTBAR_WIDTH / 2;
        PreviewDrawing.bindTexture(WIDGETS);
        PreviewDrawing.drawTexture(x, y, 0, 0, HOTBAR_WIDTH, HOTBAR_HEIGHT);
        PreviewDrawing.drawTexture(x - 1, y - 1, 0, HOTBAR_HEIGHT, 24, 24);
        HudBounds bounds = new HudBounds(x, y, HOTBAR_WIDTH, HOTBAR_HEIGHT);
        if (DashboardConfig.hotbarNumberUnderItems) {
            HotbarNumbers.draw(bounds);
        }

        for (int slot = 0; slot < SAMPLE_ITEMS.length; slot++) {
            PreviewDrawing.drawItem(SAMPLE_ITEMS[slot], x + 3 + slot * 20, y + 3);
        }

        if (!DashboardConfig.hotbarNumberUnderItems) {
            HotbarNumbers.draw(bounds);
        }
    }
}
