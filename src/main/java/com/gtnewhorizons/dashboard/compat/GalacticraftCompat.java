package com.gtnewhorizons.dashboard.compat;

import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Galacticraft's overlays. They are drawn after the rest of the HUD and reset the matrix first, so the move is applied
 * right after that reset. The docking overlay is left alone, since its text is spread over the whole screen.
 */
@SideOnly(Side.CLIENT)
public final class GalacticraftCompat {

    private static final int OXYGEN_WIDTH = 54;
    private static final int OXYGEN_HEIGHT = 47;
    private static final int OXYGEN_EDGE_MARGIN = 10;
    private static final int COUNTDOWN_WIDTH = 64;
    private static final int COUNTDOWN_HEIGHT = 32;
    private static final int ROCKET_ALTITUDE_WIDTH = 14;
    private static final int ROCKET_ALTITUDE_HEIGHT = 158;

    public static final HudElement OXYGEN = new HudElement(
        "galacticraft:oxygen",
        "dashboard.element.galacticraft.oxygen",
        GalacticraftCompat::getOxygenBounds).setOwnSettings("dashboard.element.galacticraft.oxygen.own_settings");

    public static final HudElement OXYGEN_WARNING = new HudElement(
        "galacticraft:oxygen_warning",
        "dashboard.element.galacticraft.oxygen_warning",
        GalacticraftCompat::getOxygenWarningBounds);

    public static final HudElement LAUNCH_COUNTDOWN = new HudElement(
        "galacticraft:launch_countdown",
        "dashboard.element.galacticraft.launch_countdown",
        (screenWidth, screenHeight) -> new HudBounds(
            screenWidth / 2 - COUNTDOWN_WIDTH / 2,
            screenHeight / 20 * 4,
            COUNTDOWN_WIDTH,
            COUNTDOWN_HEIGHT));

    public static final HudElement LANDER = new HudElement(
        "galacticraft:lander",
        "dashboard.element.galacticraft.lander",
        GalacticraftCompat::getLanderBounds);

    public static final HudElement ROCKET_ALTITUDE = new HudElement(
        "galacticraft:rocket_altitude",
        "dashboard.element.galacticraft.rocket_altitude",
        (screenWidth,
            screenHeight) -> new HudBounds(0, screenHeight / 2 - 85, ROCKET_ALTITUDE_WIDTH, ROCKET_ALTITUDE_HEIGHT));

    private static Fields.ClassFields<Object>.Field<Boolean> oxygenLeft;
    private static Fields.ClassFields<Object>.Field<Boolean> oxygenBottom;
    private static final ShiftTracker OVERLAY_SHIFT = new ShiftTracker();

    private static Method noAtmosphericCombustionMethod;
    private static int airlessDimension = Integer.MIN_VALUE;
    private static boolean airless;

    static {
        // Here rather than in register(), so addons like GalaxySpace can use them in any load order
        try {
            Fields.ClassFields<Object> config = ModFields
                .of("micdoodle8.mods.galacticraft.core.util.ConfigManagerCore");
            oxygenLeft = ModFields.booleanField(config, "oxygenIndicatorLeft");
            oxygenBottom = ModFields.booleanField(config, "oxygenIndicatorBottom");
            noAtmosphericCombustionMethod = Class.forName("micdoodle8.mods.galacticraft.core.util.OxygenUtil")
                .getMethod("noAtmosphericCombustion", WorldProvider.class);
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not read the Galacticraft HUD settings, assuming the top right corner", e);
        }
    }

    private GalacticraftCompat() {}

    public static void register() {
        setPreviews();
        for (HudElement element : new HudElement[] { OXYGEN, OXYGEN_WARNING, LAUNCH_COUNTDOWN, LANDER,
            ROCKET_ALTITUDE }) {
            HudEditor.register(element.setModId(Mods.GALACTICRAFT.getID()));
        }
    }

    /**
     * Called by the overlay mixins right after the overlay resets the matrix. False means the element is hidden, so the
     * overlay should stop.
     */
    public static boolean beginOverlay(HudElement element) {
        return OVERLAY_SHIFT.begin(element);
    }

    /** Called when the overlay ends. Overlays that stopped early or never reset the matrix were not moved. */
    public static void endOverlay() {
        OVERLAY_SHIFT.end();
    }

    /**
     * The oxygen gauge and warning use Galacticraft's own drawing code with sample values, which the overlay mixins
     * move
     * like the real thing. The others need a rocket or lander to draw, so they are imitated.
     */
    private static void setPreviews() {
        try {
            String overlays = "micdoodle8.mods.galacticraft.core.client.gui.overlay.";
            Method drawOxygen = Class.forName(overlays + "OverlayOxygenTanks")
                .getMethod(
                    "renderOxygenTankIndicator",
                    int.class,
                    int.class,
                    int.class,
                    boolean.class,
                    boolean.class,
                    boolean.class);
            // Normal temperature (22), tanks partly empty. Galacticraft takes "right" and "top", not left and bottom
            OXYGEN.setPreview(
                (screenWidth, screenHeight) -> invokeOverlay(
                    drawOxygen,
                    22,
                    30,
                    60,
                    !isIndicatorOnLeft(),
                    !isIndicatorAtBottom(),
                    false));
            Method drawWarning = Class.forName(overlays + "OverlayOxygenWarning")
                .getMethod("renderOxygenWarningOverlay");
            OXYGEN_WARNING.setPreview((screenWidth, screenHeight) -> invokeOverlay(drawWarning));
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not find the Galacticraft overlays, their HUD editor previews are off", e);
        }

        PreviewDrawing.setImitation(LAUNCH_COUNTDOWN, GalacticraftCompat::drawCountdownPreview);
        PreviewDrawing.setImitation(LANDER, GalacticraftCompat::drawLanderPreview);
        PreviewDrawing.setImitationInBounds(ROCKET_ALTITUDE, GalacticraftCompat::drawRocketAltitudePreview);
    }

    private static void invokeOverlay(Method overlay, Object... arguments) {
        try {
            overlay.invoke(null, arguments);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not draw Galacticraft overlay " + overlay.getName(), e);
        }
    }

    /** Like Galacticraft's: a red number at 4 times the normal text size. */
    private static void drawCountdownPreview(int screenWidth, int screenHeight) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String count = "10";
        GL11.glPushMatrix();
        GL11.glScalef(4, 4, 1);
        font.drawString(count, screenWidth / 8 - font.getStringWidth(count) / 2, screenHeight / 20, 0xFF0000);
        GL11.glPopMatrix();
    }

    /** Like Galacticraft's: the double size warning and the speed line below it. */
    private static void drawLanderPreview(int screenWidth, int screenHeight) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String warning = translate("gui.warning");
        String pressKey = getPressKeyLine();
        String speed = getSpeedLine();

        GL11.glPushMatrix();
        GL11.glScalef(2, 2, 1);
        font.drawString(warning, screenWidth / 4 - font.getStringWidth(warning) / 2, screenHeight / 8 - 20, 0xFF0000);
        font.drawString(pressKey, screenWidth / 4 - font.getStringWidth(pressKey) / 2, screenHeight / 8, 0xFFFFFF);
        GL11.glPopMatrix();
        font.drawString(speed, screenWidth / 2 - font.getStringWidth(speed) / 2, screenHeight / 3, 0xFFFF00);
    }

    /** A simple stand-in: the bar with the rocket marker halfway up. */
    private static void drawRocketAltitudePreview(HudBounds bounds) {
        Gui.drawRect(bounds.x + 2, bounds.y, bounds.getRight() - 2, bounds.getBottom(), 0xA0303030);
        int markerY = bounds.y + bounds.height / 2;
        Gui.drawRect(bounds.x, markerY - 3, bounds.getRight(), markerY + 3, 0xFFE0E0E0);
    }

    /** "Warning!" and the oxygen setup message, both double size and centered. */
    private static HudBounds getOxygenWarningBounds(int screenWidth, int screenHeight) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int width = 2 * Math
            .max(font.getStringWidth(translate("gui.warning")), font.getStringWidth(translate("gui.oxygen.warning")));
        int top = getWarningTop(screenHeight);
        int bottom = screenHeight / 8 * 2 + font.FONT_HEIGHT * 2;
        return new HudBounds(screenWidth / 2 - width / 2, top, width, bottom - top);
    }

    /** The two double size warning lines, plus the normal size speed line at a third of the screen height. */
    private static HudBounds getLanderBounds(int screenWidth, int screenHeight) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int width = Math.max(2 * font.getStringWidth(getPressKeyLine()), font.getStringWidth(getSpeedLine()));
        int top = getWarningTop(screenHeight);
        int bottom = Math.max(screenHeight / 3 + font.FONT_HEIGHT, top + font.FONT_HEIGHT * 2);
        return new HudBounds(screenWidth / 2 - width / 2, top, width, bottom - top);
    }

    private static String getPressKeyLine() {
        return translate("gui.lander.warning2") + "SPACE" + translate("gui.lander.warning3");
    }

    private static String getSpeedLine() {
        return translate("gui.lander.velocity") + ": -10.0 " + translate("gui.lander.velocityu");
    }

    /** The warnings are drawn at double size, at {@code height / 8 - 20} in their scaled coordinates. */
    private static int getWarningTop(int screenHeight) {
        return (screenHeight / 8 - 20) * 2;
    }

    /** Like Galacticraft's {@code GCCoreUtil.translate}, which cuts off comments after a '#'. */
    private static String translate(String key) {
        String text = StatCollector.translateToLocal(key);
        int commentStart = text.indexOf('#');
        return commentStart > 0 ? text.substring(0, commentStart)
            .trim() : text;
    }

    /** The gauge's warning arrow sticks out 5 pixels on the left. */
    private static HudBounds getOxygenBounds(int screenWidth, int screenHeight) {
        int x = isIndicatorOnLeft() ? OXYGEN_EDGE_MARGIN - 5 : screenWidth - OXYGEN_EDGE_MARGIN - OXYGEN_WIDTH;
        int y = isIndicatorAtBottom() ? screenHeight - OXYGEN_HEIGHT - OXYGEN_EDGE_MARGIN : OXYGEN_EDGE_MARGIN;
        return new HudBounds(x, y, OXYGEN_WIDTH, OXYGEN_HEIGHT);
    }

    /** Galacticraft's {@code oxygenIndicatorLeft} config, also used by addons for their gauges. */
    public static boolean isIndicatorOnLeft() {
        return ModFields.readBoolean(oxygenLeft, false);
    }

    /** Galacticraft's {@code oxygenIndicatorBottom} config, also used by addons for their gauges. */
    public static boolean isIndicatorAtBottom() {
        return ModFields.readBoolean(oxygenBottom, false);
    }

    /** True on planets without breathable air, where Galacticraft shows the oxygen gauge. */
    public static boolean isCurrentWorldAirless() {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null || noAtmosphericCombustionMethod == null) {
            return false;
        }
        // Asked again only after changing dimension, since the answer belongs to the dimension
        int dimension = world.provider.dimensionId;
        if (dimension != airlessDimension) {
            airlessDimension = dimension;
            try {
                airless = (boolean) noAtmosphericCombustionMethod.invoke(null, world.provider);
            } catch (ReflectiveOperationException e) {
                airless = false;
            }
        }
        return airless;
    }
}
