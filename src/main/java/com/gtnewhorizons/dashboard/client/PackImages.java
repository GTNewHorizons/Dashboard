package com.gtnewhorizons.dashboard.client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Pictures from resource packs, each its own HUD element. Packs list them in images.json next to the bar themes, with
 * the picture in the images folder. Animated pictures stack their frames from top to bottom, like vanilla's.
 */
@SideOnly(Side.CLIENT)
@EventBusSubscriber(side = Side.CLIENT)
public final class PackImages {

    private static final String FOLDER = "textures/gui/";
    private static final String MOD_NAME_ADDON = "Resource Pack";
    private static final int DEFAULT_SIZE = 32;
    private static final long MILLIS_PER_TICK = 50;
    private static final Map<String, PackImage> IMAGES = new LinkedHashMap<>();

    private PackImages() {}

    public static void load(IResourceManager resources) {
        for (PackImage image : IMAGES.values()) {
            image.available = false;
        }
        List<?> lists;
        try {
            lists = resources.getAllResources(new ResourceLocation(Dashboard.MODID, FOLDER + "images.json"));
        } catch (IOException e) {
            lists = Collections.emptyList();
        }
        for (Object entry : lists) {
            IResource list = (IResource) entry;
            try (Reader reader = new InputStreamReader(list.getInputStream(), StandardCharsets.UTF_8)) {
                for (Map.Entry<String, JsonElement> image : new JsonParser().parse(reader)
                    .getAsJsonObject()
                    .entrySet()) {
                    try {
                        define(
                            image.getKey(),
                            image.getValue()
                                .getAsJsonObject());
                    } catch (RuntimeException e) {
                        Dashboard.LOG.warn("Pack image {} could not be read, skipping it", image.getKey(), e);
                    }
                }
            } catch (IOException | RuntimeException e) {
                Dashboard.LOG.error("Could not read an images.json", e);
            }
        }
    }

    private static void define(String id, JsonObject json) {
        ResourceLocation texture = new ResourceLocation(Dashboard.MODID, FOLDER + "images/" + id + ".png");
        if (!ResourceUtil.resourceExists(texture)) {
            Dashboard.LOG.warn("Pack image {} has no picture, skipping it", id);
            return;
        }
        PackImage image = IMAGES.get(id);
        if (image == null) {
            image = new PackImage(id, getString(json, "name", id), getBoolean(json, "visible", true));
            IMAGES.put(id, image);
        }
        image.texture = texture;
        image.width = Math.max(1, getInt(json, "width", DEFAULT_SIZE));
        image.height = Math.max(1, getInt(json, "height", DEFAULT_SIZE));
        String anchor = getString(json, "anchor", "top_left").toLowerCase(Locale.ROOT);
        image.anchorX = anchor.contains("left") ? 0 : anchor.contains("right") ? 2 : 1;
        image.anchorY = anchor.contains("top") ? 0 : anchor.contains("bottom") ? 2 : 1;
        image.offsetX = getInt(json, "x", 0);
        image.offsetY = getInt(json, "y", 0);
        image.opacity = MathHelper.clamp_float(getFloat(json, "opacity", 1), 0, 1);
        image.frames = Math.max(1, getInt(json, "frames", 1));
        image.frameTime = Math.max(1, getInt(json, "frametime", 1));
        image.available = true;
    }

    private static String getString(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key)
            .getAsString() : fallback;
    }

    private static boolean getBoolean(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key)
            .getAsBoolean() : fallback;
    }

    private static int getInt(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key)
            .getAsInt() : fallback;
    }

    private static float getFloat(JsonObject json, String key, float fallback) {
        return json.has(key) ? json.get(key)
            .getAsFloat() : fallback;
    }

    /** On top of the rest of the HUD, like a frame around the hotbar would need. */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        int screenWidth = event.resolution.getScaledWidth();
        int screenHeight = event.resolution.getScaledHeight();
        for (PackImage image : IMAGES.values()) {
            HudEditor.render(image.element, () -> image.draw(image.getDefaultBounds(screenWidth, screenHeight)));
        }
    }

    private static final class PackImage {

        private final HudElement element;
        private boolean available;
        private ResourceLocation texture;
        private int width;
        private int height;
        private int anchorX;
        private int anchorY;
        private int offsetX;
        private int offsetY;
        private float opacity;
        private int frames;
        private int frameTime;

        /**
         * The name and whether it shows before the player chooses stay the ones first read, since an element can not
         * change them. The name may be a lang key.
         */
        private PackImage(String id, String name, boolean visible) {
            element = new HudElement("dashboard:image_" + id, name, this::getDefaultBounds)
                .setActiveCheck(() -> available)
                .setModNameAddon(MOD_NAME_ADDON);
            if (!visible) {
                element.setHiddenByDefault();
            }
            HudEditor.register(element);
        }

        /** Anchored to a corner, edge or the middle of the screen, then moved by the offset. */
        private HudBounds getDefaultBounds(int screenWidth, int screenHeight) {
            return new HudBounds(
                anchorX * (screenWidth - width) / 2 + offsetX,
                anchorY * (screenHeight - height) / 2 + offsetY,
                width,
                height);
        }

        /** Stretched to its size. Without alpha testing, since that would drop see-through pictures. */
        private void draw(HudBounds bounds) {
            int frame = (int) (System.currentTimeMillis() / MILLIS_PER_TICK / frameTime % frames);
            double top = (double) frame / frames;
            double bottom = (double) (frame + 1) / frames;
            PreviewDrawing.bindTexture(texture);
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glColor4f(1, 1, 1, opacity);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(bounds.x, bounds.getBottom(), 0, 0, bottom);
            tessellator.addVertexWithUV(bounds.getRight(), bounds.getBottom(), 0, 1, bottom);
            tessellator.addVertexWithUV(bounds.getRight(), bounds.y, 0, 1, top);
            tessellator.addVertexWithUV(bounds.x, bounds.y, 0, 0, top);
            tessellator.draw();
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glPopAttrib();
        }
    }
}
