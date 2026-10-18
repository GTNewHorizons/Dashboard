package com.gtnewhorizons.dashboard.compat;

import net.minecraft.client.gui.ScaledResolution;

import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

/** Remembers whether a move was started, for mixins that start and end it in separate hooks. */
final class ShiftTracker {

    private boolean shifted;
    private boolean invisible;

    /** False means the element is hidden, so its drawing should be skipped. */
    boolean begin(HudElement element) {
        shifted = HudEditor.beginRender(element);
        return shifted;
    }

    boolean begin(HudElement element, ScaledResolution resolution) {
        shifted = HudEditor.beginRender(element, resolution);
        return shifted;
    }

    /**
     * For drawing a mixin can not skip, like one of several things in the same method: moved when shown, drawn
     * invisibly when hidden. Call {@link #end()} after it either way.
     */
    void beginOrHide(HudElement element) {
        begin(element);
        invisible = !shifted;
        if (invisible) {
            PreviewDrawing.setColorWrites(false);
        }
    }

    /** Safe to call also when the drawing stopped early or the move was never started. */
    void end() {
        if (shifted) {
            shifted = false;
            HudEditor.endRender();
        }
        if (invisible) {
            invisible = false;
            PreviewDrawing.setColorWrites(true);
        }
    }
}
