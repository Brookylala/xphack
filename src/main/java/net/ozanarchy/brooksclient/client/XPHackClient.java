package net.ozanarchy.brooksclient.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.ozanarchy.brooksclient.client.gui.theme.UiSettingsStore;
import net.ozanarchy.brooksclient.client.gui.theme.XpShellManager;
import net.ozanarchy.brooksclient.client.hud.InfoHudRenderer;
import net.ozanarchy.brooksclient.client.module.Module;
import net.ozanarchy.brooksclient.client.module.ModuleManager;
import net.ozanarchy.brooksclient.client.module.modules.InfoModule;
import net.ozanarchy.brooksclient.client.util.ChatUtils;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class XPHackClient implements ClientModInitializer {
    private static XPHackClient instance;
    private static volatile int openGuiKeybind = InputConstants.KEY_RSHIFT;
    private static volatile boolean moduleChatMessagesEnabled = true;

    private final ModuleManager moduleManager = ModuleManager.getInstance();
    private final Set<Integer> heldKeys = new HashSet<>();
    private final InfoHudRenderer infoHudRenderer = new InfoHudRenderer();
    private final Map<InfoModule.Line, InfoHudRenderer.HudRect> lastInfoBounds = new EnumMap<>(InfoModule.Line.class);

    private InfoModule.Line draggingLine;
    private int dragOffsetX;
    private int dragOffsetY;

    @Override
    public void onInitializeClient() {
        instance = this;
        loadUiToggles();
        registerHudCallback();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleGuiHotkey(client);

            if (client.player == null || client.level == null || client.screen != null) {
                return;
            }

            handleInfoHudDragging(client);
            handleModuleHotkeys(client);
            moduleManager.tickEnabledModules();
        });
    }

    public static void renderInfoHudHook(Minecraft client, Object graphicsObject) {
        if (instance == null) {
            return;
        }
        instance.renderInfoHud(client, graphicsObject);
    }

    private void handleGuiHotkey(Minecraft client) {
        int guiKeybind = getOpenGuiKeybind();
        if (guiKeybind < 0) {
            return;
        }

        boolean pressed = InputConstants.isKeyDown(client.getWindow(), guiKeybind);
        if (pressed && heldKeys.add(guiKeybind)) {
            client.setScreen(XpShellManager.getInstance().createClickGuiScreen());
            return;
        }
        if (!pressed) {
            heldKeys.remove(guiKeybind);
        }
    }

    private void handleModuleHotkeys(Minecraft client) {
        for (Module module : moduleManager.getModules()) {
            int keybind = module.getKeybind();
            if (keybind < 0) {
                continue;
            }

            boolean pressed = InputConstants.isKeyDown(client.getWindow(), keybind);
            if (pressed && heldKeys.add(keybind)) {
                module.toggle();
                notifyModuleToggle(module);
                moduleManager.saveSettings();
            } else if (!pressed) {
                heldKeys.remove(keybind);
            }
        }
    }

    private void renderInfoHud(Minecraft client, Object graphicsObj) {
        if (client.player == null || client.level == null || client.options.hideGui) {
            return;
        }

        InfoModule infoModule = moduleManager.getModule(InfoModule.class);
        if (infoModule == null || !infoModule.isEnabled()) {
            return;
        }

        lastInfoBounds.clear();
        lastInfoBounds.putAll(infoHudRenderer.renderAndCollectBounds(client, graphicsObj, infoModule));
    }

    private void handleInfoHudDragging(Minecraft client) {
        InfoModule infoModule = moduleManager.getModule(InfoModule.class);
        if (infoModule == null || !infoModule.isEnabled() || lastInfoBounds.isEmpty()) {
            draggingLine = null;
            return;
        }

        boolean editHeld = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
        if (!editHeld) {
            draggingLine = null;
            return;
        }

        boolean mouseDown = isLeftMouseDown(client);
        if (!mouseDown) {
            draggingLine = null;
            return;
        }

        double rawMouseX = client.mouseHandler.xpos();
        double rawMouseY = client.mouseHandler.ypos();
        double scaledMouseX = rawMouseX * client.getWindow().getGuiScaledWidth() / client.getWindow().getScreenWidth();
        double scaledMouseY = rawMouseY * client.getWindow().getGuiScaledHeight() / client.getWindow().getScreenHeight();

        if (draggingLine == null) {
            for (Map.Entry<InfoModule.Line, InfoHudRenderer.HudRect> entry : lastInfoBounds.entrySet()) {
                if (entry.getValue().contains(scaledMouseX, scaledMouseY)) {
                    draggingLine = entry.getKey();
                    InfoModule.HudPoint point = infoModule.getLinePosition(draggingLine);
                    dragOffsetX = (int) scaledMouseX - point.x();
                    dragOffsetY = (int) scaledMouseY - point.y();
                    break;
                }
            }
        }

        if (draggingLine != null) {
            int newX = (int) scaledMouseX - dragOffsetX;
            int newY = (int) scaledMouseY - dragOffsetY;
            int maxX = Math.max(0, client.getWindow().getGuiScaledWidth() - 20);
            int maxY = Math.max(0, client.getWindow().getGuiScaledHeight() - infoHudRenderer.lineHeight());
            newX = Math.max(0, Math.min(maxX, newX));
            newY = Math.max(0, Math.min(maxY, newY));
            infoModule.setLinePosition(draggingLine, newX, newY);
            moduleManager.saveSettings();
        }
    }

    public static int getOpenGuiKeybind() {
        return openGuiKeybind;
    }

    public static void setOpenGuiKeybind(int keybind) {
        openGuiKeybind = keybind;
    }

    public static boolean isModuleChatMessagesEnabled() {
        return moduleChatMessagesEnabled;
    }

    public static void setModuleChatMessagesEnabled(boolean enabled) {
        moduleChatMessagesEnabled = enabled;
    }

    public static void notifyModuleToggle(Module module) {
        if (!moduleChatMessagesEnabled || module == null) {
            return;
        }
        String state = module.isEnabled() ? "&aEnabled" : "&cDisabled";
        ChatUtils.message(module.getName() + ": " + state);
    }

    private void loadUiToggles() {
        Minecraft mc = Minecraft.getInstance();
        Path path = mc.gameDirectory.toPath().resolve("xphack").resolve("ui-settings.yml");
        UiSettingsStore.UiSettings settings = UiSettingsStore.load(path);
        moduleChatMessagesEnabled = settings.moduleChatMessages();
    }

    private void registerHudCallback() {
        try {
            Class<?> callbackClass = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback");
            Object callback = Proxy.newProxyInstance(
                    callbackClass.getClassLoader(),
                    new Class<?>[]{callbackClass},
                    (proxy, method, args) -> {
                        if ("onHudRender".equals(method.getName()) && args != null && args.length >= 1) {
                            renderInfoHud(Minecraft.getInstance(), args[0]);
                        }
                        return null;
                    }
            );

            Object event = callbackClass.getField("EVENT").get(null);
            Method register = event.getClass().getMethod("register", callbackClass);
            register.invoke(event, callback);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private boolean isLeftMouseDown(Minecraft client) {
        try {
            Object mouseHandler = client.mouseHandler;
            Method method = mouseHandler.getClass().getMethod("isLeftPressed");
            Object result = method.invoke(mouseHandler);
            if (result instanceof Boolean value) {
                return value;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Object mouseHandler = client.mouseHandler;
            Method method = mouseHandler.getClass().getMethod("leftButtonDown");
            Object result = method.invoke(mouseHandler);
            if (result instanceof Boolean value) {
                return value;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return false;
    }
}
