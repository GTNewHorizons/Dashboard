package com.gtnewhorizons.dashboard.compat;

import java.lang.reflect.Method;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.client.HudLayout;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Botania's bars and texts. Its config and classes are found by name. */
@SideOnly(Side.CLIENT)
public final class BotaniaCompat {

    private static final String PACKAGE = "vazkii.botania.";

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int FLIGHT_BAR_WIDTH = 81;
    private static final int FLIGHT_BAR_HEIGHT = 11;
    private static final int BOSS_BAR_WIDTH = 185;
    private static final int BOSS_BAR_HEIGHT = 25;
    private static final int BOSS_BAR_Y = 10;
    private static final int WAND_MODE_Y_OFFSET = 70;
    private static final int TEXT_HEIGHT = 9;
    /** From the Crafting Halo above the crosshair to the bind icon and popups right of and below it. */
    private static final int CROSSHAIR_INFO_LEFT = 80;
    private static final int CROSSHAIR_INFO_TOP = 71;
    private static final int CROSSHAIR_INFO_WIDTH = 200;
    private static final int CROSSHAIR_INFO_HEIGHT = 108;
    private static final int MANA_POOL_COLOR = 0x4444FF;

    private static final ResourceLocation MANA_TEXTURE = new ResourceLocation(
        Mods.BOTANIA.getResourceLocation(),
        "textures/gui/manaHud.png");
    private static final ResourceLocation HUD_ICONS = new ResourceLocation(
        Mods.BOTANIA.getResourceLocation(),
        "textures/gui/hudIcons.png");
    private static final ResourceLocation BOSS_TEXTURE = new ResourceLocation(
        Mods.BOTANIA.getResourceLocation(),
        "textures/gui/bossBar.png");

    public static final HudElement MANA_BAR = new HudElement(
        "botania:mana_bar",
        "dashboard.element.botania.mana_bar",
        BotaniaCompat::getManaBarBounds);

    public static final HudElement FLIGHT_BAR = new HudElement(
        "botania:flight_bar",
        "dashboard.element.botania.flight_bar",
        BotaniaCompat::getFlightBarBounds);

    public static final HudElement BOSS_BAR = new HudElement(
        "botania:boss_bar",
        "dashboard.element.botania.boss_bar",
        (screenWidth, screenHeight) -> new HudBounds(
            screenWidth / 2 - BOSS_BAR_WIDTH / 2,
            BOSS_BAR_Y,
            BOSS_BAR_WIDTH,
            BOSS_BAR_HEIGHT));

    public static final HudElement WAND_MODE = new HudElement(
        "botania:wand_mode",
        "dashboard.element.botania.wand_mode",
        BotaniaCompat::getWandModeBounds);

    public static final HudElement CORPOREA_HINT = new HudElement(
        "botania:corporea_hint",
        "dashboard.element.botania.corporea_hint",
        BotaniaCompat::getCorporeaHintBounds);

    /** Everything Botania draws around the crosshair, moved together so it keeps its layout. */
    public static final HudElement CROSSHAIR_INFO = new HudElement(
        "botania:crosshair_info",
        "dashboard.element.botania.crosshair_info",
        (screenWidth, screenHeight) -> new HudBounds(
            screenWidth / 2 - CROSSHAIR_INFO_LEFT,
            screenHeight / 2 - CROSSHAIR_INFO_TOP,
            CROSSHAIR_INFO_WIDTH,
            CROSSHAIR_INFO_HEIGHT));

    private static Fields.ClassFields<Object>.Field<Integer> manaBarHeight;
    private static Fields.ClassFields<Object>.Field<Integer> flightBarHeight;
    private static Fields.ClassFields<Object>.Field<Integer> flightBarBreathHeight;
    private static Fields.ClassFields<Object>.Field<?> currentBoss;
    private static Item flightTiara;
    private static Method getBaubles;
    private static Method drawTiara;
    private static Fields.ClassFields<Object>.Field<Integer> itemsRemainingTicks;
    private static Method drawManaInfo;
    private static Block manaPool;
    private static Block corporeaIndex;
    private static final ShiftTracker MANA_BAR_SHIFT = new ShiftTracker();
    private static final ShiftTracker CORPOREA_HINT_SHIFT = new ShiftTracker();
    private static boolean crosshairInfoInvisible;

    private BotaniaCompat() {}

    public static void register() {
        try {
            Fields.ClassFields<Object> config = ModFields.of(PACKAGE + "common.core.handler.ConfigHandler");
            manaBarHeight = ModFields.intField(config, "manaBarHeight");
            flightBarHeight = ModFields.intField(config, "flightBarHeight");
            flightBarBreathHeight = ModFields.intField(config, "flightBarBreathHeight");
            currentBoss = ModFields.of(PACKAGE + "client.core.handler.BossBarHandler")
                .getUntypedField(Fields.LookupType.DECLARED, "currentBoss");
            flightTiara = (Item) Class.forName(PACKAGE + "common.item.ModItems")
                .getField("flightTiara")
                .get(null);
            getBaubles = Class.forName("baubles.common.lib.PlayerHandler")
                .getMethod("getPlayerBaubles", EntityPlayer.class);
            drawTiara = Class.forName(PACKAGE + "common.item.equipment.bauble.ItemFlightTiara")
                .getMethod("renderHUD", ScaledResolution.class, EntityPlayer.class, ItemStack.class);
            itemsRemainingTicks = ModFields
                .intField(ModFields.of(PACKAGE + "client.core.handler.ItemsRemainingRenderHandler"), "ticks");
            drawManaInfo = Class.forName(PACKAGE + "client.core.handler.HUDHandler")
                .getMethod("drawSimpleManaHUD", int.class, int.class, int.class, String.class, ScaledResolution.class);
            PreviewDrawing.setImitation(CROSSHAIR_INFO, (screenWidth, screenHeight) -> drawCrosshairInfoPreview());
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not read the Botania HUD settings, some positions or the flight bar are off", e);
        }
        manaPool = GameRegistry.findBlock(Mods.BOTANIA.getID(), "pool");
        corporeaIndex = GameRegistry.findBlock(Mods.BOTANIA.getID(), "corporeaIndex");
        PreviewDrawing.setImitationInBounds(MANA_BAR, BotaniaCompat::drawManaBarPreview);
        PreviewDrawing.setImitationInBounds(FLIGHT_BAR, BotaniaCompat::drawFlightBarPreview);
        PreviewDrawing.setImitationInBounds(BOSS_BAR, BotaniaCompat::drawBossBarPreview);
        PreviewDrawing.setImitationInBounds(WAND_MODE, BotaniaCompat::drawWandModePreview);
        PreviewDrawing.setImitationInBounds(CORPOREA_HINT, BotaniaCompat::drawCorporeaHintPreview);
        for (HudElement element : new HudElement[] { MANA_BAR, FLIGHT_BAR, BOSS_BAR, WAND_MODE, CORPOREA_HINT,
            CROSSHAIR_INFO }) {
            HudEditor.register(element);
        }
    }

    /** Botania draws nothing without mana, so the bar only counts as showing when there is some. */
    public static boolean beginManaBar(ScaledResolution resolution, boolean creative, int totalMana) {
        if (!creative && totalMana <= 0) {
            return true;
        }
        return MANA_BAR_SHIFT.begin(MANA_BAR, resolution);
    }

    public static void endManaBar() {
        MANA_BAR_SHIFT.end();
    }

    public static boolean beginCorporeaHint(ScaledResolution resolution) {
        return CORPOREA_HINT_SHIFT.begin(CORPOREA_HINT, resolution);
    }

    public static void endCorporeaHint() {
        CORPOREA_HINT_SHIFT.end();
    }

    /**
     * Called before each crosshair info draw call. The mixin can not skip these one by one, so a hidden element is
     * drawn invisibly instead.
     */
    public static void beginCrosshairInfo() {
        crosshairInfoInvisible = !HudEditor.beginRender(CROSSHAIR_INFO);
        if (crosshairInfoInvisible) {
            GL11.glPushMatrix();
            PreviewDrawing.setColorWrites(false);
        }
    }

    public static void endCrosshairInfo() {
        if (crosshairInfoInvisible) {
            PreviewDrawing.setColorWrites(true);
        }
        HudEditor.endRender();
    }

    /** Botania calls it every frame, but it only draws for a moment after using some rods. */
    public static boolean isShowingItemsRemaining() {
        return ModFields.readInt(itemsRemainingTicks) > 0;
    }

    public static boolean hasBoss() {
        return currentBoss != null && currentBoss.getValue(null) != null;
    }

    /**
     * Drawn at the end of the HUD instead of when the health bar starts, where Botania draws it. There it would be
     * moved with the health bar, and skipped whenever Dashboard skips vanilla's health. Blending is set up like it is
     * there, since things drawn before it (like the Petal Apothecary info) leave it off.
     */
    public static void drawFlightBar(ScaledResolution resolution) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null || getBaubles == null || drawTiara == null) {
            return;
        }
        try {
            IInventory baubles = (IInventory) getBaubles.invoke(null, player);
            ItemStack amulet = baubles == null ? null : baubles.getStackInSlot(0);
            if (amulet == null || amulet.getItem() != flightTiara || !HudEditor.beginRender(FLIGHT_BAR, resolution)) {
                return;
            }
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
            try {
                PreviewDrawing.bindTexture(HUD_ICONS);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                drawTiara.invoke(null, resolution, player, amulet);
            } finally {
                GL11.glPopAttrib();
                HudEditor.endRender();
            }
        } catch (ReflectiveOperationException e) {
            drawTiara = null;
            Dashboard.LOG.warn("Could not draw the Botania flight bar, turning it off", e);
        }
    }

    private static HudBounds getManaBarBounds(int screenWidth, int screenHeight) {
        return new HudBounds(
            screenWidth / 2 - BAR_WIDTH / 2,
            screenHeight - ModFields.readInt(manaBarHeight),
            BAR_WIDTH,
            BAR_HEIGHT);
    }

    /** Higher while underwater, so it stays above the air bubbles. */
    private static HudBounds getFlightBarBounds(int screenWidth, int screenHeight) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        boolean underwater = player != null && player.getAir() < 300;
        int height = ModFields.readInt(underwater ? flightBarBreathHeight : flightBarHeight);
        return new HudBounds(screenWidth / 2 + 10, screenHeight - height - 2, FLIGHT_BAR_WIDTH, FLIGHT_BAR_HEIGHT);
    }

    private static HudBounds getWandModeBounds(int screenWidth, int screenHeight) {
        int width = Minecraft.getMinecraft().fontRenderer.getStringWidth(getWandModeSample());
        return new HudBounds(screenWidth / 2 - width / 2, screenHeight - WAND_MODE_Y_OFFSET, width, TEXT_HEIGHT);
    }

    private static String getWandModeSample() {
        return StatCollector.translateToLocal("botaniamisc.wandMode.function");
    }

    /** Three lines right of the corporea index icon, in two dark boxes, near the bottom right corner. */
    private static HudBounds getCorporeaHintBounds(int screenWidth, int screenHeight) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int textWidth = 0;
        for (String line : getCorporeaHintLines()) {
            textWidth = Math.max(textWidth, font.getStringWidth(line));
        }
        int width = textWidth + 20;
        int x = screenWidth - width - 20;
        int y = screenHeight - 60;
        return new HudBounds(x - 6, y - 6, width + 12, 43);
    }

    private static String[] getCorporeaHintLines() {
        return new String[] { StatCollector.translateToLocal("botaniamisc.nearIndex0"),
            StatCollector.translateToLocal("botaniamisc.nearIndex1"),
            StatCollector.translateToLocal("botaniamisc.nearIndex2") };
    }

    /** A full bar in Botania's light blue. */
    private static void drawManaBarPreview(HudBounds bounds) {
        PreviewDrawing.bindTexture(MANA_TEXTURE);
        GL11.glColor4f(0.4f, 0.85f, 1, 1);
        PreviewDrawing.drawTexture(bounds.x, bounds.y, 0, 251, BAR_WIDTH, BAR_HEIGHT);
        GL11.glColor4f(1, 1, 1, 1);
    }

    /** All ten feathers, like a fully rested tiara. */
    private static void drawFlightBarPreview(HudBounds bounds) {
        PreviewDrawing.bindTexture(HUD_ICONS);
        for (int i = 0; i < 10; i++) {
            PreviewDrawing.drawTexture(bounds.x + i * 8, bounds.y + 2, 0, 0, 9, 9);
        }
    }

    /** Gaia's bar at two thirds health, with its name above in Botania's purple. */
    private static void drawBossBarPreview(HudBounds bounds) {
        int barY = bounds.y + 10;
        PreviewDrawing.bindTexture(BOSS_TEXTURE);
        PreviewDrawing.drawTexture(bounds.x, barY, 0, 0, BOSS_BAR_WIDTH, 15);
        PreviewDrawing.drawTexture(bounds.x + 2, barY + 4, 0, 15, 181 * 2 / 3, 7);
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String name = StatCollector.translateToLocal("entity.Botania.botania:doppleganger.name");
        font.drawStringWithShadow(
            name,
            bounds.x + bounds.width / 2 - font.getStringWidth(name) / 2,
            bounds.y,
            0xA2018C);
    }

    private static void drawWandModePreview(HudBounds bounds) {
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(getWandModeSample(), bounds.x, bounds.y, 0x00CC00);
    }

    /** Botania's own mana info, like when looking at a half full Mana Pool with a Wand of the Forest. */
    private static void drawCrosshairInfoPreview() {
        String name = manaPool != null ? new ItemStack(manaPool).getDisplayName() : "";
        try {
            drawManaInfo.invoke(null, MANA_POOL_COLOR, 500_000, 1_000_000, name, HudLayout.getResolution());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not draw the Botania mana info", e);
        }
    }

    /** Like Botania's: the index icon and three lines of text on two see-through boxes. */
    private static void drawCorporeaHintPreview(HudBounds bounds) {
        Gui.drawRect(bounds.x, bounds.y, bounds.getRight(), bounds.getBottom(), 0x44000000);
        Gui.drawRect(bounds.x + 2, bounds.y + 2, bounds.getRight() - 2, bounds.getBottom() - 2, 0x44000000);
        int x = bounds.x + 6;
        int y = bounds.y + 6;
        if (corporeaIndex != null) {
            PreviewDrawing.drawItem(new ItemStack(corporeaIndex), x, y + 10);
        }
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String[] lines = getCorporeaHintLines();
        font.drawStringWithShadow(lines[0], x + 20, y, 0xFFFFFF);
        font.drawStringWithShadow(lines[1], x + 20, y + 14, 0xAAAAAA);
        font.drawStringWithShadow(lines[2], x + 20, y + 24, 0xAAAAAA);
    }
}
