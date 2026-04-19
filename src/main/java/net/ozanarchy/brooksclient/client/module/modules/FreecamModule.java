package net.ozanarchy.brooksclient.client.module.modules;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
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
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class FreecamModule extends Module {
    private static final int FREECAM_ENTITY_ID = -9132451;

    private enum InitialPosition {
        INSIDE,
        IN_FRONT,
        ABOVE
    }

    private enum ApplyInputTo {
        CAMERA,
        PLAYER
    }

    private enum InteractFrom {
        PLAYER,
        CAMERA
    }

    private final Minecraft mc = Minecraft.getInstance();
    private final SliderSetting speed = addSetting(new SliderSetting("Speed", 1.0D, 0.05D, 5.0D, 0.05D));
    private final ModeSetting initialPosition = addSetting(new ModeSetting(
            "Initial Position",
            List.of("Inside", "In Front", "Above"),
            "Inside"
    ));
    private final ModeSetting applyInputTo = addSetting(new ModeSetting(
            "Apply Input To",
            List.of("Camera", "Player"),
            "Camera"
    ));
    private final ModeSetting interactFrom = addSetting(new ModeSetting(
            "Interact From",
            List.of("Player", "Camera"),
            "Player"
    ));

    private RemotePlayer cameraEntity;
    private Vec3 frozenPlayerPos;
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
        prevNoPhysics = player.noPhysics;
        prevNoGravity = player.isNoGravity();

        player.setDeltaMovement(Vec3.ZERO);
        player.noPhysics = true;
        player.setNoGravity(true);

        removeFreecamEntity(level);
        GameProfile profile = player.getGameProfile();
        cameraEntity = new RemotePlayer(level, profile);
        cameraEntity.setId(FREECAM_ENTITY_ID);

        Vec3 basePos = player.position();
        Vec3 spawnPos = basePos.add(resolveOffset(player, parseInitialPosition()));
        cameraEntity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        syncCameraRotationFromPlayer(player);
        cameraEntity.setDeltaMovement(Vec3.ZERO);
        cameraEntity.noPhysics = true;
        cameraEntity.setNoGravity(true);
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
            player.setDeltaMovement(Vec3.ZERO);
            if (frozenPlayerPos != null) {
                player.setPos(frozenPlayerPos.x, frozenPlayerPos.y, frozenPlayerPos.z);
            }
        }

        if (level != null) {
            removeFreecamEntity(level);
        }

        cameraEntity = null;
        frozenPlayerPos = null;
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

        player.noPhysics = true;
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        if (frozenPlayerPos != null) {
            player.setPos(frozenPlayerPos.x, frozenPlayerPos.y, frozenPlayerPos.z);
        }

        if (parseApplyInputTo() == ApplyInputTo.CAMERA) {
            moveCameraFromInput(cameraEntity);
        }

        // Retained as a functional setting hook for later raycast routing.
        if (parseInteractFrom() == InteractFrom.CAMERA) {
            cameraEntity.setOldPosAndRot();
        }
    }

    private void moveCameraFromInput(Entity entity) {
        double inputForward = 0.0D;
        double inputStrafe = 0.0D;
        double inputY = 0.0D;

        boolean forwardKey = mc.options.keyUp.isDown() || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_W);
        boolean backKey = mc.options.keyDown.isDown() || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_S);
        boolean leftKey = mc.options.keyLeft.isDown() || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_A);
        boolean rightKey = mc.options.keyRight.isDown() || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_D);
        boolean jumpKey = mc.options.keyJump.isDown() || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_SPACE);
        boolean sneakKey = mc.options.keyShift.isDown()
                || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);

        if (forwardKey) {
            inputForward += 1.0D;
        }
        if (backKey) {
            inputForward -= 1.0D;
        }
        if (leftKey) {
            inputStrafe -= 1.0D;
        }
        if (rightKey) {
            inputStrafe += 1.0D;
        }
        if (jumpKey) {
            inputY += 1.0D;
        }
        if (sneakKey) {
            inputY -= 1.0D;
        }

        if (inputForward == 0.0D && inputStrafe == 0.0D && inputY == 0.0D) {
            return;
        }

        double magnitude = Math.sqrt(inputForward * inputForward + inputStrafe * inputStrafe);
        if (magnitude > 0.0D) {
            inputForward /= magnitude;
            inputStrafe /= magnitude;
        }

        float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);

        double baseSpeed = speed.getValue() * 0.35D;
        double motionX = (inputForward * -sin + inputStrafe * cos) * baseSpeed;
        double motionZ = (inputForward * cos + inputStrafe * sin) * baseSpeed;
        double motionY = inputY * baseSpeed;

        Vec3 pos = entity.position();
        entity.setPos(pos.x + motionX, pos.y + motionY, pos.z + motionZ);
    }

    private Vec3 resolveOffset(LocalPlayer player, InitialPosition position) {
        double distance = 0.55D * player.getScale();
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
        return switch (initialPosition.getValue().toLowerCase()) {
            case "in front" -> InitialPosition.IN_FRONT;
            case "above" -> InitialPosition.ABOVE;
            default -> InitialPosition.INSIDE;
        };
    }

    private ApplyInputTo parseApplyInputTo() {
        return "player".equalsIgnoreCase(applyInputTo.getValue()) ? ApplyInputTo.PLAYER : ApplyInputTo.CAMERA;
    }

    private InteractFrom parseInteractFrom() {
        return "camera".equalsIgnoreCase(interactFrom.getValue()) ? InteractFrom.CAMERA : InteractFrom.PLAYER;
    }

    private void removeFreecamEntity(ClientLevel level) {
        Entity existing = level.getEntity(FREECAM_ENTITY_ID);
        if (existing != null) {
            level.removeEntity(FREECAM_ENTITY_ID, Entity.RemovalReason.DISCARDED);
        }
    }

    private void syncCameraRotationFromPlayer(LocalPlayer player) {
        if (cameraEntity == null || player == null) {
            return;
        }
        cameraEntity.setYRot(player.getYRot());
        cameraEntity.setXRot(player.getXRot());
        cameraEntity.setYBodyRot(player.getYRot());
        cameraEntity.setYHeadRot(player.getYRot());
    }
}
