package com.gtnewhorizon.dashboard.client;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.boss.BossStatus;
import net.minecraftforge.client.GuiIngameForge;
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
    private static final int ICON_ROW_WIDTH = 81;
    private static final int ICON_SIZE = 9;
    private static final int ROW_HEIGHT = 10;

    private static final Map<ElementType, HudElement> ELEMENTS = new EnumMap<>(ElementType.class);
    private static final Map<ElementType, StackedIconRow> STACKED_ROWS = new EnumMap<>(ElementType.class);
    private static final Set<ElementType> SHIFTED = EnumSet.noneOf(ElementType.class);

    private VanillaHudElements() {}

    public static void register() {
        add(ElementType.HOTBAR, "hotbar", (w, h) -> new HudBounds(w / 2 - 91, h - 22, BAR_WIDTH, 22));
        add(ElementType.EXPERIENCE, "experience", (w, h) -> new HudBounds(w / 2 - 91, h - 29, BAR_WIDTH, 5));
        add(ElementType.JUMPBAR, "jump_bar", (w, h) -> new HudBounds(w / 2 - 91, h - 29, BAR_WIDTH, 5));
        // The boss name is drawn 10 pixels above the bar
        add(ElementType.BOSSHEALTH, "boss_health", (w, h) -> new HudBounds(w / 2 - 91, 2, BAR_WIDTH, 15));
        addStacked(ElementType.HEALTH, "health", false);
    }

    private static void add(ElementType type, String name, HudElement.DefaultBoundsProvider bounds) {
        String id = "minecraft:" + name;
        ELEMENTS.put(type, HudEditor.register(new HudElement(id, "dashboard.element.minecraft." + name, bounds)));
    }

    private static void addStacked(ElementType type, String name, boolean rightSide) {
        StackedIconRow row = new StackedIconRow(rightSide);
        STACKED_ROWS.put(type, row);
        add(type, name, row);
    }

    /** Runs first, so other mods drawing this element (like Tinkers' hearts) are moved too. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPreRender(RenderGameOverlayEvent.Pre event) {
        HudElement element = ELEMENTS.get(event.type);
        if (element == null) {
            return;
        }
        endAllShifts();

        StackedIconRow row = STACKED_ROWS.get(event.type);
        if (row != null) {
            row.heightBefore = row.currentStackHeight();
        }

        if (!willDraw(event.type)) {
            return;
        }
        if (HudEditor.beginRender(element, event.resolution)) {
            SHIFTED.add(event.type);
        } else {
            event.setCanceled(true);
        }
    }

    /** The boss bar's event fires every frame, but it is only drawn with a boss nearby. */
    private static boolean willDraw(ElementType type) {
        return switch (type) {
            case BOSSHEALTH -> BossStatus.bossName != null && BossStatus.statusBarTime > 0;
            default -> true;
        };
    }

    /** Bars stop moving first, so unrelated HUD other mods draw here is not moved. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPostRenderBars(RenderGameOverlayEvent.Post event) {
        if (event.type == ElementType.ALL) {
            endAllShifts();
        } else if (!STACKED_ROWS.containsKey(event.type)) {
            endShift(event.type);
        }
    }

    /** Rows stop moving last, so things drawn on top of them (like AppleCore's) move too. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPostRenderRows(RenderGameOverlayEvent.Post event) {
        StackedIconRow row = STACKED_ROWS.get(event.type);
        if (row != null) {
            endShift(event.type);
            row.growth = row.currentStackHeight() - row.heightBefore;
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

    /**
     * A row of icons stacked above the hotbar, like the hearts. Its position depends on the rows below it, so it is
     * measured while drawing.
     */
    private static final class StackedIconRow implements HudElement.DefaultBoundsProvider {

        private final boolean rightSide;
        private int heightBefore = 39;
        /** How much taller the stack got from this row. */
        private int growth = ROW_HEIGHT;

        private StackedIconRow(boolean rightSide) {
            this.rightSide = rightSide;
        }

        private int currentStackHeight() {
            return rightSide ? GuiIngameForge.right_height : GuiIngameForge.left_height;
        }

        @Override
        public HudBounds getDefaultBounds(int screenWidth, int screenHeight) {
            int x = rightSide ? screenWidth / 2 + 91 - ICON_ROW_WIDTH : screenWidth / 2 - 91;
            int top = screenHeight - heightBefore;
            // Extra rows (like many hearts) go above the first one
            int extraRowsHeight = Math.max(0, growth - ROW_HEIGHT);
            return new HudBounds(x, top - extraRowsHeight, ICON_ROW_WIDTH, ICON_SIZE + extraRowsHeight);
        }
    }
}
