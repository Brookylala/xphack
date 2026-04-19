package net.ozanarchy.brooksclient.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ServerData;
import net.ozanarchy.brooksclient.client.module.modules.InfoModule;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class InfoHudRenderer {
    private static final int LINE_HEIGHT = 10;

    public record HudRect(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    public record HudLine(InfoModule.Line line, String text) {
    }

    public Map<InfoModule.Line, HudRect> renderAndCollectBounds(Minecraft client, Object graphicsObject, InfoModule infoModule) {
        List<HudLine> lines = buildLines(client, infoModule);
        Map<InfoModule.Line, HudRect> bounds = new EnumMap<>(InfoModule.Line.class);
        if (lines.isEmpty()) {
            return bounds;
        }

        int baseColor = composeColor(infoModule);
        long now = System.currentTimeMillis();
        Font font = client.font;
        for (int i = 0; i < lines.size(); i++) {
            HudLine line = lines.get(i);
            InfoModule.HudPoint point = infoModule.getLinePosition(line.line());
            int textColor = infoModule.isRainbow() ? rainbow(now + i * 130L, infoModule.getTextAlpha()) : baseColor;
            drawText(graphicsObject, font, line.text(), point.x(), point.y(), textColor);
            int width = Math.max(20, font.width(line.text()));
            bounds.put(line.line(), new HudRect(point.x(), point.y(), width, LINE_HEIGHT));
        }
        return bounds;
    }

    public List<HudLine> buildLines(Minecraft client, InfoModule infoModule) {
        List<HudLine> lines = new ArrayList<>();

        if (infoModule.isShowCoordinates() && client.player != null) {
            double x = client.player.getX();
            double y = client.player.getY();
            double z = client.player.getZ();
            StringBuilder coord = new StringBuilder("Coords:");
            if (infoModule.isShowCoordX()) {
                coord.append(" X ").append((int) Math.floor(x));
            }
            if (infoModule.isShowCoordY()) {
                coord.append(" Y ").append((int) Math.floor(y));
            }
            if (infoModule.isShowCoordZ()) {
                coord.append(" Z ").append((int) Math.floor(z));
            }
            lines.add(new HudLine(InfoModule.Line.COORDS, coord.toString()));
        }

        if (infoModule.isShowPlayerPing()) {
            lines.add(new HudLine(InfoModule.Line.PING, "Ping: " + resolvePlayerPing(client) + "ms"));
        }

        if (infoModule.isShowTps()) {
            lines.add(new HudLine(InfoModule.Line.TPS, "TPS: " + String.format(Locale.ROOT, "%.1f", resolveTps(client))));
        }

        if (infoModule.isShowServerIp()) {
            lines.add(new HudLine(InfoModule.Line.SERVER, "Server: " + resolveServerIp(client)));
        }

        return lines;
    }

    public int lineHeight() {
        return LINE_HEIGHT;
    }

    private int composeColor(InfoModule infoModule) {
        int alpha = clamp8(infoModule.getTextAlpha());
        int red = clamp8(infoModule.getTextRed());
        int green = clamp8(infoModule.getTextGreen());
        int blue = clamp8(infoModule.getTextBlue());
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private int rainbow(long timeMs, int alpha) {
        float hue = (timeMs % 3600L) / 3600.0F;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.9F, 1.0F) & 0x00FFFFFF;
        return (clamp8(alpha) << 24) | rgb;
    }

    private int clamp8(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private void drawText(Object graphicsObject, Font font, String text, int x, int y, int color) {
        if (graphicsObject instanceof GuiGraphicsExtractor graphics) {
            graphics.text(font, text, x, y, color, true);
            return;
        }

        try {
            Method textMethod = graphicsObject.getClass().getMethod(
                    "text",
                    Font.class,
                    String.class,
                    int.class,
                    int.class,
                    int.class,
                    boolean.class
            );
            textMethod.invoke(graphicsObject, font, text, x, y, color, true);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private int resolvePlayerPing(Minecraft client) {
        try {
            Object connection = client.getConnection();
            if (connection == null || client.player == null) {
                return 0;
            }
            UUID uuid = client.player.getUUID();
            Method getPlayerInfo = connection.getClass().getMethod("getPlayerInfo", UUID.class);
            Object playerInfo = getPlayerInfo.invoke(connection, uuid);
            if (playerInfo == null) {
                return 0;
            }
            Method latency = playerInfo.getClass().getMethod("getLatency");
            Object ping = latency.invoke(playerInfo);
            if (ping instanceof Number number) {
                return Math.max(0, number.intValue());
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return 0;
    }

    private double resolveTps(Minecraft client) {
        try {
            if (client.level == null) {
                return 20.0D;
            }
            Object level = client.level;
            Method tickRateManagerGetter = level.getClass().getMethod("tickRateManager");
            Object tickRateManager = tickRateManagerGetter.invoke(level);
            if (tickRateManager == null) {
                return 20.0D;
            }
            Method tickRateGetter = tickRateManager.getClass().getMethod("tickrate");
            Object tps = tickRateGetter.invoke(tickRateManager);
            if (tps instanceof Number number) {
                return Math.max(0.0D, number.doubleValue());
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return 20.0D;
    }

    private String resolveServerIp(Minecraft client) {
        if (client.hasSingleplayerServer()) {
            return "Singleplayer";
        }
        ServerData server = client.getCurrentServer();
        if (server == null || server.ip == null || server.ip.isBlank()) {
            return "Unknown";
        }
        return server.ip;
    }
}
