package net.ozanarchy.brooksclient.client.module;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.ozanarchy.brooksclient.client.XPHackClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class KeybindSettingsStore {
    private static final String FILE_NAME = "keybinds.yml";

    private KeybindSettingsStore() {
    }

    public static void load(List<Module> modules) {
        Path path = path();
        if (!Files.exists(path)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            Map<String, Integer> moduleKeys = new HashMap<>();
            int guiKeybind = InputConstants.KEY_RSHIFT;
            String currentSection = "";

            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.equals("gui:") || line.equals("modules:")) {
                    currentSection = line.substring(0, line.length() - 1);
                    continue;
                }

                if (!line.contains(":")) {
                    continue;
                }

                String key = line.substring(0, line.indexOf(':')).trim();
                String valueRaw = line.substring(line.indexOf(':') + 1).trim();
                int value;
                try {
                    value = Integer.parseInt(valueRaw);
                } catch (NumberFormatException ignored) {
                    continue;
                }

                if ("gui".equals(currentSection) && "open".equalsIgnoreCase(key)) {
                    guiKeybind = value;
                } else if ("modules".equals(currentSection)) {
                    moduleKeys.put(key, value);
                }
            }

            XPHackClient.setOpenGuiKeybind(guiKeybind);
            for (Module module : modules) {
                Integer keybind = moduleKeys.get(module.getName());
                if (keybind != null) {
                    module.setKeybind(keybind);
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static void save(List<Module> modules) {
        Path path = path();
        List<String> lines = new ArrayList<>();
        lines.add("gui:");
        lines.add("  open: " + XPHackClient.getOpenGuiKeybind());
        lines.add("modules:");
        for (Module module : modules) {
            lines.add("  " + module.getName() + ": " + module.getKeybind());
        }

        try {
            Files.createDirectories(path.getParent());
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("xphack")
                .resolve(FILE_NAME);
    }
}
