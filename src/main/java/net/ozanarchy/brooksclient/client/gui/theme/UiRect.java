package net.ozanarchy.brooksclient.client.gui.theme;

public record UiRect(int x, int y, int w, int h) {
    public int right() {
        return x + w;
    }

    public int bottom() {
        return y + h;
    }

    public UiRect inset(int amount) {
        return new UiRect(x + amount, y + amount, Math.max(0, w - amount * 2), Math.max(0, h - amount * 2));
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= right() && py >= y && py <= bottom();
    }
}
