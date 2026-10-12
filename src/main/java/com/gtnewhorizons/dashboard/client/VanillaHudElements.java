package com.gtnewhorizons.dashboard.client;

import java.awt.Color;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.ForgeHooks;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;

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
    private static final Set<ElementType> INVISIBLE = EnumSet.noneOf(ElementType.class);
    private static final Map<HudElement, BarSlot> THEMED_BARS = new LinkedHashMap<>();

    private static final int ITEM_NAME_WIDTH = 150;
    private static final int ACTION_BAR_WIDTH = 200;

    public static final HudElement ITEM_NAME = new HudElement(
        "minecraft:item_name",
        "dashboard.element.minecraft.item_name",
        VanillaHudElements::getItemNameBounds);

    public static final HudElement ACTION_BAR = new HudElement(
        "minecraft:action_bar",
        "dashboard.element.minecraft.action_bar",
        (w, h) -> new HudBounds(w / 2 - ACTION_BAR_WIDTH / 2, h - 52, ACTION_BAR_WIDTH, ICON_SIZE));

    private VanillaHudElements() {}

    public static void register() {
        HudEditor.register(ITEM_NAME);
        PreviewDrawing.setImitationInBounds(ITEM_NAME, VanillaHudElements::drawItemNamePreview);
        HudEditor.register(ACTION_BAR);
        PreviewDrawing.setImitationInBounds(ACTION_BAR, VanillaHudElements::drawActionBarPreview);
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
        PreviewDrawing
            .setImitationInBounds(jumpBar, bounds -> drawBarSample(jumpBar, BarSlot.JUMP_BAR, bounds.x, bounds.y));
        // The boss name is drawn 10 pixels above the bar
        HudElement bossHealth = add(
            ElementType.BOSSHEALTH,
            "boss_health",
            (w, h) -> new HudBounds(w / 2 - 91, 2, BAR_WIDTH, 15));
        PreviewDrawing.setImitationInBounds(bossHealth, VanillaHudElements::drawBossHealthPreview);
        HudElement health = addStacked(ElementType.HEALTH, "health", false);
        PreviewDrawing.setImitationInBounds(health, rowSample(health, BarSlot.HEALTH));
        HudElement armor = addStacked(ElementType.ARMOR, "armor", false);
        PreviewDrawing.setImitationInBounds(armor, rowSample(armor, BarSlot.ARMOR));
        HudElement food = addStacked(ElementType.FOOD, "food", true);
        PreviewDrawing.setImitationInBounds(food, rowSample(food, BarSlot.FOOD));
        HudElement air = addStacked(ElementType.AIR, "air", true);
        PreviewDrawing.setImitationInBounds(air, rowSample(air, BarSlot.AIR));
        HudElement mountHealth = addStacked(ElementType.HEALTHMOUNT, "mount_health", true);
        PreviewDrawing.setImitationInBounds(mountHealth, rowSample(mountHealth, BarSlot.MOUNT_HEALTH));

        THEMED_BARS.put(health, BarSlot.HEALTH);
        THEMED_BARS.put(armor, BarSlot.ARMOR);
        THEMED_BARS.put(food, BarSlot.FOOD);
        THEMED_BARS.put(air, BarSlot.AIR);
        THEMED_BARS.put(mountHealth, BarSlot.MOUNT_HEALTH);
        THEMED_BARS.put(experience, BarSlot.EXPERIENCE);
        THEMED_BARS.put(jumpBar, BarSlot.JUMP_BAR);
        THEMED_BARS.put(bossHealth, BarSlot.BOSS_HEALTH);
    }

    /** Can use a bar theme. */
    public static Map<HudElement, BarSlot> getThemedBars() {
        return Collections.unmodifiableMap(THEMED_BARS);
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

    /** 59px above the bottom screen border, or 14px lower without hearts and food (creative). */
    private static HudBounds getItemNameBounds(int screenWidth, int screenHeight) {
        PlayerControllerMP controller = Minecraft.getMinecraft().playerController;
        int y = screenHeight - 59;
        if (controller != null && !controller.shouldDrawHUD()) {
            y += 14;
        }
        return new HudBounds(screenWidth / 2 - ITEM_NAME_WIDTH / 2, y, ITEM_NAME_WIDTH, ICON_SIZE);
    }

    /** The vanilla look of a bar, as full as the themed previews. Used in the editor and the Customize screen. */
    public static void drawVanillaSample(BarSlot slot, int x, int y) {
        switch (slot) {
            // Texture positions from GuiIngameForge
            case EXPERIENCE -> drawBarPreview(x, y, 64);
            case JUMP_BAR -> drawBarPreview(x, y, 84);
            case BOSS_HEALTH -> drawBarPreview(x, y, 74);
            // empty u, full u, half u, v
            case HEALTH -> drawIconRowPreview(x, y, false, 16, 52, 61, 0);
            case ARMOR -> drawIconRowPreview(x, y, false, 16, 34, 25, 9);
            case FOOD -> drawIconRowPreview(x, y, true, 16, 52, 61, 27);
            case AIR -> drawIconRowPreview(x, y, true, -1, 16, -1, 18);
            case MOUNT_HEALTH -> drawIconRowPreview(x, y, true, 52, 88, 97, 9);
            case HEALTH_POISONED -> drawIconRowPreview(x, y, false, 16, 88, 97, 0);
            case HEALTH_WITHERED -> drawIconRowPreview(x, y, false, 16, 124, 133, 0);
            case HEALTH_ABSORPTION -> drawIconRowPreview(x, y, false, 16, 160, 169, 0);
            case FOOD_HUNGER -> drawIconRowPreview(x, y, true, 133, 88, 97, 27);
            default -> {}
        }
    }

    /** A stacked row sample, following the row's theme. */
    private static Consumer<HudBounds> rowSample(HudElement element, BarSlot slot) {
        return bounds -> drawBarSample(element, slot, bounds.x, bounds.getBottom() - ICON_SIZE);
    }

    private static void drawBarPreview(int x, int y, int v) {
        PreviewDrawing.bindTexture(Gui.icons);
        PreviewDrawing.drawTexture(x, y, 0, v, BAR_WIDTH, 5);
        PreviewDrawing.drawTexture(x, y, 0, v + 5, Math.round(BAR_WIDTH * BarThemes.PREVIEW_FILL), 5);
    }

    /**
     * Ten icons worth two points each.
     */
    private static void drawIconRowPreview(int left, int y, boolean fromRight, int emptyU, int fullU, int halfU,
        int v) {
        PreviewDrawing.bindTexture(Gui.icons);
        int points = Math.round(BarThemes.PREVIEW_FILL * 20);
        for (int i = 0; i < 10; i++) {
            int x = fromRight ? left + ICON_ROW_WIDTH - ICON_SIZE - i * ICON_STEP : left + i * ICON_STEP;
            if (emptyU >= 0) {
                PreviewDrawing.drawTexture(x, y, emptyU, v, ICON_SIZE, ICON_SIZE);
            }
            if (i * 2 + 2 <= points) {
                PreviewDrawing.drawTexture(x, y, fullU, v, ICON_SIZE, ICON_SIZE);
            } else if (i * 2 + 1 == points && halfU >= 0) {
                PreviewDrawing.drawTexture(x, y, halfU, v, ICON_SIZE, ICON_SIZE);
            }
        }
    }

    /** Action bar - music disc message. */
    private static void drawActionBarPreview(HudBounds bounds) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String message = StatCollector.translateToLocalFormatted("record.nowPlaying", "C418 - cat");
        int x = bounds.x + bounds.width / 2 - font.getStringWidth(message) / 2;
        float ticks = (System.currentTimeMillis() % 100_000L) / 50f;
        int color = Color.HSBtoRGB(-ticks / 50f, 0.7f, 0.6f) & 0xFFFFFF;
        font.drawString(message, x, bounds.y, color);
    }

    /** A Wither boss bar, two thirds full. */
    private static void drawBossHealthPreview(HudBounds bounds) {
        drawBossName(bounds, StatCollector.translateToLocal("entity.WitherBoss.name"));
        drawBarSample(ELEMENTS.get(ElementType.BOSSHEALTH), BarSlot.BOSS_HEALTH, bounds.x, bounds.y + 10);
    }

    private static void drawBossName(HudBounds bounds, String name) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int nameX = bounds.x + BAR_WIDTH / 2 - font.getStringWidth(name) / 2;
        font.drawStringWithShadow(name, nameX, bounds.y, 0xFFFFFF);
    }

    /** Experience bar, with the number (vanilla colors) */
    private static void drawExperiencePreview(HudBounds bounds) {
        drawBarSample(ELEMENTS.get(ElementType.EXPERIENCE), BarSlot.EXPERIENCE, bounds.x, bounds.y);
        drawLevelNumber(bounds, 16);
    }

    /** Themed or vanilla editor sample. */
    private static void drawBarSample(HudElement element, BarSlot slot, int x, int y) {
        String theme = HudLayout.getTheme(element);
        if (theme == null) {
            drawVanillaSample(slot, x, y);
        } else {
            BarThemes.drawBar(theme, slot, x, y, BarThemes.PREVIEW_FILL, BarThemes.fillsFromRight(element, slot));
        }
    }

    private static void drawLevelNumber(HudBounds bar, int level) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String text = String.valueOf(level);
        int x = bar.x + BAR_WIDTH / 2 - font.getStringWidth(text) / 2;
        int y = bar.y - 6;
        font.drawString(text, x + 1, y, 0x000000);
        font.drawString(text, x - 1, y, 0x000000);
        font.drawString(text, x, y + 1, 0x000000);
        font.drawString(text, x, y - 1, 0x000000);
        font.drawString(text, x, y, 0x80FF20);
    }

    /** Held item name preview. */
    private static void drawItemNamePreview(HudBounds bounds) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String name = new ItemStack(Items.diamond_sword).getDisplayName();
        int x = bounds.x + bounds.width / 2 - font.getStringWidth(name) / 2;
        font.drawStringWithShadow(name, x, bounds.y, 0xFFFFFF);
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
        String theme = THEMED_BARS.containsKey(element) ? HudLayout.getTheme(element) : null;
        if (theme != null && drawThemed(event, element, theme)) {
            return;
        }
        if (HudEditor.isHidingContent() && HudLayout.isVisible(element)) {
            // Drawn invisibly instead of skipped, so the rows above keep their place
            HudEditor.shouldRender(element);
            GL11.glPushMatrix();
            setColorWrites(false);
            SHIFTED.add(event.type);
            INVISIBLE.add(event.type);
            return;
        }
        if (HudEditor.beginRender(element, event.resolution)) {
            SHIFTED.add(event.type);
        } else {
            cancel(event);
        }
    }

    /** Vanilla counts the boss bar timer down while drawing it. */
    private static void cancel(RenderGameOverlayEvent.Pre event) {
        event.setCanceled(true);
        if (event.type == ElementType.BOSSHEALTH) {
            BossStatus.statusBarTime--;
        }
    }

    /** Draws the theme instead of vanilla. False if the bar is not themable yet. */
    private static boolean drawThemed(RenderGameOverlayEvent.Pre event, HudElement element, String theme) {
        Minecraft mc = Minecraft.getMinecraft();
        float fill = switch (event.type) {
            case EXPERIENCE -> mc.thePlayer.experience;
            case JUMPBAR -> mc.thePlayer.getHorseJumpPower();
            case BOSSHEALTH -> BossStatus.healthScale;
            case HEALTH -> mc.thePlayer.getHealth() / mc.thePlayer.getMaxHealth();
            case ARMOR -> ForgeHooks.getTotalArmorValue(mc.thePlayer) / 20f;
            case FOOD -> mc.thePlayer.getFoodStats()
                .getFoodLevel() / 20f;
            case AIR -> Math.max(0, mc.thePlayer.getAir()) / 300f;
            case HEALTHMOUNT -> mc.thePlayer.ridingEntity instanceof EntityLivingBase mount
                ? mount.getHealth() / mount.getMaxHealth()
                : -1;
            default -> -1;
        };
        if (fill < 0) {
            return false;
        }
        cancel(event);

        // Absorption above health / TODO: posibble re-do (smaller bar, bar as overlay)
        float absorption = event.type == ElementType.HEALTH ? mc.thePlayer.getAbsorptionAmount() : 0;
        int rows = absorption > 0 ? 2 : 1;
        StackedIconRow row = STACKED_ROWS.get(event.type);
        if (row != null && HudLayout.isVisible(element)) {
            row.addToStack(ROW_HEIGHT * rows);
            row.growth = ROW_HEIGHT * rows;
        }
        if (!HudEditor.beginRender(element, event.resolution)) {
            return true;
        }
        BarSlot slot = THEMED_BARS.get(element);
        boolean fromRight = BarThemes.fillsFromRight(element, slot);
        HudBounds bounds = element
            .getDefaultBounds(event.resolution.getScaledWidth(), event.resolution.getScaledHeight());
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            int barY = bounds.getBottom() - slot.height;
            BarThemes.drawBar(theme, getStateSlot(slot, mc.thePlayer), bounds.x, barY, fill, fromRight);
            if (absorption > 0) {
                float absorptionFill = Math.min(1, absorption / mc.thePlayer.getMaxHealth());
                BarThemes
                    .drawBar(theme, BarSlot.HEALTH_ABSORPTION, bounds.x, barY - ROW_HEIGHT, absorptionFill, fromRight);
            }
            if (event.type == ElementType.EXPERIENCE && mc.thePlayer.experienceLevel > 0) {
                drawLevelNumber(bounds, mc.thePlayer.experienceLevel);
            } else if (event.type == ElementType.BOSSHEALTH) {
                drawBossName(bounds, BossStatus.bossName);
            }
        } finally {
            GL11.glPopAttrib();
            HudEditor.endRender();
            mc.getTextureManager()
                .bindTexture(Gui.icons);
        }
        return true;
    }

    private static BarSlot getStateSlot(BarSlot slot, EntityPlayer player) {
        if (slot == BarSlot.HEALTH && player.isPotionActive(Potion.poison)) {
            return BarSlot.HEALTH_POISONED;
        }
        if (slot == BarSlot.HEALTH && player.isPotionActive(Potion.wither)) {
            return BarSlot.HEALTH_WITHERED;
        }
        if (slot == BarSlot.FOOD && player.isPotionActive(Potion.hunger)) {
            return BarSlot.FOOD_HUNGER;
        }
        return slot;
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
        if (INVISIBLE.remove(type)) {
            setColorWrites(true);
        }
    }

    private static void endAllShifts() {
        for (int i = 0; i < SHIFTED.size(); i++) {
            HudEditor.endRender();
        }
        SHIFTED.clear();
        if (!INVISIBLE.isEmpty()) {
            setColorWrites(true);
            INVISIBLE.clear();
        }
    }

    private static void setColorWrites(boolean enabled) {
        GL11.glColorMask(enabled, enabled, enabled, enabled);
        GL11.glDepthMask(enabled);
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

        private void addToStack(int height) {
            if (rightSide) {
                GuiIngameForge.right_height += height;
            } else {
                GuiIngameForge.left_height += height;
            }
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
