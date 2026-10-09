package com.gtnewhorizon.dashboard.client;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;

import com.gtnewhorizon.dashboard.api.HudBounds;
import com.gtnewhorizon.dashboard.api.HudEditor;
import com.gtnewhorizon.dashboard.api.HudElement;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Vanilla HUD elements.
 */
@SideOnly(Side.CLIENT)
@EventBusSubscriber(side = Side.CLIENT)
public final class VanillaHudElements {

    private static final int BAR_WIDTH = 182;

    private static final Map<ElementType, HudElement> ELEMENTS = new EnumMap<>(ElementType.class);
    private static final Set<ElementType> SHIFTED = EnumSet.noneOf(ElementType.class);

    private VanillaHudElements() {}

    public static void register() {
        add(ElementType.HOTBAR, "hotbar", (w, h) -> new HudBounds(w / 2 - 91, h - 22, BAR_WIDTH, 22));
        add(ElementType.EXPERIENCE, "experience", (w, h) -> new HudBounds(w / 2 - 91, h - 29, BAR_WIDTH, 5));
        add(ElementType.JUMPBAR, "jump_bar", (w, h) -> new HudBounds(w / 2 - 91, h - 29, BAR_WIDTH, 5));
    }

    private static void add(ElementType type, String name, HudElement.DefaultBoundsProvider bounds) {
        String id = "minecraft:" + name;
        ELEMENTS.put(type, HudEditor.register(new HudElement(id, "dashboard.element.minecraft." + name, bounds)));
    }

    /**
     * Runs first, so mods that draw their own version of an element are shifted too, and hiding an element also hides
     * their version.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPreRender(RenderGameOverlayEvent.Pre event) {
        HudElement element = ELEMENTS.get(event.type);
        if (element == null) {
            return;
        }
        endAllShifts();

        if (HudEditor.beginRender(element, event.resolution)) {
            SHIFTED.add(event.type);
        } else {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPostRender(RenderGameOverlayEvent.Post event) {
        if (event.type == ElementType.ALL) {
            endAllShifts();
        } else {
            endShift(event.type);
        }
    }

    private static void endShift(ElementType type) {
        if (SHIFTED.remove(type)) {
            HudEditor.endRender();
        }
    }

    private static void endAllShifts() {
        for (int i = 0; i < SHIFTED.size(); i++) {
            HudEditor.endRender();
        }
        SHIFTED.clear();
    }
}


// Pre HOTBAR: no leftover. Save, shift up 30px, Add shifted(30). [If hidden = cancel POST]
// Draw in normal spot, then add 30 pixels.
// Post HOTBAR: remove HOTBAR, restore the position. Remove shifted.
