package net.ozanarchy.brooksclient.client.gui.theme;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class UiSettingsStore {
    private UiSettingsStore() {
    }

    public static UiSettings load(Path path) {
        if (!Files.exists(path)) {
            return new UiSettings("Default", true, new LinkedHashMap<>());
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String background = "Default";
            boolean moduleChatMessages = true;
            Map<String, WindowState> windows = new LinkedHashMap<>();
            String currentWindow = null;
            WindowStateBuilder builder = null;

            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("background:")) {
                    background = valueOf(line);
                    continue;
                }
                if (line.startsWith("module_chat_messages:")) {
                    moduleChatMessages = boolValue(line);
                    continue;
                }

                if (line.equals("windows:")) {
                    currentWindow = null;
                    builder = null;
                    continue;
                }

                if (line.endsWith(":") && !line.contains(" ")) {
                    if (currentWindow != null && builder != null) {
                        windows.put(currentWindow, builder.build());
                    }
                    currentWindow = line.substring(0, line.length() - 1);
                    builder = new WindowStateBuilder();
                    continue;
                }

                if (currentWindow != null && builder != null) {
                    if (line.startsWith("open:")) {
                        builder.open = boolValue(line);
                    } else if (line.startsWith("minimized:")) {
                        builder.minimized = boolValue(line);
                    } else if (line.startsWith("x:")) {
                        builder.x = intValue(line, builder.x);
                    } else if (line.startsWith("y:")) {
                        builder.y = intValue(line, builder.y);
                    } else if (line.startsWith("width:")) {
                        builder.width = intValue(line, builder.width);
                    } else if (line.startsWith("height:")) {
                        builder.height = intValue(line, builder.height);
                    } else if (line.startsWith("order:")) {
                        builder.order = intValue(line, builder.order);
                    }
                }
            }

            if (currentWindow != null && builder != null) {
                windows.put(currentWindow, builder.build());
            }

            return new UiSettings(background, moduleChatMessages, windows);
        } catch (IOException ignored) {
            return new UiSettings("Default", true, new LinkedHashMap<>());
        }
    }

    public static void save(Path path, UiSettings settings) {
        List<String> lines = new ArrayList<>();
        lines.add("background: " + settings.backgroundLabel());
        lines.add("module_chat_messages: " + settings.moduleChatMessages());
        lines.add("windows:");
        for (Map.Entry<String, WindowState> entry : settings.windows().entrySet()) {
            WindowState state = entry.getValue();
            lines.add("  " + entry.getKey() + ":");
            lines.add("    open: " + state.open());
            lines.add("    minimized: " + state.minimized());
            lines.add("    x: " + state.x());
            lines.add("    y: " + state.y());
            lines.add("    width: " + state.width());
            lines.add("    height: " + state.height());
            lines.add("    order: " + state.order());
        }

        try {
            Files.createDirectories(path.getParent());
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static String valueOf(String line) {
        int idx = line.indexOf(':');
        if (idx < 0 || idx + 1 >= line.length()) {
            return "";
        }
        return line.substring(idx + 1).trim();
    }

    private static boolean boolValue(String line) {
        return "true".equalsIgnoreCase(valueOf(line));
    }

    private static int intValue(String line, int fallback) {
        try {
            return Integer.parseInt(valueOf(line));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public record UiSettings(String backgroundLabel, boolean moduleChatMessages, Map<String, WindowState> windows) {
    }

    public record WindowState(boolean open, boolean minimized, int x, int y, int width, int height, int order) {
    }

    private static final class WindowStateBuilder {
        private boolean open;
        private boolean minimized;
        private int x = 40;
        private int y = 40;
        private int width = 520;
        private int height = 338;
        private int order;

        private WindowState build() {
            return new WindowState(open, minimized, x, y, width, height, order);
        }
    }
}
