package com.gtnewhorizons.dashboard.mixins.early;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.network.play.server.S0DPacketCollectItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.dashboard.client.PickupFeed;

/**
 * Tells the pickup feed about items the player picks up, from the same message the pickup animation uses, so it also
 * works on servers.
 */
@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient_Pickup {

    /**
     * Before the item is removed from the world. Vanilla treats an unknown collector as the player, so check the id.
     */
    @Inject(method = "handleCollectItem", at = @At("HEAD"))
    private void dashboard$addPickup(S0DPacketCollectItem packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || packet.func_149353_d() != mc.thePlayer.getEntityId()) {
            return;
        }
        Entity entity = mc.theWorld.getEntityByID(packet.func_149354_c());
        if (entity instanceof EntityItem item) {
            PickupFeed.add(item.getEntityItem());
        }
    }
}
