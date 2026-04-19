package net.ozanarchy.brooksclient.client.gui.theme;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.ozanarchy.brooksclient.client.XPHackClient;
import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;
import net.ozanarchy.brooksclient.client.module.ModuleManager;
import net.ozanarchy.brooksclient.client.module.modules.InfoModule;
import net.ozanarchy.brooksclient.client.setting.BooleanSetting;
import net.ozanarchy.brooksclient.client.setting.ModeSetting;
import net.ozanarchy.brooksclient.client.setting.Setting;
import net.ozanarchy.brooksclient.client.setting.SliderSetting;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class XpShellScreen extends Screen {
    private static final int DEFAULT_WINDOW_WIDTH = 520;
    private static final int DEFAULT_WINDOW_HEIGHT = 338;
    private static final int MIN_WINDOW_WIDTH = 420;
    private static final int MIN_WINDOW_HEIGHT = 260;
    private static final int ROW_HEIGHT = 18;
    private static final int TASKBAR_HEIGHT = 28;
    private static final int RESIZE_GRIP_SIZE = 12;
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private final XpShellRenderer renderer;
    private final ModuleManager moduleManager;
    private final StartMenuComponent startMenu = new StartMenuComponent();
    private final List<CategoryWindow> windows = new ArrayList<>();
    private final List<BackgroundOption> backgroundOptions = new ArrayList<>();
    private final Map<Path, Identifier> wallpaperTextures = new HashMap<>();

    private CategoryWindow activeWindow;
    private CategoryWindow draggingWindow;
    private CategoryWindow resizingWindow;
    private int dragOffsetX;
    private int dragOffsetY;
    private int resizeStartMouseX;
    private int resizeStartMouseY;
    private int resizeStartWidth;
    private int resizeStartHeight;
    private int selectedBackgroundIndex;
    private Path wallpapersDirectory;
    private Path uiSettingsPath;

    public XpShellScreen(XpShellManager themeManager) {
        super(Component.literal("XPHack Shell"));
        this.renderer = themeManager.getRenderer();
        this.moduleManager = ModuleManager.getInstance();
    }

    @Override
    protected void init() {
        if (minecraft != null) {
            wallpapersDirectory = minecraft.gameDirectory.toPath().resolve("xphack").resolve("wallpapers");
            uiSettingsPath = minecraft.gameDirectory.toPath().resolve("xphack").resolve("ui-settings.yml");
            ensureWallpaperFolder();
            reloadBackgroundOptions();
            loadUiSettings();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        BackgroundOption selectedBackground = selectedBackground();
        if (selectedBackground.type() == BackgroundType.NONE) {
            renderer.renderTaskbarBase(graphics, width, height);
        } else if (selectedBackground.type() == BackgroundType.FILE && selectedBackground.textureId() != null) {
            renderer.renderWallpaperDesktop(graphics, width, height, selectedBackground.textureId());
        } else {
            renderer.renderDesktop(graphics, width, height);
        }

        for (CategoryWindow window : windows) {
            if (!window.minimized) {
                renderCategoryWindow(graphics, window, mouseX, mouseY);
            }
        }

        DesktopLayout desktopLayout = buildDesktopLayout();
        renderTaskbarButtons(graphics, desktopLayout.taskButtons());
        renderer.renderTaskbarTray(graphics, font, desktopLayout.trayBounds(), currentClockText());
        boolean startHovered = desktopLayout.startButtonBounds().contains(mouseX, mouseY);
        renderer.renderStartButton(graphics, font, desktopLayout.startButtonBounds(), startHovered, startMenu.isOpen());

        Category currentCategory = activeWindow == null ? Category.MOVEMENT : activeWindow.category;
        startMenu.render(graphics, renderer, font, moduleManager, mouseX, mouseY, currentCategory, currentPlayerName(), currentPlayerSkin());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        DesktopLayout desktopLayout = buildDesktopLayout();

        if (desktopLayout.startButtonBounds().contains(mouseX, mouseY) && button == 0) {
            Category current = activeWindow == null ? Category.MOVEMENT : activeWindow.category;
            startMenu.toggle(width, height, TASKBAR_HEIGHT, desktopLayout.startButtonBounds(), current);
            return true;
        }

        if (startMenu.isOpen()) {
            if (!startMenu.contains(mouseX, mouseY)) {
                startMenu.close();
                return true;
            }
            StartMenuComponent.StartMenuAction action = startMenu.mouseClicked(mouseX, mouseY, button, moduleManager);
            if (applyStartMenuAction(action)) {
                return true;
            }
        }

        if (button == 0) {
            for (TaskbarButton taskbarButton : desktopLayout.taskButtons()) {
                if (taskbarButton.bounds().contains(mouseX, mouseY)) {
                    toggleTaskbarWindow(taskbarButton.window());
                    return true;
                }
            }
        }

        for (int i = windows.size() - 1; i >= 0; i--) {
            CategoryWindow window = windows.get(i);
            if (window.minimized) {
                continue;
            }
            WindowLayout layout = buildWindowLayout(window);
            if (!layout.windowBounds().contains(mouseX, mouseY)) {
                continue;
            }

            bringToFront(window);

            if (button == 0 && layout.closeButtonBounds().contains(mouseX, mouseY)) {
                closeWindow(window);
                return true;
            }
            if (button == 0 && layout.minimizeButtonBounds().contains(mouseX, mouseY)) {
                window.minimized = true;
                if (activeWindow == window) {
                    activeWindow = topMostVisibleWindow();
                }
                saveUiSettings();
                return true;
            }
            if (button == 0 && layout.maximizeButtonBounds().contains(mouseX, mouseY)) {
                toggleMaximize(window);
                saveUiSettings();
                return true;
            }
            if (button == 0 && !window.maximized && layout.resizeGripBounds().contains(mouseX, mouseY)) {
                resizingWindow = window;
                resizeStartMouseX = (int) mouseX;
                resizeStartMouseY = (int) mouseY;
                resizeStartWidth = window.width;
                resizeStartHeight = window.height;
                return true;
            }
            if (button == 0 && !window.maximized && layout.dragHandleBounds().contains(mouseX, mouseY)) {
                draggingWindow = window;
                dragOffsetX = (int) (mouseX - window.x);
                dragOffsetY = (int) (mouseY - window.y);
                return true;
            }
            if (window.settingsWindow) {
                if (window.settingsView == SettingsView.KEYBINDS) {
                    if (clickKeybindOptions(window, layout.modulesPane(), mouseX, mouseY, button)) {
                        return true;
                    }
                    if (clickKeybindActions(window, mouseX, mouseY, button)) {
                        return true;
                    }
                } else {
                    if (clickBackgroundOptions(window, layout.modulesPane(), mouseX, mouseY, button)) {
                        return true;
                    }
                    if (clickBackgroundActions(window, mouseX, mouseY, button)) {
                        return true;
                    }
                }
                return true;
            }
            if (clickModules(window, layout.modulesPane(), mouseX, mouseY, button)) {
                return true;
            }
            if (clickSettings(window, layout.settingsPane(), mouseX, mouseY, button)) {
                return true;
            }
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingWindow = null;
        resizingWindow = null;
        for (CategoryWindow window : windows) {
            window.draggingSlider = null;
            window.draggingSliderTrack = null;
        }
        saveUiSettings();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (draggingWindow != null) {
            draggingWindow.x = (int) mouseX - dragOffsetX;
            draggingWindow.y = (int) mouseY - dragOffsetY;
            clampWindowToDesktop(draggingWindow);
            return true;
        }

        if (resizingWindow != null) {
            int dx = (int) mouseX - resizeStartMouseX;
            int dy = (int) mouseY - resizeStartMouseY;
            resizingWindow.width = resizeStartWidth + dx;
            resizingWindow.height = resizeStartHeight + dy;
            clampWindowSizeAndPosition(resizingWindow);
            return true;
        }

        if (activeWindow != null && activeWindow.draggingSlider != null && activeWindow.draggingSliderTrack != null) {
            updateSliderFromMouse(activeWindow.draggingSlider, activeWindow.draggingSliderTrack, mouseX);
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (startMenu.isOpen() && startMenu.mouseScrolled(mouseX, mouseY, scrollY, moduleManager)) {
            return true;
        }

        for (int i = windows.size() - 1; i >= 0; i--) {
            CategoryWindow window = windows.get(i);
            if (window.minimized) {
                continue;
            }
            WindowLayout layout = buildWindowLayout(window);
            if (!layout.windowBounds().contains(mouseX, mouseY)) {
                continue;
            }

            if (layout.modulesPane().contains(mouseX, mouseY)) {
                int rows;
                if (window.settingsWindow) {
                    rows = window.settingsView == SettingsView.KEYBINDS ? keybindRows().size() : backgroundOptions.size();
                } else {
                    rows = modulesForCategory(window.category).size();
                }
                int max = Math.max(0, rows - maxRows(layout.modulesPane()));
                window.moduleScroll = Mth.clamp(window.moduleScroll - (int) Math.signum(scrollY), 0, max);
                return true;
            }
            if (layout.settingsPane().contains(mouseX, mouseY)) {
                if (window.settingsWindow) {
                    return true;
                }
                int extraRows = 0;
                if (window.selectedModule != null) {
                    extraRows = 1;
                    if (window.selectedModule instanceof InfoModule) {
                        extraRows++;
                    }
                }
                int settingRows = visibleSettings(window).size() + extraRows;
                int max = Math.max(0, settingRows - maxSettingRows(layout.settingsPane()));
                window.settingScroll = Mth.clamp(window.settingScroll - (int) Math.signum(scrollY), 0, max);
                return true;
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            if (startMenu.isOpen()) {
                startMenu.close();
                return true;
            }
            onClose();
            return true;
        }

        if (startMenu.isOpen()) {
            StartMenuComponent.StartMenuAction action = startMenu.keyPressed(event, moduleManager);
            if (applyStartMenuAction(action)) {
                return true;
            }
        }

        if (activeWindow != null
                && !activeWindow.settingsWindow
                && activeWindow.awaitingModuleKeybind
                && activeWindow.selectedModule != null) {
            if (event.key() == InputConstants.KEY_ESCAPE) {
                activeWindow.awaitingModuleKeybind = false;
                return true;
            }
            activeWindow.selectedModule.setKeybind(event.key());
            activeWindow.awaitingModuleKeybind = false;
            moduleManager.saveSettings();
            return true;
        }

        if (activeWindow != null
                && activeWindow.settingsWindow
                && activeWindow.settingsView == SettingsView.KEYBINDS
                && activeWindow.awaitingKeybind) {
            if (event.key() == InputConstants.KEY_ESCAPE) {
                activeWindow.awaitingKeybind = false;
                return true;
            }
            applyCapturedKeybind(activeWindow, event.key());
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (startMenu.isOpen() && startMenu.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        moduleManager.saveSettings();
        saveUiSettings();
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderCategoryWindow(GuiGraphicsExtractor graphics, CategoryWindow window, int mouseX, int mouseY) {
        WindowLayout layout = buildWindowLayout(window);
        String title = window.settingsWindow ? "Settings" : prettify(window.category.name()) + " Category";
        renderer.renderWindowFrame(
                graphics,
                layout.windowBounds().x(),
                layout.windowBounds().y(),
                layout.windowBounds().w(),
                layout.windowBounds().h(),
                title,
                font
        );

        if (window.settingsWindow) {
            renderSettingsToolbar(graphics, layout.contentPane(), window);
            if (window.settingsView == SettingsView.KEYBINDS) {
                renderKeybindOptions(graphics, layout.modulesPane(), window, mouseX, mouseY);
                renderKeybindDetails(graphics, layout.settingsPane(), window);
            } else {
                renderBackgroundOptions(graphics, layout.modulesPane(), window, mouseX, mouseY);
                renderBackgroundPreview(graphics, layout.settingsPane());
            }
        } else {
            renderToolbar(graphics, layout.contentPane(), window);
            renderModules(graphics, layout.modulesPane(), window, mouseX, mouseY);
            renderSettings(graphics, layout.settingsPane(), window, mouseX, mouseY);
        }
        renderResizeGrip(graphics, layout.resizeGripBounds());
    }

    private void renderToolbar(GuiGraphicsExtractor graphics, UiRect contentPane, CategoryWindow window) {
        graphics.fill(contentPane.x(), contentPane.y(), contentPane.right(), contentPane.y() + 18, 0xFFF8FAFF);
        graphics.horizontalLine(contentPane.x(), contentPane.right() - 1, contentPane.y(), 0xFFFFFFFF);
        graphics.horizontalLine(contentPane.x(), contentPane.right() - 1, contentPane.y() + 17, 0xFF8FA5C6);
        String label = "Category: " + prettify(window.category.name()) + "   Modules: " + modulesForCategory(window.category).size();
        graphics.text(font, label, contentPane.x() + 8, contentPane.y() + 5, 0xFF1D2B4F, false);
    }

    private void renderSettingsToolbar(GuiGraphicsExtractor graphics, UiRect contentPane, CategoryWindow window) {
        graphics.fill(contentPane.x(), contentPane.y(), contentPane.right(), contentPane.y() + 18, 0xFFF8FAFF);
        graphics.horizontalLine(contentPane.x(), contentPane.right() - 1, contentPane.y(), 0xFFFFFFFF);
        graphics.horizontalLine(contentPane.x(), contentPane.right() - 1, contentPane.y() + 17, 0xFF8FA5C6);
        String label = window.settingsView == SettingsView.KEYBINDS ? "Keybind Settings" : "Desktop Background";
        graphics.text(font, label, contentPane.x() + 8, contentPane.y() + 5, 0xFF1D2B4F, false);
    }

    private void renderBackgroundOptions(GuiGraphicsExtractor graphics, UiRect pane, CategoryWindow window, int mouseX, int mouseY) {
        renderer.renderPanel(graphics, pane.x(), pane.y(), pane.w(), pane.h());
        graphics.text(font, "Backgrounds", pane.x() + 8, pane.y() + 6, 0xFF0E2C67, false);

        int top = pane.y() + 22;
        int rows = maxRows(pane);
        int end = Math.min(backgroundOptions.size(), window.moduleScroll + rows);
        int rowY = top;
        for (int i = window.moduleScroll; i < end; i++) {
            String label = backgroundOptions.get(i).label();
            boolean hovered = mouseX >= pane.x() + 6 && mouseX <= pane.right() - 6 && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT - 2;
            boolean selected = i == selectedBackgroundIndex;
            renderer.renderListRow(
                    graphics,
                    font,
                    pane.x() + 6,
                    rowY,
                    pane.w() - 12,
                    ROW_HEIGHT - 2,
                    label,
                    selected || hovered,
                    selected
            );
            rowY += ROW_HEIGHT;
        }
    }

    private void renderBackgroundPreview(GuiGraphicsExtractor graphics, UiRect pane) {
        renderer.renderPanel(graphics, pane.x(), pane.y(), pane.w(), pane.h());
        graphics.text(font, "Preview", pane.x() + 8, pane.y() + 6, 0xFF0E2C67, false);
        graphics.text(font, "Selected: " + selectedBackground().label(), pane.x() + 10, pane.y() + 28, 0xFF4A5F80, false);
        graphics.text(font, "Applied to desktop immediately.", pane.x() + 10, pane.y() + 42, 0xFF4A5F80, false);
        UiRect button = new UiRect(pane.x() + 10, pane.bottom() - 24, 120, 16);
        UiRect keybindsButton = new UiRect(pane.x() + 138, pane.bottom() - 24, 120, 16);
        UiRect moduleChatButton = new UiRect(pane.x() + 266, pane.bottom() - 24, 130, 16);
        renderer.renderModeButton(graphics, font, button.x(), button.y(), button.w(), "Open Folder");
        renderer.renderModeButton(graphics, font, keybindsButton.x(), keybindsButton.y(), keybindsButton.w(), "Keybinds");
        renderer.renderModeButton(
                graphics,
                font,
                moduleChatButton.x(),
                moduleChatButton.y(),
                moduleChatButton.w(),
                "Module Chat: " + (XPHackClient.isModuleChatMessagesEnabled() ? "On" : "Off")
        );
        if (activeWindow != null && activeWindow.settingsWindow) {
            activeWindow.openFolderButtonBounds = button;
            activeWindow.keybindsButtonBounds = keybindsButton;
            activeWindow.moduleChatButtonBounds = moduleChatButton;
        }
    }

    private void renderKeybindOptions(GuiGraphicsExtractor graphics, UiRect pane, CategoryWindow window, int mouseX, int mouseY) {
        renderer.renderPanel(graphics, pane.x(), pane.y(), pane.w(), pane.h());
        graphics.text(font, "Bindings", pane.x() + 8, pane.y() + 6, 0xFF0E2C67, false);

        List<KeybindRow> rowsData = keybindRows();
        int top = pane.y() + 22;
        int rows = maxRows(pane);
        int end = Math.min(rowsData.size(), window.moduleScroll + rows);
        int rowY = top;
        for (int i = window.moduleScroll; i < end; i++) {
            KeybindRow row = rowsData.get(i);
            boolean hovered = mouseX >= pane.x() + 6 && mouseX <= pane.right() - 6 && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT - 2;
            boolean selected = row.guiToggle ? window.selectedGuiKeybind : row.module == window.selectedKeybindModule;
            String keyText = keybindName(row.guiToggle ? XPHackClient.getOpenGuiKeybind() : row.module.getKeybind());
            renderer.renderListRow(
                    graphics,
                    font,
                    pane.x() + 6,
                    rowY,
                    pane.w() - 12,
                    ROW_HEIGHT - 2,
                    row.label + " : " + keyText,
                    selected || hovered,
                    selected
            );
            rowY += ROW_HEIGHT;
        }
    }

    private void renderKeybindDetails(GuiGraphicsExtractor graphics, UiRect pane, CategoryWindow window) {
        renderer.renderPanel(graphics, pane.x(), pane.y(), pane.w(), pane.h());
        graphics.text(font, "Keybind Details", pane.x() + 8, pane.y() + 6, 0xFF0E2C67, false);

        KeybindRow selected = window.selectedGuiKeybind
                ? new KeybindRow("Open GUI", null, true)
                : (window.selectedKeybindModule == null ? null : new KeybindRow(window.selectedKeybindModule.getName(), window.selectedKeybindModule, false));
        if (selected == null) {
            graphics.text(font, "Select a module keybind from the list.", pane.x() + 10, pane.y() + 28, 0xFF4A5F80, false);
            return;
        }

        int keybind = selected.guiToggle ? XPHackClient.getOpenGuiKeybind() : selected.module.getKeybind();
        graphics.text(font, "Target: " + selected.label, pane.x() + 10, pane.y() + 28, 0xFF1D2B4F, false);
        graphics.text(font, "Current: " + keybindName(keybind), pane.x() + 10, pane.y() + 44, 0xFF1D2B4F, false);
        graphics.text(
                font,
                window.awaitingKeybind ? "Press any key... (Esc cancels)" : "Use Set Key to capture a new bind.",
                pane.x() + 10,
                pane.y() + 60,
                window.awaitingKeybind ? 0xFF214F8F : 0xFF4A5F80,
                false
        );

        UiRect setButton = new UiRect(pane.x() + 10, pane.bottom() - 24, 110, 16);
        UiRect clearButton = new UiRect(pane.x() + 128, pane.bottom() - 24, 110, 16);
        UiRect backgroundsButton = new UiRect(pane.x() + 246, pane.bottom() - 24, 120, 16);
        renderer.renderModeButton(graphics, font, setButton.x(), setButton.y(), setButton.w(), "Set Key");
        renderer.renderModeButton(graphics, font, clearButton.x(), clearButton.y(), clearButton.w(), "Clear");
        renderer.renderModeButton(graphics, font, backgroundsButton.x(), backgroundsButton.y(), backgroundsButton.w(), "Backgrounds");
        window.setKeybindButtonBounds = setButton;
        window.clearKeybindButtonBounds = clearButton;
        window.backgroundsButtonBounds = backgroundsButton;
    }

    private void renderModules(GuiGraphicsExtractor graphics, UiRect pane, CategoryWindow window, int mouseX, int mouseY) {
        renderer.renderPanel(graphics, pane.x(), pane.y(), pane.w(), pane.h());
        graphics.text(font, "Modules", pane.x() + 8, pane.y() + 6, 0xFF0E2C67, false);

        List<Module> modules = modulesForCategory(window.category);
        if (modules.isEmpty()) {
            graphics.text(font, "No modules in this category.", pane.x() + 10, pane.y() + 28, 0xFF4A5F80, false);
            return;
        }

        int top = pane.y() + 22;
        int rows = maxRows(pane);
        int end = Math.min(modules.size(), window.moduleScroll + rows);
        int rowY = top;
        for (int i = window.moduleScroll; i < end; i++) {
            Module module = modules.get(i);
            boolean hovered = mouseX >= pane.x() + 6 && mouseX <= pane.right() - 6 && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT - 2;
            boolean selected = module == window.selectedModule;
            renderer.renderListRow(
                    graphics,
                    font,
                    pane.x() + 6,
                    rowY,
                    pane.w() - 12,
                    ROW_HEIGHT - 2,
                    module.getName(),
                    selected || hovered,
                    module.isEnabled()
            );
            rowY += ROW_HEIGHT;
        }
    }

    private void renderSettings(GuiGraphicsExtractor graphics, UiRect pane, CategoryWindow window, int mouseX, int mouseY) {
        renderer.renderPanel(graphics, pane.x(), pane.y(), pane.w(), pane.h());
        String header = window.selectedModule == null ? "Module Settings" : window.selectedModule.getName() + " Settings";
        graphics.text(font, header, pane.x() + 8, pane.y() + 6, 0xFF0E2C67, false);

        if (window.selectedModule == null) {
            graphics.text(font, "Select a module to view settings.", pane.x() + 10, pane.y() + 28, 0xFF4A5F80, false);
            return;
        }

        List<Setting<?>> settings = visibleSettings(window);
        boolean infoSelected = window.selectedModule instanceof InfoModule;
        int specialRows = infoSelected ? 2 : 1;

        int top = pane.y() + 22;
        int rows = maxSettingRows(pane);
        int totalRows = settings.size() + specialRows;
        int end = Math.min(totalRows, window.settingScroll + rows);
        int rowY = top;
        for (int i = window.settingScroll; i < end; i++) {
            graphics.fill(pane.x() + 6, rowY, pane.right() - 6, rowY + ROW_HEIGHT - 2, (i % 2 == 0) ? 0xFFEFF3FA : 0xFFE7EDF8);
            if (i == 0) {
                renderer.renderSettingLabel(graphics, font, pane.x() + 10, rowY + 5, "Keybind");
                String keybindText = window.awaitingModuleKeybind ? "Press key..." : keybindName(window.selectedModule.getKeybind());
                renderer.renderModeButton(graphics, font, pane.right() - 126, rowY + 2, 116, keybindText);
            } else if (infoSelected && i == 1) {
                renderer.renderSettingLabel(graphics, font, pane.x() + 10, rowY + 5, "Reset Pos");
                renderer.renderModeButton(graphics, font, pane.right() - 126, rowY + 2, 116, "Reset");
            } else {
                Setting<?> setting = settings.get(i - specialRows);
                renderer.renderSettingLabel(graphics, font, pane.x() + 10, rowY + 5, setting.getName());

                if (setting instanceof BooleanSetting bool) {
                    renderer.renderCheckbox(graphics, pane.right() - 20, rowY + 3, bool.getValue());
                } else if (setting instanceof ModeSetting mode) {
                    renderer.renderModeButton(graphics, font, pane.right() - 126, rowY + 2, 116, mode.getValue());
                } else if (setting instanceof SliderSetting slider) {
                    double normalized = (slider.getValue() - slider.getMin()) / (slider.getMax() - slider.getMin());
                    int trackX = pane.right() - 136;
                    renderer.renderSlider(graphics, trackX, rowY + 3, 92, normalized);
                    graphics.text(font, formatSliderValue(slider.getValue(), slider.getStep()), pane.right() - 40, rowY + 5, 0xFF0E2C67, false);
                }
            }
            rowY += ROW_HEIGHT;
        }

        if (settings.isEmpty()) {
            String message = window.detailMessage == null ? "This module has no settings." : window.detailMessage;
            graphics.text(font, message, pane.x() + 10, pane.bottom() - 16, 0xFF4A5F80, false);
        }
    }

    private void renderResizeGrip(GuiGraphicsExtractor graphics, UiRect grip) {
        int c1 = 0xFFBFCBE0;
        int c2 = 0xFF7892BC;
        graphics.fill(grip.right() - 2, grip.y() + 6, grip.right() - 1, grip.bottom() - 1, c2);
        graphics.fill(grip.x() + 6, grip.bottom() - 2, grip.right() - 1, grip.bottom() - 1, c2);
        graphics.fill(grip.x() + 2, grip.bottom() - 4, grip.right() - 1, grip.bottom() - 3, c1);
        graphics.fill(grip.x() + 4, grip.bottom() - 6, grip.right() - 1, grip.bottom() - 5, c1);
    }

    private void renderTaskbarButtons(GuiGraphicsExtractor graphics, List<TaskbarButton> buttons) {
        for (TaskbarButton button : buttons) {
            boolean active = button.window() == activeWindow && !button.window().minimized;
            String label = button.window().settingsWindow ? "Settings" : prettify(button.window().category.name());
            renderer.renderTaskbarWindowButton(
                    graphics,
                    font,
                    button.bounds(),
                    label,
                    active,
                    button.window().minimized
            );
        }
    }

    private boolean clickBackgroundOptions(CategoryWindow window, UiRect pane, double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        int top = pane.y() + 22;
        int rows = maxRows(pane);
        int end = Math.min(backgroundOptions.size(), window.moduleScroll + rows);
        int rowY = top;
        for (int i = window.moduleScroll; i < end; i++) {
            if (mouseX >= pane.x() + 6 && mouseX <= pane.right() - 6 && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT - 2) {
                selectedBackgroundIndex = i;
                saveUiSettings();
                return true;
            }
            rowY += ROW_HEIGHT;
        }
        return false;
    }

    private boolean clickBackgroundActions(CategoryWindow window, double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (window.moduleChatButtonBounds != null && window.moduleChatButtonBounds.contains(mouseX, mouseY)) {
            XPHackClient.setModuleChatMessagesEnabled(!XPHackClient.isModuleChatMessagesEnabled());
            saveUiSettings();
            return true;
        }
        if (window.keybindsButtonBounds != null && window.keybindsButtonBounds.contains(mouseX, mouseY)) {
            window.settingsView = SettingsView.KEYBINDS;
            window.moduleScroll = 0;
            window.awaitingKeybind = false;
            return true;
        }
        if (window.openFolderButtonBounds == null || !window.openFolderButtonBounds.contains(mouseX, mouseY)) {
            return false;
        }
        openWallpaperFolder();
        reloadBackgroundOptions();
        window.moduleScroll = Mth.clamp(window.moduleScroll, 0, Math.max(0, backgroundOptions.size() - 1));
        saveUiSettings();
        return true;
    }

    private boolean clickKeybindOptions(CategoryWindow window, UiRect pane, double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        List<KeybindRow> rows = keybindRows();
        int top = pane.y() + 22;
        int visibleRows = maxRows(pane);
        int end = Math.min(rows.size(), window.moduleScroll + visibleRows);
        int rowY = top;
        for (int i = window.moduleScroll; i < end; i++) {
            if (mouseX >= pane.x() + 6 && mouseX <= pane.right() - 6 && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT - 2) {
                KeybindRow row = rows.get(i);
                window.selectedGuiKeybind = row.guiToggle;
                window.selectedKeybindModule = row.module;
                window.awaitingKeybind = false;
                return true;
            }
            rowY += ROW_HEIGHT;
        }
        return false;
    }

    private boolean clickKeybindActions(CategoryWindow window, double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (window.backgroundsButtonBounds != null && window.backgroundsButtonBounds.contains(mouseX, mouseY)) {
            window.settingsView = SettingsView.BACKGROUNDS;
            window.moduleScroll = 0;
            window.awaitingKeybind = false;
            return true;
        }
        if (window.setKeybindButtonBounds != null && window.setKeybindButtonBounds.contains(mouseX, mouseY)) {
            if (window.selectedGuiKeybind || window.selectedKeybindModule != null) {
                window.awaitingKeybind = true;
                return true;
            }
            return false;
        }
        if (window.clearKeybindButtonBounds != null && window.clearKeybindButtonBounds.contains(mouseX, mouseY)) {
            if (window.selectedGuiKeybind) {
                XPHackClient.setOpenGuiKeybind(-1);
                moduleManager.saveSettings();
                window.awaitingKeybind = false;
                return true;
            }
            if (window.selectedKeybindModule != null) {
                window.selectedKeybindModule.setKeybind(-1);
                moduleManager.saveSettings();
                window.awaitingKeybind = false;
                return true;
            }
        }
        return false;
    }

    private boolean clickModules(CategoryWindow window, UiRect pane, double mouseX, double mouseY, int button) {
        List<Module> modules = modulesForCategory(window.category);
        if (modules.isEmpty()) {
            return false;
        }

        int top = pane.y() + 22;
        int rows = maxRows(pane);
        int end = Math.min(modules.size(), window.moduleScroll + rows);
        int rowY = top;
        for (int i = window.moduleScroll; i < end; i++) {
            Module module = modules.get(i);
            if (mouseX >= pane.x() + 6 && mouseX <= pane.right() - 6 && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT - 2) {
                window.selectedModule = module;
                window.settingScroll = 0;
                window.detailMessage = null;
                window.awaitingModuleKeybind = false;
                if (button == 0) {
                    module.toggle();
                    XPHackClient.notifyModuleToggle(module);
                    moduleManager.saveSettings();
                } else if (button == 1 && module.getSettings().isEmpty()) {
                    window.detailMessage = "This module has no settings.";
                }
                return true;
            }
            rowY += ROW_HEIGHT;
        }
        return false;
    }

    private boolean clickSettings(CategoryWindow window, UiRect pane, double mouseX, double mouseY, int button) {
        if (window.selectedModule == null) {
            return false;
        }
        List<Setting<?>> settings = visibleSettings(window);
        boolean infoSelected = window.selectedModule instanceof InfoModule;
        int specialRows = infoSelected ? 2 : 1;

        int top = pane.y() + 22;
        int rows = maxSettingRows(pane);
        int totalRows = settings.size() + specialRows;
        int end = Math.min(totalRows, window.settingScroll + rows);
        int rowY = top;
        for (int i = window.settingScroll; i < end; i++) {
            if (mouseY < rowY || mouseY > rowY + ROW_HEIGHT - 2 || mouseX < pane.x() + 6 || mouseX > pane.right() - 6) {
                rowY += ROW_HEIGHT;
                continue;
            }

            if (i == 0) {
                if (mouseX >= pane.right() - 126 && mouseX <= pane.right() - 10) {
                    if (button == 1) {
                        window.selectedModule.setKeybind(-1);
                        window.awaitingModuleKeybind = false;
                        moduleManager.saveSettings();
                    } else {
                        window.awaitingModuleKeybind = true;
                    }
                    return true;
                }
            } else if (infoSelected && i == 1) {
                if (mouseX >= pane.right() - 126 && mouseX <= pane.right() - 10) {
                    InfoModule info = (InfoModule) window.selectedModule;
                    info.resetLinePositions();
                    moduleManager.saveSettings();
                    return true;
                }
            } else {
                Setting<?> setting = settings.get(i - specialRows);
                if (setting instanceof BooleanSetting bool) {
                    if (mouseX >= pane.right() - 20 && mouseX <= pane.right() - 8) {
                        bool.toggle();
                        moduleManager.saveSettings();
                        return true;
                    }
                } else if (setting instanceof ModeSetting mode) {
                    if (mouseX >= pane.right() - 126 && mouseX <= pane.right() - 10) {
                        if (button == 1) {
                            cycleModeBackward(mode);
                        } else {
                            mode.next();
                        }
                        moduleManager.saveSettings();
                        return true;
                    }
                } else if (setting instanceof SliderSetting slider) {
                    UiRect track = new UiRect(pane.right() - 136, rowY + 3, 92, 12);
                    if (track.contains(mouseX, mouseY)) {
                        window.draggingSlider = slider;
                        window.draggingSliderTrack = track;
                        updateSliderFromMouse(slider, track, mouseX);
                        return true;
                    }
                }
            }
            rowY += ROW_HEIGHT;
        }
        return false;
    }

    private boolean applyStartMenuAction(StartMenuComponent.StartMenuAction action) {
        if (action == null || !action.consumed()) {
            return false;
        }

        if (action.type() == StartMenuComponent.ActionType.OPEN_SETTINGS_WINDOW) {
            openOrFocusSettingsWindow();
        } else if (action.type() == StartMenuComponent.ActionType.OPEN_CATEGORY_GUI && action.category() != null) {
            openOrFocusWindow(action.category());
        } else if (action.type() == StartMenuComponent.ActionType.TOGGLE_MODULE && action.module() != null) {
            action.module().toggle();
            XPHackClient.notifyModuleToggle(action.module());
            moduleManager.saveSettings();
        } else if (action.type() == StartMenuComponent.ActionType.OPEN_MODULE_DETAILS && action.module() != null) {
            CategoryWindow window = openOrFocusWindow(action.module().getCategory());
            window.selectedModule = action.module();
            window.settingScroll = 0;
            window.detailMessage = action.module().getSettings().isEmpty() ? "This module has no settings." : null;
        } else if (action.type() == StartMenuComponent.ActionType.CLOSE_GUI) {
            onClose();
            return true;
        }

        if (action.closeMenu()) {
            startMenu.close();
        }
        saveUiSettings();
        return true;
    }

    private CategoryWindow openOrFocusWindow(Category category) {
        for (CategoryWindow window : windows) {
            if (!window.settingsWindow && window.category == category) {
                window.minimized = false;
                bringToFront(window);
                ensureSelection(window);
                return window;
            }
        }

        UiRect work = desktopWorkArea();
        CategoryWindow created = new CategoryWindow(category);
        created.width = Math.min(DEFAULT_WINDOW_WIDTH, work.w() - 10);
        created.height = Math.min(DEFAULT_WINDOW_HEIGHT, work.h() - 10);
        created.x = work.x() + 24 + windows.size() * 18;
        created.y = work.y() + 20 + windows.size() * 14;
        clampWindowToDesktop(created);
        ensureSelection(created);
        windows.add(created);
        bringToFront(created);
        saveUiSettings();
        return created;
    }

    private CategoryWindow openOrFocusSettingsWindow() {
        for (CategoryWindow window : windows) {
            if (window.settingsWindow) {
                window.minimized = false;
                bringToFront(window);
                reloadBackgroundOptions();
                window.awaitingKeybind = false;
                return window;
            }
        }

        UiRect work = desktopWorkArea();
        CategoryWindow created = new CategoryWindow(Category.UTILITY);
        created.settingsWindow = true;
        created.settingsView = SettingsView.BACKGROUNDS;
        created.selectedGuiKeybind = true;
        created.width = Math.min(460, work.w() - 10);
        created.height = Math.min(300, work.h() - 10);
        created.x = work.x() + 38 + windows.size() * 14;
        created.y = work.y() + 30 + windows.size() * 10;
        clampWindowToDesktop(created);
        windows.add(created);
        bringToFront(created);
        reloadBackgroundOptions();
        saveUiSettings();
        return created;
    }

    private void bringToFront(CategoryWindow window) {
        windows.remove(window);
        windows.add(window);
        activeWindow = window;
    }

    private void closeWindow(CategoryWindow window) {
        windows.remove(window);
        if (activeWindow == window) {
            activeWindow = topMostVisibleWindow();
        }
        saveUiSettings();
    }

    private void toggleTaskbarWindow(CategoryWindow window) {
        if (window.minimized) {
            window.minimized = false;
            bringToFront(window);
            saveUiSettings();
            return;
        }
        if (activeWindow == window) {
            window.minimized = true;
            activeWindow = topMostVisibleWindow();
            saveUiSettings();
            return;
        }
        bringToFront(window);
        saveUiSettings();
    }

    private CategoryWindow topMostVisibleWindow() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            CategoryWindow window = windows.get(i);
            if (!window.minimized) {
                return window;
            }
        }
        return null;
    }

    private void ensureSelection(CategoryWindow window) {
        if (window.settingsWindow) {
            return;
        }
        List<Module> modules = modulesForCategory(window.category);
        if (modules.isEmpty()) {
            window.selectedModule = null;
            return;
        }
        if (window.selectedModule == null || window.selectedModule.getCategory() != window.category) {
            window.selectedModule = modules.getFirst();
        }
    }

    private List<Module> modulesForCategory(Category category) {
        List<Module> modules = new ArrayList<>();
        for (Module module : moduleManager.getModules()) {
            if (module.getCategory() == category) {
                modules.add(module);
            }
        }
        modules.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
        return modules;
    }

    private List<Setting<?>> visibleSettings(CategoryWindow window) {
        return window.selectedModule == null ? List.of() : window.selectedModule.getSettings();
    }

    private WindowLayout buildWindowLayout(CategoryWindow window) {
        UiRect bounds = new UiRect(window.x, window.y, window.width, window.height);
        UiRect contentPane = new UiRect(bounds.x() + 8, bounds.y() + 29, bounds.w() - 16, bounds.h() - 37);
        int modulesHeight = Math.max(110, Math.min(160, contentPane.h() / 2));
        UiRect modulesPane = new UiRect(contentPane.x() + 6, contentPane.y() + 22, contentPane.w() - 12, modulesHeight);
        UiRect settingsPane = new UiRect(contentPane.x() + 6, modulesPane.bottom() + 8, contentPane.w() - 12, contentPane.bottom() - modulesPane.bottom() - 12);
        UiRect minButton = new UiRect(bounds.right() - 56, bounds.y() + 5, 15, 14);
        UiRect maxButton = new UiRect(bounds.right() - 38, bounds.y() + 5, 15, 14);
        UiRect closeButton = new UiRect(bounds.right() - 20, bounds.y() + 5, 15, 14);
        UiRect dragHandle = new UiRect(bounds.x() + 4, bounds.y() + 4, Math.max(10, bounds.w() - 64), 18);
        UiRect resizeGrip = new UiRect(bounds.right() - RESIZE_GRIP_SIZE, bounds.bottom() - RESIZE_GRIP_SIZE, RESIZE_GRIP_SIZE, RESIZE_GRIP_SIZE);
        return new WindowLayout(bounds, contentPane, modulesPane, settingsPane, minButton, maxButton, closeButton, dragHandle, resizeGrip);
    }

    private DesktopLayout buildDesktopLayout() {
        UiRect startButtonBounds = startButtonBounds(height);
        UiRect trayBounds = new UiRect(width - 108, height - TASKBAR_HEIGHT + 3, 102, TASKBAR_HEIGHT - 6);

        int left = startButtonBounds.right() + 6;
        int right = trayBounds.x() - 6;
        int available = Math.max(0, right - left);
        int count = Math.max(1, windows.size());
        int slotWidth = Mth.clamp(available / count, 96, 176);

        List<TaskbarButton> buttons = new ArrayList<>();
        int x = left;
        for (CategoryWindow window : windows) {
            if (x + slotWidth > right) {
                break;
            }
            buttons.add(new TaskbarButton(window, new UiRect(x, height - TASKBAR_HEIGHT + 3, slotWidth - 4, TASKBAR_HEIGHT - 6)));
            x += slotWidth;
        }
        return new DesktopLayout(startButtonBounds, trayBounds, buttons);
    }

    private void clampWindowToDesktop(CategoryWindow window) {
        clampWindowSizeAndPosition(window);
        UiRect work = desktopWorkArea();
        int maxX = work.right() - window.width;
        int maxY = work.bottom() - window.height;
        window.x = Mth.clamp(window.x, work.x(), Math.max(work.x(), maxX));
        window.y = Mth.clamp(window.y, work.y(), Math.max(work.y(), maxY));
    }

    private void toggleMaximize(CategoryWindow window) {
        UiRect work = desktopWorkArea();
        if (window.maximized) {
            window.maximized = false;
            window.x = window.restoreX;
            window.y = window.restoreY;
            window.width = window.restoreWidth;
            window.height = window.restoreHeight;
            clampWindowToDesktop(window);
            return;
        }

        window.restoreX = window.x;
        window.restoreY = window.y;
        window.restoreWidth = window.width;
        window.restoreHeight = window.height;
        window.maximized = true;
        window.x = work.x();
        window.y = work.y();
        window.width = work.w();
        window.height = work.h();
    }

    private void clampWindowSizeAndPosition(CategoryWindow window) {
        UiRect work = desktopWorkArea();
        window.width = Mth.clamp(window.width, MIN_WINDOW_WIDTH, work.w());
        window.height = Mth.clamp(window.height, MIN_WINDOW_HEIGHT, work.h());
    }

    private UiRect desktopWorkArea() {
        return new UiRect(4, 4, Math.max(300, width - 8), Math.max(180, height - TASKBAR_HEIGHT - 8));
    }

    private UiRect startButtonBounds(int screenHeight) {
        int buttonWidth = 98;
        int buttonHeight = 22;
        int x = 4;
        int y = screenHeight - TASKBAR_HEIGHT + (TASKBAR_HEIGHT - buttonHeight) / 2;
        return new UiRect(x, y, buttonWidth, buttonHeight);
    }

    private int maxRows(UiRect pane) {
        return Math.max(1, (pane.h() - 24) / ROW_HEIGHT);
    }

    private int maxSettingRows(UiRect pane) {
        return Math.max(1, (pane.h() - 24) / ROW_HEIGHT);
    }

    private void updateSliderFromMouse(SliderSetting slider, UiRect track, double mouseX) {
        double t = Mth.clamp((mouseX - (track.x() + 3)) / (track.w() - 6.0D), 0.0D, 1.0D);
        double value = slider.getMin() + t * (slider.getMax() - slider.getMin());
        double snapped = Math.round(value / slider.getStep()) * slider.getStep();
        slider.setValue(snapped);
        moduleManager.saveSettings();
    }

    private void cycleModeBackward(ModeSetting mode) {
        List<String> modes = mode.getModes();
        int index = modes.indexOf(mode.getValue());
        int previous = index <= 0 ? modes.size() - 1 : index - 1;
        mode.setValue(modes.get(previous));
    }

    private String prettify(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT).replace('_', ' ');
        if (lower.isEmpty()) {
            return raw;
        }
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String formatSliderValue(double value, double step) {
        if (step >= 1.0D) {
            return Integer.toString((int) Math.round(value));
        }
        if (step >= 0.1D) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        if (step >= 0.01D) {
            return String.format(Locale.ROOT, "%.2f", value);
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private String currentClockText() {
        return LocalTime.now().format(CLOCK_FORMAT);
    }

    private void applyCapturedKeybind(CategoryWindow window, int keyCode) {
        if (window.selectedGuiKeybind) {
            XPHackClient.setOpenGuiKeybind(keyCode);
        } else if (window.selectedKeybindModule != null) {
            window.selectedKeybindModule.setKeybind(keyCode);
        }
        window.awaitingKeybind = false;
        moduleManager.saveSettings();
    }

    private List<KeybindRow> keybindRows() {
        List<KeybindRow> rows = new ArrayList<>();
        rows.add(new KeybindRow("Open GUI", null, true));

        List<Module> modules = new ArrayList<>(moduleManager.getModules());
        modules.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
        for (Module module : modules) {
            rows.add(new KeybindRow(module.getName(), module, false));
        }
        return rows;
    }

    private String keybindName(int keybind) {
        if (keybind < 0) {
            return "None";
        }
        String named = GLFW.glfwGetKeyName(keybind, 0);
        if (named != null && !named.isBlank()) {
            return named.toUpperCase(Locale.ROOT);
        }
        return switch (keybind) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "Left Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "Right Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "Left Ctrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "Right Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT -> "Left Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "Right Alt";
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_ENTER -> "Enter";
            case GLFW.GLFW_KEY_ESCAPE -> "Escape";
            case GLFW.GLFW_KEY_BACKSPACE -> "Backspace";
            case GLFW.GLFW_KEY_UP -> "Up";
            case GLFW.GLFW_KEY_DOWN -> "Down";
            case GLFW.GLFW_KEY_LEFT -> "Left";
            case GLFW.GLFW_KEY_RIGHT -> "Right";
            default -> "Key " + keybind;
        };
    }

    private String currentPlayerName() {
        if (minecraft != null && minecraft.player != null) {
            return minecraft.player.getName().getString();
        }
        return "Player";
    }

    private Identifier currentPlayerSkin() {
        if (minecraft == null || minecraft.player == null) {
            return null;
        }

        Identifier fromPlayerSkin = invokeTextureGetter(minecraft.player, "getSkin");
        if (fromPlayerSkin != null) {
            return fromPlayerSkin;
        }
        Identifier direct = invokeIdentifierGetter(minecraft.player, "getSkinTextureLocation");
        if (direct != null) {
            return direct;
        }
        try {
            Object skin = minecraft.player.getSkin();
            Method bodyMethod = skin.getClass().getMethod("body");
            Object body = bodyMethod.invoke(skin);
            Method idMethod = body.getClass().getMethod("id");
            Object id = idMethod.invoke(body);
            if (id instanceof Identifier identifier) {
                return identifier;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private Identifier invokeTextureGetter(Object target, String getterName) {
        try {
            Method getter = target.getClass().getMethod(getterName);
            Object skinObject = getter.invoke(target);
            if (skinObject == null) {
                return null;
            }
            Method textureMethod = skinObject.getClass().getMethod("texture");
            Object texture = textureMethod.invoke(skinObject);
            if (texture instanceof Identifier identifier) {
                return identifier;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private Identifier invokeIdentifierGetter(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            if (value instanceof Identifier identifier) {
                return identifier;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private BackgroundOption selectedBackground() {
        if (backgroundOptions.isEmpty()) {
            return new BackgroundOption("Default", BackgroundType.DEFAULT, null, null);
        }
        int index = Mth.clamp(selectedBackgroundIndex, 0, backgroundOptions.size() - 1);
        return backgroundOptions.get(index);
    }

    private void ensureWallpaperFolder() {
        if (wallpapersDirectory == null) {
            return;
        }
        try {
            Files.createDirectories(wallpapersDirectory);
        } catch (IOException ignored) {
        }
    }

    private void reloadBackgroundOptions() {
        ensureWallpaperFolder();
        String previouslySelected = selectedBackground().label();

        backgroundOptions.clear();
        backgroundOptions.add(new BackgroundOption("Default", BackgroundType.DEFAULT, null, null));
        backgroundOptions.add(new BackgroundOption("None", BackgroundType.NONE, null, null));

        if (wallpapersDirectory != null) {
            try (Stream<Path> files = Files.list(wallpapersDirectory)) {
                files.filter(Files::isRegularFile)
                        .filter(this::isWallpaperFile)
                        .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .forEach(path -> {
                            Identifier texture = loadWallpaperTexture(path);
                            String label = path.getFileName().toString();
                            backgroundOptions.add(new BackgroundOption(label, BackgroundType.FILE, path, texture));
                        });
            } catch (IOException ignored) {
            }
        }

        selectedBackgroundIndex = 0;
        for (int i = 0; i < backgroundOptions.size(); i++) {
            if (backgroundOptions.get(i).label().equalsIgnoreCase(previouslySelected)) {
                selectedBackgroundIndex = i;
                break;
            }
        }
    }

    private void loadUiSettings() {
        if (uiSettingsPath == null) {
            return;
        }

        UiSettingsStore.UiSettings saved = UiSettingsStore.load(uiSettingsPath);
        XPHackClient.setModuleChatMessagesEnabled(saved.moduleChatMessages());
        for (int i = 0; i < backgroundOptions.size(); i++) {
            if (backgroundOptions.get(i).label().equalsIgnoreCase(saved.backgroundLabel())) {
                selectedBackgroundIndex = i;
                break;
            }
        }

        windows.clear();
        activeWindow = null;
        List<Map.Entry<String, UiSettingsStore.WindowState>> entries = new ArrayList<>(saved.windows().entrySet());
        entries.sort(Comparator.comparingInt(entry -> entry.getValue().order()));
        for (Map.Entry<String, UiSettingsStore.WindowState> entry : entries) {
            UiSettingsStore.WindowState state = entry.getValue();
            if (!state.open()) {
                continue;
            }

            CategoryWindow window = null;
            if ("SETTINGS".equalsIgnoreCase(entry.getKey())) {
                window = new CategoryWindow(Category.UTILITY);
                window.settingsWindow = true;
            } else {
                try {
                    Category category = Category.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
                    window = new CategoryWindow(category);
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (window == null) {
                continue;
            }

            window.x = state.x();
            window.y = state.y();
            window.width = state.width();
            window.height = state.height();
            window.minimized = state.minimized();
            clampWindowToDesktop(window);
            ensureSelection(window);
            windows.add(window);
            if (!window.minimized) {
                activeWindow = window;
            }
        }
    }

    private void saveUiSettings() {
        if (uiSettingsPath == null) {
            return;
        }

        Map<String, UiSettingsStore.WindowState> states = new java.util.LinkedHashMap<>();
        for (Category category : Category.values()) {
            CategoryWindow window = findCategoryWindow(category);
            states.put(
                    category.name(),
                    new UiSettingsStore.WindowState(
                            window != null,
                            window != null && window.minimized,
                            window != null ? window.x : 40,
                            window != null ? window.y : 40,
                            window != null ? window.width : DEFAULT_WINDOW_WIDTH,
                            window != null ? window.height : DEFAULT_WINDOW_HEIGHT,
                            window != null ? windows.indexOf(window) : 999
                    )
            );
        }

        CategoryWindow settingsWindow = findSettingsWindow();
        states.put(
                "SETTINGS",
                new UiSettingsStore.WindowState(
                        settingsWindow != null,
                        settingsWindow != null && settingsWindow.minimized,
                        settingsWindow != null ? settingsWindow.x : 40,
                        settingsWindow != null ? settingsWindow.y : 40,
                        settingsWindow != null ? settingsWindow.width : 460,
                        settingsWindow != null ? settingsWindow.height : 300,
                        settingsWindow != null ? windows.indexOf(settingsWindow) : 999
                )
        );

        UiSettingsStore.save(
                uiSettingsPath,
                new UiSettingsStore.UiSettings(
                        selectedBackground().label(),
                        XPHackClient.isModuleChatMessagesEnabled(),
                        states
                )
        );
    }

    private CategoryWindow findCategoryWindow(Category category) {
        for (CategoryWindow window : windows) {
            if (!window.settingsWindow && window.category == category) {
                return window;
            }
        }
        return null;
    }

    private CategoryWindow findSettingsWindow() {
        for (CategoryWindow window : windows) {
            if (window.settingsWindow) {
                return window;
            }
        }
        return null;
    }

    private boolean isWallpaperFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    private Identifier loadWallpaperTexture(Path path) {
        Identifier existing = wallpaperTextures.get(path);
        if (existing != null) {
            return existing;
        }

        if (minecraft == null) {
            return null;
        }

        try (InputStream stream = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(stream);
            DynamicTexture texture = new DynamicTexture(() -> "Wallpaper " + path.getFileName(), image);
            Identifier id = Identifier.parse("xphack:wallpaper/" + Math.abs(path.toString().hashCode()));
            minecraft.getTextureManager().register(id, texture);
            wallpaperTextures.put(path, id);
            return id;
        } catch (IOException ignored) {
            return null;
        }
    }

    private void openWallpaperFolder() {
        if (wallpapersDirectory == null) {
            return;
        }
        ensureWallpaperFolder();
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("win")) {
                new ProcessBuilder("explorer.exe", wallpapersDirectory.toString()).start();
                return;
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(wallpapersDirectory.toFile());
            }
        } catch (IOException ignored) {
        }
    }

    private static final class CategoryWindow {
        private final Category category;
        private int x;
        private int y;
        private int width = DEFAULT_WINDOW_WIDTH;
        private int height = DEFAULT_WINDOW_HEIGHT;
        private boolean minimized;
        private boolean settingsWindow;
        private boolean maximized;
        private int moduleScroll;
        private int settingScroll;
        private Module selectedModule;
        private SliderSetting draggingSlider;
        private UiRect draggingSliderTrack;
        private UiRect openFolderButtonBounds;
        private UiRect keybindsButtonBounds;
        private UiRect moduleChatButtonBounds;
        private UiRect backgroundsButtonBounds;
        private UiRect setKeybindButtonBounds;
        private UiRect clearKeybindButtonBounds;
        private String detailMessage;
        private SettingsView settingsView = SettingsView.BACKGROUNDS;
        private Module selectedKeybindModule;
        private boolean selectedGuiKeybind = true;
        private boolean awaitingKeybind;
        private boolean awaitingModuleKeybind;
        private int restoreX;
        private int restoreY;
        private int restoreWidth = DEFAULT_WINDOW_WIDTH;
        private int restoreHeight = DEFAULT_WINDOW_HEIGHT;

        private CategoryWindow(Category category) {
            this.category = category;
        }
    }

    private record WindowLayout(
            UiRect windowBounds,
            UiRect contentPane,
            UiRect modulesPane,
            UiRect settingsPane,
            UiRect minimizeButtonBounds,
            UiRect maximizeButtonBounds,
            UiRect closeButtonBounds,
            UiRect dragHandleBounds,
            UiRect resizeGripBounds
    ) {
    }

    private record TaskbarButton(CategoryWindow window, UiRect bounds) {
    }

    private record DesktopLayout(UiRect startButtonBounds, UiRect trayBounds, List<TaskbarButton> taskButtons) {
    }

    private enum BackgroundType {
        DEFAULT,
        NONE,
        FILE
    }

    private enum SettingsView {
        BACKGROUNDS,
        KEYBINDS
    }

    private record KeybindRow(String label, Module module, boolean guiToggle) {
    }

    private record BackgroundOption(String label, BackgroundType type, Path file, Identifier textureId) {
    }
}
