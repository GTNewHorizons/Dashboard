package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.DashboardConfig;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The player's potion effects, which 1.7.10 only shows in the inventory. */
@SideOnly(Side.CLIENT)
@EventBusSubscriber(side = Side.CLIENT)
public final class StatusEffects {

    private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");
    private static final int ICON_SIZE = 18;
    private static final int ICON_X = 6;
    private static final int ICON_Y = 7;
    private static final int TEXT_X = 28;
    private static final int NAME_Y = 6;
    private static final int TIME_Y = 16;
    private static final int TEXT_RIGHT_PADDING = 6;
    private static final int BOX_V = 166;
    private static final int BOX_TEXTURE_WIDTH = 120;
    private static final int BOX_EDGE = 4;
    private static final int BOX_MIDDLE_U = 40;
    private static final int BOX_MIDDLE_WIDTH = 60;
    private static final int BOX_HEIGHT = 32;
    public static final int DEFAULT_SPACING = 1;
    public static final int MIN_SPACING = -15;
    public static final int MAX_SPACING = 20;
    public static final int DEFAULT_PULSE_SECONDS = 20;
    public static final int MAX_PULSE_SECONDS = 120;
    static final int PULSE_STEP_SECONDS = 5;
    private static final int RIGHT_MARGIN = 162;
    private static final int TOP_MARGIN = 5;
    private static final List<PotionEffect> SAMPLES = Arrays.asList(
        new PotionEffect(Potion.moveSpeed.id, 3600),
        new PotionEffect(Potion.poison.id, 300),
        new PotionEffect(Potion.regeneration.id, 900, 1));

    public static final HudElement ELEMENT = new HudElement(
        "minecraft:status_effects",
        "dashboard.element.minecraft.status_effects",
        StatusEffects::getBounds).setModId(Dashboard.MODID);

    private static long widthKey = Long.MIN_VALUE;
    private static int measuredWidth;

    private StatusEffects() {}

    public static void register() {
        HudEditor.register(ELEMENT);
        PreviewDrawing.setImitationInBounds(ELEMENT, bounds -> draw(SAMPLES, bounds));
    }

    /** Big enough for current effects, or for the preview samples when you have none. */
    private static HudBounds getBounds(int screenWidth, int screenHeight) {
        Collection<PotionEffect> effects = getEffects();
        if (effects.isEmpty()) {
            effects = SAMPLES;
        }
        int width = getWidth(effects.size(), getBoxWidth(effects));
        return new HudBounds(screenWidth - width - RIGHT_MARGIN, TOP_MARGIN, width, getHeight(effects.size()));
    }

    /** A column, or a row with the horizontal setting. */
    private static int getWidth(int count, int boxWidth) {
        return DashboardConfig.statusEffectHorizontal ? count * (boxWidth + getSpacing()) - getSpacing() : boxWidth;
    }

    private static int getHeight(int count) {
        return DashboardConfig.statusEffectHorizontal ? BOX_HEIGHT : count * (BOX_HEIGHT + getSpacing()) - getSpacing();
    }

    private static int getSpacing() {
        return MathHelper.clamp_int(DashboardConfig.statusEffectSpacing, MIN_SPACING, MAX_SPACING);
    }

    static int getSamplesWidth() {
        return getWidth(SAMPLES.size(), getBoxWidth(SAMPLES));
    }

    static int getSamplesHeight() {
        return getHeight(SAMPLES.size());
    }

    /** The tallest the samples get, so the Customize preview keeps one size. */
    static int getMaxSamplesHeight() {
        return SAMPLES.size() * (BOX_HEIGHT + MAX_SPACING) - MAX_SPACING;
    }

    /** The poison sample counts down through the pulse time, so the pulsing can be seen. */
    static void drawSamples(int x, int y) {
        List<PotionEffect> samples = SAMPLES;
        int pulseTicks = getPulseTicks();
        if (pulseTicks > 0) {
            int ticks = pulseTicks - (int) (Minecraft.getSystemTime() / 50 % pulseTicks);
            samples = Arrays.asList(SAMPLES.get(0), new PotionEffect(Potion.poison.id, ticks), SAMPLES.get(2));
        }
        draw(samples, new HudBounds(x, y, getSamplesWidth(), getHeight(samples.size())));
    }

    private static int getPulseTicks() {
        return MathHelper.clamp_int(DashboardConfig.statusEffectPulseSeconds, 0, MAX_PULSE_SECONDS) * 20;
    }

    /** A steady pulse twice a second, between fully and a quarter visible. 1 while not pulsing. */
    private static float getPulseAlpha(PotionEffect effect) {
        int pulseTicks = getPulseTicks();
        int ticks = effect.getDuration();
        if (pulseTicks == 0 || ticks > pulseTicks || isPermanent(effect)) {
            return 1;
        }
        return 0.625f + 0.375f * MathHelper.cos(ticks * (float) Math.PI / 5f);
    }

    /** Good effects first with the group setting, then by time left with the sort setting. */
    private static List<PotionEffect> order(Collection<PotionEffect> effects) {
        List<PotionEffect> ordered = new ArrayList<>(effects);
        Comparator<PotionEffect> comparator = (first, second) -> 0;
        if (DashboardConfig.statusEffectGroup) {
            comparator = comparator.thenComparing(StatusEffects::isBad);
        }
        if (DashboardConfig.statusEffectSortByTime) {
            comparator = comparator.thenComparingInt(StatusEffects::getTicksLeft);
        }
        ordered.sort(comparator);
        return ordered;
    }

    private static boolean isBad(PotionEffect effect) {
        Potion potion = Potion.potionTypes[effect.getPotionID()];
        return potion != null && potion.isBadEffect();
    }

    /** Endless effects last. */
    private static int getTicksLeft(PotionEffect effect) {
        return effect.getIsPotionDurationMax() ? Integer.MAX_VALUE : effect.getDuration();
    }

    @SuppressWarnings("unchecked")
    private static Collection<PotionEffect> getEffects() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) {
            return Collections.emptyList();
        }
        Collection<PotionEffect> effects = player.getActivePotionEffects();
        if (DashboardConfig.statusEffectPermanent) {
            return effects;
        }
        List<PotionEffect> shown = new ArrayList<>();
        for (PotionEffect effect : effects) {
            if (!isPermanent(effect)) {
                shown.add(effect);
            }
        }
        return shown;
    }

    /** Infinite, or kept up by a beacon. */
    private static boolean isPermanent(PotionEffect effect) {
        return effect.getIsPotionDurationMax() || effect.getIsAmbient();
    }

    /** Measured again only when an effect, its time in seconds or a shown part changes. */
    private static int getBoxWidth(Collection<PotionEffect> effects) {
        long key = getWidthKey(effects);
        if (key != widthKey) {
            widthKey = key;
            measuredWidth = measureBoxWidth(effects);
        }
        return measuredWidth;
    }

    private static long getWidthKey(Collection<PotionEffect> effects) {
        long key = (DashboardConfig.statusEffectIcon ? 1 : 0) | (DashboardConfig.statusEffectName ? 2 : 0)
            | (DashboardConfig.statusEffectTime ? 4 : 0);
        for (PotionEffect effect : effects) {
            key = key * 31 + effect.getPotionID();
            key = key * 31 + effect.getAmplifier();
            key = key * 31 + (effect.getIsPotionDurationMax() ? -1 : effect.getDuration() / 20);
        }
        return key;
    }

    /** Vanilla's width with both name and time, otherwise only as wide as what is shown. */
    private static int measureBoxWidth(Collection<PotionEffect> effects) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int textWidth = 0;
        for (PotionEffect effect : effects) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) {
                continue;
            }
            if (DashboardConfig.statusEffectName) {
                textWidth = Math.max(textWidth, font.getStringWidth(getName(effect, potion)));
            }
            if (DashboardConfig.statusEffectTime) {
                textWidth = Math.max(textWidth, font.getStringWidth(getTime(effect)));
            }
        }
        int width = textWidth > 0 ? getTextX() + textWidth + TEXT_RIGHT_PADDING : ICON_X + ICON_SIZE + ICON_X;
        if (DashboardConfig.statusEffectName && DashboardConfig.statusEffectTime) {
            return Math.max(BOX_TEXTURE_WIDTH - (TEXT_X - getTextX()), width);
        }
        return width;
    }

    private static int getTextX() {
        return DashboardConfig.statusEffectIcon ? TEXT_X : ICON_X;
    }

    /** Like the inventory, with the level for anything above I. */
    private static String getName(PotionEffect effect, Potion potion) {
        String name = StatCollector.translateToLocal(potion.getName());
        int level = effect.getAmplifier() + 1;
        if (level > 1) {
            String key = "enchantment.level." + level;
            name += " " + (StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key) : level);
        }
        return name;
    }

    /** "Infinite" instead of vanilla's **:** */
    private static String getTime(PotionEffect effect) {
        return effect.getIsPotionDurationMax() ? StatCollector.translateToLocal("dashboard.status_effects.infinite")
            : Potion.getDurationString(effect);
    }

    @SubscribeEvent
    public static void onPostRender(RenderGameOverlayEvent.Post event) {
        Collection<PotionEffect> effects = getEffects();
        if (event.type != ElementType.ALL || effects.isEmpty()) {
            return;
        }
        int screenWidth = event.resolution.getScaledWidth();
        int screenHeight = event.resolution.getScaledHeight();
        if (!HudEditor.beginRender(ELEMENT, screenWidth, screenHeight)) {
            return;
        }
        try {
            draw(effects, ELEMENT.getDefaultBounds(screenWidth, screenHeight));
        } finally {
            HudEditor.endRender();
        }
    }

    /** Icons like the inventory draws them, so modded effects with their own texture work too. */
    private static void draw(Collection<PotionEffect> effects, HudBounds bounds) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int boxWidth = getBoxWidth(effects);
        int x = bounds.x;
        int y = bounds.y;
        for (PotionEffect effect : order(effects)) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) {
                continue;
            }
            PreviewDrawing.bindTexture(INVENTORY);
            if (DashboardConfig.statusEffectBackground) {
                drawBox(x, y, boxWidth);
            }
            float alpha = getPulseAlpha(effect);
            if (DashboardConfig.statusEffectIcon) {
                GL11.glColor4f(1, 1, 1, alpha);
                if (potion.hasStatusIcon()) {
                    int index = potion.getStatusIconIndex();
                    PreviewDrawing.drawTexture(
                        x + ICON_X,
                        y + ICON_Y,
                        index % 8 * ICON_SIZE,
                        198 + index / 8 * ICON_SIZE,
                        ICON_SIZE,
                        ICON_SIZE);
                }
                // Forge hook for mods that draw their own icon
                potion.renderInventoryEffect(x, y, effect, Minecraft.getMinecraft());
            }
            int textColor = Math.max(4, Math.round(alpha * 255)) << 24 | 0xFFFFFF;
            int textX = x + getTextX();
            boolean text = potion.shouldRenderInvText(effect);
            boolean name = text && DashboardConfig.statusEffectName;
            boolean time = text && DashboardConfig.statusEffectTime;
            int centeredY = y + (BOX_HEIGHT - font.FONT_HEIGHT) / 2 + 1;
            if (name) {
                font.drawStringWithShadow(getName(effect, potion), textX, time ? y + NAME_Y : centeredY, textColor);
            }
            if (time) {
                String duration = getTime(effect);
                font.drawStringWithShadow(duration, textX, name ? y + TIME_Y : centeredY, textColor);
            }
            if (DashboardConfig.statusEffectHorizontal) {
                x += boxWidth + getSpacing();
            } else {
                y += BOX_HEIGHT + getSpacing();
            }
        }
    }

    /** The texture's left part and right edge, with its plain middle repeated for long names. */
    private static void drawBox(int x, int y, int width) {
        int inner = width - BOX_EDGE;
        int drawn = Math.min(inner, BOX_TEXTURE_WIDTH - BOX_EDGE);
        PreviewDrawing.drawTexture(x, y, 0, BOX_V, drawn, BOX_HEIGHT);
        while (drawn < inner) {
            int piece = Math.min(inner - drawn, BOX_MIDDLE_WIDTH);
            PreviewDrawing.drawTexture(x + drawn, y, BOX_MIDDLE_U, BOX_V, piece, BOX_HEIGHT);
            drawn += piece;
        }
        PreviewDrawing.drawTexture(x + inner, y, BOX_TEXTURE_WIDTH - BOX_EDGE, BOX_V, BOX_EDGE, BOX_HEIGHT);
    }
}
