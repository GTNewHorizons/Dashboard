package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.List;

import com.gtnewhorizons.dashboard.compat.AppleCoreCompat;

/**
 * Where each bar sits on a theme sheet (512 x 512), see docs/theme_template.png. Every slot is the bar plus 8px of
 * space around it for flair.
 */
public enum BarSlot {

    // u, v / bar width, height / fills from the right / wide
    EXPERIENCE(0, 0, 182, 5, false, true),
    JUMP_BAR(0, 42, 182, 5, false, true),
    BOSS_HEALTH(0, 84, 182, 5, false, true),
    HEALTH(0, 126, 81, 9, false, false),
    HEALTH_POISONED(0, 151, 81, 9, false, false),
    HEALTH_WITHERED(0, 176, 81, 9, false, false),
    HEALTH_HARDCORE(0, 201, 81, 9, false, false),
    HEALTH_FLASH(0, 226, 81, 9, false, false),
    HEALTH_ABSORPTION(0, 276, 81, 9, false, false),
    ARMOR(0, 301, 81, 9, false, false),
    FOOD(0, 326, 81, 9, true, false),
    FOOD_HUNGER(0, 351, 81, 9, true, false),
    AIR(0, 376, 81, 9, true, false),
    MOUNT_HEALTH(0, 401, 81, 9, true, false),

    HEALTH_FLASH_POISONED(0, 251, 81, 9, false),
    HEALTH_FLASH_WITHERED(97, 251, 81, 9, false),
    HEALTH_REGEN(0, 426, 9, 9, false),
    AIR_POP(25, 426, 9, 9, true),
    FOOD_SATURATION(97, 426, 81, 9, true),
    FOOD_EXHAUSTION(194, 426, 81, 9, true);

    public enum Layer {
        EMPTY,
        FULL,
        ORNAMENT,
        ORNAMENT_ACTIVE
    }

    private static final int ROW_SLOT_WIDTH = 97;
    private static final int WIDE_SLOT_WIDTH = 198;
    private static final int WIDE_SLOT_HEIGHT = 21;

    private final int u;
    private final int v;
    public final int width;
    public final int height;
    public final boolean fromRight;
    private final boolean wide;
    private final boolean single;

    BarSlot(int u, int v, int width, int height, boolean fromRight, boolean wide) {
        this(u, v, width, height, fromRight, wide, false);
    }

    BarSlot(int u, int v, int width, int height, boolean fromRight) {
        this(u, v, width, height, fromRight, false, true);
    }

    BarSlot(int u, int v, int width, int height, boolean fromRight, boolean wide, boolean single) {
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
        this.fromRight = fromRight;
        this.wide = wide;
        this.single = single;
    }

    /** The slot's top left corner, with its 8px space. */
    public int getU(Layer layer) {
        if (single) {
            return u;
        }
        return wide ? u + layer.ordinal() % 2 * WIDE_SLOT_WIDTH : u + layer.ordinal() * ROW_SLOT_WIDTH;
    }

    public int getV(Layer layer) {
        return wide ? v + layer.ordinal() / 2 * WIDE_SLOT_HEIGHT : v;
    }

    public BarSlot getFlashFill() {
        return switch (this) {
            case HEALTH_POISONED -> HEALTH_FLASH_POISONED;
            case HEALTH_WITHERED -> HEALTH_FLASH_WITHERED;
            default -> HEALTH_FLASH;
        };
    }

    public BarSlot[] getStates() {
        return switch (this) {
            case HEALTH -> new BarSlot[] { HEALTH_POISONED, HEALTH_WITHERED, HEALTH_ABSORPTION, HEALTH_FLASH,
                HEALTH_FLASH_POISONED, HEALTH_FLASH_WITHERED, HEALTH_REGEN, HEALTH_HARDCORE };
            case FOOD -> getFoodStates();
            case AIR -> new BarSlot[] { AIR_POP };
            default -> new BarSlot[0];
        };
    }

    private static BarSlot[] getFoodStates() {
        List<BarSlot> states = new ArrayList<>();
        states.add(FOOD_HUNGER);
        if (AppleCoreCompat.showsSaturation()) {
            states.add(FOOD_SATURATION);
        }
        if (AppleCoreCompat.showsExhaustion()) {
            states.add(FOOD_EXHAUSTION);
        }
        return states.toArray(new BarSlot[0]);
    }
}
