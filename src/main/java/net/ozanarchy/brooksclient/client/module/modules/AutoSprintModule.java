package net.ozanarchy.brooksclient.client.module.modules;

import net.minecraft.client.Minecraft;
import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;

public final class AutoSprintModule extends Module {
    private final Minecraft mc = Minecraft.getInstance();

    public AutoSprintModule() {
        super("Auto Sprint", "Automatically sprints when moving forward.", Category.MOVEMENT, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (mc.player.zza > 0.0F && !mc.player.isCrouching() && !mc.player.horizontalCollision) {
            mc.player.setSprinting(true);
        }
    }
}
