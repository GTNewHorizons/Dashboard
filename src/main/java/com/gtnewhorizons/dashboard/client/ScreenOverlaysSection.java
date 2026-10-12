package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizons.dashboard.DashboardConfig;

import cpw.mods.fml.client.config.GuiSlider;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The screen overlay and camera sliders of the Customize screen, each overlay with a preview button. */
@SideOnly(Side.CLIENT)
final class ScreenOverlaysSection extends Gui implements GuiSlider.ISlider {

    private static final int BUTTON_RESET = 201;
    private static final int BUTTON_PUMPKIN_OPACITY = 202;
    private static final int BUTTON_PORTAL_OPACITY = 203;
    private static final int BUTTON_FIRE_OPACITY = 204;
    private static final int BUTTON_FIRE_HEIGHT = 205;
    private static final int BUTTON_WATER_OPACITY = 206;
    private static final int BUTTON_VIGNETTE_STRENGTH = 207;
    private static final int BUTTON_SLEEP_FADE_STRENGTH = 208;
    private static final int BUTTON_HURT_SHAKE_STRENGTH = 209;
    private static final int BUTTON_NAUSEA_STRENGTH = 210;
    private static final int BUTTON_NIGHT_VISION_FLICKER = 211;
    private static final int PREVIEW_OFFSET = 100;
    private static final int MAX_SLIDER_WIDTH = 200;
    private static final int PREVIEW_BUTTON_WIDTH = 20;
    private static final int PREVIEW_GAP = 4;
    private static final int COLUMN_GAP = 8;
    private static final int BUTTON_HEIGHT = 20;
    private static final ResourceLocation PUMPKIN_BLUR = new ResourceLocation("textures/misc/pumpkinblur.png");
    private static final ResourceLocation UNDERWATER = new ResourceLocation("textures/misc/underwater.png");
    private static final ResourceLocation VIGNETTE = new ResourceLocation("textures/misc/vignette.png");
    /** Vanilla repeats the water texture 4 times across the screen. */
    private static final int WATER_REPEATS = 4;
    /** Vanilla's sleep fade at its darkest. */
    private static final int SLEEP_FADE_COLOR = 220 << 24 | 0x101020;

    private final GuiButton[][] rows;
    private final List<GuiSlider> sliders = new ArrayList<>();
    private final List<PreviewButton> previewButtons = new ArrayList<>();
    private final GuiButton resetButton;
    private final GuiButton nightVisionButton;
    private final int overlaysCenterX;
    private final int cameraCenterX;
    private boolean changed;
    /** The slider whose overlay is previewed, or 0. */
    private int previewedSlider;

    /** Two columns centered on centerX, narrower sliders if maxWidth is small. */
    ScreenOverlaysSection(int centerX, int maxWidth, int resetWidth) {
        int sliderWidth = Math.min(MAX_SLIDER_WIDTH, (maxWidth - PREVIEW_GAP - PREVIEW_BUTTON_WIDTH - COLUMN_GAP) / 2);
        int overlaysWidth = sliderWidth + PREVIEW_GAP + PREVIEW_BUTTON_WIDTH;
        int overlaysX = centerX - (overlaysWidth + COLUMN_GAP + sliderWidth) / 2;
        int cameraX = overlaysX + overlaysWidth + COLUMN_GAP;
        overlaysCenterX = overlaysX + overlaysWidth / 2;
        cameraCenterX = cameraX + sliderWidth / 2;

        resetButton = new GuiButton(
            BUTTON_RESET,
            0,
            0,
            resetWidth,
            BUTTON_HEIGHT,
            StatCollector.translateToLocal("dashboard.customize.reset"));
        nightVisionButton = new GuiButton(BUTTON_NIGHT_VISION_FLICKER, cameraX, 0, sliderWidth, BUTTON_HEIGHT, "");
        List<GuiButton> camera = new ArrayList<>();
        camera.add(createSlider(BUTTON_HURT_SHAKE_STRENGTH, cameraX, sliderWidth));
        camera.add(createSlider(BUTTON_NAUSEA_STRENGTH, cameraX, sliderWidth));
        camera.add(nightVisionButton);

        int overlayCount = BUTTON_SLEEP_FADE_STRENGTH - BUTTON_PUMPKIN_OPACITY + 1;
        rows = new GuiButton[overlayCount][];
        for (int row = 0; row < overlayCount; row++) {
            int id = BUTTON_PUMPKIN_OPACITY + row;
            GuiSlider slider = createSlider(id, overlaysX, sliderWidth);
            PreviewButton preview = new PreviewButton(id + PREVIEW_OFFSET, overlaysX + sliderWidth + PREVIEW_GAP);
            previewButtons.add(preview);
            rows[row] = row < camera.size() ? new GuiButton[] { slider, preview, camera.get(row) }
                : new GuiButton[] { slider, preview };
        }
        updateNightVisionButton();
        updatePreviewButtons();
        resetButton.enabled = !isDefault();
        // Creating the sliders counts as a change
        changed = false;
    }

    private GuiSlider createSlider(int id, int x, int width) {
        GuiSlider slider = new GuiSlider(
            id,
            x,
            0,
            width,
            BUTTON_HEIGHT,
            getName(id) + ": ",
            "%",
            0,
            ScreenOverlays.MAX_PERCENT,
            getValue(id),
            false,
            true,
            this);
        sliders.add(slider);
        return slider;
    }

    GuiButton getResetButton() {
        return resetButton;
    }

    /** Each row holds an overlay slider and its preview button, the first rows also a camera setting. */
    GuiButton[][] getRows() {
        return rows;
    }

    void drawColumnTitles(FontRenderer font, int y) {
        int color = ColorUtils.text.getColor();
        drawCenteredString(
            font,
            StatCollector.translateToLocal("dashboard.screen_overlays.overlays"),
            overlaysCenterX,
            y,
            color);
        drawCenteredString(
            font,
            StatCollector.translateToLocal("dashboard.screen_overlays.camera"),
            cameraCenterX,
            y,
            color);
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

    /** Sliders are saved here, since dragging changes them every step. */
    void save() {
        if (changed) {
            ConfigurationManager.save(DashboardConfig.class);
            changed = false;
        }
    }

    /** Null if the button is not one of this section's preview buttons. */
    List<String> getTooltip(GuiButton button) {
        if (!(button instanceof PreviewButton)) {
            return null;
        }
        return Collections.singletonList(StatCollector.translateToLocal("dashboard.screen_overlays.preview"));
    }

    private void updateNightVisionButton() {
        String state = StatCollector.translateToLocal(
            DashboardConfig.nightVisionFlicker ? "dashboard.customize.number.on" : "dashboard.customize.number.off");
        nightVisionButton.displayString = getName(BUTTON_NIGHT_VISION_FLICKER) + ": " + state;
    }

    private void updatePreviewButtons() {
        for (PreviewButton button : previewButtons) {
            button.enabled = button.id - PREVIEW_OFFSET == previewedSlider;
        }
    }

    private static String getName(int buttonId) {
        String key = switch (buttonId) {
            case BUTTON_PUMPKIN_OPACITY -> "pumpkin_opacity";
            case BUTTON_PORTAL_OPACITY -> "portal_opacity";
            case BUTTON_FIRE_OPACITY -> "fire_opacity";
            case BUTTON_FIRE_HEIGHT -> "fire_height";
            case BUTTON_WATER_OPACITY -> "water_opacity";
            case BUTTON_VIGNETTE_STRENGTH -> "vignette_strength";
            case BUTTON_SLEEP_FADE_STRENGTH -> "sleep_fade_strength";
            case BUTTON_HURT_SHAKE_STRENGTH -> "hurt_shake_strength";
            case BUTTON_NAUSEA_STRENGTH -> "nausea_strength";
            default -> "night_vision_flicker";
        };
        return StatCollector.translateToLocal("dashboard.screen_overlays." + key);
    }

    private static int getValue(int buttonId) {
        return switch (buttonId) {
            case BUTTON_PUMPKIN_OPACITY -> DashboardConfig.pumpkinOpacity;
            case BUTTON_PORTAL_OPACITY -> DashboardConfig.portalOpacity;
            case BUTTON_FIRE_OPACITY -> DashboardConfig.fireOpacity;
            case BUTTON_FIRE_HEIGHT -> DashboardConfig.fireHeight;
            case BUTTON_WATER_OPACITY -> DashboardConfig.waterOpacity;
            case BUTTON_VIGNETTE_STRENGTH -> DashboardConfig.vignetteStrength;
            case BUTTON_SLEEP_FADE_STRENGTH -> DashboardConfig.sleepFadeStrength;
            case BUTTON_HURT_SHAKE_STRENGTH -> DashboardConfig.hurtShakeStrength;
            default -> DashboardConfig.nauseaStrength;
        };
    }

    private static boolean isDefault() {
        return DashboardConfig.pumpkinOpacity == ScreenOverlays.MAX_PERCENT
            && DashboardConfig.portalOpacity == ScreenOverlays.MAX_PERCENT
            && DashboardConfig.fireOpacity == ScreenOverlays.MAX_PERCENT
            && DashboardConfig.fireHeight == ScreenOverlays.MAX_PERCENT
            && DashboardConfig.waterOpacity == ScreenOverlays.MAX_PERCENT
            && DashboardConfig.vignetteStrength == ScreenOverlays.MAX_PERCENT
            && DashboardConfig.sleepFadeStrength == ScreenOverlays.MAX_PERCENT
            && DashboardConfig.hurtShakeStrength == ScreenOverlays.MAX_PERCENT
            && DashboardConfig.nauseaStrength == ScreenOverlays.MAX_PERCENT
            && DashboardConfig.nightVisionFlicker;
    }

    @Override
    public void onChangeSliderValue(GuiSlider slider) {
        int value = slider.getValueInt();
        switch (slider.id) {
            case BUTTON_PUMPKIN_OPACITY -> DashboardConfig.pumpkinOpacity = value;
            case BUTTON_PORTAL_OPACITY -> DashboardConfig.portalOpacity = value;
            case BUTTON_FIRE_OPACITY -> DashboardConfig.fireOpacity = value;
            case BUTTON_FIRE_HEIGHT -> DashboardConfig.fireHeight = value;
            case BUTTON_WATER_OPACITY -> DashboardConfig.waterOpacity = value;
            case BUTTON_VIGNETTE_STRENGTH -> DashboardConfig.vignetteStrength = value;
            case BUTTON_SLEEP_FADE_STRENGTH -> DashboardConfig.sleepFadeStrength = value;
            case BUTTON_HURT_SHAKE_STRENGTH -> DashboardConfig.hurtShakeStrength = value;
            default -> DashboardConfig.nauseaStrength = value;
        }
        changed = true;
        resetButton.enabled = !isDefault();
    }

    /** False if the button is not one of this section's. */
    boolean actionPerformed(GuiButton button) {
        if (button.id == BUTTON_NIGHT_VISION_FLICKER) {
            DashboardConfig.nightVisionFlicker = !DashboardConfig.nightVisionFlicker;
            changed = true;
            updateNightVisionButton();
            resetButton.enabled = !isDefault();
        } else if (button instanceof PreviewButton) {
            int slider = button.id - PREVIEW_OFFSET;
            previewedSlider = previewedSlider == slider ? 0 : slider;
            updatePreviewButtons();
        } else if (button.id == BUTTON_RESET) {
            for (GuiSlider slider : sliders) {
                slider.setValue(ScreenOverlays.MAX_PERCENT);
                slider.updateSlider();
            }
            DashboardConfig.nightVisionFlicker = true;
            changed = true;
            updateNightVisionButton();
            resetButton.enabled = false;
        } else {
            return sliders.contains(button);
        }
        return true;
    }

    boolean isPreviewing() {
        return previewedSlider != 0;
    }

    /** Over the world instead of the dark background, with the current slider values. */
    void drawPreview(Minecraft mc, int width, int height) {
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        switch (previewedSlider) {
            case BUTTON_PUMPKIN_OPACITY -> {
                GL11.glColor4f(1, 1, 1, ScreenOverlays.getPumpkinOpacity());
                mc.getTextureManager()
                    .bindTexture(PUMPKIN_BLUR);
                func_146110_a(0, 0, 0, 0, width, height, width, height);
            }
            case BUTTON_PORTAL_OPACITY -> {
                GL11.glColor4f(1, 1, 1, ScreenOverlays.getPortalOpacity());
                mc.getTextureManager()
                    .bindTexture(TextureMap.locationBlocksTexture);
                drawTexturedModelRectFromIcon(0, 0, Blocks.portal.getBlockTextureFromSide(1), width, height);
            }
            case BUTTON_FIRE_OPACITY, BUTTON_FIRE_HEIGHT -> drawFirePreview(mc, width, height);
            case BUTTON_WATER_OPACITY -> {
                GL11.glColor4f(1, 1, 1, 0.5f * ScreenOverlays.getWaterOpacity());
                mc.getTextureManager()
                    .bindTexture(UNDERWATER);
                func_146110_a(0, 0, 0, 0, width, height, width / (float) WATER_REPEATS, height / (float) WATER_REPEATS);
            }
            case BUTTON_VIGNETTE_STRENGTH -> {
                float strength = ScreenOverlays.getVignetteStrength();
                OpenGlHelper.glBlendFunc(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR, 1, 0);
                GL11.glColor4f(strength, strength, strength, 1);
                mc.getTextureManager()
                    .bindTexture(VIGNETTE);
                func_146110_a(0, 0, 0, 0, width, height, width, height);
                OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            }
            default -> drawRect(
                0,
                0,
                width,
                height,
                ScreenOverlays.scaleAlpha(SLEEP_FADE_COLOR, ScreenOverlays.getSleepFadeStrength()));
        }
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glColor4f(1, 1, 1, 1);
    }

    /** A flat copy of the two flames vanilla draws in front of the camera. */
    private void drawFirePreview(Minecraft mc, int width, int height) {
        // One block of the flames, seen half a block in front of the camera
        int size = height * 10 / 7;
        int y = height / 2 - size / 5 + Math.round(ScreenOverlays.getFireDrop() * size);
        int offset = size * 24 / 100;
        IIcon flame = Blocks.fire.getFireIcon(1);
        GL11.glColor4f(1, 1, 1, 0.9f * ScreenOverlays.getFireOpacity());
        mc.getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        drawTexturedModelRectFromIcon(width / 2 - offset - size / 2, y, flame, size, size);
        drawTexturedModelRectFromIcon(width / 2 + offset - size / 2, y, flame, size, size);
    }

    /** Greyed out while its preview is off, but still clickable. */
    private static final class PreviewButton extends GuiButton {

        private PreviewButton(int id, int x) {
            super(id, x, 0, PREVIEW_BUTTON_WIDTH, BUTTON_HEIGHT, "P");
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            return visible && mouseX >= xPosition
                && mouseY >= yPosition
                && mouseX < xPosition + width
                && mouseY < yPosition + height;
        }
    }
}
