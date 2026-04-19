package net.ozanarchy.brooksclient.client.setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class Setting<T> {
    private final String name;
    private T value;
    private final List<Consumer<T>> changeListeners = new ArrayList<>();

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        if (Objects.equals(this.value, value)) {
            return;
        }
        this.value = value;
        for (Consumer<T> listener : changeListeners) {
            listener.accept(value);
        }
    }

    public void addChangeListener(Consumer<T> listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }
}
