package net.ozanarchy.brooksclient.client.module.modules;

import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class FullbrightModule extends Module {
    private static final int NV_DURATION_TICKS = 240;
    private static final double FULL_BRIGHT_GAMMA = 1.0D;

    private final Minecraft mc = Minecraft.getInstance();
    private Double originalGamma;
    private boolean appliedNightVision;

    public FullbrightModule() {
        super("Fullbright", Category.RENDER, -1);
    }

    @Override
    public void onEnable() {
        OptionInstance<Double> gammaOption = mc.options.gamma();
        originalGamma = gammaOption.get();
        gammaOption.set(FULL_BRIGHT_GAMMA);
        appliedNightVision = false;

        if (mc.player != null && !mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
            mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NV_DURATION_TICKS, 0, false, false, false));
            appliedNightVision = true;
        }
    }

    @Override
    public void onDisable() {
        if (originalGamma == null) {
            return;
        }

        mc.options.gamma().set(originalGamma);
        originalGamma = null;

        if (mc.player != null && appliedNightVision) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
        appliedNightVision = false;
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null) {
            return;
        }

        // Keep both mechanisms active for version-specific rendering differences.
        OptionInstance<Double> gammaOption = mc.options.gamma();
        if (Double.compare(gammaOption.get(), FULL_BRIGHT_GAMMA) != 0) {
            gammaOption.set(FULL_BRIGHT_GAMMA);
        }
        if (!mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
            mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NV_DURATION_TICKS, 0, false, false, false));
            appliedNightVision = true;
        }
    }
}
