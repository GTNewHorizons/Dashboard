package com.gtnewhorizons.dashboard.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.util.Constants;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudEditor;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The 9 items stored in a Tinkers' Traveller's Belt, which swaps them with the hotbar, shown
 * as a second hotbar. Tinkers swaps them on the client too, so the belt's items here are always current.
 */
@SideOnly(Side.CLIENT)
@EventBusSubscriber(side = Side.CLIENT)
public final class TinkersBeltHotbar {

    private static final ResourceLocation HOTBAR_TEXTURE = new ResourceLocation("textures/gui/widgets.png");
    private static final int WIDTH = 182;
    private static final int HEIGHT = 22;
    private static final int SLOTS = 9;
    /** Tinkers' belt slot in its own armor slots. */
    private static final int BELT_SLOT = 3;
    private static final ItemStack[] EMPTY_SLOTS = new ItemStack[SLOTS];

    public static final HudElement BELT_HOTBAR = new HudElement(
        "dashboard:tinkers_belt_hotbar",
        "dashboard.element.tinkers.belt_hotbar",
        TinkersBeltHotbar::getDefaultBounds).setHiddenByDefault()
            .setDescription("dashboard.element.tinkers.belt_hotbar.description", "key.tbelt")
            .setModNameAddon("Tinkers' Construct");

    private static Field armorExtended;
    private static Field armorInventory;
    private static Method getBaublesBelt;
    private static boolean baublesLoaded;
    /** The belt list read last and its items, since Tinkers puts in a new list on every swap. */
    private static NBTTagList lastList;
    private static ItemStack[] lastItems = EMPTY_SLOTS;

    private TinkersBeltHotbar() {}

    @EventBusSubscriber.Condition
    public static boolean isTinkersLoaded() {
        return Mods.TINKERS.isModLoaded();
    }

    public static void register() {
        try {
            armorExtended = Class.forName("tconstruct.armor.ArmorProxyClient")
                .getField("armorExtended");
            armorInventory = Class.forName("tconstruct.armor.player.ArmorExtended")
                .getField("inventory");
            getBaublesBelt = Class.forName("tconstruct.armor.PlayerAbilityHelper")
                .getDeclaredMethod("getBaublesBelt", EntityPlayer.class);
            getBaublesBelt.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not find the Tinkers' Traveller's Belt, its belt hotbar is off", e);
            return;
        }
        baublesLoaded = Loader.isModLoaded("Baubles");
        PreviewDrawing.setImitationInBounds(BELT_HOTBAR, bounds -> draw(bounds, EMPTY_SLOTS));
        HudEditor.register(BELT_HOTBAR);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || armorExtended == null) {
            return;
        }
        ItemStack belt = findBelt(Minecraft.getMinecraft().thePlayer);
        if (belt != null) {
            HudEditor.render(
                BELT_HOTBAR,
                () -> draw(
                    BELT_HOTBAR.getDefaultBounds(event.resolution.getScaledWidth(), event.resolution.getScaledHeight()),
                    getItems(belt)));
        }
    }

    /** Above the rows stacked on the hotbar, like the hearts and food, so it moves up with them. */
    private static HudBounds getDefaultBounds(int screenWidth, int screenHeight) {
        int rowsTop = screenHeight - Math.max(GuiIngameForge.left_height, GuiIngameForge.right_height) + 10;
        return new HudBounds(screenWidth / 2 - WIDTH / 2, rowsTop - 2 - HEIGHT, WIDTH, HEIGHT);
    }

    /** Like Tinkers: its own belt slot first, then a Baubles belt slot. Null without a belt. */
    private static ItemStack findBelt(EntityPlayer player) {
        if (player == null) {
            return null;
        }
        try {
            Object armor = armorExtended.get(null);
            ItemStack belt = armor != null ? ((ItemStack[]) armorInventory.get(armor))[BELT_SLOT] : null;
            if (belt == null && baublesLoaded) {
                belt = (ItemStack) getBaublesBelt.invoke(null, player);
            }
            return belt;
        } catch (ReflectiveOperationException | RuntimeException e) {
            armorExtended = null;
            Dashboard.LOG.error("Could not read the Tinkers' Traveller's Belt, its belt hotbar is turned off", e);
            return null;
        }
    }

    /** The items Tinkers keeps in the belt, by hotbar slot. */
    private static ItemStack[] getItems(ItemStack belt) {
        NBTTagCompound tag = belt.getTagCompound();
        if (tag == null) {
            return EMPTY_SLOTS;
        }
        NBTTagList list = tag.getTagList("Inventory", Constants.NBT.TAG_COMPOUND);
        if (list == lastList) {
            return lastItems;
        }
        ItemStack[] items = new ItemStack[SLOTS];
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound slotTag = list.getCompoundTagAt(i);
            int slot = slotTag.getByte("Slot");
            if (slot >= 0 && slot < SLOTS) {
                items[slot] = ItemStack.loadItemStackFromNBT(slotTag);
            }
        }
        lastList = list;
        lastItems = items;
        return items;
    }

    /** Like the vanilla hotbar, without the selection, since its slots can not be selected. */
    private static void draw(HudBounds bounds, ItemStack[] items) {
        PreviewDrawing.bindTexture(HOTBAR_TEXTURE);
        PreviewDrawing.drawTexture(bounds.x, bounds.y, 0, 0, WIDTH, HEIGHT);
        for (int slot = 0; slot < SLOTS; slot++) {
            if (items[slot] != null) {
                PreviewDrawing.drawItem(items[slot], bounds.x + 3 + slot * 20, bounds.y + 3);
            }
        }
    }
}
