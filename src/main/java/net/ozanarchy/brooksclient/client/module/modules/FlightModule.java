package net.ozanarchy.brooksclient.client.module.modules;

import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;
import net.ozanarchy.brooksclient.client.module.ModuleManager;
import net.ozanarchy.brooksclient.client.setting.BooleanSetting;
import net.ozanarchy.brooksclient.client.setting.SliderSetting;
import net.ozanarchy.brooksclient.client.util.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class FlightModule extends Module {
    private enum DropTargetType {
        GROUND,
        WATER,
        LAVA,
        NONE
    }

    private record DropTarget(DropTargetType type, double targetY) {
    }

    private final Minecraft mc = Minecraft.getInstance();
    private final SliderSetting horizontalSpeed = addSetting(new SliderSetting("Horizontal Speed", 1.0D, 0.05D, 10.0D, 0.05D));
    private final SliderSetting verticalSpeed = addSetting(new SliderSetting("Vertical Speed", 1.0D, 0.05D, 10.0D, 0.05D));
    private final BooleanSetting tieVerticalToHorizontal = addSetting(new BooleanSetting("Tie Vertical To Horizontal", false));
    private final BooleanSetting allowUnsafeVerticalSpeed = addSetting(new BooleanSetting("Allow Unsafe Vertical Speed", false));

    private final BooleanSetting antiKick = addSetting(new BooleanSetting("Anti-Kick", false));
    private final SliderSetting antiKickInterval = addSetting(new SliderSetting("Anti-Kick Interval", 70.0D, 5.0D, 80.0D, 1.0D));
    private final SliderSetting antiKickDistance = addSetting(new SliderSetting("Anti-Kick Distance", 0.035D, 0.01D, 0.2D, 0.001D));

    private final BooleanSetting dontGetCaught = addSetting(new BooleanSetting("Don't Get Caught", false));
    private final SliderSetting alertRange = addSetting(new SliderSetting("Alert Range", 24.0D, 8.0D, 96.0D, 1.0D));
    private final SliderSetting escapeDropSpeed = addSetting(new SliderSetting("Escape Drop Speed", 3.9D, 0.05D, 3.9D, 0.05D));

    private final BooleanSetting enableNoFallOnFlight = addSetting(new BooleanSetting("Enable NoFall With Flight", false));
    private final BooleanSetting ignoreShiftInGuis = addSetting(new BooleanSetting("Ignore Shift In GUIs", true));
    private final BooleanSetting slowSneaking = addSetting(new BooleanSetting("Slow Sneaking", false));

    private int tickCounter = 0;
    private boolean triggered = false;
    private boolean escapeDropActive = false;
    private double escapeTargetY = 0.0D;
    private boolean enabledNoFallByFlight = false;
    private boolean enabledNoFallByEscape = false;

    public FlightModule() {
        super("Flight", Category.MOVEMENT, -1);
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.level == null) {
            return;
        }

        tickCounter = 0;
        triggered = false;
        escapeDropActive = false;

        mc.player.getAbilities().mayfly = true;
        mc.player.getAbilities().flying = true;

        if (enableNoFallOnFlight.getValue()) {
            NoFallModule noFall = ModuleManager.getInstance().getModule(NoFallModule.class);
            if (noFall != null && !noFall.isEnabled()) {
                noFall.setEnabled(true);
                enabledNoFallByFlight = true;
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.player == null || mc.level == null) {
            return;
        }

        mc.player.getAbilities().flying = false;
        if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().mayfly = false;
        }

        if (enabledNoFallByFlight || enabledNoFallByEscape) {
            NoFallModule noFall = ModuleManager.getInstance().getModule(NoFallModule.class);
            if (noFall != null && noFall.isEnabled()) {
                noFall.setEnabled(false);
            }
        }

        enabledNoFallByFlight = false;
        enabledNoFallByEscape = false;
        escapeDropActive = false;
        triggered = false;
    }

    @Override
    public void onTick() {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        player.getAbilities().flying = true;
        player.getAbilities().setFlyingSpeed((float) (horizontalSpeed.getValue() * 0.05D));

        if (enableNoFallOnFlight.getValue()) {
            NoFallModule noFall = ModuleManager.getInstance().getModule(NoFallModule.class);
            if (noFall != null && !noFall.isEnabled()) {
                noFall.setEnabled(true);
                enabledNoFallByFlight = true;
            }
        }

        if (dontGetCaught.getValue() && !triggered) {
            checkDangerPlayers(player);
        }

        if (escapeDropActive) {
            handleEscapeDrop(player);
            return;
        }

        double vSpeed = getActualVerticalSpeed(player);
        Vec3 velocity = player.getDeltaMovement();

        if (mc.options.keyJump.isDown()) {
            player.setDeltaMovement(velocity.x, vSpeed, velocity.z);
            return;
        }

        boolean allowShift = !ignoreShiftInGuis.getValue() || mc.screen == null;
        if (mc.options.keyShift.isDown() && allowShift) {
            double descend = slowSneaking.getValue() ? vSpeed * 0.3D : vSpeed;
            player.setDeltaMovement(velocity.x, -descend, velocity.z);
            return;
        }

        if (antiKick.getValue()) {
            doAntiKick(player);
            return;
        }

        player.setDeltaMovement(velocity.x, 0.0D, velocity.z);
    }

    private void checkDangerPlayers(LocalPlayer self) {
        double maxRangeSq = alertRange.getValue() * alertRange.getValue();
        for (Player other : mc.level.players()) {
            if (other == self || !other.isAlive() || other.isSpectator()) {
                continue;
            }

            if (self.distanceToSqr(other) > maxRangeSq) {
                continue;
            }

            DropTarget target = findDropTarget(self);
            if (target.type == DropTargetType.LAVA) {
                sendMessage("Flight cancelled: lava detected below.");
                setEnabled(false);
                return;
            }

            if (target.type == DropTargetType.NONE) {
                sendMessage("Flight cancelled: no safe drop target.");
                setEnabled(false);
                return;
            }

            triggered = true;
            NoFallModule noFall = ModuleManager.getInstance().getModule(NoFallModule.class);
            if (noFall != null && !noFall.isEnabled()) {
                noFall.setEnabled(true);
                enabledNoFallByEscape = true;
            }

            escapeTargetY = target.targetY;
            escapeDropActive = true;
            return;
        }
    }

    private void handleEscapeDrop(LocalPlayer player) {
        double currentY = player.getY();
        if (currentY <= escapeTargetY + 0.05D) {
            escapeDropActive = false;
            sendMessage("Flight cancelled: reached the ground.");
            setEnabled(false);
            return;
        }

        double maxDrop = escapeDropSpeed.getValue();
        double remaining = currentY - escapeTargetY;
        double step = Math.min(Math.max(remaining, 0.0D), maxDrop);
        if (step <= 0.0D) {
            step = remaining;
        }

        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, -step, velocity.z);
    }

    private void doAntiKick(LocalPlayer player) {
        int interval = (int) Math.round(antiKickInterval.getValue());
        if (tickCounter > interval + 1) {
            tickCounter = 0;
        }

        Vec3 velocity = player.getDeltaMovement();
        double pulse = antiKickDistance.getValue();

        if (tickCounter == 0) {
            if (velocity.y <= -pulse) {
                tickCounter = 2;
            } else {
                player.setDeltaMovement(velocity.x, -pulse, velocity.z);
            }
        } else if (tickCounter == 1) {
            player.setDeltaMovement(velocity.x, pulse, velocity.z);
        }

        tickCounter++;
    }

    private DropTarget findDropTarget(LocalPlayer player) {
        if (mc.level == null) {
            return new DropTarget(DropTargetType.NONE, 0.0D);
        }

        int x = (int) Math.floor(player.getX());
        int z = (int) Math.floor(player.getZ());
        int startY = (int) Math.floor(player.getY());
        int minY = mc.level.getMinY();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = startY; y >= minY; y--) {
            pos.set(x, y, z);
            BlockState state = mc.level.getBlockState(pos);

            if (!state.getFluidState().isEmpty()) {
                if (state.getFluidState().is(FluidTags.LAVA)) {
                    return new DropTarget(DropTargetType.LAVA, 0.0D);
                }
                if (state.getFluidState().is(FluidTags.WATER)) {
                    return new DropTarget(DropTargetType.WATER, y + 0.1D);
                }
            }

            if (!state.getCollisionShape(mc.level, pos).isEmpty()) {
                return new DropTarget(DropTargetType.GROUND, y + 1.0D);
            }
        }

        return new DropTarget(DropTargetType.GROUND, minY);
    }

    private double getActualVerticalSpeed(LocalPlayer player) {
        double value = getRawVerticalSpeed();
        boolean limitVerticalSpeed = !allowUnsafeVerticalSpeed.getValue() && !player.getAbilities().invulnerable;
        return Math.max(0.05D, Math.min(limitVerticalSpeed ? 3.95D : 10.0D, value));
    }

    private double getRawVerticalSpeed() {
        if (tieVerticalToHorizontal.getValue()) {
            return horizontalSpeed.getValue() * verticalSpeed.getValue();
        }
        return verticalSpeed.getValue();
    }

    private void sendMessage(String message) {
        ChatUtils.message(message);
    }
}
