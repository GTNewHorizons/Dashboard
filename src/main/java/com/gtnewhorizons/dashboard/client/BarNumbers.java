package com.gtnewhorizons.dashboard.client;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.ForgeHooks;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.FormatOptions;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Numbers like 13/20 next to the bars. Each is its own element, so players can move it apart from its bar. */
@SideOnly(Side.CLIENT)
@EventBusSubscriber(side = Side.CLIENT)
public final class BarNumbers {

    private static final int GAP = 2;
    private static final int HEIGHT = 9;
    static final int MAX_DECIMALS = 2;
    private static final FormatOptions[] DECIMALS = { new FormatOptions().setDecimalPlaces(0),
        new FormatOptions().setDecimalPlaces(1), new FormatOptions().setDecimalPlaces(2) };
    private static final FormatOptions PERCENT = DECIMALS[0];

    private static final List<BarNumber> NUMBERS = new ArrayList<>();
    private static final Set<ElementType> DRAWN_BARS = EnumSet.noneOf(ElementType.class);
    private static final Map<HudElement, HudElement> BAR_NUMBERS = new HashMap<>();
    private static HudElement saturationNumber;
    private static WeakReference<IBossDisplayData> boss = new WeakReference<>(null);

    private BarNumbers() {}

    public static void register() {
        add(ElementType.HEALTH, "health_number", true, null, 0, player -> {
            float absorption = player.getAbsorptionAmount();
            return new float[] { player.getHealth() + absorption, player.getMaxHealth() + absorption };
        }, previewFill(20), 20);
        add(
            ElementType.ARMOR,
            "armor_number",
            true,
            null,
            0,
            player -> new float[] { ForgeHooks.getTotalArmorValue(player), 20 },
            previewPoints(20),
            20);
        BarNumber food = add(
            ElementType.FOOD,
            "food_number",
            false,
            null,
            0,
            player -> new float[] { player.getFoodStats()
                .getFoodLevel(), 20 },
            previewPoints(20),
            20);
        saturationNumber = add(
            ElementType.FOOD,
            "saturation_number",
            false,
            food,
            1,
            player -> new float[] { player.getFoodStats()
                .getSaturationLevel(),
                player.getFoodStats()
                    .getFoodLevel() },
            4.6f,
            previewPoints(20)).element;
        add(
            ElementType.AIR,
            "air_number",
            false,
            null,
            0,
            player -> new float[] { (float) Math.ceil(Math.max(0, player.getAir()) * 10 / 300.0), 10 },
            previewPoints(10),
            10);
        add(ElementType.HEALTHMOUNT, "mount_health_number", false, null, 0, player -> {
            if (player.ridingEntity instanceof EntityLivingBase mount) {
                return new float[] { mount.getHealth(), mount.getMaxHealth() };
            }
            return null;
        }, previewFill(20), 20);
        add(ElementType.BOSSHEALTH, "boss_health_number", false, null, 0, player -> {
            IBossDisplayData current = boss.get();
            if (current != null) {
                return new float[] { current.getHealth(), current.getMaxHealth() };
            }
            return new float[] { BossStatus.healthScale * 100, 100 };
        }, previewFill(300), 300);
    }

    /** As full as the bar previews. */
    private static float previewFill(float max) {
        return BarThemes.PREVIEW_FILL * max;
    }

    /** For values that are always whole, like armor points. */
    private static float previewPoints(float max) {
        return Math.round(previewFill(max));
    }

    /** Vanilla only keeps the boss health as a fraction, so the number reads it from the boss itself. */
    public static void setBoss(IBossDisplayData displayData) {
        if (boss.get() != displayData) {
            boss = new WeakReference<>(displayData);
        }
    }

    private static BarNumber add(ElementType type, String name, boolean leftSide, BarNumber besideNumber,
        int defaultDecimals, Values values, float sampleCurrent, float sampleMax) {
        BarNumber number = new BarNumber(
            type,
            leftSide,
            besideNumber,
            defaultDecimals,
            values,
            new float[] { sampleCurrent, sampleMax });
        HudElement element = new HudElement("minecraft:" + name, "dashboard.element.minecraft." + name, number);
        element.setActiveCheck(() -> HudLayout.isNumberOn(element))
            .setModId(Dashboard.MODID);
        number.element = element;
        HudRegistry.register(element);
        PreviewDrawing.setImitationInBounds(element, bounds -> number.drawText(number.getSampleText(), bounds));
        NUMBERS.add(number);
        if (besideNumber == null) {
            BAR_NUMBERS.put(VanillaHudElements.getElement(type), element);
        }
        return number;
    }

    /** The number next to this bar, or null if it has none. */
    static HudElement getNumber(HudElement bar) {
        return BAR_NUMBERS.get(bar);
    }

    static HudElement getSaturationNumber() {
        return saturationNumber;
    }

    static String getSampleText(HudElement element) {
        return find(element).getSampleText();
    }

    static int getDecimals(HudElement element) {
        return find(element).getDecimals();
    }

    /** The number's own default is saved as nothing, so it follows if the default changes. */
    static void setDecimals(HudElement element, int decimals) {
        HudLayout.setNumberDecimals(element, decimals == find(element).defaultDecimals ? null : decimals);
    }

    private static BarNumber find(HudElement element) {
        for (BarNumber number : NUMBERS) {
            if (number.element == element) {
                return number;
            }
        }
        throw new IllegalArgumentException("Not a bar number: " + element.getId());
    }

    /** Canceled bars count too, since themed bars cancel vanilla and draw themselves. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onPreRender(RenderGameOverlayEvent.Pre event) {
        if (VanillaHudElements.willDraw(event.type)) {
            DRAWN_BARS.add(event.type);
        }
    }

    /** At the end of the HUD, so no bar's move or resize is still applied. */
    @SubscribeEvent
    public static void onPostRender(RenderGameOverlayEvent.Post event) {
        if (event.type != ElementType.ALL) {
            return;
        }
        int screenWidth = event.resolution.getScaledWidth();
        int screenHeight = event.resolution.getScaledHeight();
        for (BarNumber number : NUMBERS) {
            if (DRAWN_BARS.contains(number.type)) {
                number.draw(screenWidth, screenHeight);
            }
        }
        DRAWN_BARS.clear();
    }

    /** {current, max}, or null when there is nothing to show. */
    @FunctionalInterface
    private interface Values {

        float[] get(EntityPlayer player);
    }

    private static final class BarNumber implements HudElement.DefaultBoundsProvider {

        private final ElementType type;
        private final boolean leftSide;
        private final BarNumber besideNumber;
        private final int defaultDecimals;
        private final Values values;
        private final float[] sample;
        private final CachedText realText = new CachedText();
        private final CachedText sampleText = new CachedText();
        private HudElement element;

        private BarNumber(ElementType type, boolean leftSide, BarNumber besideNumber, int defaultDecimals,
            Values values, float[] sample) {
            this.type = type;
            this.leftSide = leftSide;
            this.besideNumber = besideNumber;
            this.defaultDecimals = defaultDecimals;
            this.values = values;
            this.sample = sample;
        }

        private void draw(int screenWidth, int screenHeight) {
            // Before reading the player, since most numbers are turned off
            if (!element.isActive()) {
                return;
            }
            String text = getText();
            if (text == null || !HudRegistry.beginRender(element, screenWidth, screenHeight)) {
                return;
            }
            try {
                drawText(text, getDefaultBounds(screenWidth, screenHeight));
            } finally {
                HudRegistry.endRender();
            }
        }

        /** Against the bar side, since the box is sized for the real text. */
        private void drawText(String text, HudBounds bounds) {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            int x = leftSide ? bounds.getRight() - font.getStringWidth(text) : bounds.x;
            font.drawStringWithShadow(text, x, bounds.y + 1, 0xFFFFFF);
        }

        private String getText() {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            float[] current = player == null ? null : values.get(player);
            return current == null ? null : realText.get(current);
        }

        private String getSampleText() {
            return sampleText.get(sample);
        }

        private int getDecimals() {
            Integer saved = HudLayout.getNumberDecimals(element);
            return saved != null ? MathHelper.clamp_int(saved, 0, MAX_DECIMALS) : defaultDecimals;
        }

        /** Formats again only when something changed, since it runs every frame. */
        private final class CachedText {

            private float current = Float.NaN;
            private float max;
            private NumberStyle style;
            private int decimals = -1;
            private String text;

            private String get(float[] currentAndMax) {
                NumberStyle newStyle = HudLayout.getNumberStyle(element);
                int newDecimals = getDecimals();
                if (currentAndMax[0] != current || currentAndMax[1] != max
                    || newStyle != style
                    || newDecimals != decimals) {
                    current = currentAndMax[0];
                    max = currentAndMax[1];
                    style = newStyle;
                    decimals = newDecimals;
                    text = format();
                }
                return text;
            }

            private String format() {
                FormatOptions format = DECIMALS[decimals];
                float percent = max > 0 ? current / max * 100 : 0;
                return style.format(
                    NumberFormatUtil.formatNumber(current, format),
                    NumberFormatUtil.formatNumber(max, format),
                    NumberFormatUtil.formatNumber(percent, PERCENT));
            }
        }

        /** Next to the default spot of the bar (or the number it sits beside). */
        @Override
        public HudBounds getDefaultBounds(int screenWidth, int screenHeight) {
            String text = getText();
            int width = Minecraft.getMinecraft().fontRenderer.getStringWidth(text != null ? text : getSampleText());
            HudElement anchor = besideNumber != null && besideNumber.element.isActive() ? besideNumber.element
                : VanillaHudElements.getElement(type);
            HudBounds anchorBounds = anchor.getDefaultBounds(screenWidth, screenHeight);
            int x = leftSide ? anchorBounds.x - GAP - width : anchorBounds.getRight() + GAP;
            return new HudBounds(x, anchorBounds.getBottom() - HEIGHT, width, HEIGHT);
        }
    }
}
