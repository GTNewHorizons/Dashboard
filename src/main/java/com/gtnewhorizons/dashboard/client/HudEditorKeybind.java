package com.gtnewhorizons.dashboard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

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

    public static final KeyBinding RESET_POSITION = new KeyBinding(
        "key.dashboard.reset_position",
        Keyboard.KEY_R,
        "key.categories.dashboard_editor");

    public static final KeyBinding SIZE_MODIFIER = new KeyBinding(
        "key.dashboard.size_modifier",
        Keyboard.KEY_LMENU,
        "key.categories.dashboard_editor");

    public static final KeyBinding OPACITY_MODIFIER = new KeyBinding(
        "key.dashboard.opacity_modifier",
        Keyboard.KEY_LCONTROL,
        "key.categories.dashboard_editor");

    public static final KeyBinding FREE_MOVE_MODIFIER = new KeyBinding(
        "key.dashboard.free_move_modifier",
        Keyboard.KEY_LSHIFT,
        "key.categories.dashboard_editor");

    private HudEditorKeybind() {}

    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN_EDITOR);
        ClientRegistry.registerKeyBinding(TOGGLE_IMAGES);
        ClientRegistry.registerKeyBinding(RESET_POSITION);
        ClientRegistry.registerKeyBinding(SIZE_MODIFIER);
        ClientRegistry.registerKeyBinding(OPACITY_MODIFIER);
        ClientRegistry.registerKeyBinding(FREE_MOVE_MODIFIER);
    }

    /** Reads the key itself, since key bindings do not update while a screen is open. Mouse buttons are below 0. */
    static boolean isHeld(KeyBinding binding) {
        int keyCode = binding.getKeyCode();
        if (keyCode < 0) {
            return Mouse.isButtonDown(keyCode + 100);
        }
        return keyCode != Keyboard.KEY_NONE && Keyboard.isKeyDown(keyCode);
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
