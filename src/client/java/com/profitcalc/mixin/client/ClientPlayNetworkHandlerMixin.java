package com.profitcalc.mixin.client;

import com.profitcalc.manager.RecipeManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        // Load recipes when joining a world
        ClientPlayNetworkHandler handler = (ClientPlayNetworkHandler) (Object) this;
        if (handler.getRecipeManager() != null) {
            RecipeManager.getInstance().loadRecipes(handler.getRecipeManager());
        }
    }
}
