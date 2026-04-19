package net.ozanarchy.brooksclient.client.module.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;

public final class AutoEatModule extends Module {
    private static final int HUNGER_THRESHOLD = 16;

    private final Minecraft mc = Minecraft.getInstance();
    private boolean forcingUse;

    public AutoEatModule() {
        super("Auto Eat", "Automatically eats when hunger is low.", Category.UTILITY, -1);
    }

    @Override
    public void onDisable() {
        if (mc.options != null && forcingUse) {
            mc.options.keyUse.setDown(false);
        }
        forcingUse = false;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.options == null || mc.gameMode == null) {
            return;
        }

        if (mc.player.isUsingItem()) {
            return;
        }

        if (mc.player.getFoodData().getFoodLevel() >= HUNGER_THRESHOLD) {
            if (forcingUse) {
                mc.options.keyUse.setDown(false);
                forcingUse = false;
            }
            return;
        }

        int foodSlot = findFoodInHotbar();
        if (foodSlot == -1) {
            if (forcingUse) {
                mc.options.keyUse.setDown(false);
                forcingUse = false;
            }
            return;
        }

        if (mc.player.getInventory().getSelectedSlot() != foodSlot) {
            mc.player.getInventory().setSelectedSlot(foodSlot);
        }

        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        mc.options.keyUse.setDown(true);
        forcingUse = true;
    }

    private int findFoodInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getUseAnimation() == net.minecraft.world.item.ItemUseAnimation.EAT) {
                return i;
            }
        }
        return -1;
    }
}
