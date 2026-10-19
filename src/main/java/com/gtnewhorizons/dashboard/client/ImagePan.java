package com.gtnewhorizons.dashboard.client;

import org.lwjgl.input.Mouse;

import com.gtnewhorizons.dashboard.api.HudElement;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Moving around inside a zoomed image while a mouse button is held. A middle click without moving resets the zoom.
 */
@SideOnly(Side.CLIENT)
final class ImagePan {

    static final int LEFT_BUTTON = 0;
    static final int MIDDLE_BUTTON = 2;

    private HudElement image;
    private int button;
    private int lastX;
    private int lastY;
    private boolean moved;

    boolean isActive() {
        return image != null;
    }

    void start(HudElement image, int button, int mouseX, int mouseY) {
        this.image = image;
        this.button = button;
        lastX = mouseX;
        lastY = mouseY;
        moved = false;
    }

    /** Call every frame while active. */
    void update(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (!Mouse.isButtonDown(button)) {
            if (!moved && button == MIDDLE_BUTTON) {
                ImageElements.resetZoom(image);
                GuiHelpers.playClickSound();
            }
            image = null;
            return;
        }
        if (mouseX != lastX || mouseY != lastY) {
            ImageElements
                .pan(image, HudLayout.getBounds(image, screenWidth, screenHeight), mouseX - lastX, mouseY - lastY);
            lastX = mouseX;
            lastY = mouseY;
            moved = true;
        }
    }
}
