package net.ozanarchy.brooksclient.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.ozanarchy.brooksclient.client.XPHackClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void xphack$drawChestEsp(DeltaTracker deltaTracker, CallbackInfo ci) {
        XPHackClient.renderChestEspHook(Minecraft.getInstance());
    }
}
