package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
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

    private static final Map<HudElement, HudBounds> MOVED = new IdentityHashMap<>();
    private static final Map<HudElement, HudBounds> FINAL_BOUNDS = new IdentityHashMap<>();
    private static final Map<HudElement, ShownArea> AREAS = new IdentityHashMap<>();
    private static final HudBounds NOT_MOVED = new HudBounds(0, 0, 0, 0);
    private static int movedFrame = -1;
    private static int movedScreenWidth;
    private static int movedScreenHeight;

    static {
        HudRegistry.addLayoutListener(AREAS::clear);
    }

    private AvoidOverlap() {}

    /** The same bounds when nothing is in the way or no side fits. Cheapest checks first, it runs for every element. */
    static HudBounds apply(HudElement element, HudBounds bounds, int screenWidth, int screenHeight, int depth) {
        if (!DashboardConfig.avoidOverlap || element.isNeverMove()
            || depth >= MAX_DEPTH
            || HudRegistry.isLayoutScreenOpen()) {
            return bounds;
        }
        String[] avoid = HudLayout.getAvoid(element);
        boolean listed = avoid.length > 0;
        if (!listed && (!DashboardConfig.avoidOverlapAuto || HudLayout.hasAvoidChoice(element))) {
            return bounds;
        }
        clearIfOutdated(screenWidth, screenHeight);
        HudBounds known = MOVED.get(element);
        if (known != null) {
            return known == NOT_MOVED ? bounds : known;
        }
        // Locked comes first: a locked element stays put, also with its own list
        List<HudBounds> inTheWay = HudLayout.isLocked(element) ? null
            : listed ? findListed(element, avoid, screenWidth, screenHeight, depth)
                : findAuto(element, screenWidth, screenHeight, depth);
        HudBounds moved = inTheWay == null ? bounds
            : moveAside(element, bounds, listed, inTheWay, screenWidth, screenHeight);
        MOVED.put(element, moved == bounds ? NOT_MOVED : moved);
        return moved;
    }

    /**
     * For the editor: where it would move if every element it makes room for showed at its own spot. With All
     * Elements, it is treated as the one that showed up last. Takes the boxes the editor already measured this frame.
     */
    static HudBounds preview(HudElement element, HudBounds bounds, Map<HudElement, HudBounds> boxes, int screenWidth,
        int screenHeight) {
        if (!DashboardConfig.avoidOverlap || element.isNeverMove() || HudLayout.isLocked(element)) {
            return bounds;
        }
        String[] avoid = HudLayout.getAvoid(element);
        boolean listed = avoid.length > 0;
        if (!listed && (!DashboardConfig.avoidOverlapAuto || HudLayout.hasAvoidChoice(element) || staysPut(element))) {
            return bounds;
        }
        List<HudBounds> inTheWay = new ArrayList<>();
        if (listed) {
            for (String id : avoid) {
                HudElement other = HudRegistry.getElement(id);
                if (other != null && other != element && HudLayout.isVisible(other)) {
                    HudBounds otherBounds = boxes.get(other);
                    inTheWay
                        .add(otherBounds != null ? otherBounds : HudLayout.getBounds(other, screenWidth, screenHeight));
                }
            }
        } else {
            // On screen now = showed up together with the others, so the smaller one moves
            boolean showing = element.isCurrentlyShowing();
            for (Map.Entry<HudElement, HudBounds> box : boxes.entrySet()) {
                HudElement other = box.getKey();
                HudBounds otherBounds = box.getValue();
                if (other == element || !HudLayout.isVisible(other) || HudLayout.isAttachedPair(element, other)) {
                    continue;
                }
                if (!showing || staysPut(other)
                    || (other.isCurrentlyShowing()
                        && losesTie(element, getArea(bounds), other, getArea(otherBounds)))) {
                    inTheWay.add(otherBounds);
                }
            }
        }
        return inTheWay.isEmpty() ? bounds : moveAside(element, bounds, listed, inTheWay, screenWidth, screenHeight);
    }

    private static void clearIfOutdated(int screenWidth, int screenHeight) {
        int frame = HudFrames.current();
        boolean resized = screenWidth != movedScreenWidth || screenHeight != movedScreenHeight;
        if (frame != movedFrame || resized) {
            MOVED.clear();
            FINAL_BOUNDS.clear();
            if (resized) {
                AREAS.clear();
            }
            movedFrame = frame;
            movedScreenWidth = screenWidth;
            movedScreenHeight = screenHeight;
        }
    }

    /** The elements on its own avoid list that are on screen. */
    private static List<HudBounds> findListed(HudElement element, String[] ids, int screenWidth, int screenHeight,
        int depth) {
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
        List<HudBounds> inTheWay = null;
        for (HudElement other : HudRegistry.getElements()) {
            if (other == element || !other.isDrawnNow()
                || !HudLayout.isVisible(other)
                || !makesWayFor(element, other, screenWidth, screenHeight)
                || HudLayout.isAttachedPair(element, other)) {
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
     * Who moves: never Minecraft's own, locked or never-move elements, otherwise the one that appeared last. When both
     * appeared together, like after joining, the smaller one.
     */
    private static boolean makesWayFor(HudElement element, HudElement other, int screenWidth, int screenHeight) {
        if (staysPut(other)) {
            return true;
        }
        int shownSince = element.getShownSinceFrame();
        int otherShownSince = other.getShownSinceFrame();
        if (shownSince != otherShownSince) {
            return otherShownSince < shownSince;
        }
        return losesTie(
            element,
            getArea(element, screenWidth, screenHeight),
            other,
            getArea(other, screenWidth, screenHeight));
    }

    /** The smaller one moves, it changes the screen the least. Same size: the id decides, so it is always the same. */
    private static boolean losesTie(HudElement element, int area, HudElement other, int otherArea) {
        return area != otherArea ? area < otherArea
            : element.getId()
                .compareTo(other.getId()) > 0;
    }

    /** Its size when it started showing, so one that grows later does not swap places with the other. */
    private static int getArea(HudElement element, int screenWidth, int screenHeight) {
        int shownSince = element.getShownSinceFrame();
        ShownArea known = AREAS.get(element);
        if (known != null && known.shownSince == shownSince) {
            return known.area;
        }
        HudBounds defaultBounds = element.getDefaultBounds(screenWidth, screenHeight);
        float scale = HudLayout.getScale(element);
        int area = Math.round(defaultBounds.width * scale) * Math.round(defaultBounds.height * scale);
        AREAS.put(element, new ShownArea(shownSince, area));
        return area;
    }

    private static int getArea(HudBounds bounds) {
        return bounds.width * bounds.height;
    }

    private static boolean staysPut(HudElement element) {
        return element.isNeverMove() || element.getId()
            .startsWith(MINECRAFT_PREFIX) || HudLayout.isLocked(element);
    }

    /** Where the other element is now, after making room itself. Measured once per frame. */
    private static HudBounds getBounds(HudElement other, int screenWidth, int screenHeight, int depth) {
        HudBounds known = FINAL_BOUNDS.get(other);
        if (known != null) {
            return known;
        }
        HudBounds otherDefault = other.getDefaultBounds(screenWidth, screenHeight);
        HudBounds bounds = HudLayout.getBounds(other, otherDefault, screenWidth, screenHeight, depth + 1);
        FINAL_BOUNDS.put(other, bounds);
        return bounds;
    }

    /**
     * The player's side first. Then the mod's sides in order if it set any, otherwise the closest free spot. Only
     * spots fully on screen count.
     */
    private static HudBounds moveAside(HudElement element, HudBounds bounds, boolean listed, List<HudBounds> inTheWay,
        int screenWidth, int screenHeight) {
        if (findOverlap(bounds, inTheWay) == null) {
            return bounds;
        }
        AvoidSide first = listed ? HudLayout.getAvoidFirstSide(element) : null;
        if (first != null) {
            HudBounds moved = pushAside(bounds, first, inTheWay, screenWidth, screenHeight);
            if (moved != null) {
                return moved;
            }
        }
        AvoidSide[] modSides = element.getAvoidSides();
        if (modSides != null) {
            for (AvoidSide side : modSides) {
                HudBounds moved = side == first ? null : pushAside(bounds, side, inTheWay, screenWidth, screenHeight);
                if (moved != null) {
                    return moved;
                }
            }
            return bounds;
        }
        return findClosestSpot(bounds, first, inTheWay, screenWidth, screenHeight);
    }

    /** The side where it has to move the least, so it stays as close to its own spot as it can. */
    private static HudBounds findClosestSpot(HudBounds bounds, AvoidSide tried, List<HudBounds> inTheWay,
        int screenWidth, int screenHeight) {
        HudBounds closest = bounds;
        int closestDistance = Integer.MAX_VALUE;
        for (AvoidSide side : AvoidSide.values()) {
            HudBounds moved = side == tried ? null : pushAside(bounds, side, inTheWay, screenWidth, screenHeight);
            if (moved == null) {
                continue;
            }
            int distance = Math.abs(moved.x - bounds.x) + Math.abs(moved.y - bounds.y);
            if (distance < closestDistance) {
                closest = moved;
                closestDistance = distance;
            }
        }
        return closest;
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

    private static final class ShownArea {

        final int shownSince;
        final int area;

        ShownArea(int shownSince, int area) {
            this.shownSince = shownSince;
            this.area = area;
        }
    }
}
