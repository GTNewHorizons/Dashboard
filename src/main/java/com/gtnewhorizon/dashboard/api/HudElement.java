package com.gtnewhorizon.dashboard.api;

import net.minecraft.util.StatCollector;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

/** Something drawn on the HUD that the player can move or hide in the HUD editor. */
public final class HudElement {

    private static final long SHOWING_TIMEOUT_MILLIS = 500;

    @FunctionalInterface
    public interface DefaultBoundsProvider {

        /** Where the element is drawn, and how big it is, when the player has not moved it. */
        HudBounds getDefaultBounds(int screenWidth, int screenHeight);
    }

    @FunctionalInterface
    public interface PreviewRenderer {

        /**
         * Draws sample content the same way the real element is drawn, through {@link HudEditor#beginRender} or
         * {@link HudEditor#render}.
         */
        void drawPreview(int screenWidth, int screenHeight);
    }

    private final String id;
    private final String translationKey;
    private final DefaultBoundsProvider defaultBoundsProvider;
    private PreviewRenderer previewRenderer;
    private String modId;
    private String modName;
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

    /** Sample content the editor shows while the real element is not showing. Optional. */
    public HudElement setPreview(PreviewRenderer previewRenderer) {
        this.previewRenderer = previewRenderer;
        return this;
    }

    public PreviewRenderer getPreview() {
        return previewRenderer;
    }

    /** Only needed when the part of the id before ':' is not the mod id (ignoring case). */
    public HudElement setModId(String modId) {
        this.modId = modId;
        this.modName = null;
        return this;
    }

    /** The name of the mod this element belongs to, as shown in the mod list. */
    public String getModName() {
        if (modName == null) {
            modName = findModName();
        }
        return modName;
    }

    private String findModName() {
        String wantedModId = modId != null ? modId : id.substring(0, Math.max(0, id.indexOf(':')));
        if (wantedModId.equalsIgnoreCase("minecraft")) {
            return "Minecraft";
        }
        for (ModContainer mod : Loader.instance()
            .getActiveModList()) {
            if (mod.getModId()
                .equalsIgnoreCase(wantedModId)) {
                return mod.getName();
            }
        }
        return wantedModId;
    }

    /** True if the element was drawn recently, even if the player hid it. */
    public boolean isCurrentlyShowing() {
        return System.currentTimeMillis() - lastRenderTime < SHOWING_TIMEOUT_MILLIS;
    }

    void markRendered() {
        lastRenderTime = System.currentTimeMillis();
    }
}
