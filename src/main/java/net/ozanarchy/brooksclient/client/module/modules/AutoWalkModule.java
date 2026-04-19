package net.ozanarchy.brooksclient.client.module.modules;

import net.minecraft.client.Minecraft;
import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;

public final class AutoWalkModule extends Module {
    private final Minecraft mc = Minecraft.getInstance();

    public AutoWalkModule() {
        super("Auto Walk", "Continuously walks forward while enabled.", Category.MOVEMENT, -1);
    }

    @Override
    public void onEnable() {
        if (mc.options != null) {
            mc.options.keyUp.setDown(true);
        }
    }

    @Override
    public void onDisable() {
        if (mc.options != null) {
            mc.options.keyUp.setDown(false);
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.options == null) {
            return;
        }

        if (!mc.options.keyUp.isDown()) {
            mc.options.keyUp.setDown(true);
        }
    }
}
