package com.gtnewhorizons.dashboard.client;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Some mods (like Waila and Galacticraft) draw their HUD in a render tick handler at the end of the frame, which is
 * after every screen, so it would cover the HUD editor. While the editor is open, their handler is skipped there and
 * run by the editor instead, before it draws its boxes. The whole handler runs, since some also do other work there.
 */
@SideOnly(Side.CLIENT)
public final class FrameEndHud {

    private static final Map<Class<?>, Consumer<TickEvent.RenderTickEvent>> HANDLERS = new LinkedHashMap<>();
    /** Handlers that failed in the editor, so they draw over it again instead of breaking it. */
    private static final Set<Class<?>> BROKEN = new HashSet<>();
    private static boolean runningInEditor;

    private FrameEndHud() {}

    /**
     * Called by the mixins at the start of a handler, which hand it over so the editor can run it.
     *
     * @return true to skip the handler, since the editor runs it
     */
    public static boolean skip(Object handler, Consumer<TickEvent.RenderTickEvent> run,
        TickEvent.RenderTickEvent event) {
        if (runningInEditor || event.phase != TickEvent.Phase.END || BROKEN.contains(handler.getClass())) {
            return false;
        }
        HANDLERS.put(handler.getClass(), run);
        return HudRegistry.isLayoutScreenOpen();
    }

    static void drawInEditor(float partialTicks) {
        runningInEditor = true;
        try {
            TickEvent.RenderTickEvent event = new TickEvent.RenderTickEvent(TickEvent.Phase.END, partialTicks);
            for (Map.Entry<Class<?>, Consumer<TickEvent.RenderTickEvent>> handler : HANDLERS.entrySet()) {
                if (BROKEN.contains(handler.getKey())) {
                    continue;
                }
                try {
                    handler.getValue()
                        .accept(event);
                } catch (RuntimeException e) {
                    BROKEN.add(handler.getKey());
                    Dashboard.LOG.error(
                        "{} failed in the HUD editor, it is drawn over the editor instead",
                        handler.getKey()
                            .getName(),
                        e);
                }
            }
        } finally {
            runningInEditor = false;
        }
    }
}
