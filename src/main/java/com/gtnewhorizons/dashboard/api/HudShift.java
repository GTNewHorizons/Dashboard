package com.gtnewhorizons.dashboard.api;

import net.minecraft.client.gui.ScaledResolution;

import com.gtnewhorizons.dashboard.client.PreviewDrawing;

/**
 * Remembers whether a move was started, for code that starts and ends it in separate places, like two mixin hooks.
 * Use one per element.
 */
public final class HudShift {

    private boolean shifted;
    private boolean invisible;

    /** False means the element is hidden, so its drawing should be skipped. */
    public boolean begin(HudElement element) {
        shifted = HudRegistry.beginRender(element);
        return shifted;
    }

    public boolean begin(HudElement element, ScaledResolution resolution) {
        shifted = HudRegistry.beginRender(element, resolution);
        return shifted;
    }

    /**
     * For drawing that can not be skipped, like one of several things in the same method: moved when shown, drawn
     * invisibly when hidden. Call {@link #end()} after it either way.
     */
    public void beginOrHide(HudElement element) {
        begin(element);
        invisible = !shifted;
        if (invisible) {
            PreviewDrawing.setColorWrites(false);
        }
    }

    /** Safe to call also when the drawing stopped early or the move was never started. */
    public void end() {
        if (shifted) {
            shifted = false;
            HudRegistry.endRender();
        }
        if (invisible) {
            invisible = false;
            PreviewDrawing.setColorWrites(true);
        }
    }
}
