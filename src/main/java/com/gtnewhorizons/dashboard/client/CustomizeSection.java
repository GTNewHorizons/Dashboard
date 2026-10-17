package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizons.dashboard.DashboardConfig;

import cpw.mods.fml.client.config.GuiSlider;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** One part of the Customize screen, so the screen can handle every section the same way. */
@SideOnly(Side.CLIENT)
abstract class CustomizeSection extends Gui {

    protected final GuiButton resetButton;
    protected final List<GuiSlider> sliders = new ArrayList<>();
    protected boolean changed;

    CustomizeSection(int resetId, int resetWidth) {
        resetButton = GuiHelpers
            .button(resetId, 0, resetWidth, StatCollector.translateToLocal("dashboard.customize.reset"));
    }

    GuiButton getResetButton() {
        return resetButton;
    }

    /** False if the button is not one of this section's. */
    abstract boolean actionPerformed(GuiButton button);

    /** Null if the button is not one of this section's, or has no tooltip. */
    List<String> getTooltip(GuiButton button) {
        return null;
    }

    /** Whole numbers with the value shown, like every slider in Customize. */
    protected GuiSlider addSlider(int id, int x, int width, String prefix, String suffix, double min, double max,
        double value, GuiSlider.ISlider listener) {
        GuiSlider slider = new GuiSlider(
            id,
            x,
            0,
            width,
            GuiHelpers.BUTTON_HEIGHT,
            prefix,
            suffix,
            min,
            max,
            value,
            false,
            true,
            listener);
        sliders.add(slider);
        return slider;
    }

    boolean isDragging() {
        for (GuiSlider slider : sliders) {
            if (slider.dragging) {
                return true;
            }
        }
        return false;
    }

    void releaseSliders(int mouseX, int mouseY) {
        for (GuiSlider slider : sliders) {
            slider.mouseReleased(mouseX, mouseY);
        }
    }

    void save() {
        if (changed) {
            ConfigurationManager.save(DashboardConfig.class);
            changed = false;
        }
    }
}
