package net.ozanarchy.brooksclient.client.module.modules;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;
import net.ozanarchy.brooksclient.client.setting.ModeSetting;
import net.ozanarchy.brooksclient.client.setting.SliderSetting;

import java.util.List;

public final class FreecamModule extends Module {
    private static final int FREECAM_ENTITY_ID = -9132451;

    private enum InitialPosition {
        INSIDE,
        IN_FRONT,
        ABOVE
    }

    private final Minecraft mc = Minecraft.getInstance();

    private final SliderSetting speed = addSetting(
            new SliderSetting("Speed", 1.0D, 0.05D, 5.0D, 0.05D)
    );

    private final ModeSetting initialPosition = addSetting(new ModeSetting(
            "Initial Position",
            List.of("Inside", "In Front", "Above"),
            "Inside"
    ));

    private RemotePlayer cameraEntity;
    private Vec3 frozenPlayerPos;
    private Vec3 frozenPlayerDelta;
    private boolean prevNoPhysics;
    private boolean prevNoGravity;

    public FreecamModule() {
        super("Freecam", "Detach camera and fly freely while keeping player position.", Category.MOVEMENT, -1);
    }

    @Override
    public void onEnable() {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;

        if (player == null || level == null) {
            setEnabled(false);
            return;
        }

        frozenPlayerPos = player.position();
        frozenPlayerDelta = player.getDeltaMovement();
        prevNoPhysics = player.noPhysics;
        prevNoGravity = player.isNoGravity();

        player.setDeltaMovement(Vec3.ZERO);
        player.noPhysics = true;
        player.setNoGravity(true);

        removeFreecamEntity(level);

        GameProfile profile = player.getGameProfile();
        cameraEntity = new RemotePlayer(level, profile);
        cameraEntity.setId(FREECAM_ENTITY_ID);

        Vec3 spawnPos = frozenPlayerPos.add(resolveOffset(player, parseInitialPosition()));
        cameraEntity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        cameraEntity.setDeltaMovement(Vec3.ZERO);
        cameraEntity.noPhysics = true;
        cameraEntity.setNoGravity(true);

        syncCameraRotationFromPlayer(player);

        level.addEntity(cameraEntity);
        mc.setCameraEntity(cameraEntity);
    }

    @Override
    public void onDisable() {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;

        if (player != null) {
            mc.setCameraEntity(player);

            player.noPhysics = prevNoPhysics;
            player.setNoGravity(prevNoGravity);

            if (frozenPlayerPos != null) {
                player.setPos(frozenPlayerPos.x, frozenPlayerPos.y, frozenPlayerPos.z);
            }

            player.setDeltaMovement(frozenPlayerDelta != null ? frozenPlayerDelta : Vec3.ZERO);
        }

        if (level != null) {
            removeFreecamEntity(level);
        }

        cameraEntity = null;
        frozenPlayerPos = null;
        frozenPlayerDelta = null;
    }

    @Override
    public void onTick() {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;

        if (player == null || level == null || cameraEntity == null) {
            return;
        }

        if (mc.getCameraEntity() != cameraEntity) {
            mc.setCameraEntity(cameraEntity);
        }

        freezeRealPlayer(player);
        syncCameraRotationFromPlayer(player);
        moveCameraFromInput(cameraEntity);
    }

    private void freezeRealPlayer(LocalPlayer player) {
        player.noPhysics = true;
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);

        if (frozenPlayerPos != null) {
            player.setPos(frozenPlayerPos.x, frozenPlayerPos.y, frozenPlayerPos.z);
        }
    }

    private void moveCameraFromInput(Entity entity) {
        if (mc.options == null) {
            return;
        }

        double forward = 0.0D;
        double strafe = 0.0D;
        double vertical = 0.0D;

        if (mc.options.keyUp.isDown()) {
            forward += 1.0D;
        }
        if (mc.options.keyDown.isDown()) {
            forward -= 1.0D;
        }
        if (mc.options.keyLeft.isDown()) {
            strafe += 1.0D;
        }
        if (mc.options.keyRight.isDown()) {
            strafe -= 1.0D;
        }
        if (mc.options.keyJump.isDown()) {
            vertical += 1.0D;
        }
        if (mc.options.keyShift.isDown()) {
            vertical -= 1.0D;
        }

        if (forward == 0.0D && strafe == 0.0D && vertical == 0.0D) {
            return;
        }

        double horizontalMagnitude = Math.sqrt(forward * forward + strafe * strafe);
        if (horizontalMagnitude > 1.0D) {
            forward /= horizontalMagnitude;
            strafe /= horizontalMagnitude;
        }

        float yawRad = entity.getYRot() * Mth.DEG_TO_RAD;

        double sinYaw = Math.sin(yawRad);
        double cosYaw = Math.cos(yawRad);

        double moveSpeed = speed.getValue() * 0.35D;

        double motionX = (-sinYaw * forward + cosYaw * strafe) * moveSpeed;
        double motionZ = (cosYaw * forward + sinYaw * strafe) * moveSpeed;
        double motionY = vertical * moveSpeed;

        Vec3 currentPos = entity.position();
        entity.setPos(
                currentPos.x + motionX,
                currentPos.y + motionY,
                currentPos.z + motionZ
        );
    }

    private Vec3 resolveOffset(LocalPlayer player, InitialPosition position) {
        double distance = 0.6D * player.getScale();

        return switch (position) {
            case INSIDE -> Vec3.ZERO;
            case IN_FRONT -> {
                float yawRad = player.getYRot() * Mth.DEG_TO_RAD;
                double offsetX = -Mth.sin(yawRad) * distance;
                double offsetZ = Mth.cos(yawRad) * distance;
                yield new Vec3(offsetX, 0.0D, offsetZ);
            }
            case ABOVE -> new Vec3(0.0D, distance, 0.0D);
        };
    }

    private InitialPosition parseInitialPosition() {
        String value = initialPosition.getValue();
        if (value == null) {
            return InitialPosition.INSIDE;
        }

        return switch (value.toLowerCase()) {
            case "in front" -> InitialPosition.IN_FRONT;
            case "above" -> InitialPosition.ABOVE;
            default -> InitialPosition.INSIDE;
        };
    }

    private void syncCameraRotationFromPlayer(LocalPlayer player) {
        if (cameraEntity == null || player == null) {
            return;
        }

        cameraEntity.setYRot(player.getYRot());
        cameraEntity.setXRot(player.getXRot());
        cameraEntity.setYHeadRot(player.getYRot());
        cameraEntity.setYBodyRot(player.getYRot());
    }

    private void removeFreecamEntity(ClientLevel level) {
        Entity existing = level.getEntity(FREECAM_ENTITY_ID);
        if (existing != null) {
            level.removeEntity(FREECAM_ENTITY_ID, Entity.RemovalReason.DISCARDED);
        }
    }
}