package net.ozanarchy.brooksclient.client.gui.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.ozanarchy.brooksclient.client.module.Category;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class DesktopWindowStateStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, WindowState>>() { }.getType();
    private static final String FILE_NAME = "xphack-window-layout.json";

    private DesktopWindowStateStore() {
    }

    public static Map<Category, WindowState> load() {
        Path path = path();
        if (!Files.exists(path)) {
            return new EnumMap<>(Category.class);
        }

        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            Map<String, WindowState> encoded = GSON.fromJson(raw, MAP_TYPE);
            if (encoded == null) {
                return new EnumMap<>(Category.class);
            }
            Map<Category, WindowState> decoded = new EnumMap<>(Category.class);
            for (Map.Entry<String, WindowState> entry : encoded.entrySet()) {
                try {
                    decoded.put(Category.valueOf(entry.getKey()), entry.getValue());
                } catch (IllegalArgumentException ignored) {
                }
            }
            return decoded;
        } catch (Exception ignored) {
            return new EnumMap<>(Category.class);
        }
    }

    public static void save(Map<Category, WindowState> states) {
        Map<String, WindowState> encoded = new HashMap<>();
        for (Map.Entry<Category, WindowState> entry : states.entrySet()) {
            encoded.put(entry.getKey().name(), entry.getValue());
        }

        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(encoded), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve(FILE_NAME);
    }

    public record WindowState(
            int x,
            int y,
            int width,
            int height,
            boolean open,
            boolean minimized,
            boolean maximized,
            String selectedModuleName
    ) {
    }
}
