package com.gtnewhorizons.dashboard.client;

import net.minecraft.client.gui.GuiChat;
import net.minecraftforge.client.event.GuiScreenEvent;

import org.lwjgl.input.Mouse;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizons.dashboard.api.HudElement;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Zooming into shown image elements and moving around in them while chat is open. Moving and resizing the elements
 * stays in the editor. Only over an image, so chat keeps its clicks and scrolling everywhere else.
 */
@SideOnly(Side.CLIENT)
@EventBusSubscriber(side = Side.CLIENT)
public final class ChatImageControls {

    private static final ImagePan PAN = new ImagePan();

    private ChatImageControls() {}

    /** Called by the mixin for each mouse event in chat. True if an image used it, so chat should not. */
    public static boolean handleMouseInput(int screenWidth, int screenHeight) {
        if (PAN.isActive()) {
            return true;
        }
        int mouseX = GuiHelpers.getEventMouseX(screenWidth);
        int mouseY = GuiHelpers.getEventMouseY(screenHeight);
        HudElement image = ImageElements.findShownImageAt(mouseX, mouseY, screenWidth, screenHeight);
        if (image == null) {
            return false;
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            ImageElements
                .zoom(image, HudLayout.getBounds(image, screenWidth, screenHeight), mouseX, mouseY, wheel > 0 ? 1 : -1);
            return true;
        }
        int button = Mouse.getEventButton();
        if (Mouse.getEventButtonState() && (button == ImagePan.LEFT_BUTTON || button == ImagePan.MIDDLE_BUTTON)) {
            PAN.start(image, button, mouseX, mouseY);
            return true;
        }
        return false;
    }

    /** Keeps moving around while the button is held, and outlines the image under the mouse. */
    @SubscribeEvent
    public static void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiChat)) {
            return;
        }
        int screenWidth = event.gui.width;
        int screenHeight = event.gui.height;
        if (PAN.isActive()) {
            PAN.update(event.mouseX, event.mouseY, screenWidth, screenHeight);
        }
        HudElement outlined = ImageElements.findShownImageAt(event.mouseX, event.mouseY, screenWidth, screenHeight);
        if (outlined != null) {
            GuiHudEditor.drawOutline(
                HudLayout.getBounds(outlined, screenWidth, screenHeight),
                ColorUtils.highlightBorder.getColor());
        }
    }
}
