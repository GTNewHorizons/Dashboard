package com.gtnewhorizon.dashboard.client;

/**
 * What the player changed about one HUD element. Saved as JSON.
 * <p>
 * A moved element is placed relative to an anchor:
 * <p>
 * - 0 is the left (or top) screen edge
 * <p>
 * - 0.5 is the center
 * <p>
 * - 1 is the right(or bottom) edge
 * <p>
 * The offset is the distance from that anchor, so the element stays near the same edge when the
 * window size or GUI scale changes.
 */
public class HudElementState {

    public boolean visible = true;
    public boolean moved = false;
    public float anchorX;
    public float anchorY;
    public int offsetX;
    public int offsetY;

    boolean isDefault() {
        return visible && !moved;
    }
}
