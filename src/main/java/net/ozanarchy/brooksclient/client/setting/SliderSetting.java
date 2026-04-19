package net.ozanarchy.brooksclient.client.setting;

public final class SliderSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public SliderSetting(String name, double defaultValue, double min, double max, double step) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = Math.max(0.0001D, step);
        setValue(clamp(defaultValue));
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public void increment() {
        setValue(clamp(getValue() + step));
    }

    public void decrement() {
        setValue(clamp(getValue() - step));
    }

    @Override
    public void setValue(Double value) {
        super.setValue(clamp(value));
    }

    private double clamp(double value) {
        return Math.max(min, Math.min(max, value));
    }
}
