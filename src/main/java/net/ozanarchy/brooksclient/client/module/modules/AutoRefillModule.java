package net.ozanarchy.brooksclient.client.module.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;

public final class AutoRefillModule extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final Item[] rememberedHotbarItems = new Item[9];
    private int actionCooldown;

    public AutoRefillModule() {
        super("Auto Refill", "Refills empty hotbar slots from inventory.", Category.UTILITY, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            return;
        }

        rememberHotbarItems();

        if (actionCooldown > 0) {
            actionCooldown--;
            return;
        }

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            ItemStack hotbarStack = mc.player.getInventory().getItem(hotbarSlot);
            if (!hotbarStack.isEmpty()) {
                continue;
            }

            Item target = rememberedHotbarItems[hotbarSlot];
            if (target == null) {
                continue;
            }

            int sourceSlot = findMatchingInventorySlot(target);
            if (sourceSlot == -1) {
                continue;
            }

            mc.gameMode.handleContainerInput(
                    mc.player.inventoryMenu.containerId,
                    sourceSlot,
                    hotbarSlot,
                    ContainerInput.SWAP,
                    mc.player
            );
            actionCooldown = 3;
            return;
        }
    }

    private void rememberHotbarItems() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                rememberedHotbarItems[i] = stack.getItem();
            }
        }
    }

    private int findMatchingInventorySlot(Item targetItem) {
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == targetItem) {
                return i;
            }
        }
        return -1;
    }
}
