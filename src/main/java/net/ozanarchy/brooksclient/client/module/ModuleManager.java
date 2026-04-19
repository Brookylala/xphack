package net.ozanarchy.brooksclient.client.module;

import net.ozanarchy.brooksclient.client.module.modules.AutoEatModule;
import net.ozanarchy.brooksclient.client.module.modules.AutoFishModule;
import net.ozanarchy.brooksclient.client.module.modules.AutoJumpModule;
import net.ozanarchy.brooksclient.client.module.modules.AutoRefillModule;
import net.ozanarchy.brooksclient.client.module.modules.AutoRespawnModule;
import net.ozanarchy.brooksclient.client.module.modules.AutoSprintModule;
import net.ozanarchy.brooksclient.client.module.modules.AutoToolModule;
import net.ozanarchy.brooksclient.client.module.modules.AutoTotemModule;
import net.ozanarchy.brooksclient.client.module.modules.AutoWalkModule;
import net.ozanarchy.brooksclient.client.module.modules.FlightModule;
import net.ozanarchy.brooksclient.client.module.modules.FreecamModule;
import net.ozanarchy.brooksclient.client.module.modules.FullbrightModule;
import net.ozanarchy.brooksclient.client.module.modules.InfoModule;
import net.ozanarchy.brooksclient.client.module.modules.KillAuraModule;
import net.ozanarchy.brooksclient.client.module.modules.NoFallModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
    private static ModuleManager instance;

    private final ArrayList<Module> modules = new ArrayList<>();

    private ModuleManager() {
        register(new FullbrightModule());
        register(new AutoSprintModule());
        register(new AutoWalkModule());
        register(new AutoJumpModule());
        register(new FlightModule());
        register(new FreecamModule());
        register(new KillAuraModule());
        register(new NoFallModule());
        register(new AutoToolModule());
        register(new AutoEatModule());
        register(new AutoRefillModule());
        register(new AutoRespawnModule());
        register(new AutoFishModule());
        register(new AutoTotemModule());
        register(new InfoModule());
        ModuleSettingsStore.load(modules);
        KeybindSettingsStore.load(modules);
    }

    public static ModuleManager getInstance() {
        if (instance == null) {
            instance = new ModuleManager();
        }
        return instance;
    }

    private void register(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public void tickEnabledModules() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }

    public void saveSettings() {
        ModuleSettingsStore.save(modules);
        KeybindSettingsStore.save(modules);
    }

    public <T extends Module> T getModule(Class<T> type) {
        for (Module module : modules) {
            if (type.isInstance(module)) {
                return type.cast(module);
            }
        }
        return null;
    }
}
