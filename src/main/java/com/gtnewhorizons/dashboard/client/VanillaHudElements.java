package com.gtnewhorizons.dashboard.client;

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.stats.Achievement;
import net.minecraft.stats.AchievementList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.FoodStats;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.compat.AppleCoreCompat;
import com.gtnewhorizons.dashboard.compat.Mods;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Makes vanilla's HUD movable through Forge's overlay events, and draws themed bars instead of vanilla's.
 */
@SideOnly(Side.CLIENT)
@EventBusSubscriber(side = Side.CLIENT)
public final class VanillaHudElements {

    private static final int BAR_WIDTH = 182;
    private static final int ICON_ROW_WIDTH = 81;
    private static final int ICON_SIZE = 9;
    private static final int ICON_STEP = 8;
    private static final int ROW_HEIGHT = 10;
    static final int SAMPLE_LEVEL = 16;

    private static final Map<ElementType, HudElement> ELEMENTS = new EnumMap<>(ElementType.class);
    private static final Map<ElementType, StackedIconRow> STACKED_ROWS = new EnumMap<>(ElementType.class);
    private static final Set<ElementType> SHIFTED = EnumSet.noneOf(ElementType.class);
    private static final Set<ElementType> INVISIBLE = EnumSet.noneOf(ElementType.class);
    private static final Set<ElementType> HIDDEN = EnumSet.noneOf(ElementType.class);
    private static final Map<HudElement, BarSlot> THEMED_BARS = new LinkedHashMap<>();
    private static final Random SHAKE = new Random();
    private static final ResourceLocation APPLECORE_ICONS = new ResourceLocation(
        Mods.APPLECORE.getResourceLocation(),
        "textures/icons.png");

    private static final int ITEM_NAME_WIDTH = 150;
    private static final int ACTION_BAR_WIDTH = 200;
    private static final int LEVEL_WIDTH = 30;
    private static final int ACHIEVEMENT_WIDTH = 160;
    private static final int ACHIEVEMENT_HEIGHT = 32;

    public static final HudElement ITEM_NAME = new HudElement(
        "minecraft:item_name",
        "dashboard.element.minecraft.item_name",
        VanillaHudElements::getItemNameBounds);

    private static String shadowedActionBarText;
    private static final Set<ElementType> POSTED_WHEN_SKIPPED = EnumSet.of(ElementType.EXPERIENCE, ElementType.HOTBAR);

    public static final HudElement ACTION_BAR = new HudElement(
        "minecraft:action_bar",
        "dashboard.element.minecraft.action_bar",
        (w, h) -> new HudBounds(w / 2 - ACTION_BAR_WIDTH / 2, h - 52, ACTION_BAR_WIDTH, ICON_SIZE));

    private static final HudElement EXPERIENCE_LEVEL = new HudElement(
        "minecraft:experience_level",
        "dashboard.element.minecraft.experience_level",
        (w, h) -> new HudBounds(w / 2 - LEVEL_WIDTH / 2, h - 35, LEVEL_WIDTH, 8));

    public static final HudElement SCOREBOARD = new HudElement(
        "minecraft:scoreboard",
        "dashboard.element.minecraft.scoreboard",
        VanillaHudElements::getScoreboardBounds);

    public static final HudElement ACHIEVEMENT = new HudElement(
        "minecraft:achievement",
        "dashboard.element.minecraft.achievement",
        (w, h) -> new HudBounds(w - ACHIEVEMENT_WIDTH, 0, ACHIEVEMENT_WIDTH, ACHIEVEMENT_HEIGHT));

    private static final ResourceLocation ACHIEVEMENT_BACKGROUND = new ResourceLocation(
        "textures/gui/achievement/achievement_background.png");

    private static final String[] SAMPLE_SCORES = { "Steve: " + EnumChatFormatting.RED + 12,
        "Alex: " + EnumChatFormatting.RED + 7, "Notch: " + EnumChatFormatting.RED + 3 };

    private VanillaHudElements() {}

    public static void register() {
        HudRegistry.register(ITEM_NAME);
        PreviewDrawing.setImitationInBounds(ITEM_NAME, VanillaHudElements::drawItemNamePreview);
        HudRegistry.register(ACTION_BAR);
        PreviewDrawing.setImitationInBounds(ACTION_BAR, VanillaHudElements::drawActionBarPreview);
        HudRegistry.register(SCOREBOARD);
        PreviewDrawing.setImitationInBounds(SCOREBOARD, VanillaHudElements::drawScoreboardPreview);
        HudRegistry.register(ACHIEVEMENT);
        PreviewDrawing.setImitationInBounds(ACHIEVEMENT, VanillaHudElements::drawAchievementPreview);
        add(ElementType.HOTBAR, "hotbar", (w, h) -> new HudBounds(w / 2 - 91, h - 22, BAR_WIDTH, 22));
        HudElement experience = add(
            ElementType.EXPERIENCE,
            "experience",
            (w, h) -> new HudBounds(w / 2 - 91, h - 29, BAR_WIDTH, 5));
        PreviewDrawing.setImitationInBounds(
            experience,
            bounds -> drawBarSample(experience, BarSlot.EXPERIENCE, bounds.x, bounds.y));
        HudRegistry.register(EXPERIENCE_LEVEL);
        PreviewDrawing.setImitationInBounds(EXPERIENCE_LEVEL, bounds -> drawLevelNumber(bounds, SAMPLE_LEVEL));
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

    public static Map<HudElement, BarSlot> getThemedBars() {
        return Collections.unmodifiableMap(THEMED_BARS);
    }

    public static HudElement getElement(ElementType type) {
        return ELEMENTS.get(type);
    }

    private static HudElement add(ElementType type, String name, HudElement.DefaultBoundsProvider bounds) {
        String id = "minecraft:" + name;
        HudElement element = HudRegistry.register(new HudElement(id, "dashboard.element.minecraft." + name, bounds));
        ELEMENTS.put(type, element);
        return element;
    }

    private static HudElement addStacked(ElementType type, String name, boolean rightSide) {
        StackedIconRow row = new StackedIconRow(rightSide);
        STACKED_ROWS.put(type, row);
        return add(type, name, row);
    }

    /** The size vanilla would draw, or three sample lines without a scoreboard. */
    private static HudBounds getScoreboardBounds(int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        ScoreObjective objective = mc.theWorld == null ? null
            : mc.theWorld.getScoreboard()
                .func_96539_a(1);
        Collection<?> scores = objective == null ? null
            : objective.getScoreboard()
                .func_96534_i(objective);

        int lines;
        int textWidth;
        if (scores != null && scores.size() <= 15) {
            lines = scores.size();
            textWidth = font.getStringWidth(objective.getDisplayName());
            Scoreboard scoreboard = objective.getScoreboard();
            for (Object entry : scores) {
                Score score = (Score) entry;
                String name = ScorePlayerTeam
                    .formatPlayerName(scoreboard.getPlayersTeam(score.getPlayerName()), score.getPlayerName());
                textWidth = Math
                    .max(textWidth, font.getStringWidth(name + ": " + EnumChatFormatting.RED + score.getScorePoints()));
            }
        } else {
            lines = SAMPLE_SCORES.length;
            textWidth = getSampleScoreboardWidth(font);
        }
        int bottom = screenHeight / 2 + lines * font.FONT_HEIGHT / 3;
        int height = (lines + 1) * font.FONT_HEIGHT + 1;
        return new HudBounds(screenWidth - textWidth - 5, bottom - height, textWidth + 4, height);
    }

    private static int getSampleScoreboardWidth(FontRenderer font) {
        int width = font.getStringWidth(StatCollector.translateToLocal("dashboard.preview.scoreboard"));
        for (String line : SAMPLE_SCORES) {
            width = Math.max(width, font.getStringWidth(line));
        }
        return width;
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

    /** As full as the themed previews, so they look alike side by side. */
    public static void drawVanillaSample(BarSlot slot, int x, int y) {
        switch (slot) {
            // Texture positions from GuiIngameForge
            case EXPERIENCE -> drawBarPreview(x, y, 64);
            case JUMP_BAR -> drawBarPreview(x, y, 84);
            case BOSS_HEALTH -> drawBarPreview(x, y, 74);
            case HEALTH -> drawIconRowPreview(x, y, false, 16, 52, 61, 0);
            case ARMOR -> drawIconRowPreview(x, y, false, 16, 34, 25, 9);
            case FOOD -> drawIconRowPreview(x, y, true, 16, 52, 61, 27);
            case AIR -> drawIconRowPreview(x, y, true, -1, 16, -1, 18);
            case MOUNT_HEALTH -> drawIconRowPreview(x, y, true, 52, 88, 97, 9);
            case HEALTH_POISONED -> drawIconRowPreview(x, y, false, 16, 88, 97, 0);
            case HEALTH_WITHERED -> drawIconRowPreview(x, y, false, 16, 124, 133, 0);
            case HEALTH_ABSORPTION -> drawIconRowPreview(x, y, false, 16, 160, 169, 0);
            case FOOD_HUNGER -> drawIconRowPreview(x, y, true, 133, 88, 97, 27);
            case AIR_POP -> drawIconRowPreview(x, y, true, -1, 16, 25, 18);
            case HEALTH_FLASH_EMPTY -> drawIconRowPreview(x, y, false, 25, 52, 61, 0);
            case HEALTH_FLASH -> drawIconRowPreview(x, y, false, 25, 70, 79, 0);
            case HEALTH_FLASH_POISONED -> drawIconRowPreview(x, y, false, 25, 106, 115, 0);
            case HEALTH_FLASH_WITHERED -> drawIconRowPreview(x, y, false, 25, 142, 151, 0);
            case HEALTH_REGEN -> drawIconRowPreview(x, y, false, 16, 52, 61, 0, BarThemes.getSampleRegenStep());
            case HEALTH_HARDCORE -> drawIconRowPreview(x, y, false, 16, 52, 61, 45);
            case FOOD_SATURATION -> drawSaturationPreview(x, y);
            case FOOD_EXHAUSTION -> drawExhaustionPreview(x, y);
            default -> {}
        }
    }

    /** AppleCore's see-through strip behind the food sample, like its exhaustion underlay. */
    private static void drawExhaustionPreview(int left, int y) {
        PreviewDrawing.bindTexture(APPLECORE_ICONS);
        GL11.glColor4f(1, 1, 1, 0.75f);
        int width = ICON_ROW_WIDTH / 2;
        PreviewDrawing.drawTexture(left + ICON_ROW_WIDTH - width, y, ICON_ROW_WIDTH - width, 18, width, ICON_SIZE);
        drawVanillaSample(BarSlot.FOOD, left, y);
    }

    /** AppleCore's gold outlines over the food sample, half as full as the food. */
    private static void drawSaturationPreview(int left, int y) {
        drawVanillaSample(BarSlot.FOOD, left, y);
        PreviewDrawing.bindTexture(APPLECORE_ICONS);
        int icons = Math.round(BarThemes.PREVIEW_FILL * 10 / 2);
        for (int i = 0; i < icons; i++) {
            PreviewDrawing
                .drawTexture(left + ICON_ROW_WIDTH - ICON_SIZE - i * ICON_STEP, y, 27, 0, ICON_SIZE, ICON_SIZE);
        }
    }

    private static Consumer<HudBounds> rowSample(HudElement element, BarSlot slot) {
        return bounds -> drawBarSample(element, slot, bounds.x, bounds.getBottom() - ICON_SIZE);
    }

    private static void drawBarPreview(int x, int y, int v) {
        PreviewDrawing.bindTexture(Gui.icons);
        PreviewDrawing.drawTexture(x, y, 0, v, BAR_WIDTH, 5);
        PreviewDrawing.drawTexture(x, y, 0, v + 5, Math.round(BAR_WIDTH * BarThemes.PREVIEW_FILL), 5);
    }

    /** Ten icons worth two points each. */
    private static void drawIconRowPreview(int left, int y, boolean fromRight, int emptyU, int fullU, int halfU,
        int v) {
        drawIconRowPreview(left, y, fromRight, emptyU, fullU, halfU, v, -1);
    }

    private static void drawIconRowPreview(int left, int y, boolean fromRight, int emptyU, int fullU, int halfU, int v,
        int liftIndex) {
        PreviewDrawing.bindTexture(Gui.icons);
        int points = Math.round(BarThemes.PREVIEW_FILL * 20);
        for (int i = 0; i < 10; i++) {
            int x = fromRight ? left + ICON_ROW_WIDTH - ICON_SIZE - i * ICON_STEP : left + i * ICON_STEP;
            int iconY = i == liftIndex ? y - 2 : y;
            if (emptyU >= 0) {
                PreviewDrawing.drawTexture(x, iconY, emptyU, v, ICON_SIZE, ICON_SIZE);
            }
            if (i * 2 + 2 <= points) {
                PreviewDrawing.drawTexture(x, iconY, fullU, v, ICON_SIZE, ICON_SIZE);
            } else if (i * 2 + 1 == points) {
                PreviewDrawing.drawTexture(x, iconY, halfU >= 0 ? halfU : fullU, v, ICON_SIZE, ICON_SIZE);
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

    private static void drawBarSample(HudElement element, BarSlot slot, int x, int y) {
        String theme = HudLayout.getTheme(element);
        if (theme == null) {
            drawVanillaSample(slot, x, y);
        } else {
            BarThemes.drawSample(theme, slot, x, y, BarThemes.fillsFromRight(element, slot));
        }
    }

    static void drawLevelNumber(HudBounds box, int level) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String text = String.valueOf(level);
        int x = box.x + box.width / 2 - font.getStringWidth(text) / 2;
        int y = box.y;
        font.drawString(text, x + 1, y, 0x000000);
        font.drawString(text, x - 1, y, 0x000000);
        font.drawString(text, x, y + 1, 0x000000);
        font.drawString(text, x, y - 1, 0x000000);
        font.drawString(text, x, y, 0x80FF20);
    }

    private static void drawScoreboardPreview(HudBounds bounds) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int lineHeight = font.FONT_HEIGHT;
        int textLeft = bounds.x + 2;
        int right = bounds.getRight();
        String title = StatCollector.translateToLocal("dashboard.preview.scoreboard");
        Gui.drawRect(bounds.x, bounds.y, right, bounds.y + lineHeight, 0x60000000);
        Gui.drawRect(bounds.x, bounds.y + lineHeight, right, bounds.y + lineHeight + 1, 0x50000000);
        int titleX = textLeft + (bounds.width - 4) / 2 - font.getStringWidth(title) / 2;
        font.drawString(title, titleX, bounds.y + 1, 0xFFFFFF);

        int y = bounds.y + lineHeight + 1;
        for (String line : SAMPLE_SCORES) {
            int split = line.indexOf(':');
            String name = line.substring(0, split);
            String score = line.substring(split + 2);
            Gui.drawRect(bounds.x, y, right, y + lineHeight, 0x50000000);
            font.drawString(name, textLeft, y, 0xFFFFFF);
            font.drawString(score, right - font.getStringWidth(score), y, 0xFFFFFF);
            y += lineHeight;
        }
    }

    /** Achievement popup - Taking Inventory. */
    private static void drawAchievementPreview(HudBounds bounds) {
        Minecraft mc = Minecraft.getMinecraft();
        Achievement achievement = AchievementList.openInventory;
        PreviewDrawing.bindTexture(ACHIEVEMENT_BACKGROUND);
        PreviewDrawing.drawTexture(bounds.x, bounds.y, 96, 202, ACHIEVEMENT_WIDTH, ACHIEVEMENT_HEIGHT);
        String title = StatCollector.translateToLocal("achievement.get");
        String name = achievement.func_150951_e()
            .getUnformattedText();
        mc.fontRenderer.drawString(title, bounds.x + 30, bounds.y + 7, 0xFFFF00);
        mc.fontRenderer.drawString(name, bounds.x + 30, bounds.y + 18, 0xFFFFFF);
        PreviewDrawing.drawItem(achievement.theItemStack, bounds.x + 8, bounds.y + 8);
    }

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
        BarSlot themedSlot = THEMED_BARS.get(element);
        String theme = themedSlot != null ? HudLayout.getTheme(element) : null;
        if (theme != null && drawThemed(event, element, themedSlot, theme)) {
            postSkippedEvent(event);
            return;
        }
        if (HudRegistry.isHidingContent(element) && HudLayout.isVisible(element)) {
            // Drawn invisibly instead of skipped, so the rows above keep their place
            HudRegistry.shouldRender(element);
            GL11.glPushMatrix();
            PreviewDrawing.setColorWrites(false);
            SHIFTED.add(event.type);
            INVISIBLE.add(event.type);
            return;
        }
        if (HudRegistry.beginRender(element, event.resolution)) {
            SHIFTED.add(event.type);
        } else {
            HIDDEN.add(event.type);
            cancel(event);
            postSkippedEvent(event);
        }
    }

    /** A message above the hotbar, where music disc names show, but with a shadow like other HUD text. */
    public static void showShadowedActionBarText(String text) {
        shadowedActionBarText = text;
        Minecraft.getMinecraft().ingameGUI.func_110326_a(text, false);
    }

    public static boolean isShadowedActionBarText(String text) {
        return text.equals(shadowedActionBarText);
    }

    /**
     * True if the element's Pre event was cancelled only because the player hid it. Lets mods that draw a separate
     * element during this element's event (Backhand's off-hand slot during the hotbar's) keep drawing it.
     */
    public static boolean isHiddenByDashboard(ElementType type) {
        return HIDDEN.contains(type);
    }

    /**
     * For a separate element another mod draws during this element's event: undoes this element's move and
     * invisibility, then moves the separate one.
     *
     * @return false if the separate element should not be drawn. Otherwise call {@link #endSeparateElement} after it.
     */
    public static boolean beginSeparateElement(ElementType type, HudElement separate, int screenWidth,
        int screenHeight) {
        if (!HudRegistry.shouldRender(separate)) {
            return false;
        }
        GL11.glPushMatrix();
        if (INVISIBLE.contains(type)) {
            PreviewDrawing.setColorWrites(true);
        } else if (SHIFTED.contains(type)) {
            HudRegistry.undoTranslateToPosition(ELEMENTS.get(type), screenWidth, screenHeight);
        }
        HudRegistry.translateToPosition(separate, screenWidth, screenHeight);
        return true;
    }

    public static void endSeparateElement(ElementType type) {
        HudRegistry.endRender();
        if (INVISIBLE.contains(type)) {
            PreviewDrawing.setColorWrites(false);
        }
    }

    /**
     * Forge skips the Post event of a canceled element. The XP bar's and hotbar's are sent anyway, since mods draw
     * their own HUD after them (Adventure Backpack after the XP bar, IC2 and Battlegear after the hotbar). Other bars'
     * are not, since mods like AppleCore draw on top of the vanilla bar there.
     */
    private static void postSkippedEvent(RenderGameOverlayEvent.Pre event) {
        if (POSTED_WHEN_SKIPPED.contains(event.type)) {
            MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Post(event, event.type));
        }
    }

    /** Instead of vanilla's, which the mixin skips. After the XP bar, so it stays on top. */
    private static void drawExperienceLevel(RenderGameOverlayEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        int level = mc.thePlayer.experienceLevel;
        ScaledResolution resolution = event.resolution;
        if (event.type != ElementType.EXPERIENCE || level <= 0
            || !willDraw(ElementType.EXPERIENCE)
            || !HudRegistry.beginRender(EXPERIENCE_LEVEL, resolution)) {
            return;
        }
        try {
            HudBounds bounds = EXPERIENCE_LEVEL
                .getDefaultBounds(resolution.getScaledWidth(), resolution.getScaledHeight());
            drawLevelNumber(bounds, level);
        } finally {
            HudRegistry.endRender();
            mc.getTextureManager()
                .bindTexture(Gui.icons);
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
    private static boolean drawThemed(RenderGameOverlayEvent.Pre event, HudElement element, BarSlot slot,
        String theme) {
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

        // Absorption gets its own row above health, like vanilla's golden hearts
        // TODO: posibble re-do (smaller bar, bar as overlay)
        float absorption = event.type == ElementType.HEALTH ? mc.thePlayer.getAbsorptionAmount() : 0;
        int rows = absorption > 0 ? 2 : 1;
        StackedIconRow row = STACKED_ROWS.get(event.type);
        if (row != null && HudLayout.isVisible(element)) {
            row.addToStack(ROW_HEIGHT * rows);
            row.growth = ROW_HEIGHT * rows;
        }
        if (!HudRegistry.beginRender(element, event.resolution)) {
            return true;
        }
        boolean fromRight = BarThemes.fillsFromRight(element, slot);
        HudBounds bounds = element
            .getDefaultBounds(event.resolution.getScaledWidth(), event.resolution.getScaledHeight());
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            int barY = bounds.getBottom() - slot.height;
            if (event.type == ElementType.HEALTH) {
                drawThemedHealth(mc, theme, bounds.x, barY, fill, absorption, fromRight);
            } else {
                if (event.type == ElementType.FOOD) {
                    barY += getHungerShake(mc);
                }
                BarThemes.drawBar(theme, getStateSlot(slot, mc.thePlayer), bounds.x, barY, fill, fromRight);
                if (event.type == ElementType.FOOD) {
                    drawAppleCoreLayers(mc.thePlayer, theme, bounds.x, barY, fromRight);
                }
                if (event.type == ElementType.AIR && isBubblePopping(mc.thePlayer.getAir())) {
                    BarThemes.drawAirPop(theme, bounds.x, barY, fill, fromRight);
                }
            }
            if (event.type == ElementType.BOSSHEALTH) {
                drawBossName(bounds, BossStatus.bossName);
            }
        } finally {
            GL11.glPopAttrib();
            HudRegistry.endRender();
            mc.getTextureManager()
                .bindTexture(Gui.icons);
        }
        return true;
    }

    /** With its shake, flash, regeneration, hardcore overlay and the absorption bar above it. */
    private static void drawThemedHealth(Minecraft mc, String theme, int x, int barY, float fill, float absorption,
        boolean fromRight) {
        EntityPlayer player = mc.thePlayer;
        if (player.getHealth() <= 4) {
            barY += getLowHealthShake(mc);
        }
        BarSlot stateSlot = getStateSlot(BarSlot.HEALTH, player);
        if (isHealthFlashing(player)) {
            float lastFill = player.prevHealth / player.getMaxHealth();
            BarThemes.drawEmpty(theme, BarSlot.HEALTH_FLASH_EMPTY, x, barY);
            BarThemes.drawFillRange(theme, getFlashSlot(stateSlot), x, barY, fill, lastFill, fromRight);
            BarThemes.drawFillRange(theme, stateSlot, x, barY, 0, fill, fromRight);
        } else {
            BarThemes.drawBar(theme, stateSlot, x, barY, fill, fromRight);
        }
        if (player.isPotionActive(Potion.regeneration)) {
            int step = BarThemes.getRegenStep(mc.ingameGUI.getUpdateCounter());
            BarThemes.drawRegenShine(theme, x, barY, step, fromRight);
        }
        if (mc.theWorld.getWorldInfo()
            .isHardcoreModeEnabled()) {
            BarThemes.drawEmpty(theme, BarSlot.HEALTH_HARDCORE, x, barY);
        }
        if (absorption > 0) {
            float absorptionFill = Math.min(1, absorption / player.getMaxHealth());
            BarThemes.drawBar(theme, BarSlot.HEALTH_ABSORPTION, x, barY - ROW_HEIGHT, absorptionFill, fromRight);
        }
    }

    /** Over the fill, unlike AppleCore's exhaustion, since a themed fill would hide anything behind it. */
    private static void drawAppleCoreLayers(EntityPlayer player, String theme, int x, int barY, boolean fromRight) {
        if (AppleCoreCompat.showsExhaustion()) {
            float exhaustion = AppleCoreCompat.getExhaustionFill(player);
            BarThemes.drawFillRange(theme, BarSlot.FOOD_EXHAUSTION, x, barY, 0, exhaustion, fromRight);
        }
        if (AppleCoreCompat.showsSaturation()) {
            float saturation = player.getFoodStats()
                .getSaturationLevel() / 20f;
            BarThemes.drawFillRange(theme, BarSlot.FOOD_SATURATION, x, barY, 0, saturation, fromRight);
        }
        if (AppleCoreCompat.showsHeldFood()) {
            drawHeldFood(player, theme, x, barY, fromRight);
        }
    }

    /** What the held food would add, blinking like AppleCore's. Saturation can not go past the new food level. */
    private static void drawHeldFood(EntityPlayer player, String theme, int x, int barY, boolean fromRight) {
        AppleCoreCompat.HeldFood heldFood = AppleCoreCompat.getHeldFood(player);
        if (heldFood == null) {
            return;
        }
        FoodStats stats = player.getFoodStats();
        int food = stats.getFoodLevel();
        float saturation = stats.getSaturationLevel();
        int newFood = Math.min(20, food + heldFood.hunger);
        float newSaturation = Math.min(newFood, saturation + heldFood.saturation);
        float alpha = getHeldFoodAlpha(Minecraft.getMinecraft().ingameGUI.getUpdateCounter());
        BarSlot foodSlot = getStateSlot(BarSlot.FOOD, player);
        BarThemes.drawFillRange(theme, foodSlot, x, barY, food / 20f, newFood / 20f, fromRight, alpha);
        BarThemes.drawFillRange(
            theme,
            BarSlot.FOOD_SATURATION,
            x,
            barY,
            saturation / 20f,
            newSaturation / 20f,
            fromRight,
            alpha);
    }

    /** AppleCore's fade: in over 12 ticks, out over 12, briefly past fully clear and fully solid. */
    private static float getHeldFoodAlpha(int tick) {
        int step = tick % 24;
        float alpha = step < 12 ? step * 0.125f : 1 - (step - 12) * 0.125f;
        return MathHelper.clamp_float(alpha, 0, 1);
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

    /** Blinks every 3 ticks during the first half of the hurt cooldown. */
    private static boolean isHealthFlashing(EntityPlayer player) {
        return player.hurtResistantTime >= 10 && player.hurtResistantTime / 3 % 2 == 1;
    }

    /** 0 or 1 px, changing every tick, with the seed vanilla uses for its hearts. */
    private static int getLowHealthShake(Minecraft mc) {
        SHAKE.setSeed(mc.ingameGUI.getUpdateCounter() * 312871L);
        return SHAKE.nextInt(2);
    }

    /** Vanilla's popping bubble: the last 2 air ticks of each bubble. */
    private static boolean isBubblePopping(int air) {
        return Math.ceil(air * 10 / 300.0) - Math.ceil((air - 2) * 10 / 300.0) == 1;
    }

    /** -1 to 1 px without saturation, more often the hungrier you are. */
    private static int getHungerShake(Minecraft mc) {
        FoodStats food = mc.thePlayer.getFoodStats();
        int tick = mc.ingameGUI.getUpdateCounter();
        if (food.getSaturationLevel() > 0 || tick % (food.getFoodLevel() * 3 + 1) != 0) {
            return 0;
        }
        SHAKE.setSeed(tick * 312871L);
        return SHAKE.nextInt(3) - 1;
    }

    private static BarSlot getFlashSlot(BarSlot healthSlot) {
        return switch (healthSlot) {
            case HEALTH_POISONED -> BarSlot.HEALTH_FLASH_POISONED;
            case HEALTH_WITHERED -> BarSlot.HEALTH_FLASH_WITHERED;
            default -> BarSlot.HEALTH_FLASH;
        };
    }

    /** These events fire even when nothing is drawn: no boss nearby, in creative, no armor, or not underwater. */
    static boolean willDraw(ElementType type) {
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
            // Before the shift ends, so the keys move and resize with the hotbar
            if (event.type == ElementType.HOTBAR && SHIFTED.contains(ElementType.HOTBAR)) {
                HotbarNumbers.drawOverItems(event.resolution.getScaledWidth(), event.resolution.getScaledHeight());
            }
            endShift(event.type);
            drawExperienceLevel(event);
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
            HudRegistry.endRender();
        }
        if (INVISIBLE.remove(type)) {
            PreviewDrawing.setColorWrites(true);
        }
    }

    private static void endAllShifts() {
        for (int i = 0; i < SHIFTED.size(); i++) {
            HudRegistry.endRender();
        }
        SHIFTED.clear();
        HIDDEN.clear();
        if (!INVISIBLE.isEmpty()) {
            PreviewDrawing.setColorWrites(true);
            INVISIBLE.clear();
        }
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
