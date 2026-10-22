package com.gtnewhorizons.dashboard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@EventBusSubscriber(side = Side.CLIENT)
public final class HudEditorKeybind {

    public static final KeyBinding OPEN_EDITOR = new KeyBinding(
        "key.dashboard.open_editor",
        Keyboard.KEY_NONE,
        "key.categories.dashboard");

    public static final KeyBinding TOGGLE_IMAGES = new KeyBinding(
        "key.dashboard.toggle_images",
        Keyboard.KEY_NONE,
        "key.categories.dashboard");

    private HudEditorKeybind() {}

    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN_EDITOR);
        ClientRegistry.registerKeyBinding(TOGGLE_IMAGES);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (OPEN_EDITOR.isPressed() && mc.currentScreen == null && mc.theWorld != null) {
            mc.displayGuiScreen(new GuiHudEditor());
        }
        if (TOGGLE_IMAGES.isPressed() && mc.currentScreen == null) {
            ImageElements.toggleShown();
        }
    }
}
