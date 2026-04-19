package net.ozanarchy.brooksclient.client.module.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;

public final class AutoFishModule extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private int actionCooldown;

    public AutoFishModule() {
        super("Auto Fish", "Automatically reels and recasts while fishing.", Category.UTILITY, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            return;
        }

        if (actionCooldown > 0) {
            actionCooldown--;
            return;
        }

        ItemStack held = mc.player.getMainHandItem();
        if (!(held.getItem() instanceof FishingRodItem)) {
            return;
        }

        FishingHook hook = mc.player.fishing;
        if (hook == null) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            actionCooldown = 12;
            return;
        }

        if (hook.isInWater() && hook.getDeltaMovement().y < -0.03D) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            actionCooldown = 8;
        }
    }
}
