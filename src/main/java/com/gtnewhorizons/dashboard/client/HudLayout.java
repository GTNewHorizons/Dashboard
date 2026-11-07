package com.gtnewhorizons.dashboard.client;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.DashboardOtherConfig;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * What the player changed per HUD element (place, size, look). There are three layouts to switch between, saved to
 * config/dashboard/hud_layout.json, hud_layout_2.json and hud_layout_3.json.
 */
@SideOnly(Side.CLIENT)
public final class HudLayout {

    public static final int MAX_SIZE_STEP = 5;
    public static final int LAYOUT_COUNT = 3;
    private static final int MAX_ATTACH_DEPTH = 16;
    private static final Map<String, String> DEFAULT_ATTACHMENTS = new HashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();
    private static final Type STATES_TYPE = new TypeToken<LinkedHashMap<String, HudElementState>>() {}.getType();

    private static Map<String, HudElementState> states;
    private static int loadedLayout;
    private static boolean changed;
    private static int scaleDisplayWidth = -1;
    private static int scaleDisplayHeight;
    private static int scaleSetting;
    private static boolean scaleUnicode;
    private static ScaledResolution resolution;

    private HudLayout() {}

    public static boolean isVisible(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        return state == null || state.visible == null ? element.isVisibleByDefault() : state.visible;
    }

    public static void setVisible(HudElement element, boolean visible) {
        getOrCreateState(element).visible = visible == element.isVisibleByDefault() ? null : visible;
        removeIfDefault(element);
        changed = true;
    }

    /** Null for vanilla, also when the theme's resource pack is gone. */
    public static String getTheme(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        return state == null || !BarThemes.isAvailable(state.theme) ? null : state.theme;
    }

    public static void setTheme(HudElement element, String theme) {
        getOrCreateState(element).theme = theme;
        removeIfDefault(element);
        changed = true;
    }

    public static boolean isFlipped(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        return state != null && state.flipped;
    }

    public static void setFlipped(HudElement element, boolean flipped) {
        getOrCreateState(element).flipped = flipped;
        removeIfDefault(element);
        changed = true;
    }

    public static boolean isNumberOn(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        return state != null && state.numberOn;
    }

    public static void setNumberOn(HudElement element, boolean on) {
        getOrCreateState(element).numberOn = on;
        removeIfDefault(element);
        changed = true;
    }

    public static NumberStyle getNumberStyle(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        return state == null || state.numberStyle == null ? NumberStyle.SLASH : state.numberStyle;
    }

    /** SLASH is saved as nothing, so untouched elements stay out of the file. */
    public static void setNumberStyle(HudElement element, NumberStyle style) {
        getOrCreateState(element).numberStyle = style == NumberStyle.SLASH ? null : style;
        removeIfDefault(element);
        changed = true;
    }

    /** Null for the number's own default. */
    public static Integer getNumberDecimals(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        return state == null ? null : state.numberDecimals;
    }

    public static void setNumberDecimals(HudElement element, Integer decimals) {
        getOrCreateState(element).numberDecimals = decimals;
        removeIfDefault(element);
        changed = true;
    }

    /**
     * How much bigger or smaller the player made this element, counted in GUI scale steps. -1 draws it one GUI scale
     * smaller than the rest of the HUD, 0 the same size.
     */
    public static int getSizeStep(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        return state == null ? 0 : MathHelper.clamp_int(state.sizeStep, -MAX_SIZE_STEP, MAX_SIZE_STEP);
    }

    public static void setSizeStep(HudElement element, int step) {
        getOrCreateState(element).sizeStep = MathHelper.clamp_int(step, -MAX_SIZE_STEP, MAX_SIZE_STEP);
        removeIfDefault(element);
        changed = true;
    }

    /** For image elements: 1 shows the whole picture, 2 half of it twice as big. */
    public static float getZoom(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        return state == null || state.zoom == null ? 1 : state.zoom;
    }

    /** The middle of the shown part, from 0 to 1 across the picture. */
    public static float getViewX(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        return state == null || state.zoom == null ? 0.5f : state.viewX;
    }

    public static float getViewY(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        return state == null || state.zoom == null ? 0.5f : state.viewY;
    }

    /** A zoom of 1 or less is saved as nothing, so untouched images stay out of the file. */
    public static void setZoom(HudElement element, float zoom, float viewX, float viewY) {
        HudElementState state = getOrCreateState(element);
        state.zoom = zoom > 1 ? zoom : null;
        state.viewX = zoom > 1 ? viewX : 0;
        state.viewY = zoom > 1 ? viewY : 0;
        removeIfDefault(element);
        changed = true;
    }

    /** Null when never changed, see {@link ElementOpacity}. */
    public static Integer getOpacity(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        return state == null ? null : state.opacity;
    }

    public static void setOpacity(HudElement element, Integer percent) {
        getOrCreateState(element).opacity = percent;
        removeIfDefault(element);
        changed = true;
    }

    public static boolean isLocked(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        return state != null && state.locked;
    }

    public static void setLocked(HudElement element, boolean locked) {
        getOrCreateState(element).locked = locked;
        removeIfDefault(element);
        changed = true;
    }

    /**
     * The GUI scale this element is drawn at. Whole GUI scales keep every texture pixel the same number of screen
     * pixels, so a resized element stays as sharp as the rest of the HUD.
     */
    public static int getElementGuiScale(HudElement element) {
        return Math.max(1, getGameGuiScale() + getSizeStep(element));
    }

    /** The factor HudRegistry resizes this element's drawing by, on top of the game's own GUI scale. */
    public static float getScale(HudElement element) {
        int step = getSizeStep(element);
        if (step == 0) {
            return 1;
        }
        int gameScale = getGameGuiScale();
        return Math.max(1, gameScale + step) / (float) gameScale;
    }

    static int getGameGuiScale() {
        return getResolution().getScaleFactor();
    }

    /** Measured again only when something it depends on changes, since the HUD asks for it every frame. */
    public static ScaledResolution getResolution() {
        Minecraft mc = Minecraft.getMinecraft();
        boolean unicode = mc.func_152349_b();
        if (mc.displayWidth != scaleDisplayWidth || mc.displayHeight != scaleDisplayHeight
            || mc.gameSettings.guiScale != scaleSetting
            || unicode != scaleUnicode) {
            scaleDisplayWidth = mc.displayWidth;
            scaleDisplayHeight = mc.displayHeight;
            scaleSetting = mc.gameSettings.guiScale;
            scaleUnicode = unicode;
            resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        }
        return resolution;
    }

    public static HudBounds getBounds(HudElement element, int screenWidth, int screenHeight) {
        return getBounds(element, element.getDefaultBounds(screenWidth, screenHeight), screenWidth, screenHeight);
    }

    /** Where the element should be drawn and how big, kept fully on screen. */
    public static HudBounds getBounds(HudElement element, HudBounds defaultBounds, int screenWidth, int screenHeight) {
        return getBounds(element, defaultBounds, screenWidth, screenHeight, 0);
    }

    /** Depth counts the attached elements followed so far, so a loop in an edited file can not go on forever. */
    private static HudBounds getBounds(HudElement element, HudBounds defaultBounds, int screenWidth, int screenHeight,
        int depth) {
        HudElementState state = getStates().get(element.getId());
        float scale = getScale(element);
        HudElement parent = depth < MAX_ATTACH_DEPTH ? getAttachedTo(element) : null;
        boolean moved = state != null && state.moved && (state.attachedTo == null || parent != null);
        if (!moved && parent == null && scale == 1) {
            return defaultBounds;
        }
        int width = Math.round(defaultBounds.width * scale);
        int height = Math.round(defaultBounds.height * scale);

        int x;
        int y;
        if (parent != null) {
            HudBounds parentDefault = parent.getDefaultBounds(screenWidth, screenHeight);
            HudBounds parentBounds = getBounds(parent, parentDefault, screenWidth, screenHeight, depth + 1);
            // Not moved yet, so it keeps its default distance from the element it follows by default
            x = parentBounds.x + (moved ? state.offsetX : defaultBounds.x - parentDefault.x);
            y = parentBounds.y + (moved ? state.offsetY : defaultBounds.y - parentDefault.y);
        } else if (moved) {
            x = anchorPosition(state.anchorX, width, screenWidth) + state.offsetX;
            y = anchorPosition(state.anchorY, height, screenHeight) + state.offsetY;
        } else {
            // Resized around the default spot's anchor, so the hotbar stays at the bottom center
            float anchorX = closestAnchor(defaultBounds.x + defaultBounds.width / 2, screenWidth);
            float anchorY = closestAnchor(defaultBounds.y + defaultBounds.height / 2, screenHeight);
            x = anchorPosition(anchorX, width, screenWidth) + defaultBounds.x
                - anchorPosition(anchorX, defaultBounds.width, screenWidth);
            y = anchorPosition(anchorY, height, screenHeight) + defaultBounds.y
                - anchorPosition(anchorY, defaultBounds.height, screenHeight);
        }
        return new HudBounds(
            clampToScreen(x, width, screenWidth),
            clampToScreen(y, height, screenHeight),
            width,
            height);
    }

    /** Moves the element's top left corner to the given position, anchored to the closest edge or the center. */
    public static void moveTo(HudElement element, int x, int y, int screenWidth, int screenHeight) {
        HudBounds defaultBounds = element.getDefaultBounds(screenWidth, screenHeight);
        float scale = getScale(element);
        int width = Math.round(defaultBounds.width * scale);
        int height = Math.round(defaultBounds.height * scale);
        x = clampToScreen(x, width, screenWidth);
        y = clampToScreen(y, height, screenHeight);

        HudElementState state = getOrCreateState(element);
        state.moved = true;
        changed = true;
        HudElement parent = getAttachedTo(element);
        if (parent != null) {
            HudBounds parentBounds = getBounds(parent, screenWidth, screenHeight);
            state.attachedTo = parent.getId();
            state.offsetX = x - parentBounds.x;
            state.offsetY = y - parentBounds.y;
            return;
        }
        state.attachedTo = null;
        state.anchorX = closestAnchor(x + width / 2, screenWidth);
        state.anchorY = closestAnchor(y + height / 2, screenHeight);
        state.offsetX = x - anchorPosition(state.anchorX, width, screenWidth);
        state.offsetY = y - anchorPosition(state.anchorY, height, screenHeight);
    }

    public static boolean isMoved(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        return state != null && state.moved;
    }

    /**
     * Null when not attached, or when the element it is attached to is not loaded, like after removing its mod.
     * Elements attached by default count until the player detaches them.
     */
    public static HudElement getAttachedTo(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        String id;
        if (state != null && state.attachedTo != null) {
            id = state.attachedTo;
        } else if (state != null && state.detached) {
            return null;
        } else {
            id = DEFAULT_ATTACHMENTS.get(element.getId());
        }
        return id == null ? null : HudRegistry.getElement(id);
    }

    /** Follows the other element until the player detaches it, like the XP level number and the XP bar. */
    static void attachByDefault(HudElement element, HudElement attachTo) {
        DEFAULT_ATTACHMENTS.put(element.getId(), attachTo.getId());
    }

    /**
     * Makes the element follow the other one, staying where it is now. False for itself, or for an element that
     * already follows this one, since they would follow each other in a circle.
     */
    public static boolean attach(HudElement element, HudElement attachTo, int screenWidth, int screenHeight) {
        HudElement parent = attachTo;
        for (int depth = 0; parent != null && depth < MAX_ATTACH_DEPTH; depth++) {
            if (parent == element) {
                return false;
            }
            parent = getAttachedTo(parent);
        }
        HudBounds bounds = getBounds(element, screenWidth, screenHeight);
        HudBounds parentBounds = getBounds(attachTo, screenWidth, screenHeight);
        HudElementState state = getOrCreateState(element);
        state.attachedTo = attachTo.getId();
        state.detached = false;
        state.moved = true;
        state.offsetX = bounds.x - parentBounds.x;
        state.offsetY = bounds.y - parentBounds.y;
        changed = true;
        return true;
    }

    /** Stops following, staying where it is now. Remembered, so an element attached by default stays loose. */
    public static void detach(HudElement element, int screenWidth, int screenHeight) {
        HudBounds bounds = getBounds(element, screenWidth, screenHeight);
        HudElementState state = getOrCreateState(element);
        state.attachedTo = null;
        state.detached = DEFAULT_ATTACHMENTS.containsKey(element.getId());
        moveTo(element, bounds.x, bounds.y, screenWidth, screenHeight);
    }

    /**
     * Moves the element back to its default position, which also puts its attachment back to the default one. Hidden
     * stays.
     */
    public static void resetPosition(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        if (state != null) {
            state.moved = false;
            state.attachedTo = null;
            state.detached = false;
            removeIfDefault(element);
            changed = true;
        }
    }

    /** Positions, sizes, image zoom, background opacity and visibility. Looks are reset in the Customize screen. */
    public static void reset(Iterable<HudElement> elements) {
        for (HudElement element : elements) {
            HudElementState state = getStates().get(element.getId());
            if (state != null) {
                state.visible = null;
                state.moved = false;
                state.attachedTo = null;
                state.detached = false;
                state.sizeStep = 0;
                state.zoom = null;
                state.opacity = null;
                removeIfDefault(element);
            }
        }
        changed = true;
    }

    /** From 1 to {@link #LAYOUT_COUNT}. */
    public static int getActiveLayout() {
        return MathHelper.clamp_int(DashboardOtherConfig.activeLayout, 1, LAYOUT_COUNT);
    }

    /** Cycles to the next layout. */
    public static void switchToNextLayout() {
        DashboardOtherConfig.activeLayout = getActiveLayout() % LAYOUT_COUNT + 1;
        ConfigurationManager.save(DashboardOtherConfig.class);
        getStates();
        VanillaHudElements.showShadowedActionBarText(
            StatCollector.translateToLocalFormatted("dashboard.message.layout", getActiveLayout()));
    }

    /** Called whenever an editor screen closes, so it only writes when something changed. */
    public static void save() {
        if (!changed) {
            return;
        }
        HudRegistry.notifyLayoutChanged();
        write();
    }

    /** To the file of the layout that is loaded, which is not the active one while switching. */
    private static void write() {
        File file = getFile(loadedLayout);
        try {
            Files.createDirectories(
                file.getParentFile()
                    .toPath());
            try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(states, STATES_TYPE, writer);
            }
            changed = false;
        } catch (Exception e) {
            Dashboard.LOG.error("Failed to save the HUD layout to {}", file, e);
        }
    }

    /** Loads the active layout, saving the one before it first when the player switched. */
    private static Map<String, HudElementState> getStates() {
        int layout = getActiveLayout();
        if (states == null || layout != loadedLayout) {
            boolean switched = states != null;
            if (changed) {
                write();
            }
            changed = false;
            loadedLayout = layout;
            states = load();
            if (switched) {
                HudRegistry.notifyLayoutChanged();
            }
        }
        return states;
    }

    private static Map<String, HudElementState> load() {
        File file = getFile(loadedLayout);
        if (file.isFile()) {
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                Map<String, HudElementState> loaded = GSON.fromJson(reader, STATES_TYPE);
                if (loaded != null) {
                    return loaded;
                }
            } catch (Exception e) {
                Dashboard.LOG.error("Failed to load the HUD layout from {}, using defaults", file, e);
            }
        }
        return new LinkedHashMap<>();
    }

    /** The first layout keeps the name it had before there were more. */
    private static File getFile(int layout) {
        String name = layout == 1 ? "hud_layout.json" : "hud_layout_" + layout + ".json";
        return new File(Minecraft.getMinecraft().mcDataDir, "config/dashboard/" + name);
    }

    private static HudElementState getOrCreateState(HudElement element) {
        return getStates().computeIfAbsent(element.getId(), id -> new HudElementState());
    }

    private static void removeIfDefault(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        if (state != null && state.isDefault()) {
            getStates().remove(element.getId());
        }
    }

    /** The left edge, center or right edge, depending on which third of the screen the point is in. */
    private static float closestAnchor(int center, int screenSize) {
        if (center < screenSize / 3) return 0f;
        if (center > screenSize * 2 / 3) return 1f;
        return 0.5f;
    }

    private static int anchorPosition(float anchor, int elementSize, int screenSize) {
        return Math.round(anchor * (screenSize - elementSize));
    }

    /** Stays at 0 if the element is bigger than the screen. */
    static int clampToScreen(int position, int elementSize, int screenSize) {
        return Math.max(0, Math.min(position, screenSize - elementSize));
    }
}
