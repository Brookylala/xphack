package net.ozanarchy.brooksclient.client.gui.theme;

import net.minecraft.client.gui.screens.Screen;

public final class XpShellManager {
    private static XpShellManager instance;

    private final XpShellAssets assets = new XpShellAssets();
    private final XpShellRenderer renderer = new XpShellRenderer(assets);

    private XpShellManager() {
    }

    public static XpShellManager getInstance() {
        if (instance == null) {
            instance = new XpShellManager();
        }
        return instance;
    }

    public XpShellRenderer getRenderer() {
        return renderer;
    }

    public Screen createClickGuiScreen() {
        return new XpShellScreen(this);
    }
}
