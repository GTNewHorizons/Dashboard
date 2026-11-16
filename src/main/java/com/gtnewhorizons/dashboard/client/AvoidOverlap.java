package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.List;

import com.gtnewhorizons.dashboard.api.AvoidSide;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * An element moves out of the way of the elements it avoids, only while one of them is on screen and would overlap
 * it. Off in the editor, so elements are arranged at their own spots there.
 */
@SideOnly(Side.CLIENT)
final class AvoidOverlap {

    private static final int GAP = 2;
    /** Elements avoiding each other in a circle would ask each other forever, and real chains are short. */
    private static final int MAX_DEPTH = 4;

    // Toward the middle of the screen first, for elements that did not pick their sides
    private static final AvoidSide[] FROM_TOP_LEFT = { AvoidSide.BELOW, AvoidSide.RIGHT, AvoidSide.LEFT,
        AvoidSide.ABOVE };
    private static final AvoidSide[] FROM_TOP_RIGHT = { AvoidSide.BELOW, AvoidSide.LEFT, AvoidSide.RIGHT,
        AvoidSide.ABOVE };
    private static final AvoidSide[] FROM_BOTTOM_LEFT = { AvoidSide.ABOVE, AvoidSide.RIGHT, AvoidSide.LEFT,
        AvoidSide.BELOW };
    private static final AvoidSide[] FROM_BOTTOM_RIGHT = { AvoidSide.ABOVE, AvoidSide.LEFT, AvoidSide.RIGHT,
        AvoidSide.BELOW };

    private AvoidOverlap() {}

    /** The same bounds when nothing is in the way or no side fits. */
    static HudBounds apply(HudElement element, HudBounds bounds, int screenWidth, int screenHeight, int depth) {
        String[] ids = element.getAvoid();
        if (ids.length == 0 || depth >= MAX_DEPTH || HudRegistry.isLayoutScreenOpen()) {
            return bounds;
        }
        List<HudBounds> inTheWay = null;
        for (String id : ids) {
            HudElement other = HudRegistry.getElement(id);
            if (other == null || other == element || !other.isDrawnNow() || !HudLayout.isVisible(other)) {
                continue;
            }
            if (inTheWay == null) {
                inTheWay = new ArrayList<>(ids.length);
            }
            HudBounds otherDefault = other.getDefaultBounds(screenWidth, screenHeight);
            inTheWay.add(HudLayout.getBounds(other, otherDefault, screenWidth, screenHeight, depth + 1));
        }
        if (inTheWay == null || findOverlap(bounds, inTheWay) == null) {
            return bounds;
        }
        for (AvoidSide side : getSides(element, bounds, screenWidth, screenHeight)) {
            HudBounds moved = moveAside(bounds, side, inTheWay, screenWidth, screenHeight);
            if (moved != null) {
                return moved;
            }
        }
        return bounds;
    }

    private static AvoidSide[] getSides(HudElement element, HudBounds bounds, int screenWidth, int screenHeight) {
        AvoidSide[] sides = element.getAvoidSides();
        if (sides != null) {
            return sides;
        }
        boolean top = bounds.y + bounds.height / 2 < screenHeight / 2;
        boolean left = bounds.x + bounds.width / 2 < screenWidth / 2;
        if (top) {
            return left ? FROM_TOP_LEFT : FROM_TOP_RIGHT;
        }
        return left ? FROM_BOTTOM_LEFT : FROM_BOTTOM_RIGHT;
    }

    /**
     * Pushed past the element in the way, again past the next one it lands on. Each push only goes further the same
     * way, so it passes each element at most once. Null when it ends up off screen.
     */
    private static HudBounds moveAside(HudBounds bounds, AvoidSide side, List<HudBounds> inTheWay, int screenWidth,
        int screenHeight) {
        HudBounds moved = bounds;
        HudBounds overlap = findOverlap(moved, inTheWay);
        while (overlap != null) {
            moved = pushPast(moved, overlap, side);
            if (moved.x < 0 || moved.y < 0 || moved.getRight() > screenWidth || moved.getBottom() > screenHeight) {
                return null;
            }
            overlap = findOverlap(moved, inTheWay);
        }
        return moved;
    }

    private static HudBounds pushPast(HudBounds bounds, HudBounds other, AvoidSide side) {
        return switch (side) {
            case BELOW -> new HudBounds(bounds.x, other.getBottom() + GAP, bounds.width, bounds.height);
            case ABOVE -> new HudBounds(bounds.x, other.y - GAP - bounds.height, bounds.width, bounds.height);
            case RIGHT -> new HudBounds(other.getRight() + GAP, bounds.y, bounds.width, bounds.height);
            case LEFT -> new HudBounds(other.x - GAP - bounds.width, bounds.y, bounds.width, bounds.height);
        };
    }

    private static HudBounds findOverlap(HudBounds bounds, List<HudBounds> others) {
        for (HudBounds other : others) {
            if (bounds.getOverlapArea(other) > 0) {
                return other;
            }
        }
        return null;
    }
}
