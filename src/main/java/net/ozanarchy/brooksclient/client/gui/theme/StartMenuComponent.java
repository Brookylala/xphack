package net.ozanarchy.brooksclient.client.gui.theme;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;
import net.ozanarchy.brooksclient.client.module.ModuleManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class StartMenuComponent {
    private static final int MENU_WIDTH = 438;
    private static final int MENU_HEIGHT = 358;
    private static final int HEADER_HEIGHT = 40;
    private static final int ACTION_HEIGHT = 36;
    private static final int LEFT_COLUMN_WIDTH = 248;
    private static final int KEY_ENTER = 257;
    private static final int KEY_NUMPAD_ENTER = 335;

    private final SearchBoxComponent searchBox = new SearchBoxComponent();
    private final StartMenuHeaderComponent headerComponent = new StartMenuHeaderComponent();
    private final StartMenuModuleListComponent moduleListComponent = new StartMenuModuleListComponent();
    private final StartMenuCategoryListComponent categoryListComponent = new StartMenuCategoryListComponent();

    private boolean open;
    private Category activeCategory = Category.MOVEMENT;
    private Category hoveredCategory;
    private UiRect menuBounds = new UiRect(0, 0, 0, 0);
    private UiRect bodyBounds = new UiRect(0, 0, 0, 0);
    private UiRect actionBounds = new UiRect(0, 0, 0, 0);
    private UiRect settingsButtonBounds = new UiRect(0, 0, 0, 0);
    private UiRect closeButtonBounds = new UiRect(0, 0, 0, 0);

    public boolean isOpen() {
        return open;
    }

    public void toggle(int screenWidth, int screenHeight, int taskbarHeight, UiRect startButtonBounds, Category currentCategory) {
        if (open) {
            close();
            return;
        }
        open(screenWidth, screenHeight, taskbarHeight, startButtonBounds, currentCategory);
    }

    public void open(int screenWidth, int screenHeight, int taskbarHeight, UiRect startButtonBounds, Category currentCategory) {
        open = true;
        activeCategory = currentCategory;
        hoveredCategory = null;
        moduleListComponent.resetScroll();
        searchBox.setFocused(false);

        int x = Math.max(4, Math.min(startButtonBounds.x() - 3, screenWidth - MENU_WIDTH - 4));
        int y = Math.max(4, screenHeight - taskbarHeight - MENU_HEIGHT + 1);
        menuBounds = new UiRect(x, y, MENU_WIDTH, MENU_HEIGHT);
        rebuildLayout();
    }

    public void close() {
        open = false;
        searchBox.setFocused(false);
        hoveredCategory = null;
    }

    public boolean contains(double mouseX, double mouseY) {
        return open && menuBounds.contains(mouseX, mouseY);
    }

    public void render(
            GuiGraphicsExtractor graphics,
            XpShellRenderer renderer,
            Font font,
            ModuleManager moduleManager,
            int mouseX,
            int mouseY,
            Category currentCategory,
            String playerName,
            Identifier playerSkin
    ) {
        if (!open) {
            return;
        }

        activeCategory = currentCategory;
        hoveredCategory = categoryListComponent.categoryAt(mouseX, mouseY);

        List<Module> visibleModules = visibleModules(moduleManager);
        moduleListComponent.clampScroll(visibleModules.size());

        renderer.renderStartMenuShell(graphics, menuBounds, bodyBounds, actionBounds);
        headerComponent.render(graphics, renderer, font, playerName, playerSkin);
        searchBox.render(graphics, renderer, font);
        moduleListComponent.render(graphics, renderer, font, visibleModules, mouseX, mouseY);
        categoryListComponent.render(graphics, renderer, font, activeCategory, hoveredCategory);
        boolean settingsHovered = settingsButtonBounds.contains(mouseX, mouseY);
        boolean closeHovered = closeButtonBounds.contains(mouseX, mouseY);
        renderer.renderStartMenuActionStrip(
                graphics,
                font,
                actionBounds,
                settingsButtonBounds,
                closeButtonBounds,
                settingsHovered,
                closeHovered
        );
    }

    public StartMenuAction mouseClicked(double mouseX, double mouseY, int button, ModuleManager moduleManager) {
        if (!open) {
            return StartMenuAction.none();
        }

        if (button == 0 && settingsButtonBounds.contains(mouseX, mouseY)) {
            return StartMenuAction.openSettings(true);
        }
        if (button == 0 && closeButtonBounds.contains(mouseX, mouseY)) {
            return StartMenuAction.closeGui(true);
        }

        if (searchBox.mouseClicked(mouseX, mouseY)) {
            return StartMenuAction.consumedAction();
        }

        if (button != 0 && button != 1) {
            return menuBounds.contains(mouseX, mouseY) ? StartMenuAction.consumedAction() : StartMenuAction.none();
        }

        Category clickedCategory = categoryListComponent.categoryAt(mouseX, mouseY);
        if (clickedCategory != null && button == 0) {
            return StartMenuAction.openCategory(clickedCategory, true);
        }

        List<Module> modules = visibleModules(moduleManager);
        Module clickedModule = moduleListComponent.moduleAt(mouseX, mouseY, modules);
        if (clickedModule != null) {
            if (button == 1) {
                return StartMenuAction.openModuleDetails(clickedModule, true);
            }
            return StartMenuAction.toggleModule(clickedModule, false);
        }

        if (menuBounds.contains(mouseX, mouseY)) {
            return StartMenuAction.consumedAction();
        }
        return StartMenuAction.none();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount, ModuleManager moduleManager) {
        if (!open) {
            return false;
        }
        return moduleListComponent.scroll(mouseX, mouseY, amount, visibleModules(moduleManager).size());
    }

    public StartMenuAction keyPressed(KeyEvent event, ModuleManager moduleManager) {
        if (!open) {
            return StartMenuAction.none();
        }

        if (searchBox.keyPressed(event)) {
            moduleListComponent.resetScroll();
            return StartMenuAction.consumedAction();
        }

        if ((event.key() == KEY_ENTER || event.key() == KEY_NUMPAD_ENTER) && searchBox.isFocused()) {
            List<Module> modules = visibleModules(moduleManager);
            if (!modules.isEmpty()) {
                return StartMenuAction.toggleModule(modules.getFirst(), false);
            }
            return StartMenuAction.consumedAction();
        }

        if (event.key() == InputConstants.KEY_DOWN || event.key() == InputConstants.KEY_UP) {
            return StartMenuAction.consumedAction();
        }

        return StartMenuAction.none();
    }

    public boolean charTyped(CharacterEvent event) {
        if (!open) {
            return false;
        }
        char typed = extractTypedCharacter(event);
        if (typed == 0) {
            return false;
        }
        boolean changed = searchBox.charTyped(typed);
        if (changed) {
            moduleListComponent.resetScroll();
        }
        return changed;
    }

    private void rebuildLayout() {
        UiRect outer = menuBounds.inset(2);
        UiRect header = new UiRect(outer.x(), outer.y(), outer.w(), HEADER_HEIGHT);
        UiRect action = new UiRect(outer.x(), outer.bottom() - ACTION_HEIGHT, outer.w(), ACTION_HEIGHT);
        UiRect body = new UiRect(outer.x(), header.bottom(), outer.w(), action.y() - header.bottom());

        UiRect leftColumn = new UiRect(body.x() + 6, body.y() + 6, LEFT_COLUMN_WIDTH, body.h() - 12);
        UiRect rightColumn = new UiRect(leftColumn.right() + 6, body.y() + 6, body.right() - leftColumn.right() - 12, body.h() - 12);
        UiRect searchBounds = new UiRect(leftColumn.x() + 4, leftColumn.y() + 4, leftColumn.w() - 8, 18);
        UiRect moduleListBounds = new UiRect(leftColumn.x() + 4, leftColumn.y() + 26, leftColumn.w() - 8, leftColumn.h() - 30);

        headerComponent.setBounds(header);
        searchBox.setBounds(searchBounds);
        moduleListComponent.setBounds(moduleListBounds);
        categoryListComponent.setBounds(rightColumn);

        bodyBounds = body;
        actionBounds = action;
        settingsButtonBounds = new UiRect(action.right() - 154, action.y() + 8, 68, 20);
        closeButtonBounds = new UiRect(action.right() - 78, action.y() + 8, 68, 20);
    }

    private List<Module> visibleModules(ModuleManager moduleManager) {
        String query = searchBox.text().trim().toLowerCase(Locale.ROOT);
        List<Module> modules = new ArrayList<>();

        if (!query.isEmpty()) {
            for (Module module : moduleManager.getModules()) {
                if (module.getName().toLowerCase(Locale.ROOT).contains(query)) {
                    modules.add(module);
                }
            }
        } else if (hoveredCategory != null) {
            for (Module module : moduleManager.getModules()) {
                if (module.getCategory() == hoveredCategory) {
                    modules.add(module);
                }
            }
        } else {
            modules.addAll(moduleManager.getModules());
        }

        modules.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
        return modules;
    }

    private char extractTypedCharacter(CharacterEvent event) {
        try {
            for (Method method : event.getClass().getMethods()) {
                if (method.getParameterCount() != 0) {
                    continue;
                }
                if (method.getReturnType() == char.class) {
                    return (char) method.invoke(event);
                }
                if (method.getReturnType() == int.class) {
                    int codePoint = (int) method.invoke(event);
                    if (Character.isValidCodePoint(codePoint) && Character.charCount(codePoint) == 1) {
                        return (char) codePoint;
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return 0;
    }

    public record StartMenuAction(ActionType type, Category category, Module module, boolean closeMenu, boolean consumed) {
        public static StartMenuAction none() {
            return new StartMenuAction(ActionType.NONE, null, null, false, false);
        }

        public static StartMenuAction consumedAction() {
            return new StartMenuAction(ActionType.NONE, null, null, false, true);
        }

        public static StartMenuAction openCategory(Category category, boolean closeMenu) {
            return new StartMenuAction(ActionType.OPEN_CATEGORY_GUI, category, null, closeMenu, true);
        }

        public static StartMenuAction openSettings(boolean closeMenu) {
            return new StartMenuAction(ActionType.OPEN_SETTINGS_WINDOW, null, null, closeMenu, true);
        }

        public static StartMenuAction toggleModule(Module module, boolean closeMenu) {
            return new StartMenuAction(ActionType.TOGGLE_MODULE, null, module, closeMenu, true);
        }

        public static StartMenuAction openModuleDetails(Module module, boolean closeMenu) {
            return new StartMenuAction(ActionType.OPEN_MODULE_DETAILS, null, module, closeMenu, true);
        }

        public static StartMenuAction closeGui(boolean closeMenu) {
            return new StartMenuAction(ActionType.CLOSE_GUI, null, null, closeMenu, true);
        }
    }

    public enum ActionType {
        NONE,
        OPEN_CATEGORY_GUI,
        OPEN_SETTINGS_WINDOW,
        TOGGLE_MODULE,
        OPEN_MODULE_DETAILS,
        CLOSE_GUI
    }

    private static final class SearchBoxComponent {
        private static final int MAX_LENGTH = 64;

        private String text = "";
        private boolean focused;
        private UiRect bounds = new UiRect(0, 0, 0, 0);

        void setBounds(UiRect bounds) {
            this.bounds = bounds;
        }

        void setFocused(boolean focused) {
            this.focused = focused;
        }

        boolean isFocused() {
            return focused;
        }

        String text() {
            return text;
        }

        boolean mouseClicked(double mouseX, double mouseY) {
            focused = bounds.contains(mouseX, mouseY);
            return focused;
        }

        boolean charTyped(char codePoint) {
            if (!focused || Character.isISOControl(codePoint)) {
                return false;
            }
            if (text.length() >= MAX_LENGTH) {
                return true;
            }
            text += codePoint;
            return true;
        }

        boolean keyPressed(KeyEvent event) {
            if (!focused) {
                return false;
            }
            if (event.key() == InputConstants.KEY_BACKSPACE) {
                if (!text.isEmpty()) {
                    text = text.substring(0, text.length() - 1);
                }
                return true;
            }
            if (event.key() == InputConstants.KEY_DELETE) {
                text = "";
                return true;
            }
            return false;
        }

        void render(GuiGraphicsExtractor graphics, XpShellRenderer renderer, Font font) {
            renderer.renderSearchBox(graphics, font, bounds, text, focused);
        }
    }

    private static final class StartMenuHeaderComponent {
        private UiRect bounds = new UiRect(0, 0, 0, 0);
        private UiRect avatarBounds = new UiRect(0, 0, 0, 0);

        void setBounds(UiRect bounds) {
            this.bounds = bounds;
            avatarBounds = new UiRect(bounds.x() + 8, bounds.y() + 8, 24, 24);
        }

        void render(GuiGraphicsExtractor graphics, XpShellRenderer renderer, Font font, String playerName, Identifier playerSkin) {
            renderer.renderStartMenuHeader(graphics, font, bounds, avatarBounds, playerName, playerSkin);
        }
    }

    private static final class StartMenuModuleListComponent {
        private static final int ROW_HEIGHT = 18;

        private UiRect bounds = new UiRect(0, 0, 0, 0);
        private int scroll;

        void setBounds(UiRect bounds) {
            this.bounds = bounds;
        }

        void resetScroll() {
            scroll = 0;
        }

        void clampScroll(int totalRows) {
            int max = Math.max(0, totalRows - maxRows());
            scroll = Mth.clamp(scroll, 0, max);
        }

        void render(GuiGraphicsExtractor graphics, XpShellRenderer renderer, Font font, List<Module> modules, int mouseX, int mouseY) {
            renderer.renderStartMenuLeftColumnPane(graphics, bounds);
            if (modules.isEmpty()) {
                graphics.text(font, "No modules found.", bounds.x() + 8, bounds.y() + 8, 0xFF5A6D8A, false);
                return;
            }

            int top = bounds.y() + 3;
            int end = Math.min(modules.size(), scroll + maxRows());
            int rowY = top;
            for (int i = scroll; i < end; i++) {
                Module module = modules.get(i);
                UiRect row = new UiRect(bounds.x() + 3, rowY, bounds.w() - 6, ROW_HEIGHT - 1);
                boolean hovered = row.contains(mouseX, mouseY);
                renderer.renderStartMenuModuleRow(graphics, font, row, module.getName(), module.isEnabled(), hovered);
                rowY += ROW_HEIGHT;
            }
        }

        Module moduleAt(double mouseX, double mouseY, List<Module> modules) {
            if (!bounds.contains(mouseX, mouseY) || modules.isEmpty()) {
                return null;
            }
            int row = (int) ((mouseY - (bounds.y() + 3)) / ROW_HEIGHT);
            if (row < 0 || row >= maxRows()) {
                return null;
            }
            int index = scroll + row;
            return index >= 0 && index < modules.size() ? modules.get(index) : null;
        }

        boolean scroll(double mouseX, double mouseY, double amount, int totalRows) {
            if (!bounds.contains(mouseX, mouseY)) {
                return false;
            }
            int max = Math.max(0, totalRows - maxRows());
            scroll = Mth.clamp(scroll - (int) Math.signum(amount), 0, max);
            return true;
        }

        private int maxRows() {
            return Math.max(1, (bounds.h() - 6) / ROW_HEIGHT);
        }
    }

    private static final class StartMenuCategoryListComponent {
        private static final int ROW_HEIGHT = 20;

        private UiRect bounds = new UiRect(0, 0, 0, 0);

        void setBounds(UiRect bounds) {
            this.bounds = bounds;
        }

        void render(GuiGraphicsExtractor graphics, XpShellRenderer renderer, Font font, Category selectedCategory, Category hoverCategory) {
            renderer.renderStartMenuRightColumnPane(graphics, bounds);
            int y = bounds.y() + 5;
            for (Category category : Category.values()) {
                UiRect row = new UiRect(bounds.x() + 4, y, bounds.w() - 8, ROW_HEIGHT - 1);
                boolean hovered = category == hoverCategory;
                boolean selected = category == selectedCategory;
                renderer.renderStartMenuCategoryRow(graphics, font, row, prettify(category.name()), selected, hovered);
                y += ROW_HEIGHT;
            }
        }

        Category categoryAt(double mouseX, double mouseY) {
            int y = bounds.y() + 5;
            for (Category category : Category.values()) {
                UiRect row = new UiRect(bounds.x() + 4, y, bounds.w() - 8, ROW_HEIGHT - 1);
                if (row.contains(mouseX, mouseY)) {
                    return category;
                }
                y += ROW_HEIGHT;
            }
            return null;
        }

        private String prettify(String raw) {
            String lower = raw.toLowerCase(Locale.ROOT).replace('_', ' ');
            if (lower.isEmpty()) {
                return raw;
            }
            return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        }
    }
}
