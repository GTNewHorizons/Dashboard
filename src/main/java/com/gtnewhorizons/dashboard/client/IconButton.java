package com.gtnewhorizons.dashboard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A vanilla button showing one of the {@link Icons} instead of text. */
@SideOnly(Side.CLIENT)
public class IconButton extends GuiButton {

    /** Can change, like the flip arrow following the fill direction. */
    public int icon;

    public IconButton(int id, int x, int y, int width, int height, int icon) {
        super(id, x, y, width, height, "");
        this.icon = icon;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        super.drawButton(mc, mouseX, mouseY);
        Icons.drawOnButton(this, icon);
    }
}
