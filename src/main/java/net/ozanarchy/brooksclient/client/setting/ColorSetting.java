package net.ozanarchy.brooksclient.client.setting;

public final class ColorSetting extends Setting<Integer> {
    private final int defaultValue;

    public ColorSetting(String name, int defaultValue) {
        super(name, defaultValue);
        this.defaultValue = defaultValue;
    }

    public int getArgb() {
        Integer value = getValue();
        return value == null ? defaultValue : value;
    }

    public void setArgb(int argb) {
        setValue(argb);
    }

    public int getDefaultArgb() {
        return defaultValue;
    }

    public int getAlpha() {
        return (getArgb() >>> 24) & 0xFF;
    }

    public int getRed() {
        return (getArgb() >>> 16) & 0xFF;
    }

    public int getGreen() {
        return (getArgb() >>> 8) & 0xFF;
    }

    public int getBlue() {
        return getArgb() & 0xFF;
    }

    public void reset() {
        setArgb(defaultValue);
    }

    public static int argb(int a, int r, int g, int b) {
        a = clamp8(a);
        r = clamp8(r);
        g = clamp8(g);
        b = clamp8(b);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clamp8(int value) {
        return Math.max(0, Math.min(255, value));
    }
}