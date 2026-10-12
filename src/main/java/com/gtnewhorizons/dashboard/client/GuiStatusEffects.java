package com.gtnewhorizons.dashboard.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizons.dashboard.DashboardConfig;

import cpw.mods.fml.client.config.GuiSlider;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Status effect customization gui. */
@SideOnly(Side.CLIENT)
public class GuiStatusEffects extends GuiScreen implements GuiSlider.ISlider {

    private static final int BUTTON_DONE = 0;
    private static final int BUTTON_BACKGROUND = 1;
    private static final int BUTTON_ICON = 2;
    private static final int BUTTON_NAME = 3;
    private static final int BUTTON_TIME = 4;
    private static final int BUTTON_STACK = 5;
    private static final int BUTTON_RESET = 6;
    private static final int BUTTON_SPACING = 7;
    private static final int BUTTON_PULSE = 8;
    private static final int BUTTON_GROUP = 9;
    private static final int BUTTON_SORT = 10;
    private static final int BUTTON_PERMANENT = 11;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_STEP = 22;
    private static final int COLUMN_GAP = 4;
    private static final int ROWS = 5;
    private static final int BOTTOM_BUTTON_WIDTH = 100;
    private static final int BOTTOM_BUTTON_GAP = 4;
    private static final int TITLE_Y = 8;
    private static final int TOP = 24;
    private static final int PREVIEW_GAP = 6;
    private static final int BOTTOM_MARGIN = 30;

    private final GuiScreen parent;
    private GuiButton resetButton;
    private GuiSlider spacingSlider;
    private GuiSlider pulseSlider;
    private boolean sliderChanged;

    public GuiStatusEffects(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int leftX = width / 2 - BUTTON_WIDTH - COLUMN_GAP / 2;
        int rightX = width / 2 + COLUMN_GAP / 2;
        int[] leftColumn = { BUTTON_BACKGROUND, BUTTON_ICON, BUTTON_NAME, BUTTON_TIME, BUTTON_PERMANENT };
        for (int row = 0; row < leftColumn.length; row++) {
            buttonList.add(new GuiButton(leftColumn[row], leftX, getRowY(row), BUTTON_WIDTH, BUTTON_HEIGHT, ""));
        }
        int[] rightColumn = { BUTTON_STACK, BUTTON_GROUP, BUTTON_SORT };
        for (int row = 0; row < rightColumn.length; row++) {
            buttonList.add(new GuiButton(rightColumn[row], rightX, getRowY(row), BUTTON_WIDTH, BUTTON_HEIGHT, ""));
        }
        spacingSlider = new GuiSlider(
            BUTTON_SPACING,
            rightX,
            getRowY(3),
            BUTTON_WIDTH,
            BUTTON_HEIGHT,
            getName(BUTTON_SPACING) + ": ",
            " px",
            StatusEffects.MIN_SPACING,
            StatusEffects.MAX_SPACING,
            DashboardConfig.statusEffectSpacing,
            false,
            true,
            this);
        buttonList.add(spacingSlider);
        pulseSlider = new GuiSlider(
            BUTTON_PULSE,
            rightX,
            getRowY(4),
            BUTTON_WIDTH,
            BUTTON_HEIGHT,
            "",
            "",
            0,
            StatusEffects.MAX_PULSE_SECONDS / StatusEffects.PULSE_STEP_SECONDS,
            DashboardConfig.statusEffectPulseSeconds / StatusEffects.PULSE_STEP_SECONDS,
            false,
            true,
            this);
        pulseSlider.displayString = getPulseLabel();
        buttonList.add(pulseSlider);
        int bottomY = height - BOTTOM_MARGIN + (BOTTOM_MARGIN - BUTTON_HEIGHT) / 2;
        int bottomX = width / 2 - BOTTOM_BUTTON_WIDTH - BOTTOM_BUTTON_GAP / 2;
        resetButton = new GuiButton(
            BUTTON_RESET,
            bottomX,
            bottomY,
            BOTTOM_BUTTON_WIDTH,
            BUTTON_HEIGHT,
            StatCollector.translateToLocal("dashboard.customize.reset"));
        buttonList.add(resetButton);
        buttonList.add(
            new GuiButton(
                BUTTON_DONE,
                bottomX + BOTTOM_BUTTON_WIDTH + BOTTOM_BUTTON_GAP,
                bottomY,
                BOTTOM_BUTTON_WIDTH,
                BUTTON_HEIGHT,
                StatCollector.translateToLocal("dashboard.editor.button.done")));
        updateButtons();
    }

    private static int getRowY(int row) {
        return TOP + row * BUTTON_STEP;
    }

    private void updateButtons() {
        for (Object entry : buttonList) {
            GuiButton button = (GuiButton) entry;
            if (button.id == BUTTON_STACK) {
                String stack = DashboardConfig.statusEffectHorizontal ? "horizontal" : "vertical";
                button.displayString = getName(button.id) + ": "
                    + StatCollector.translateToLocal("dashboard.status_effects.stack." + stack);
            } else if (button.id == BUTTON_GROUP) {
                button.displayString = getName(button.id) + ": " + getOnOff(DashboardConfig.statusEffectGroup);
            } else if (button.id == BUTTON_PERMANENT) {
                String permanent = DashboardConfig.statusEffectPermanent ? "show" : "hide";
                button.displayString = getName(button.id) + ": "
                    + StatCollector.translateToLocal("dashboard.status_effects.permanent." + permanent);
            } else if (button.id == BUTTON_SORT) {
                String sort = DashboardConfig.statusEffectSortByTime ? "time" : "added";
                button.displayString = getName(button.id) + ": "
                    + StatCollector.translateToLocal("dashboard.status_effects.sort." + sort);
            } else if (button.id >= BUTTON_BACKGROUND && button.id <= BUTTON_TIME) {
                button.displayString = getName(button.id) + ": " + getOnOff(isOn(button.id));
                button.enabled = !isLastShownPart(button.id);
            }
        }
        resetButton.enabled = !isDefault();
    }

    private static String getOnOff(boolean on) {
        return StatCollector.translateToLocal(on ? "dashboard.customize.number.on" : "dashboard.customize.number.off");
    }

    private static String getName(int buttonId) {
        String key = switch (buttonId) {
            case BUTTON_BACKGROUND -> "background";
            case BUTTON_ICON -> "icon";
            case BUTTON_NAME -> "name";
            case BUTTON_TIME -> "time";
            case BUTTON_STACK -> "stack";
            case BUTTON_SPACING -> "spacing";
            case BUTTON_GROUP -> "group";
            case BUTTON_SORT -> "sort";
            case BUTTON_PERMANENT -> "permanent";
            default -> "pulse";
        };
        return StatCollector.translateToLocal("dashboard.status_effects." + key);
    }

    private static boolean isOn(int buttonId) {
        return switch (buttonId) {
            case BUTTON_BACKGROUND -> DashboardConfig.statusEffectBackground;
            case BUTTON_ICON -> DashboardConfig.statusEffectIcon;
            case BUTTON_NAME -> DashboardConfig.statusEffectName;
            default -> DashboardConfig.statusEffectTime;
        };
    }

    /** Icon, name and time can not all be hidden, or the effects would be empty :<. */
    private static boolean isLastShownPart(int buttonId) {
        if (buttonId == BUTTON_BACKGROUND || !isOn(buttonId)) {
            return false;
        }
        int shown = 0;
        for (int id = BUTTON_ICON; id <= BUTTON_TIME; id++) {
            if (isOn(id)) {
                shown++;
            }
        }
        return shown == 1;
    }

    private static boolean isDefault() {
        return DashboardConfig.statusEffectBackground && DashboardConfig.statusEffectIcon
            && DashboardConfig.statusEffectName
            && DashboardConfig.statusEffectTime
            && !DashboardConfig.statusEffectHorizontal
            && DashboardConfig.statusEffectSpacing == StatusEffects.DEFAULT_SPACING
            && DashboardConfig.statusEffectPulseSeconds == StatusEffects.DEFAULT_PULSE_SECONDS
            && DashboardConfig.statusEffectGroup
            && !DashboardConfig.statusEffectSortByTime
            && DashboardConfig.statusEffectPermanent;
    }

    private void toggle(int buttonId) {
        switch (buttonId) {
            case BUTTON_BACKGROUND -> DashboardConfig.statusEffectBackground = !DashboardConfig.statusEffectBackground;
            case BUTTON_ICON -> DashboardConfig.statusEffectIcon = !DashboardConfig.statusEffectIcon;
            case BUTTON_NAME -> DashboardConfig.statusEffectName = !DashboardConfig.statusEffectName;
            case BUTTON_TIME -> DashboardConfig.statusEffectTime = !DashboardConfig.statusEffectTime;
            case BUTTON_STACK -> DashboardConfig.statusEffectHorizontal = !DashboardConfig.statusEffectHorizontal;
            case BUTTON_GROUP -> DashboardConfig.statusEffectGroup = !DashboardConfig.statusEffectGroup;
            case BUTTON_SORT -> DashboardConfig.statusEffectSortByTime = !DashboardConfig.statusEffectSortByTime;
            case BUTTON_PERMANENT -> DashboardConfig.statusEffectPermanent = !DashboardConfig.statusEffectPermanent;
            case BUTTON_RESET -> {
                DashboardConfig.statusEffectBackground = true;
                DashboardConfig.statusEffectIcon = true;
                DashboardConfig.statusEffectName = true;
                DashboardConfig.statusEffectTime = true;
                DashboardConfig.statusEffectHorizontal = false;
                DashboardConfig.statusEffectGroup = true;
                DashboardConfig.statusEffectSortByTime = false;
                DashboardConfig.statusEffectPermanent = true;
                spacingSlider.setValue(StatusEffects.DEFAULT_SPACING);
                spacingSlider.updateSlider();
                pulseSlider.setValue(StatusEffects.DEFAULT_PULSE_SECONDS / StatusEffects.PULSE_STEP_SECONDS);
                pulseSlider.updateSlider();
            }
            default -> {}
        }
        ConfigurationManager.save(DashboardConfig.class);
        updateButtons();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(
            fontRendererObj,
            StatCollector.translateToLocal("dashboard.status_effects.title"),
            width / 2,
            TITLE_Y,
            ColorUtils.text.getColor());
        int previewY = getRowY(ROWS) + PREVIEW_GAP;
        StatusEffects.drawSamples(width / 2 - StatusEffects.getSamplesWidth() / 2, previewY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BUTTON_DONE) {
            mc.displayGuiScreen(parent);
        } else {
            toggle(button.id);
        }
    }

    /** Saved when the screen closes, since dragging changes it every step. */
    @Override
    public void onChangeSliderValue(GuiSlider slider) {
        if (slider.id == BUTTON_SPACING) {
            DashboardConfig.statusEffectSpacing = slider.getValueInt();
        } else {
            DashboardConfig.statusEffectPulseSeconds = slider.getValueInt() * StatusEffects.PULSE_STEP_SECONDS;
            slider.displayString = getPulseLabel();
        }
        sliderChanged = true;
        if (resetButton != null) {
            resetButton.enabled = !isDefault();
        }
    }

    private static String getPulseLabel() {
        int seconds = DashboardConfig.statusEffectPulseSeconds;
        String value = seconds == 0 ? StatCollector.translateToLocal("dashboard.customize.number.off")
            : StatCollector.translateToLocalFormatted("dashboard.status_effects.pulse.seconds", seconds);
        return getName(BUTTON_PULSE) + ": " + value;
    }

    @Override
    public void onGuiClosed() {
        if (sliderChanged) {
            ConfigurationManager.save(DashboardConfig.class);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            mc.displayGuiScreen(parent);
        }
    }
}
