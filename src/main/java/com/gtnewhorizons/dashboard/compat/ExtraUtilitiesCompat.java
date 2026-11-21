package com.gtnewhorizons.dashboard.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizons.dashboard.Dashboard;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;
import com.gtnewhorizons.dashboard.client.PreviewDrawing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The server TPS line Extra Utilities draws in the top left corner after {@code /xu_tps}. */
@SideOnly(Side.CLIENT)
public final class ExtraUtilitiesCompat {

    private static final String SAMPLE_TEXT = "TPS: 12.50(20.00), 10.25(20.00), 31.75(20.00)";

    public static final HudElement TPS = new HudElement("extrautilities:tps", ExtraUtilitiesCompat::getTpsBounds);

    private static Fields.ClassFields<Object>.Field<String> tpsText;

    private ExtraUtilitiesCompat() {}

    public static void register() {
        try {
            tpsText = ModFields
                .field(ModFields.of("com.rwtema.extrautils.CommandTPSTimer"), "displayString", String.class);
        } catch (ReflectiveOperationException e) {
            Dashboard.LOG.warn("Could not read the Extra Utilities TPS text, assuming the sample width", e);
        }
        PreviewDrawing.setImitation(
            TPS,
            (screenWidth, screenHeight) -> Minecraft.getMinecraft().fontRenderer
                .drawStringWithShadow(SAMPLE_TEXT, 0, 0, 0xFFFFFF));
        HudRegistry.registerFallback(TPS);
    }

    /** As wide as the text shown now, or the sample while the command is off, plus its shadow. */
    private static HudBounds getTpsBounds(int screenWidth, int screenHeight) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String text = tpsText == null ? null : tpsText.getValue(null);
        if (text == null || text.isEmpty()) {
            text = SAMPLE_TEXT;
        }
        return new HudBounds(0, 0, font.getStringWidth(text) + 1, font.FONT_HEIGHT);
    }
}
