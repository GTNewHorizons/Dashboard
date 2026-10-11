package com.gtnewhorizon.dashboard.client;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.ForgeHooks;

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
    private static final int ICON_STEP = 8;
    private static final int ROW_HEIGHT = 10;

    private static final Map<ElementType, HudElement> ELEMENTS = new EnumMap<>(ElementType.class);
    private static final Map<ElementType, StackedIconRow> STACKED_ROWS = new EnumMap<>(ElementType.class);
    private static final Set<ElementType> SHIFTED = EnumSet.noneOf(ElementType.class);

    private VanillaHudElements() {}

    public static void register() {
        add(ElementType.HOTBAR, "hotbar", (w, h) -> new HudBounds(w / 2 - 91, h - 22, BAR_WIDTH, 22));
        HudElement experience = add(
            ElementType.EXPERIENCE,
            "experience",
            (w, h) -> new HudBounds(w / 2 - 91, h - 29, BAR_WIDTH, 5));
        PreviewDrawing.setImitationInBounds(experience, VanillaHudElements::drawExperiencePreview);
        HudElement jumpBar = add(
            ElementType.JUMPBAR,
            "jump_bar",
            (w, h) -> new HudBounds(w / 2 - 91, h - 29, BAR_WIDTH, 5));
        PreviewDrawing.setImitationInBounds(jumpBar, bounds -> drawBarPreview(bounds.x, bounds.y, 84));
        // The boss name is drawn 10 pixels above the bar
        HudElement bossHealth = add(
            ElementType.BOSSHEALTH,
            "boss_health",
            (w, h) -> new HudBounds(w / 2 - 91, 2, BAR_WIDTH, 15));
        PreviewDrawing.setImitationInBounds(bossHealth, VanillaHudElements::drawBossHealthPreview);
        HudElement health = addStacked(ElementType.HEALTH, "health", false);
        PreviewDrawing.setImitationInBounds(health, bounds -> drawIconRowPreview(bounds, false, 16, 52, 0));
        HudElement armor = addStacked(ElementType.ARMOR, "armor", false);
        PreviewDrawing.setImitationInBounds(armor, bounds -> drawIconRowPreview(bounds, false, -1, 34, 9));
        HudElement food = addStacked(ElementType.FOOD, "food", true);
        PreviewDrawing.setImitationInBounds(food, bounds -> drawIconRowPreview(bounds, true, 16, 52, 27));
        HudElement air = addStacked(ElementType.AIR, "air", true);
        PreviewDrawing.setImitationInBounds(air, bounds -> drawIconRowPreview(bounds, true, -1, 16, 18));
        HudElement mountHealth = addStacked(ElementType.HEALTHMOUNT, "mount_health", true);
        PreviewDrawing.setImitationInBounds(mountHealth, bounds -> drawIconRowPreview(bounds, true, 52, 88, 9));
    }

    private static HudElement add(ElementType type, String name, HudElement.DefaultBoundsProvider bounds) {
        String id = "minecraft:" + name;
        HudElement element = HudEditor.register(new HudElement(id, "dashboard.element.minecraft." + name, bounds));
        ELEMENTS.put(type, element);
        return element;
    }

    private static HudElement addStacked(ElementType type, String name, boolean rightSide) {
        StackedIconRow row = new StackedIconRow(rightSide);
        STACKED_ROWS.put(type, row);
        return add(type, name, row);
    }

    /** A bar two thirds full. The empty bar is at row v of icons.png, the full bar right below it. */
    private static void drawBarPreview(int x, int y, int v) {
        PreviewDrawing.bindTexture(Gui.icons);
        PreviewDrawing.drawTexture(x, y, 0, v, BAR_WIDTH, 5);
        PreviewDrawing.drawTexture(x, y, 0, v + 5, BAR_WIDTH * 2 / 3, 5);
    }

    /** A row of ten icons, like the hearts. A negative u skips that layer. */
    private static void drawIconRowPreview(HudBounds bounds, boolean fromRight, int backgroundU, int foregroundU,
        int v) {
        PreviewDrawing.bindTexture(Gui.icons);
        int y = bounds.getBottom() - ICON_SIZE;
        for (int i = 0; i < 10; i++) {
            int x = fromRight ? bounds.getRight() - ICON_SIZE - i * ICON_STEP : bounds.x + i * ICON_STEP;
            if (backgroundU >= 0) {
                PreviewDrawing.drawTexture(x, y, backgroundU, v, ICON_SIZE, ICON_SIZE);
            }
            if (foregroundU >= 0) {
                PreviewDrawing.drawTexture(x, y, foregroundU, v, ICON_SIZE, ICON_SIZE);
            }
        }
    }

    /** A Wither boss bar, two thirds full. */
    private static void drawBossHealthPreview(HudBounds bounds) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String name = StatCollector.translateToLocal("entity.WitherBoss.name");
        int nameX = bounds.x + BAR_WIDTH / 2 - font.getStringWidth(name) / 2;
        font.drawStringWithShadow(name, nameX, bounds.y, ColorUtils.textPreview.getColor());
        drawBarPreview(bounds.x, bounds.y + 10, 74);
    }

    /** Experience bar, with the number (vanilla colors) */
    private static void drawExperiencePreview(HudBounds bounds) {
        drawBarPreview(bounds.x, bounds.y, 64);

        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String level = "16";
        int x = bounds.x + BAR_WIDTH / 2 - font.getStringWidth(level) / 2;
        int y = bounds.y - 6;
        font.drawString(level, x + 1, y, 0x000000);
        font.drawString(level, x - 1, y, 0x000000);
        font.drawString(level, x, y + 1, 0x000000);
        font.drawString(level, x, y - 1, 0x000000);
        font.drawString(level, x, y, 0x80FF20);
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

    /** These events fire even when nothing is drawn: no boss nearby, in creative, no armor, or not underwater. */
    private static boolean willDraw(ElementType type) {
        return switch (type) {
            case BOSSHEALTH -> BossStatus.bossName != null && BossStatus.statusBarTime > 0;
            case EXPERIENCE -> {
                PlayerControllerMP controller = Minecraft.getMinecraft().playerController;
                yield controller != null && controller.gameIsSurvivalOrAdventure();
            }
            case ARMOR -> {
                EntityPlayer player = Minecraft.getMinecraft().thePlayer;
                yield player != null && ForgeHooks.getTotalArmorValue(player) > 0;
            }
            case AIR -> {
                EntityPlayer player = Minecraft.getMinecraft().thePlayer;
                yield player != null && player.isInsideOfMaterial(Material.water);
            }
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
