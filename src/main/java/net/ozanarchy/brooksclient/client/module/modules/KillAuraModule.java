package net.ozanarchy.brooksclient.client.module.modules;

import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;
import net.ozanarchy.brooksclient.client.setting.BooleanSetting;
import net.ozanarchy.brooksclient.client.setting.ModeSetting;
import net.ozanarchy.brooksclient.client.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import java.util.List;

public final class KillAuraModule extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final SliderSetting range = addSetting(new SliderSetting("Range", 5.0D, 1.0D, 6.0D, 0.05D));
    private final SliderSetting attackSpeed = addSetting(new SliderSetting("Attack Speed", 2.5D, 0.5D, 12.0D, 0.1D));
    private final SliderSetting fov = addSetting(new SliderSetting("FOV", 360.0D, 30.0D, 360.0D, 5.0D));
    private final ModeSetting priority = addSetting(new ModeSetting("Priority", List.of("Distance", "Health"), "Distance"));
    private final BooleanSetting swing = addSetting(new BooleanSetting("Swing", true));
    private final BooleanSetting pauseOnContainers = addSetting(new BooleanSetting("Pause On Containers", false));

    private int attackDelayTicks;
    private int attackTimer;

    public KillAuraModule() {
        super("KillAura", Category.COMBAT, -1);
    }

    @Override
    public void onEnable() {
        attackDelayTicks = getDelayTicks();
        attackTimer = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.getConnection() == null || mc.gameMode == null) {
            return;
        }

        if (pauseOnContainers.getValue() && mc.screen != null) {
            return;
        }

        attackDelayTicks = getDelayTicks();
        if (attackTimer < attackDelayTicks) {
            attackTimer++;
            return;
        }

        LivingEntity target = null;
        double maxRangeSq = range.getValue() * range.getValue();
        double bestMetric = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }
            if (entity == mc.player || !livingEntity.isAlive() || livingEntity.isSpectator()) {
                continue;
            }

            double distanceSq = mc.player.distanceToSqr(entity);
            if (distanceSq > maxRangeSq) {
                continue;
            }

            if (fov.getValue() < 360.0D) {
                Vec3 center = livingEntity.getBoundingBox().getCenter();
                if (angleTo(center) > fov.getValue() / 2.0D) {
                    continue;
                }
            }

            double metric = "Health".equals(priority.getValue()) ? livingEntity.getHealth() : distanceSq;
            if (metric <= bestMetric) {
                bestMetric = metric;
                target = livingEntity;
            }
        }

        if (target == null) {
            return;
        }

        mc.gameMode.attack(mc.player, target);
        if (swing.getValue()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
        attackTimer = 0;
    }

    private int getDelayTicks() {
        double attacksPerSecond = Math.max(0.5D, attackSpeed.getValue());
        return Math.max(1, Mth.ceil(20.0D / attacksPerSecond));
    }

    private double angleTo(Vec3 target) {
        if (mc.player == null) {
            return 180.0D;
        }

        Vec3 look = mc.player.getLookAngle().normalize();
        Vec3 delta = target.subtract(mc.player.getEyePosition(1.0F)).normalize();
        double dot = Mth.clamp(look.dot(delta), -1.0D, 1.0D);
        return Math.toDegrees(Math.acos(dot));
    }
}
