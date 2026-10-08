package com.gtnewhorizon.dashboard.api;

import net.minecraft.util.StatCollector;

/** Something drawn on the HUD that the player can move or hide in the HUD editor. */
public final class HudElement {

    private static final long SHOWING_TIMEOUT_MILLIS = 500;

    @FunctionalInterface
    public interface DefaultBoundsProvider {

        /** Where the element is drawn, and how big it is, when the player has not moved it. */
        HudBounds getDefaultBounds(int screenWidth, int screenHeight);
    }

    private final String id;
    private final String translationKey;
    private final DefaultBoundsProvider defaultBoundsProvider;
    private long lastRenderTime;

    /**
     * @param id             unique id used in the saved layout, like {@code "mymod:mana_bar"}
     * @param translationKey lang key of the name shown in the editor
     */
    public HudElement(String id, String translationKey, DefaultBoundsProvider defaultBoundsProvider) {
        this.id = id;
        this.translationKey = translationKey;
        this.defaultBoundsProvider = defaultBoundsProvider;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return StatCollector.translateToLocal(translationKey);
    }

    public HudBounds getDefaultBounds(int screenWidth, int screenHeight) {
        return defaultBoundsProvider.getDefaultBounds(screenWidth, screenHeight);
    }

    /** True if the element was drawn recently, even if the player hid it. */
    public boolean isCurrentlyShowing() {
        return System.currentTimeMillis() - lastRenderTime < SHOWING_TIMEOUT_MILLIS;
    }

    void markRendered() {
        lastRenderTime = System.currentTimeMillis();
    }
}
