package com.gtnewhorizon.dashboard.client;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.gtnewhorizon.dashboard.Dashboard;
import com.gtnewhorizon.dashboard.api.HudBounds;
import com.gtnewhorizon.dashboard.api.HudElement;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Where the player moved each HUD element and which ones they hid, saved to config/dashboard/hud_layout.json. */
@SideOnly(Side.CLIENT)
public final class HudLayout {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();
    private static final Type STATES_TYPE = new TypeToken<LinkedHashMap<String, HudElementState>>() {}.getType();

    private static Map<String, HudElementState> states;
    private static boolean changed;

    private HudLayout() {}

    public static boolean isVisible(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        return state == null || state.visible;
    }

    public static void setVisible(HudElement element, boolean visible) {
        getOrCreateState(element).visible = visible;
        removeIfDefault(element);
        changed = true;
    }

    public static HudBounds getBounds(HudElement element, int screenWidth, int screenHeight) {
        return getBounds(element, element.getDefaultBounds(screenWidth, screenHeight), screenWidth, screenHeight);
    }

    /** Where the element should be drawn, kept fully on screen. */
    public static HudBounds getBounds(HudElement element, HudBounds defaultBounds, int screenWidth, int screenHeight) {
        HudElementState state = getStates().get(element.getId());
        if (state == null || !state.moved) {
            return defaultBounds;
        }

        int x = anchorPosition(state.anchorX, defaultBounds.width, screenWidth) + state.offsetX;
        int y = anchorPosition(state.anchorY, defaultBounds.height, screenHeight) + state.offsetY;
        return new HudBounds(
            clamp(x, defaultBounds.width, screenWidth),
            clamp(y, defaultBounds.height, screenHeight),
            defaultBounds.width,
            defaultBounds.height);
    }

    /** Moves the element's top left corner to the given position, anchored to the closest edge or the center. */
    public static void moveTo(HudElement element, int x, int y, int screenWidth, int screenHeight) {
        HudBounds defaultBounds = element.getDefaultBounds(screenWidth, screenHeight);
        x = clamp(x, defaultBounds.width, screenWidth);
        y = clamp(y, defaultBounds.height, screenHeight);

        HudElementState state = getOrCreateState(element);
        state.moved = true;
        state.anchorX = closestAnchor(x + defaultBounds.width / 2, screenWidth);
        state.anchorY = closestAnchor(y + defaultBounds.height / 2, screenHeight);
        state.offsetX = x - anchorPosition(state.anchorX, defaultBounds.width, screenWidth);
        state.offsetY = y - anchorPosition(state.anchorY, defaultBounds.height, screenHeight);
        changed = true;
    }

    /** Moves the element back to its default position. A hidden element stays hidden. */
    public static void resetPosition(HudElement element) {
        HudElementState state = getStates().get(element.getId());
        if (state != null) {
            state.moved = false;
            removeIfDefault(element);
            changed = true;
        }
    }

    public static void resetAll() {
        getStates().clear();
        changed = true;
    }

    /** Only write the file if something changed. */
    public static void save() {
        if (!changed) {
            return;
        }
        File file = getFile();
        try {
            Files.createDirectories(
                file.getParentFile()
                    .toPath());
            try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(getStates(), STATES_TYPE, writer);
            }
            changed = false;
        } catch (Exception e) {
            Dashboard.LOG.error("Failed to save the HUD layout to {}", file, e);
        }
    }

    private static Map<String, HudElementState> getStates() {
        if (states == null) {
            states = load();
        }
        return states;
    }

    private static Map<String, HudElementState> load() {
        File file = getFile();
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

    private static File getFile() {
        return new File(Minecraft.getMinecraft().mcDataDir, "config/dashboard/hud_layout.json");
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

    private static int clamp(int position, int elementSize, int screenSize) {
        return Math.max(0, Math.min(position, screenSize - elementSize));
    }
}
