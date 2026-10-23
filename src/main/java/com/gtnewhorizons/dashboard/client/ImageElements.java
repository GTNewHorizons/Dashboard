package com.gtnewhorizons.dashboard.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.apache.commons.io.FilenameUtils;
import org.lwjgl.opengl.GL11;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.DashboardOtherConfig;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Pictures from resource packs and from the config/dashboard/images folder, each its own HUD element. Packs list them
 * in images.json next to the bar themes, with the picture in the images folder. Pictures in the config folder show on
 * their own, and an images.json there can change their settings. Animated pictures stack their frames from top to
 * bottom, like vanilla's.
 */
@SideOnly(Side.CLIENT)
@EventBusSubscriber(side = Side.CLIENT)
public final class ImageElements {

    private static final String FOLDER = "textures/gui/";
    private static final String TOGGLE_KEY = "key.dashboard.toggle_images";
    private static final int DEFAULT_SIZE = 32;
    private static final int MAX_FOLDER_SIZE = 128;
    private static final float MAX_ZOOM = 16;
    private static final float ZOOM_STEP = 1.25f;
    private static final long MILLIS_PER_TICK = 50;
    private static final Map<String, ImageElement> IMAGES = new LinkedHashMap<>();

    private ImageElements() {}

    public static void load(IResourceManager resources) {
        for (ImageElement image : IMAGES.values()) {
            image.available = false;
        }
        loadPackImages(resources);
        loadFolderImages();
    }

    private static void loadPackImages(IResourceManager resources) {
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
                        definePackImage(
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

    private static void definePackImage(String id, JsonObject json) {
        ResourceLocation texture = new ResourceLocation(Dashboard.MODID, FOLDER + "images/" + id + ".png");
        if (!ResourceUtil.resourceExists(texture)) {
            Dashboard.LOG.warn("Pack image {} has no picture, skipping it", id);
            return;
        }
        ImageElement image = getOrCreate(Source.PACK, id, json);
        image.texture = texture;
        applySettings(image, json, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    /** Every PNG in the folder, sorted by name. F3+T reads the folder again. */
    private static void loadFolderImages() {
        File folder = new File(Minecraft.getMinecraft().mcDataDir, "config/dashboard/images");
        if (!folder.isDirectory() && !folder.mkdirs()) {
            return;
        }
        JsonObject settings = readFolderSettings(new File(folder, "images.json"));
        File[] pictures = folder.listFiles(
            (dir, name) -> name.toLowerCase(Locale.ROOT)
                .endsWith(".png"));
        if (pictures == null) {
            return;
        }
        Arrays.sort(pictures);
        int maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        for (File picture : pictures) {
            String id = FilenameUtils.getBaseName(picture.getName());
            try {
                defineFolderImage(
                    id,
                    picture,
                    settings.has(id) ? settings.getAsJsonObject(id) : new JsonObject(),
                    maxTextureSize);
            } catch (IOException | RuntimeException e) {
                Dashboard.LOG.warn("Image {} in config/dashboard/images could not be read, skipping it", id, e);
            }
        }
    }

    private static JsonObject readFolderSettings(File file) {
        if (!file.isFile()) {
            return new JsonObject();
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader)
                .getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            Dashboard.LOG.error("Could not read config/dashboard/images/images.json, using the defaults", e);
            return new JsonObject();
        }
    }

    /** Fitted into the starting size, keeping its shape, unless images.json says otherwise. */
    private static void defineFolderImage(String id, File file, JsonObject json, int maxTextureSize)
        throws IOException {
        BufferedImage picture = ImageIO.read(file);
        if (picture == null) {
            throw new IOException("Not a picture");
        }
        if (picture.getWidth() > maxTextureSize || picture.getHeight() > maxTextureSize) {
            Dashboard.LOG.warn(
                "Image {} is {} x {}, but the graphics card allows {} at most, skipping it",
                id,
                picture.getWidth(),
                picture.getHeight(),
                maxTextureSize);
            return;
        }
        ImageElement image = getOrCreate(Source.FOLDER, id, json);
        TextureManager textures = Minecraft.getMinecraft()
            .getTextureManager();
        if (image.texture != null) {
            textures.deleteTexture(image.texture);
        }
        image.texture = textures.getDynamicTextureLocation("dashboard_image", new DynamicTexture(picture));
        int frameHeight = picture.getHeight()
            / Math.max(1, JsonUtils.getJsonObjectIntegerFieldValueOrDefault(json, "frames", 1));
        float scale = Math.min(1, (float) MAX_FOLDER_SIZE / Math.max(picture.getWidth(), frameHeight));
        applySettings(image, json, Math.round(picture.getWidth() * scale), Math.round(frameHeight * scale));
    }

    /** The name and whether it starts shown are only read the first time, since an element can not change them. */
    private static ImageElement getOrCreate(Source source, String id, JsonObject json) {
        String elementId = source.idPrefix + id;
        ImageElement image = IMAGES.get(elementId);
        if (image == null) {
            image = new ImageElement(
                elementId,
                JsonUtils.getJsonObjectStringFieldValueOrDefault(json, "name", id),
                JsonUtils.getJsonObjectBooleanFieldValueOrDefault(json, "visible", true),
                source);
            IMAGES.put(elementId, image);
        }
        return image;
    }

    private static void applySettings(ImageElement image, JsonObject json, int defaultWidth, int defaultHeight) {
        image.width = Math.max(1, JsonUtils.getJsonObjectIntegerFieldValueOrDefault(json, "width", defaultWidth));
        image.height = Math.max(1, JsonUtils.getJsonObjectIntegerFieldValueOrDefault(json, "height", defaultHeight));
        String anchor = JsonUtils.getJsonObjectStringFieldValueOrDefault(json, "anchor", "top_left")
            .toLowerCase(Locale.ROOT);
        image.anchorX = anchor.contains("left") ? 0 : anchor.contains("right") ? 2 : 1;
        image.anchorY = anchor.contains("top") ? 0 : anchor.contains("bottom") ? 2 : 1;
        image.offsetX = JsonUtils.getJsonObjectIntegerFieldValueOrDefault(json, "x", 0);
        image.offsetY = JsonUtils.getJsonObjectIntegerFieldValueOrDefault(json, "y", 0);
        image.opacity = MathHelper
            .clamp_float(JsonUtils.getJsonObjectFloatFieldValueOrDefault(json, "opacity", 1), 0, 1);
        image.frames = Math.max(1, JsonUtils.getJsonObjectIntegerFieldValueOrDefault(json, "frames", 1));
        image.frameTime = Math.max(1, JsonUtils.getJsonObjectIntegerFieldValueOrDefault(json, "frametime", 1));
        image.available = true;
    }

    /** Hides or shows every image at once, with a message above the hotbar. Saved, so it lasts. */
    public static void toggleShown() {
        DashboardOtherConfig.showImages = !DashboardOtherConfig.showImages;
        ConfigurationManager.save(DashboardOtherConfig.class);
        VanillaHudElements.showShadowedActionBarText(
            StatCollector.translateToLocal(
                DashboardOtherConfig.showImages ? "dashboard.message.images_shown"
                    : "dashboard.message.images_hidden"));
    }

    /** The editor still shows them, so they can be arranged while toggled off. */
    private static boolean areShown() {
        return DashboardOtherConfig.showImages || HudRegistry.isLayoutScreenOpen();
    }

    public static boolean isImage(HudElement element) {
        return IMAGES.containsKey(element.getId());
    }

    /** The shown image under the mouse, or null. The last one wins, since it is drawn on top. */
    public static HudElement findShownImageAt(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (!areShown()) {
            return null;
        }
        HudElement found = null;
        for (ImageElement image : IMAGES.values()) {
            if (image.available && HudLayout.isVisible(image.element)
                && HudLayout.getBounds(image.element, screenWidth, screenHeight)
                    .contains(mouseX, mouseY)) {
                found = image.element;
            }
        }
        return found;
    }

    /** Toward the mouse, so the part of the picture under it stays there. Positive steps zoom in. */
    public static void zoom(HudElement element, HudBounds bounds, int mouseX, int mouseY, int steps) {
        float zoom = HudLayout.getZoom(element);
        float newZoom = MathHelper.clamp_float(zoom * (float) Math.pow(ZOOM_STEP, steps), 1, MAX_ZOOM);
        float mouseFractionX = (mouseX - bounds.x) / (float) bounds.width;
        float mouseFractionY = (mouseY - bounds.y) / (float) bounds.height;
        float pointX = HudLayout.getViewX(element) + (mouseFractionX - 0.5f) / zoom;
        float pointY = HudLayout.getViewY(element) + (mouseFractionY - 0.5f) / zoom;
        setView(
            element,
            newZoom,
            pointX - (mouseFractionX - 0.5f) / newZoom,
            pointY - (mouseFractionY - 0.5f) / newZoom);
    }

    /** Drags the picture along with the mouse. */
    public static void pan(HudElement element, HudBounds bounds, int movedX, int movedY) {
        float zoom = HudLayout.getZoom(element);
        if (zoom > 1) {
            setView(
                element,
                zoom,
                HudLayout.getViewX(element) - movedX / (float) bounds.width / zoom,
                HudLayout.getViewY(element) - movedY / (float) bounds.height / zoom);
        }
    }

    public static void resetZoom(HudElement element) {
        HudLayout.setZoom(element, 1, 0.5f, 0.5f);
    }

    /** Kept inside the picture, so no empty space shows at the edges. */
    private static void setView(HudElement element, float zoom, float viewX, float viewY) {
        float half = 0.5f / zoom;
        HudLayout.setZoom(
            element,
            zoom,
            MathHelper.clamp_float(viewX, half, 1 - half),
            MathHelper.clamp_float(viewY, half, 1 - half));
    }

    /**
     * Before the rest of the HUD, and before other mods, so every other element is drawn over the images. Forge only
     * switches to screen drawing after this event, so it is switched here first.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || IMAGES.isEmpty() || !areShown()) {
            return;
        }
        Minecraft.getMinecraft().entityRenderer.setupOverlayRendering();
        int screenWidth = event.resolution.getScaledWidth();
        int screenHeight = event.resolution.getScaledHeight();
        for (ImageElement image : IMAGES.values()) {
            HudRegistry.render(image.element, () -> image.draw(image.getDefaultBounds(screenWidth, screenHeight)));
        }
    }

    /** Where a picture comes from, which its element id, mod name and tooltip tell apart. */
    private enum Source {

        PACK("dashboard:image_", "Resource Pack", "dashboard.element.image.pack"),
        FOLDER("dashboard:file_", "Image Folder", "dashboard.element.image.folder");

        private final String idPrefix;
        private final String modNameAddon;
        private final String description;

        Source(String idPrefix, String modNameAddon, String description) {
            this.idPrefix = idPrefix;
            this.modNameAddon = modNameAddon;
            this.description = description;
        }
    }

    private static final class ImageElement {

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

        /** The name may be a lang key. */
        private ImageElement(String elementId, String name, boolean visible, Source source) {
            element = new HudElement(elementId, name, this::getDefaultBounds).setActiveCheck(() -> available)
                .setModNameAddon(source.modNameAddon)
                .setDescription(source.description, TOGGLE_KEY);
            if (!visible) {
                element.setHiddenByDefault();
            }
            HudRegistry.register(element);
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
            float half = 0.5f / HudLayout.getZoom(element);
            float viewX = HudLayout.getViewX(element);
            float viewY = HudLayout.getViewY(element);
            double left = viewX - half;
            double right = viewX + half;
            double top = (frame + viewY - half) / frames;
            double bottom = (frame + viewY + half) / frames;
            PreviewDrawing.bindTexture(texture);
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glColor4f(1, 1, 1, opacity);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(bounds.x, bounds.getBottom(), 0, left, bottom);
            tessellator.addVertexWithUV(bounds.getRight(), bounds.getBottom(), 0, right, bottom);
            tessellator.addVertexWithUV(bounds.getRight(), bounds.y, 0, right, top);
            tessellator.addVertexWithUV(bounds.x, bounds.y, 0, left, top);
            tessellator.draw();
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glPopAttrib();
        }
    }
}
