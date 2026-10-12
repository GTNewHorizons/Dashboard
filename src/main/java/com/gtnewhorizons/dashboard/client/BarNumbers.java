package com.gtnewhorizons.dashboard.client;

import java.lang.ref.WeakReference;
import java.math.RoundingMode;
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
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.ForgeHooks;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.FormatOptions;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Numbers like 13/20 next to the bars, each its own element. Turned on in the Customize screen. */
@SideOnly(Side.CLIENT)
@EventBusSubscriber(side = Side.CLIENT)
public final class BarNumbers {

    private static final int GAP = 2;
    private static final int HEIGHT = 9;
    private static final FormatOptions WHOLE = new FormatOptions().setDecimalPlaces(0)
        .setRoundingMode(RoundingMode.CEILING);
    private static final FormatOptions ONE_DECIMAL = new FormatOptions().setDecimalPlaces(1);
    private static final FormatOptions PERCENT = new FormatOptions().setDecimalPlaces(0);

    private static final List<BarNumber> NUMBERS = new ArrayList<>();
    private static final Set<ElementType> DRAWN_BARS = EnumSet.noneOf(ElementType.class);
    private static final Map<HudElement, HudElement> BAR_NUMBERS = new HashMap<>();
    private static HudElement saturationNumber;
    private static WeakReference<IBossDisplayData> boss = new WeakReference<>(null);

    private BarNumbers() {}

    public static void register() {
        add(
            ElementType.HEALTH,
            "health_number",
            true,
            null,
            WHOLE,
            player -> new float[] { player.getHealth(), player.getMaxHealth() },
            previewFill(20),
            20);
        add(
            ElementType.ARMOR,
            "armor_number",
            true,
            null,
            WHOLE,
            player -> new float[] { ForgeHooks.getTotalArmorValue(player), 20 },
            previewFill(20),
            20);
        BarNumber food = add(
            ElementType.FOOD,
            "food_number",
            false,
            null,
            WHOLE,
            player -> new float[] { player.getFoodStats()
                .getFoodLevel(), 20 },
            previewFill(20),
            20);
        saturationNumber = add(
            ElementType.FOOD,
            "saturation_number",
            false,
            food,
            ONE_DECIMAL,
            player -> new float[] { player.getFoodStats()
                .getSaturationLevel(),
                player.getFoodStats()
                    .getFoodLevel() },
            4.6f,
            previewFill(20)).element;
        add(
            ElementType.AIR,
            "air_number",
            false,
            null,
            WHOLE,
            player -> new float[] { (float) Math.ceil(Math.max(0, player.getAir()) * 10 / 300.0), 10 },
            previewFill(10),
            10);
        add(ElementType.HEALTHMOUNT, "mount_health_number", false, null, WHOLE, player -> {
            if (player.ridingEntity instanceof EntityLivingBase mount) {
                return new float[] { mount.getHealth(), mount.getMaxHealth() };
            }
            return null;
        }, previewFill(20), 20);
        add(ElementType.BOSSHEALTH, "boss_health_number", false, null, WHOLE, player -> {
            IBossDisplayData current = boss.get();
            if (current != null) {
                return new float[] { current.getHealth(), current.getMaxHealth() };
            }
            return new float[] { BossStatus.healthScale * 100, 100 };
        }, previewFill(300), 300);
    }

    /** As full as the bar previews. */
    private static float previewFill(float max) {
        return Math.round(BarThemes.PREVIEW_FILL * max);
    }

    /** Vanilla only keeps the boss health as a fraction. */
    public static void setBoss(IBossDisplayData displayData) {
        if (boss.get() != displayData) {
            boss = new WeakReference<>(displayData);
        }
    }

    private static BarNumber add(ElementType type, String name, boolean leftSide, BarNumber besideNumber,
        FormatOptions format, Values values, float sampleCurrent, float sampleMax) {
        BarNumber number = new BarNumber(
            type,
            leftSide,
            besideNumber,
            format,
            values,
            new float[] { sampleCurrent, sampleMax });
        HudElement element = new HudElement("minecraft:" + name, "dashboard.element.minecraft." + name, number);
        element.setActiveCheck(() -> HudLayout.isNumberOn(element));
        number.element = element;
        HudEditor.register(element);
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

    /** Like the editor preview, in the number's style. */
    static String getSampleText(HudElement element) {
        for (BarNumber number : NUMBERS) {
            if (number.element == element) {
                return number.getSampleText();
            }
        }
        return "";
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onPreRender(RenderGameOverlayEvent.Pre event) {
        if (VanillaHudElements.willDraw(event.type)) {
            DRAWN_BARS.add(event.type);
        }
    }

    /** After every bar, so no bar is still moved. */
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
        private final FormatOptions format;
        private final Values values;
        private final float[] sample;
        private HudElement element;

        private BarNumber(ElementType type, boolean leftSide, BarNumber besideNumber, FormatOptions format,
            Values values, float[] sample) {
            this.type = type;
            this.leftSide = leftSide;
            this.besideNumber = besideNumber;
            this.format = format;
            this.values = values;
            this.sample = sample;
        }

        private void draw(int screenWidth, int screenHeight) {
            String text = getText();
            if (text == null || !HudEditor.beginRender(element, screenWidth, screenHeight)) {
                return;
            }
            try {
                drawText(text, getDefaultBounds(screenWidth, screenHeight));
            } finally {
                HudEditor.endRender();
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
            return current == null ? null : format(current);
        }

        private String getSampleText() {
            return format(sample);
        }

        private String format(float[] currentAndMax) {
            float current = currentAndMax[0];
            float max = currentAndMax[1];
            float percent = max > 0 ? current / max * 100 : 0;
            return HudLayout.getNumberStyle(element)
                .format(
                    NumberFormatUtil.formatNumber(current, format),
                    NumberFormatUtil.formatNumber(max, format),
                    NumberFormatUtil.formatNumber(percent, PERCENT));
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
