package net.ozanarchy.brooksclient.client.module.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;

public final class AutoTotemModule extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private int actionCooldown;

    public AutoTotemModule() {
        super("Auto Totem", "Keeps a totem in offhand when available.", Category.UTILITY, -1);
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

        ItemStack offhand = mc.player.getOffhandItem();
        if (!offhand.isEmpty() && offhand.is(Items.TOTEM_OF_UNDYING)) {
            return;
        }

        int sourceSlot = findTotemSlot();
        if (sourceSlot == -1) {
            return;
        }

        mc.gameMode.handleContainerInput(
                mc.player.inventoryMenu.containerId,
                sourceSlot,
                40,
                ContainerInput.SWAP,
                mc.player
        );
        actionCooldown = 3;
    }

    private int findTotemSlot() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }
        return -1;
    }
}
