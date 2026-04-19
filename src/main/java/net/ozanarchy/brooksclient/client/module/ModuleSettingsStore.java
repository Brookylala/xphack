package net.ozanarchy.brooksclient.client.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.ozanarchy.brooksclient.client.setting.BooleanSetting;
import net.ozanarchy.brooksclient.client.setting.ModeSetting;
import net.ozanarchy.brooksclient.client.setting.Setting;
import net.ozanarchy.brooksclient.client.setting.SliderSetting;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleSettingsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() { }.getType();
    private static final String FILE_NAME = "xphack-settings.json";

    private ModuleSettingsStore() {
    }

    public static void load(List<Module> modules) {
        Path path = path();
        if (!Files.exists(path)) {
            return;
        }

        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            Map<String, Object> data = GSON.fromJson(raw, MAP_TYPE);
            if (data == null) {
                return;
            }
            for (Module module : modules) {
                for (Setting<?> setting : module.getSettings()) {
                    String key = key(module, setting);
                    if (!data.containsKey(key)) {
                        continue;
                    }
                    Object value = data.get(key);
                    apply(setting, value);
                }
            }
            for (Module module : modules) {
                String enabledKey = enabledKey(module);
                if (!data.containsKey(enabledKey)) {
                    continue;
                }
                Object value = data.get(enabledKey);
                if (value instanceof Boolean enabled) {
                    module.setEnabled(enabled);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void save(List<Module> modules) {
        Map<String, Object> data = new HashMap<>();
        for (Module module : modules) {
            data.put(enabledKey(module), module.isEnabled());
            for (Setting<?> setting : module.getSettings()) {
                data.put(key(module, setting), setting.getValue());
            }
        }

        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static void apply(Setting<?> setting, Object value) {
        if (setting instanceof BooleanSetting booleanSetting && value instanceof Boolean boolVal) {
            booleanSetting.setValue(boolVal);
            return;
        }
        if (setting instanceof SliderSetting sliderSetting && value instanceof Number numVal) {
            sliderSetting.setValue(numVal.doubleValue());
            return;
        }
        if (setting instanceof ModeSetting modeSetting && value instanceof String strVal
                && modeSetting.getModes().contains(strVal)) {
            modeSetting.setValue(strVal);
        }
    }

    private static String key(Module module, Setting<?> setting) {
        return module.getName() + "." + setting.getName();
    }

    private static String enabledKey(Module module) {
        return module.getName() + ".enabled";
    }

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve(FILE_NAME);
    }
}
