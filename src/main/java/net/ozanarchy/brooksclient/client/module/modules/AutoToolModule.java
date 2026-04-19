package net.ozanarchy.brooksclient.client.module.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;

public final class AutoToolModule extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private int lastSwitchedSlot = -1;

    public AutoToolModule() {
        super("Auto Tool", "Switches to the best hotbar tool while mining.", Category.UTILITY, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.options == null) {
            return;
        }

        if (!mc.options.keyAttack.isDown() || !(mc.hitResult instanceof BlockHitResult bhr)
                || bhr.getType() != HitResult.Type.BLOCK) {
            lastSwitchedSlot = -1;
            return;
        }

        BlockPos pos = bhr.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        int currentSlot = mc.player.getInventory().getSelectedSlot();
        double currentSpeed = mc.player.getMainHandItem().getDestroySpeed(state);

        int bestSlot = currentSlot;
        double bestSpeed = currentSpeed;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            double speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed + 0.001D) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        if (bestSlot != currentSlot && bestSlot != lastSwitchedSlot) {
            mc.player.getInventory().setSelectedSlot(bestSlot);
            mc.player.swing(InteractionHand.MAIN_HAND);
            lastSwitchedSlot = bestSlot;
        }
    }
}
