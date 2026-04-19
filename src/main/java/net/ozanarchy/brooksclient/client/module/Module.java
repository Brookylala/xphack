package net.ozanarchy.brooksclient.client.module;

import net.ozanarchy.brooksclient.client.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private int keybind;
    private boolean enabled;

    protected Module(String name, Category category, int keybind) {
        this(name, "", category, keybind);
    }

    protected Module(String name, String description, Category category, int keybind) {
        this.name = name;
        this.description = description == null ? "" : description;
        this.category = category;
        this.keybind = keybind;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public int getKeybind() {
        return keybind;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    protected <T extends Setting<?>> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    public void setKeybind(int keybind) {
        this.keybind = keybind;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick() {
    }
}
