package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.DashboardConfig;
import com.gtnewhorizons.dashboard.DashboardOtherConfig;

import cpw.mods.fml.client.config.DummyConfigElement;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiConfigEntries;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Dashboard's config screen, with a button into the HUD editor at the top of the list. */
@SideOnly(Side.CLIENT)
public class DashboardGuiConfig extends GuiConfig {

    public DashboardGuiConfig(GuiScreen parent) throws ConfigException {
        super(parent, getElements(), Dashboard.MODID, false, false, "Dashboard Configuration");
    }

    @SuppressWarnings("rawtypes")
    private static List<IConfigElement> getElements() throws ConfigException {
        List<IConfigElement> elements = new ArrayList<>();
        elements.add(
            new DummyConfigElement.DummyCategoryElement<>(
                "open_editor",
                "dashboard.config.open_editor",
                OpenEditorEntry.class));
        elements.addAll(
            ConfigurationManager.getConfigElementsMulti(true, DashboardConfig.class, DashboardOtherConfig.class));
        return elements;
    }

    /** Looks like a category button, but opens the editor. Only usable in a world, since the editor shows the HUD. */
    public static class OpenEditorEntry extends GuiConfigEntries.CategoryEntry {

        public OpenEditorEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList,
            IConfigElement<?> configElement) {
            super(owningScreen, owningEntryList, configElement);
            btnUndoChanges.visible = false;
            btnDefault.visible = false;
        }

        @Override
        protected GuiScreen buildChildScreen() {
            return null;
        }

        @Override
        public boolean enabled() {
            return Minecraft.getMinecraft().theWorld != null;
        }

        @Override
        public boolean mousePressed(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            if (!btnSelectCategory.mousePressed(mc, x, y)) {
                return false;
            }
            btnSelectCategory.func_146113_a(mc.getSoundHandler());
            mc.displayGuiScreen(new GuiHudEditor());
            return true;
        }
    }
}
