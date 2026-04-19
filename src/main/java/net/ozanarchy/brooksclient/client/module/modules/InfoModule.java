package net.ozanarchy.brooksclient.client.module.modules;

import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;
import net.ozanarchy.brooksclient.client.setting.BooleanSetting;
import net.ozanarchy.brooksclient.client.setting.SliderSetting;

import java.util.EnumMap;
import java.util.Map;

public final class InfoModule extends Module {
    public enum Line {
        COORDS,
        PING,
        TPS,
        SERVER
    }

    private static final int DEFAULT_X = 8;
    private static final int DEFAULT_Y = 8;
    private static final int DEFAULT_LINE_SPACING = 10;

    private final BooleanSetting showCoordinates = addSetting(new BooleanSetting("Show Coordinates", true));
    private final BooleanSetting showCoordX = addSetting(new BooleanSetting("Show X", true));
    private final BooleanSetting showCoordY = addSetting(new BooleanSetting("Show Y", true));
    private final BooleanSetting showCoordZ = addSetting(new BooleanSetting("Show Z", true));
    private final BooleanSetting showPlayerPing = addSetting(new BooleanSetting("Show Player Ping", true));
    private final BooleanSetting showTps = addSetting(new BooleanSetting("Show TPS", true));
    private final BooleanSetting showServerIp = addSetting(new BooleanSetting("Show Server IP", true));

    private final SliderSetting textRed = addSetting(new SliderSetting("Text Red", 255.0D, 0.0D, 255.0D, 1.0D));
    private final SliderSetting textGreen = addSetting(new SliderSetting("Text Green", 255.0D, 0.0D, 255.0D, 1.0D));
    private final SliderSetting textBlue = addSetting(new SliderSetting("Text Blue", 255.0D, 0.0D, 255.0D, 1.0D));
    private final SliderSetting textAlpha = addSetting(new SliderSetting("Text Alpha", 255.0D, 32.0D, 255.0D, 1.0D));
    private final BooleanSetting rainbow = addSetting(new BooleanSetting("Rainbow", false));

    private final Map<Line, HudPoint> linePositions = new EnumMap<>(Line.class);

    public InfoModule() {
        super("Info", "Shows player/server info overlay.", Category.UI, -1);
        resetLinePositions();
    }

    public boolean isShowCoordinates() {
        return showCoordinates.getValue();
    }

    public boolean isShowCoordX() {
        return showCoordX.getValue();
    }

    public boolean isShowCoordY() {
        return showCoordY.getValue();
    }

    public boolean isShowCoordZ() {
        return showCoordZ.getValue();
    }

    public boolean isShowPlayerPing() {
        return showPlayerPing.getValue();
    }

    public boolean isShowTps() {
        return showTps.getValue();
    }

    public boolean isShowServerIp() {
        return showServerIp.getValue();
    }

    public boolean isRainbow() {
        return rainbow.getValue();
    }

    public int getTextRed() {
        return (int) Math.round(textRed.getValue());
    }

    public int getTextGreen() {
        return (int) Math.round(textGreen.getValue());
    }

    public int getTextBlue() {
        return (int) Math.round(textBlue.getValue());
    }

    public int getTextAlpha() {
        return (int) Math.round(textAlpha.getValue());
    }

    public HudPoint getLinePosition(Line line) {
        return linePositions.getOrDefault(line, new HudPoint(DEFAULT_X, DEFAULT_Y));
    }

    public void setLinePosition(Line line, int x, int y) {
        linePositions.put(line, new HudPoint(x, y));
    }

    public void resetLinePositions() {
        int y = DEFAULT_Y;
        for (Line line : Line.values()) {
            linePositions.put(line, new HudPoint(DEFAULT_X, y));
            y += DEFAULT_LINE_SPACING;
        }
    }

    public record HudPoint(int x, int y) {
    }
}
