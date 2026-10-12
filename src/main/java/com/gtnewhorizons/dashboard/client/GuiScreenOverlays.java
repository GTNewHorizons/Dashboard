package com.gtnewhorizons.dashboard.client;

import java.util.Collections;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
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

/** Screen overlay sliders, each with a preview. */
@SideOnly(Side.CLIENT)
public class GuiScreenOverlays extends GuiScreen implements GuiSlider.ISlider {

    private static final int BUTTON_DONE = 0;
    private static final int BUTTON_RESET = 1;
    private static final int BUTTON_PUMPKIN_OPACITY = 2;
    private static final int BUTTON_PORTAL_OPACITY = 3;
    private static final int BUTTON_FIRE_OPACITY = 4;
    private static final int BUTTON_FIRE_HEIGHT = 5;
    private static final int BUTTON_WATER_OPACITY = 6;
    private static final int BUTTON_VIGNETTE_STRENGTH = 7;
    private static final int BUTTON_SLEEP_FADE_STRENGTH = 8;
    private static final int BUTTON_HURT_SHAKE_STRENGTH = 9;
    private static final int BUTTON_NAUSEA_STRENGTH = 10;
    private static final int BUTTON_NIGHT_VISION_FLICKER = 11;
    private static final int PREVIEW_OFFSET = 100;
    private static final int SLIDER_WIDTH = 200;
    private static final int PREVIEW_BUTTON_WIDTH = 20;
    private static final int PREVIEW_GAP = 4;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_STEP = 22;
    private static final int BOTTOM_BUTTON_WIDTH = 100;
    private static final int BOTTOM_BUTTON_GAP = 4;
    private static final int TITLE_Y = 8;
    private static final int TOP = 24;
    private static final int HEADER_HEIGHT = 12;
    private static final int COLUMN_GAP = 8;
    private static final int BOTTOM_MARGIN = 30;
    private static final ResourceLocation PUMPKIN_BLUR = new ResourceLocation("textures/misc/pumpkinblur.png");
    private static final ResourceLocation UNDERWATER = new ResourceLocation("textures/misc/underwater.png");
    private static final ResourceLocation VIGNETTE = new ResourceLocation("textures/misc/vignette.png");
    /** Vanilla repeats the water texture 4 times across the screen. */
    private static final int WATER_REPEATS = 4;
    /** Vanilla's sleep fade at its darkest. */
    private static final int SLEEP_FADE_COLOR = 220 << 24 | 0x101020;

    private final GuiScreen parent;
    private GuiButton resetButton;
    private GuiButton nightVisionButton;
    private int overlaysX;
    private int cameraX;
    private boolean changed;
    /** The slider whose overlay is previewed, or 0. */
    private int previewedSlider;

    public GuiScreenOverlays(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int overlaysWidth = SLIDER_WIDTH + PREVIEW_GAP + PREVIEW_BUTTON_WIDTH;
        overlaysX = width / 2 - (overlaysWidth + COLUMN_GAP + SLIDER_WIDTH) / 2;
        cameraX = overlaysX + overlaysWidth + COLUMN_GAP;
        for (int id = BUTTON_PUMPKIN_OPACITY; id <= BUTTON_SLEEP_FADE_STRENGTH; id++) {
            int y = getRowY(id - BUTTON_PUMPKIN_OPACITY);
            buttonList.add(new PreviewButton(id + PREVIEW_OFFSET, overlaysX + SLIDER_WIDTH + PREVIEW_GAP, y));
            buttonList.add(createSlider(id, overlaysX, y));
        }
        buttonList.add(createSlider(BUTTON_HURT_SHAKE_STRENGTH, cameraX, getRowY(0)));
        buttonList.add(createSlider(BUTTON_NAUSEA_STRENGTH, cameraX, getRowY(1)));
        nightVisionButton = new GuiButton(
            BUTTON_NIGHT_VISION_FLICKER,
            cameraX,
            getRowY(2),
            SLIDER_WIDTH,
            BUTTON_HEIGHT,
            "");
        buttonList.add(nightVisionButton);
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
        updateNightVisionButton();
        resetButton.enabled = !isDefault();
        updatePreviewButtons();
    }

    private GuiSlider createSlider(int id, int x, int y) {
        return new GuiSlider(
            id,
            x,
            y,
            SLIDER_WIDTH,
            BUTTON_HEIGHT,
            getName(id) + ": ",
            "%",
            0,
            ScreenOverlays.MAX_PERCENT,
            getValue(id),
            false,
            true,
            this);
    }

    private static int getRowY(int row) {
        return TOP + HEADER_HEIGHT + row * BUTTON_STEP;
    }

    private void updateNightVisionButton() {
        String state = StatCollector.translateToLocal(
            DashboardConfig.nightVisionFlicker ? "dashboard.customize.number.on" : "dashboard.customize.number.off");
        nightVisionButton.displayString = getName(BUTTON_NIGHT_VISION_FLICKER) + ": " + state;
    }

    private void updatePreviewButtons() {
        for (Object entry : buttonList) {
            if (entry instanceof PreviewButton button) {
                button.enabled = button.id - PREVIEW_OFFSET == previewedSlider;
            }
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

    /** Saved when the screen closes, since dragging changes it every step. */
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
        if (resetButton != null) {
            resetButton.enabled = !isDefault();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (previewedSlider == 0) {
            drawDefaultBackground();
        } else {
            drawPreview();
        }
        drawCenteredString(
            fontRendererObj,
            StatCollector.translateToLocal("dashboard.screen_overlays.title"),
            width / 2,
            TITLE_Y,
            ColorUtils.text.getColor());
        int overlaysCenter = overlaysX + (SLIDER_WIDTH + PREVIEW_GAP + PREVIEW_BUTTON_WIDTH) / 2;
        drawCenteredString(
            fontRendererObj,
            StatCollector.translateToLocal("dashboard.screen_overlays.overlays"),
            overlaysCenter,
            TOP,
            ColorUtils.text.getColor());
        drawCenteredString(
            fontRendererObj,
            StatCollector.translateToLocal("dashboard.screen_overlays.camera"),
            cameraX + SLIDER_WIDTH / 2,
            TOP,
            ColorUtils.text.getColor());
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (Object entry : buttonList) {
            if (entry instanceof PreviewButton button && button.func_146115_a()) {
                String preview = StatCollector.translateToLocal("dashboard.screen_overlays.preview");
                drawHoveringText(Collections.singletonList(preview), mouseX, mouseY, fontRendererObj);
            }
        }
    }

    /** Over the world instead of the dark background, with the current slider values. */
    private void drawPreview() {
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        switch (previewedSlider) {
            case BUTTON_PUMPKIN_OPACITY -> {
                GL11.glColor4f(1, 1, 1, ScreenOverlays.getPumpkinOpacity());
                mc.getTextureManager()
                    .bindTexture(PUMPKIN_BLUR);
                Gui.func_146110_a(0, 0, 0, 0, width, height, width, height);
            }
            case BUTTON_PORTAL_OPACITY -> {
                GL11.glColor4f(1, 1, 1, ScreenOverlays.getPortalOpacity());
                mc.getTextureManager()
                    .bindTexture(TextureMap.locationBlocksTexture);
                drawTexturedModelRectFromIcon(0, 0, Blocks.portal.getBlockTextureFromSide(1), width, height);
            }
            case BUTTON_FIRE_OPACITY, BUTTON_FIRE_HEIGHT -> drawFirePreview();
            case BUTTON_WATER_OPACITY -> {
                GL11.glColor4f(1, 1, 1, 0.5f * ScreenOverlays.getWaterOpacity());
                mc.getTextureManager()
                    .bindTexture(UNDERWATER);
                Gui.func_146110_a(
                    0,
                    0,
                    0,
                    0,
                    width,
                    height,
                    width / (float) WATER_REPEATS,
                    height / (float) WATER_REPEATS);
            }
            case BUTTON_VIGNETTE_STRENGTH -> {
                float strength = ScreenOverlays.getVignetteStrength();
                OpenGlHelper.glBlendFunc(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR, 1, 0);
                GL11.glColor4f(strength, strength, strength, 1);
                mc.getTextureManager()
                    .bindTexture(VIGNETTE);
                Gui.func_146110_a(0, 0, 0, 0, width, height, width, height);
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
    private void drawFirePreview() {
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

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BUTTON_DONE) {
            mc.displayGuiScreen(parent);
        } else if (button.id == BUTTON_NIGHT_VISION_FLICKER) {
            DashboardConfig.nightVisionFlicker = !DashboardConfig.nightVisionFlicker;
            changed = true;
            updateNightVisionButton();
            resetButton.enabled = !isDefault();
        } else if (button.id > PREVIEW_OFFSET) {
            int slider = button.id - PREVIEW_OFFSET;
            previewedSlider = previewedSlider == slider ? 0 : slider;
            updatePreviewButtons();
        } else if (button.id == BUTTON_RESET) {
            for (Object entry : buttonList) {
                if (entry instanceof GuiSlider slider) {
                    slider.setValue(ScreenOverlays.MAX_PERCENT);
                    slider.updateSlider();
                }
            }
            DashboardConfig.nightVisionFlicker = true;
            changed = true;
            updateNightVisionButton();
            resetButton.enabled = false;
        }
    }

    @Override
    public void onGuiClosed() {
        if (changed) {
            ConfigurationManager.save(DashboardConfig.class);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            mc.displayGuiScreen(parent);
        }
    }

    private static final class PreviewButton extends GuiButton {

        private PreviewButton(int id, int x, int y) {
            super(id, x, y, PREVIEW_BUTTON_WIDTH, BUTTON_HEIGHT, "P");
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
