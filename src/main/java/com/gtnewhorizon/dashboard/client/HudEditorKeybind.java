package com.gtnewhorizon.dashboard.client;

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
        Keyboard.KEY_NUMPAD7,
        "key.categories.dashboard");

    private HudEditorKeybind() {}

    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN_EDITOR);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (OPEN_EDITOR.isPressed() && mc.currentScreen == null && mc.theWorld != null) {
            mc.displayGuiScreen(new GuiHudEditor());
        }
    }
}
