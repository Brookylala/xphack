package net.ozanarchy.brooksclient.client.gui.theme;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class XpShellAssets {
    public static final int DESKTOP_SKY_TOP = 0xFF1A66D8;
    public static final int DESKTOP_SKY_BOTTOM = 0xFF4DA4FF;
    public static final int DESKTOP_FIELD_TOP = 0xFF5AA630;
    public static final int DESKTOP_FIELD_BOTTOM = 0xFF7FBD45;

    public static final int WINDOW_BORDER = 0xFF0A246A;
    public static final int WINDOW_FILL = 0xFFF1F3F7;
    public static final int TITLE_LEFT = 0xFF275DC2;
    public static final int TITLE_MID = 0xFF3A7CE8;
    public static final int TITLE_RIGHT = 0xFF0F3A90;

    public static final int TASK_PANE_TOP = 0xFFD6E4FF;
    public static final int TASK_PANE_BOTTOM = 0xFFB8CDF9;
    public static final int TASK_PANE_HEADER = 0xFF2D60B8;

    public static final int PANEL_LIGHT = 0xFFFFFFFF;
    public static final int PANEL_DARK = 0xFF7C8EA8;
    public static final int PANEL_MID = 0xFFE7EEF9;

    public static final int XP_GREEN_BUTTON = 0xFF36A12B;
    public static final int XP_GREEN_BUTTON_DARK = 0xFF247A1D;
    public static final int XP_RED_BUTTON = 0xFFD8332A;

    public static final int TEXT_DARK = 0xFF0B1E45;
    public static final int TEXT_LIGHT = 0xFFFFFFFF;

    public void fillVerticalGradient(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int topColor, int bottomColor) {
        graphics.fillGradient(x1, y1, x2, y2, topColor, bottomColor);
    }

    public void fillHorizontalGradient(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int leftColor, int rightColor) {
        int width = Math.max(1, x2 - x1);
        for (int i = 0; i < width; i++) {
            float t = width <= 1 ? 0.0F : (float) i / (float) (width - 1);
            int color = mix(leftColor, rightColor, t);
            graphics.fill(x1 + i, y1, x1 + i + 1, y2, color);
        }
    }

    public void drawRaisedBox(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int fillColor) {
        graphics.fill(x1, y1, x2, y2, fillColor);
        graphics.horizontalLine(x1, x2 - 1, y1, PANEL_LIGHT);
        graphics.verticalLine(x1, y1, y2 - 1, PANEL_LIGHT);
        graphics.horizontalLine(x1, x2 - 1, y2 - 1, PANEL_DARK);
        graphics.verticalLine(x2 - 1, y1, y2 - 1, PANEL_DARK);
    }

    public void drawSunkenBox(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int fillColor) {
        graphics.fill(x1, y1, x2, y2, fillColor);
        graphics.horizontalLine(x1, x2 - 1, y1, PANEL_DARK);
        graphics.verticalLine(x1, y1, y2 - 1, PANEL_DARK);
        graphics.horizontalLine(x1, x2 - 1, y2 - 1, PANEL_LIGHT);
        graphics.verticalLine(x2 - 1, y1, y2 - 1, PANEL_LIGHT);
    }

    private int mix(int colorA, int colorB, float t) {
        int aA = (colorA >>> 24) & 0xFF;
        int rA = (colorA >>> 16) & 0xFF;
        int gA = (colorA >>> 8) & 0xFF;
        int bA = colorA & 0xFF;

        int aB = (colorB >>> 24) & 0xFF;
        int rB = (colorB >>> 16) & 0xFF;
        int gB = (colorB >>> 8) & 0xFF;
        int bB = colorB & 0xFF;

        int a = (int) (aA + (aB - aA) * t);
        int r = (int) (rA + (rB - rA) * t);
        int g = (int) (gA + (gB - gA) * t);
        int b = (int) (bA + (bB - bA) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
