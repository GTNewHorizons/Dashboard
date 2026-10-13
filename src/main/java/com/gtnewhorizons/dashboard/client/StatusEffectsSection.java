package com.gtnewhorizons.dashboard.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizons.dashboard.DashboardConfig;

import cpw.mods.fml.client.config.GuiSlider;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The status effect settings in the Customize screen. */
@SideOnly(Side.CLIENT)
final class StatusEffectsSection extends CustomizeSection implements GuiSlider.ISlider {

    private static final int BUTTON_BACKGROUND = 101;
    private static final int BUTTON_ICON = 102;
    private static final int BUTTON_NAME = 103;
    private static final int BUTTON_TIME = 104;
    private static final int BUTTON_STACK = 105;
    private static final int BUTTON_RESET = 106;
    private static final int BUTTON_SPACING = 107;
    private static final int BUTTON_PULSE = 108;
    private static final int BUTTON_GROUP = 109;
    private static final int BUTTON_SORT = 110;
    private static final int BUTTON_PERMANENT = 111;

    private final GuiButton[][] rows;
    private final GuiSlider spacingSlider;
    private final GuiSlider pulseSlider;

    StatusEffectsSection(int leftX, int rightX, int buttonWidth, int resetWidth) {
        super(BUTTON_RESET, resetWidth);
        spacingSlider = addSlider(
            new GuiSlider(
                BUTTON_SPACING,
                rightX,
                0,
                buttonWidth,
                GuiHelpers.BUTTON_HEIGHT,
                getName(BUTTON_SPACING) + ": ",
                " px",
                StatusEffects.MIN_SPACING,
                StatusEffects.MAX_SPACING,
                DashboardConfig.statusEffectSpacing,
                false,
                true,
                this));
        pulseSlider = addSlider(
            new GuiSlider(
                BUTTON_PULSE,
                rightX,
                0,
                buttonWidth,
                GuiHelpers.BUTTON_HEIGHT,
                "",
                "",
                0,
                StatusEffects.MAX_PULSE_SECONDS / StatusEffects.PULSE_STEP_SECONDS,
                DashboardConfig.statusEffectPulseSeconds / StatusEffects.PULSE_STEP_SECONDS,
                false,
                true,
                this));
        pulseSlider.displayString = getPulseLabel();
        int[] leftColumn = { BUTTON_BACKGROUND, BUTTON_ICON, BUTTON_NAME, BUTTON_TIME, BUTTON_PERMANENT };
        int[] rightColumn = { BUTTON_STACK, BUTTON_GROUP, BUTTON_SORT };
        rows = new GuiButton[leftColumn.length][];
        for (int row = 0; row < leftColumn.length; row++) {
            GuiButton left = new GuiButton(leftColumn[row], leftX, 0, buttonWidth, GuiHelpers.BUTTON_HEIGHT, "");
            GuiButton right = row < rightColumn.length
                ? new GuiButton(rightColumn[row], rightX, 0, buttonWidth, GuiHelpers.BUTTON_HEIGHT, "")
                : row == rightColumn.length ? spacingSlider : pulseSlider;
            rows[row] = new GuiButton[] { left, right };
        }
        updateButtons();
        // Creating the sliders counts as a change
        changed = false;
    }

    /** Each row holds a left and a right button. */
    GuiButton[][] getRows() {
        return rows;
    }

    private void updateButtons() {
        for (GuiButton[] row : rows) {
            for (GuiButton button : row) {
                updateButton(button);
            }
        }
        resetButton.enabled = !isDefault();
    }

    private static void updateButton(GuiButton button) {
        if (button.id == BUTTON_STACK) {
            String stack = DashboardConfig.statusEffectHorizontal ? "horizontal" : "vertical";
            button.displayString = getName(button.id) + ": "
                + StatCollector.translateToLocal("dashboard.status_effects.stack." + stack);
        } else if (button.id == BUTTON_GROUP) {
            button.displayString = getName(button.id) + ": " + GuiHelpers.getOnOff(DashboardConfig.statusEffectGroup);
        } else if (button.id == BUTTON_PERMANENT) {
            String permanent = DashboardConfig.statusEffectPermanent ? "show" : "hide";
            button.displayString = getName(button.id) + ": "
                + StatCollector.translateToLocal("dashboard.status_effects.permanent." + permanent);
        } else if (button.id == BUTTON_SORT) {
            String sort = DashboardConfig.statusEffectSortByTime ? "time" : "added";
            button.displayString = getName(button.id) + ": "
                + StatCollector.translateToLocal("dashboard.status_effects.sort." + sort);
        } else if (button.id >= BUTTON_BACKGROUND && button.id <= BUTTON_TIME) {
            button.displayString = getName(button.id) + ": " + GuiHelpers.getOnOff(isOn(button.id));
            button.enabled = !isLastShownPart(button.id);
        }
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

    @Override
    boolean actionPerformed(GuiButton button) {
        switch (button.id) {
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
            case BUTTON_SPACING, BUTTON_PULSE -> {
                return true;
            }
            default -> {
                return false;
            }
        }
        ConfigurationManager.save(DashboardConfig.class);
        updateButtons();
        return true;
    }

    @Override
    public void onChangeSliderValue(GuiSlider slider) {
        if (slider.id == BUTTON_SPACING) {
            DashboardConfig.statusEffectSpacing = slider.getValueInt();
        } else {
            DashboardConfig.statusEffectPulseSeconds = slider.getValueInt() * StatusEffects.PULSE_STEP_SECONDS;
            slider.displayString = getPulseLabel();
        }
        changed = true;
        resetButton.enabled = !isDefault();
    }

    private static String getPulseLabel() {
        int seconds = DashboardConfig.statusEffectPulseSeconds;
        String value = seconds == 0 ? GuiHelpers.getOnOff(false)
            : StatCollector.translateToLocalFormatted("dashboard.status_effects.pulse.seconds", seconds);
        return getName(BUTTON_PULSE) + ": " + value;
    }
}
