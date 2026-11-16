package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.gtnewhorizons.dashboard.DashboardConfig;
import com.gtnewhorizons.dashboard.api.AvoidSide;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Moves an element out of the way while another element would cover it. Uses the element's own avoid list, or every
 * element on screen with All Elements on. Does nothing in the editor or while the setting is off.
 */
@SideOnly(Side.CLIENT)
final class AvoidOverlap {

    private static final int GAP = 2;
    /** Stops elements that avoid each other from asking each other forever. */
    private static final int MAX_DEPTH = 4;
    private static final String MINECRAFT_PREFIX = "minecraft:";

    // Toward the middle of the screen first, when the element picked no sides
    private static final AvoidSide[] FROM_TOP_LEFT = { AvoidSide.BELOW, AvoidSide.RIGHT, AvoidSide.LEFT,
        AvoidSide.ABOVE };
    private static final AvoidSide[] FROM_TOP_RIGHT = { AvoidSide.BELOW, AvoidSide.LEFT, AvoidSide.RIGHT,
        AvoidSide.ABOVE };
    private static final AvoidSide[] FROM_BOTTOM_LEFT = { AvoidSide.ABOVE, AvoidSide.RIGHT, AvoidSide.LEFT,
        AvoidSide.BELOW };
    private static final AvoidSide[] FROM_BOTTOM_RIGHT = { AvoidSide.ABOVE, AvoidSide.LEFT, AvoidSide.RIGHT,
        AvoidSide.BELOW };

    private static final Map<HudElement, HudBounds> MOVED = new IdentityHashMap<>();
    private static final HudBounds NOT_MOVED = new HudBounds(0, 0, 0, 0);
    private static int movedFrame = -1;
    private static int movedScreenWidth;
    private static int movedScreenHeight;

    private AvoidOverlap() {}

    /** The same bounds when nothing is in the way or no side fits. */
    static HudBounds apply(HudElement element, HudBounds bounds, int screenWidth, int screenHeight, int depth) {
        if (!DashboardConfig.avoidOverlap || element.isNeverMove()
            || depth >= MAX_DEPTH
            || HudRegistry.isLayoutScreenOpen()) {
            return bounds;
        }
        boolean listed = HudLayout.getAvoid(element).length > 0;
        if (!listed && !DashboardConfig.avoidOverlapAuto) {
            return bounds;
        }
        clearIfOutdated(screenWidth, screenHeight);
        HudBounds known = MOVED.get(element);
        if (known != null) {
            return known == NOT_MOVED ? bounds : known;
        }
        List<HudBounds> inTheWay = listed ? findListed(element, screenWidth, screenHeight, depth)
            : findAuto(element, screenWidth, screenHeight, depth);
        HudBounds moved = inTheWay == null ? bounds : moveAside(element, bounds, inTheWay, screenWidth, screenHeight);
        MOVED.put(element, moved == bounds ? NOT_MOVED : moved);
        return moved;
    }

    private static void clearIfOutdated(int screenWidth, int screenHeight) {
        int frame = HudFrames.current();
        if (frame != movedFrame || screenWidth != movedScreenWidth || screenHeight != movedScreenHeight) {
            MOVED.clear();
            movedFrame = frame;
            movedScreenWidth = screenWidth;
            movedScreenHeight = screenHeight;
        }
    }

    /** The elements on its own avoid list that are on screen. */
    private static List<HudBounds> findListed(HudElement element, int screenWidth, int screenHeight, int depth) {
        String[] ids = HudLayout.getAvoid(element);
        List<HudBounds> inTheWay = null;
        for (String id : ids) {
            HudElement other = HudRegistry.getElement(id);
            if (other == null || other == element || !other.isDrawnNow() || !HudLayout.isVisible(other)) {
                continue;
            }
            if (inTheWay == null) {
                inTheWay = new ArrayList<>(ids.length);
            }
            inTheWay.add(getBounds(other, screenWidth, screenHeight, depth));
        }
        return inTheWay;
    }

    /** Every element on screen it has to make room for. Attached elements belong together, so they are skipped. */
    private static List<HudBounds> findAuto(HudElement element, int screenWidth, int screenHeight, int depth) {
        if (staysPut(element) || !element.isDrawnNow()) {
            return null;
        }
        HudElement parent = HudLayout.getAttachedTo(element);
        List<HudBounds> inTheWay = null;
        for (HudElement other : HudRegistry.getElements()) {
            if (other == element || other == parent
                || !other.isDrawnNow()
                || !HudLayout.isVisible(other)
                || !makesWayFor(element, other)
                || HudLayout.getAttachedTo(other) == element) {
                continue;
            }
            if (inTheWay == null) {
                inTheWay = new ArrayList<>();
            }
            inTheWay.add(getBounds(other, screenWidth, screenHeight, depth));
        }
        return inTheWay;
    }

    /**
     * Who moves: nobody if both appeared together, never Minecraft's own, locked or never-move elements, otherwise the
     * one that appeared last.
     */
    private static boolean makesWayFor(HudElement element, HudElement other) {
        int shownSince = element.getShownSinceFrame();
        int otherShownSince = other.getShownSinceFrame();
        if (shownSince == otherShownSince) {
            return false;
        }
        return staysPut(other) || otherShownSince < shownSince;
    }

    private static boolean staysPut(HudElement element) {
        return element.isNeverMove() || element.getId()
            .startsWith(MINECRAFT_PREFIX) || HudLayout.isLocked(element);
    }

    /** Where the other element is now, after making room itself. */
    private static HudBounds getBounds(HudElement other, int screenWidth, int screenHeight, int depth) {
        HudBounds otherDefault = other.getDefaultBounds(screenWidth, screenHeight);
        return HudLayout.getBounds(other, otherDefault, screenWidth, screenHeight, depth + 1);
    }

    /** Tries each side in order and takes the first spot that fits on screen. */
    private static HudBounds moveAside(HudElement element, HudBounds bounds, List<HudBounds> inTheWay, int screenWidth,
        int screenHeight) {
        if (findOverlap(bounds, inTheWay) == null) {
            return bounds;
        }
        for (AvoidSide side : getSides(element, bounds, screenWidth, screenHeight)) {
            HudBounds moved = pushAside(bounds, side, inTheWay, screenWidth, screenHeight);
            if (moved != null) {
                return moved;
            }
        }
        return bounds;
    }

    /** The player's first side, then the mod's order, or the automatic one. */
    private static AvoidSide[] getSides(HudElement element, HudBounds bounds, int screenWidth, int screenHeight) {
        AvoidSide[] sides = element.getAvoidSides();
        if (sides == null) {
            sides = getAutomaticSides(bounds, screenWidth, screenHeight);
        }
        AvoidSide first = HudLayout.getAvoidFirstSide(element);
        if (first == null) {
            return sides;
        }
        List<AvoidSide> ordered = new ArrayList<>(Arrays.asList(sides));
        ordered.remove(first);
        ordered.add(0, first);
        return ordered.toArray(new AvoidSide[0]);
    }

    private static AvoidSide[] getAutomaticSides(HudBounds bounds, int screenWidth, int screenHeight) {
        boolean top = bounds.y + bounds.height / 2 < screenHeight / 2;
        boolean left = bounds.x + bounds.width / 2 < screenWidth / 2;
        if (top) {
            return left ? FROM_TOP_LEFT : FROM_TOP_RIGHT;
        }
        return left ? FROM_BOTTOM_LEFT : FROM_BOTTOM_RIGHT;
    }

    /** Pushes past each element in the way until it is clear. Null if it ends up off screen. */
    private static HudBounds pushAside(HudBounds bounds, AvoidSide side, List<HudBounds> inTheWay, int screenWidth,
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
