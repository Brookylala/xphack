package net.ozanarchy.brooksclient.client.module.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;

public final class AutoRespawnModule extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private boolean requestedRespawn;

    public AutoRespawnModule() {
        super("Auto Respawn", "Automatically respawns after death.", Category.UTILITY, -1);
    }

    @Override
    public void onTick() {
        if (!(mc.screen instanceof DeathScreen)) {
            requestedRespawn = false;
            return;
        }

        if (!requestedRespawn && mc.player != null) {
            mc.player.respawn();
            requestedRespawn = true;
        }
    }
}
