package net.ozanarchy.brooksclient.client.module.modules;

import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public final class NoFallModule extends Module {
    private final Minecraft mc = Minecraft.getInstance();

    public NoFallModule() {
        super("NoFall", Category.WORLD, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.getConnection() == null) {
            return;
        }

        if (mc.player.onGround() || mc.player.fallDistance < 2.0F) {
            return;
        }

        mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, mc.player.horizontalCollision));
    }
}
