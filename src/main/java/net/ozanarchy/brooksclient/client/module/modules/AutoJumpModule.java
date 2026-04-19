package net.ozanarchy.brooksclient.client.module.modules;

import net.minecraft.client.Minecraft;
import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;

public final class AutoJumpModule extends Module {
    private final Minecraft mc = Minecraft.getInstance();

    public AutoJumpModule() {
        super("Auto Jump", "Automatically jumps while moving forward on ground.", Category.MOVEMENT, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (mc.player.zza > 0.0F && mc.player.onGround() && !mc.player.isCrouching()) {
            mc.player.jumpFromGround();
        }
    }
}
