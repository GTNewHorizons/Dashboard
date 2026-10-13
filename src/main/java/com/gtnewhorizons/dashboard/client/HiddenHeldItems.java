package com.gtnewhorizons.dashboard.client;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizon.gtnhlib.util.ItemUtil;
import com.gtnewhorizon.gtnhlib.util.data.ItemId;
import com.gtnewhorizons.dashboard.DashboardConfig;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Rendering of the item that player hold, listed in the config as modid:name (every
 * meta) or modid:name:meta. They are drawn as an empty hand instead.
 */
@SideOnly(Side.CLIENT)
public final class HiddenHeldItems {

    private static final Set<ItemId> HIDDEN = new HashSet<>();
    private static String[] parsedList;

    private HiddenHeldItems() {}

    /** Called by the first person mixin. */
    public static boolean isHidden(ItemStack stack) {
        String[] list = DashboardConfig.hiddenHeldItems;
        if (stack == null || list.length == 0) {
            return false;
        }
        // The config gives a new list whenever it changes
        if (list != parsedList) {
            parsedList = list;
            HIDDEN.clear();
            for (String entry : list) {
                ItemId id = parse(entry);
                if (id != null) {
                    HIDDEN.add(id);
                }
            }
        }
        Item item = stack.getItem();
        return HIDDEN.contains(ItemId.createNoCopy(item, ItemUtil.getStackMeta(stack), null))
            || HIDDEN.contains(ItemId.createNoCopy(item, OreDictionary.WILDCARD_VALUE, null));
    }

    /**
     * The entry for a stack, the other way around from {@link #parse}. The meta is only added for items whose meta
     * picks a variant, like wool colors. For tools it is the durability, which would only hide one damage value.
     */
    public static String toEntry(ItemStack stack) {
        String name = Item.itemRegistry.getNameForObject(stack.getItem());
        if (name == null || !stack.getHasSubtypes()) {
            return name;
        }
        return name + ":" + ItemUtil.getStackMeta(stack);
    }

    /** Null if no such item exists. A meta of * or none means every meta. */
    public static ItemId parse(String entry) {
        String name = entry.trim();
        int meta = OreDictionary.WILDCARD_VALUE;
        int lastColon = name.lastIndexOf(':');
        // modid:name has one colon, so a meta needs a second one
        if (lastColon > name.indexOf(':')) {
            String metaText = name.substring(lastColon + 1);
            if (!metaText.equals("*")) {
                try {
                    meta = Integer.parseInt(metaText);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            name = name.substring(0, lastColon);
        }
        Object item = Item.itemRegistry.getObject(name);
        return item instanceof Item found ? ItemId.createNoCopy(found, meta, null) : null;
    }
}
