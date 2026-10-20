package com.gtnewhorizons.dashboard.api;

import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

/** Something drawn on the HUD that the player can move, resize or hide in the HUD editor. */
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
         * Draw through {@link HudRegistry#beginRender} or {@link HudRegistry#render}, so the sample moves and resizes
         * like the real element.
         */
        void drawPreview(int screenWidth, int screenHeight);
    }

    private final String id;
    private final String translationKey;
    private final DefaultBoundsProvider defaultBoundsProvider;
    private PreviewRenderer previewRenderer;
    private boolean previewOnTop;
    private BooleanSupplier activeCheck = () -> true;
    private String modId;
    private String modName;
    private String modNameAddon;
    private String ownSettingsKey;
    private String[] ownSettingsKeyBindings;
    private boolean visibleByDefault = true;
    private String descriptionKey;
    private String[] descriptionKeyBindings;
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

    /**
     * Draws the sample on top of other elements, even where one is showing, like a bar a mod draws over another one.
     * Without it, the sample is left out where another element shows, so samples do not pile up.
     */
    public HudElement setPreviewOnTop() {
        this.previewOnTop = true;
        return this;
    }

    public boolean isPreviewOnTop() {
        return previewOnTop;
    }

    /** False hides the element from the editor completely, like for a mod feature that is turned off. */
    public HudElement setActiveCheck(BooleanSupplier activeCheck) {
        this.activeCheck = activeCheck;
        return this;
    }

    public boolean isActive() {
        return activeCheck.getAsBoolean();
    }

    /** For HUD the player did not have before, like Dashboard's own additions, which they turn on in the editor. */
    public HudElement setHiddenByDefault() {
        this.visibleByDefault = false;
        return this;
    }

    public boolean isVisibleByDefault() {
        return visibleByDefault;
    }

    /**
     * Extra lines in the element's editor tooltip, explaining what it is. Optional.
     *
     * @param translationKey lang key of the text, with {@code %s} for key bindings, see {@link #translate}
     * @param keyBindings    descriptions of those keys in Controls, in order
     */
    public HudElement setDescription(String translationKey, String... keyBindings) {
        this.descriptionKey = translationKey;
        this.descriptionKeyBindings = keyBindings;
        return this;
    }

    /** Null without {@link #setDescription}. */
    public String getDescription() {
        return translate(descriptionKey, descriptionKeyBindings);
    }

    /** Only needed when the part of the id before ':' is not the mod id (ignoring case). */
    public HudElement setModId(String modId) {
        this.modId = modId;
        this.modName = null;
        return this;
    }

    /** The mod this element belongs to: the one set with {@link #setModId}, or the part of the id before ':'. */
    public String getModId() {
        return modId != null ? modId : id.substring(0, Math.max(0, id.indexOf(':')));
    }

    /** The name of the mod this element belongs to, as shown in the mod list. */
    public String getModName() {
        if (modName == null) {
            modName = findModName();
        }
        return modName;
    }

    /** Shown after the mod name in the editor, like another mod that draws this element. Optional. */
    public HudElement setModNameAddon(String modNameAddon) {
        this.modNameAddon = modNameAddon;
        return this;
    }

    /** The mod name as shown in the editor, with the {@link #setModNameAddon addon} after it. */
    public String getDisplayModName() {
        return modNameAddon != null ? getModName() + " / " + modNameAddon : getModName();
    }

    /**
     * Where the mod's own position setting for this element is, shown in the editor, since moving the element there
     * overrides it. Optional.
     *
     * @param translationKey lang key of the text, with {@code %s} for key bindings, like the one that opens
     *                       the settings
     * @param keyBindings    descriptions of those keys in Controls, in order
     */
    public HudElement setOwnSettings(String translationKey, String... keyBindings) {
        this.ownSettingsKey = translationKey;
        this.ownSettingsKeyBindings = keyBindings;
        return this;
    }

    /** Null without {@link #setOwnSettings}. */
    public String getOwnSettings() {
        return translate(ownSettingsKey, ownSettingsKeyBindings);
    }

    /**
     * Null without a key. Each key binding fills two {@code %s} in order: the bound key, then its name in Controls. A
     * text with a single key can leave out the name.
     */
    private static String translate(String translationKey, String[] keyBindings) {
        if (translationKey == null) {
            return null;
        }
        if (keyBindings.length == 0) {
            return StatCollector.translateToLocal(translationKey);
        }
        Object[] keyTexts = new Object[keyBindings.length * 2];
        for (int i = 0; i < keyBindings.length; i++) {
            keyTexts[i * 2] = getKeyName(keyBindings[i]);
            keyTexts[i * 2 + 1] = StatCollector.translateToLocal(keyBindings[i]);
        }
        return StatCollector.translateToLocalFormatted(translationKey, keyTexts);
    }

    /** The key the player bound, so the text stays right when they change it. */
    private static String getKeyName(String keyBinding) {
        for (KeyBinding key : Minecraft.getMinecraft().gameSettings.keyBindings) {
            if (key.getKeyDescription()
                .equals(keyBinding) && key.getKeyCode() != 0) {
                return GameSettings.getKeyDisplayString(key.getKeyCode());
            }
        }
        return StatCollector.translateToLocal("dashboard.editor.element_tooltip.key_not_set");
    }

    private String findModName() {
        String wantedModId = getModId();
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
