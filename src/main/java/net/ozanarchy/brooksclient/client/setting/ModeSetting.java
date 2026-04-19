package net.ozanarchy.brooksclient.client.setting;

import java.util.List;

public final class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, List<String> modes, String defaultMode) {
        super(name, defaultMode);
        if (modes == null || modes.isEmpty()) {
            throw new IllegalArgumentException("ModeSetting requires at least one mode.");
        }
        this.modes = List.copyOf(modes);
        if (!this.modes.contains(defaultMode)) {
            setValue(this.modes.get(0));
        }
    }

    public List<String> getModes() {
        return modes;
    }

    public void next() {
        int index = modes.indexOf(getValue());
        int nextIndex = (index + 1) % modes.size();
        setValue(modes.get(nextIndex));
    }
}

