package net.ozanarchy.brooksclient.client.gui.theme;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class XpShellRenderer {
    private final XpShellAssets assets;

    public XpShellRenderer(XpShellAssets assets) {
        this.assets = assets;
    }

    public void renderDesktop(GuiGraphicsExtractor graphics, int width, int height) {
        int horizon = (int) (height * 0.62F);
        assets.fillVerticalGradient(graphics, 0, 0, width, horizon, XpShellAssets.DESKTOP_SKY_TOP,
                XpShellAssets.DESKTOP_SKY_BOTTOM);
        assets.fillVerticalGradient(graphics, 0, horizon, width, height, XpShellAssets.DESKTOP_FIELD_TOP,
                XpShellAssets.DESKTOP_FIELD_BOTTOM);
        renderTaskbarBase(graphics, width, height);
    }

    public void renderWallpaperDesktop(GuiGraphicsExtractor graphics, int width, int height,
            Identifier wallpaperTexture) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, wallpaperTexture, 0, 0, 0.0F, 0.0F, width, height, width, height);
        renderTaskbarBase(graphics, width, height);
    }

    public void renderTaskbarBase(GuiGraphicsExtractor graphics, int width, int height) {
        int taskbarHeight = 28;
        assets.fillVerticalGradient(graphics, 0, height - taskbarHeight, width, height, 0xFF1B66D1, 0xFF0A3C97);
        graphics.horizontalLine(0, width - 1, height - taskbarHeight, 0xFF74A4F2);
        graphics.horizontalLine(0, width - 1, height - 1, 0xFF032660);
    }

    public void renderWindowFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height, String title,
            Font font) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, XpShellAssets.WINDOW_BORDER);
        assets.drawRaisedBox(graphics, x, y, x + width, y + height, XpShellAssets.WINDOW_FILL);

        int titleHeight = 24;
        assets.fillHorizontalGradient(graphics, x + 1, y + 1, x + width - 1, y + titleHeight, XpShellAssets.TITLE_LEFT,
                XpShellAssets.TITLE_RIGHT);
        graphics.horizontalLine(x + 1, x + width - 2, y + titleHeight, 0xFF6FA0F7);
        graphics.text(font, title, x + 10, y + 8, XpShellAssets.TEXT_LIGHT, false);

        drawCaptionButtons(graphics, x + width - 56, y + 5);
    }

    public void renderTaskPane(GuiGraphicsExtractor graphics, int x, int y, int width, int height, String header,
            Font font) {
        assets.drawRaisedBox(graphics, x, y, x + width, y + height, XpShellAssets.PANEL_MID);
        assets.fillVerticalGradient(graphics, x + 1, y + 1, x + width - 1, y + height - 1, XpShellAssets.TASK_PANE_TOP,
                XpShellAssets.TASK_PANE_BOTTOM);
        assets.fillVerticalGradient(graphics, x + 1, y + 1, x + width - 1, y + 22, XpShellAssets.TASK_PANE_HEADER,
                0xFF1B418F);
        graphics.text(font, header, x + 8, y + 8, XpShellAssets.TEXT_LIGHT, false);
    }

    public void renderPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        assets.drawRaisedBox(graphics, x, y, x + width, y + height, 0xFFF8FAFF);
    }

    public void renderListRow(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            String text, boolean selected, boolean enabled) {
        int bg = selected ? 0xFF2D63BD : 0xFFE9EEF7;
        int fg = selected ? XpShellAssets.TEXT_LIGHT : XpShellAssets.TEXT_DARK;
        graphics.fill(x, y, x + width, y + height, bg);
        if (enabled) {
            graphics.fill(x + 4, y + 4, x + 10, y + height - 4, 0xFF35A02A);
            graphics.fill(x + 5, y + 5, x + 9, y + height - 5, 0xFF72D564);
        } else {
            graphics.fill(x + 4, y + 4, x + 10, y + height - 4, 0xFF9AA8C0);
        }
        graphics.text(font, text, x + 15, y + 5, fg, false);
    }

    public void renderSettingLabel(GuiGraphicsExtractor graphics, Font font, int x, int y, String text) {
        graphics.text(font, text, x, y, XpShellAssets.TEXT_DARK, false);
    }

    public void renderCheckbox(GuiGraphicsExtractor graphics, int x, int y, boolean checked) {
        assets.drawSunkenBox(graphics, x, y, x + 12, y + 12, 0xFFFFFFFF);
        if (checked) {
            graphics.fill(x + 3, y + 5, x + 5, y + 9, 0xFF0E4F12);
            graphics.fill(x + 5, y + 7, x + 9, y + 9, 0xFF0E4F12);
            graphics.fill(x + 7, y + 3, x + 9, y + 7, 0xFF0E4F12);
        }
    }

    public void renderModeButton(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, String value) {
        assets.drawRaisedBox(graphics, x, y, x + width, y + 14, 0xFFF4F6FD);
        graphics.text(font, value, x + 5, y + 3, XpShellAssets.TEXT_DARK, false);
    }

    public void renderSlider(GuiGraphicsExtractor graphics, int x, int y, int width, double normalized) {
        assets.drawSunkenBox(graphics, x, y, x + width, y + 12, 0xFFFFFFFF);
        graphics.fill(x + 2, y + 5, x + width - 2, y + 7, 0xFF6D7F99);
        int knobCenter = x + 3 + (int) Math.round(normalized * (width - 6));
        assets.drawRaisedBox(graphics, knobCenter - 4, y + 1, knobCenter + 4, y + 11, 0xFFE8EEF9);
    }

    public void renderStartButton(GuiGraphicsExtractor graphics, Font font, UiRect bounds, boolean hovered,
            boolean open) {
        int top = open ? 0xFF72DD5A : (hovered ? 0xFF7FE46A : 0xFF69D651);
        int bottom = open ? 0xFF2A7A20 : (hovered ? 0xFF328C27 : 0xFF2E8324);
        int x1 = bounds.x();
        int y1 = bounds.y();
        int x2 = bounds.right();
        int y2 = bounds.bottom();

        assets.fillVerticalGradient(graphics, x1 + 9, y1, x2, y2, top, bottom);
        assets.fillVerticalGradient(graphics, x1 + 4, y1 + 2, x1 + 9, y2 - 2, top, bottom);
        assets.fillVerticalGradient(graphics, x1 + 2, y1 + 4, x1 + 4, y2 - 4, top, bottom);
        graphics.fill(x1 + 1, y1 + 6, x1 + 2, y2 - 6, bottom);

        graphics.horizontalLine(x1 + 4, x2 - 1, y1, 0xFFC9F8AE);
        graphics.verticalLine(x2 - 1, y1, y2 - 1, 0xFF1C6415);
        graphics.horizontalLine(x1 + 4, x2 - 1, y2 - 1, 0xFF1B6314);

        renderStartFlagIcon(graphics, x1 + 12, y1 + 6);
        graphics.text(font, "start", x1 + 29, y1 + 7, XpShellAssets.TEXT_LIGHT, false);
    }

    public void renderTaskbarTray(GuiGraphicsExtractor graphics, Font font, UiRect bounds, String clockText) {
        assets.fillVerticalGradient(graphics, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0xFF2E78D8,
                0xFF1D58B8);
        graphics.horizontalLine(bounds.x(), bounds.right() - 1, bounds.y(), 0xFF95BEFF);
        graphics.verticalLine(bounds.x(), bounds.y(), bounds.bottom() - 1, 0xFF7FB0F6);
        graphics.verticalLine(bounds.right() - 1, bounds.y(), bounds.bottom() - 1, 0xFF0B3782);
        graphics.horizontalLine(bounds.x(), bounds.right() - 1, bounds.bottom() - 1, 0xFF0B3782);

        graphics.fill(bounds.x() + 8, bounds.y() + 7, bounds.x() + 11, bounds.y() + 10, 0xFFEFF7FF);
        graphics.fill(bounds.x() + 14, bounds.y() + 7, bounds.x() + 17, bounds.y() + 10, 0xFFEFF7FF);
        graphics.text(font, clockText, bounds.right() - 42, bounds.y() + 7, 0xFFF5FBFF, false);
    }

    public void renderTaskbarWindowButton(GuiGraphicsExtractor graphics, Font font, UiRect bounds, String label,
            boolean active, boolean minimized) {
        int top = active ? 0xFF67A5F2 : 0xFF3C83DD;
        int bottom = active ? 0xFF2C63B9 : 0xFF1C56AF;
        assets.fillVerticalGradient(graphics, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), top, bottom);
        graphics.horizontalLine(bounds.x(), bounds.right() - 1, bounds.y(), 0xFF9EC7FF);
        graphics.horizontalLine(bounds.x(), bounds.right() - 1, bounds.bottom() - 1, 0xFF0A3B8D);
        graphics.verticalLine(bounds.x(), bounds.y(), bounds.bottom() - 1, 0xFF8ABAF8);
        graphics.verticalLine(bounds.right() - 1, bounds.y(), bounds.bottom() - 1, 0xFF0A3B8D);

        int iconColor = minimized ? 0xFFE8F0FF : 0xFFB8E2A8;
        graphics.fill(bounds.x() + 5, bounds.y() + 6, bounds.x() + 10, bounds.y() + 11, iconColor);
        graphics.text(font, label, bounds.x() + 14, bounds.y() + 6, 0xFFF6FBFF, false);
    }

    public void renderStartMenuShell(GuiGraphicsExtractor graphics, UiRect bounds, UiRect bodyBounds,
            UiRect actionBounds) {
        graphics.fill(bounds.x() + 2, bounds.y() + 2, bounds.right() + 4, bounds.bottom() + 4, 0x40000000);
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0xFF0A3A90);
        assets.drawRaisedBox(graphics, bounds.x() + 1, bounds.y() + 1, bounds.right() - 1, bounds.bottom() - 1,
                0xFFF6F2E9);
        graphics.fill(bodyBounds.x(), bodyBounds.y(), bodyBounds.right(), bodyBounds.bottom(), 0xFFF3EEDF);
        assets.fillVerticalGradient(graphics, actionBounds.x(), actionBounds.y(), actionBounds.right(),
                actionBounds.bottom(), 0xFFD7E6FF, 0xFFBCD4F6);
        graphics.horizontalLine(actionBounds.x(), actionBounds.right() - 1, actionBounds.y(), 0xFF8EADE0);
    }

    public void renderStartMenuHeader(GuiGraphicsExtractor graphics, Font font, UiRect bounds, UiRect avatarBounds,
            String playerName, Identifier playerSkin) {
        assets.fillHorizontalGradient(graphics, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0xFF1A4EA8,
                0xFF4E8FE3);
        graphics.horizontalLine(bounds.x(), bounds.right() - 1, bounds.bottom() - 1, 0xFF84AEED);
        graphics.verticalLine(bounds.x(), bounds.y(), bounds.bottom() - 1, 0xFF92BCF8);
        graphics.verticalLine(bounds.right() - 1, bounds.y(), bounds.bottom() - 1, 0xFF173A7F);

        renderStartMenuAvatar(graphics, avatarBounds, playerSkin);
        graphics.text(font, playerName, avatarBounds.right() + 8, bounds.y() + 11, 0xFFFFFFFF, false);
    }

    public void renderStartMenuLeftColumnPane(GuiGraphicsExtractor graphics, UiRect bounds) {
        assets.drawSunkenBox(graphics, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0xFFFDFBF6);
    }

    public void renderStartMenuRightColumnPane(GuiGraphicsExtractor graphics, UiRect bounds) {
        assets.drawRaisedBox(graphics, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0xFFE9EEF8);
        assets.fillVerticalGradient(graphics, bounds.x() + 1, bounds.y() + 1, bounds.right() - 1, bounds.bottom() - 1,
                0xFFEFF4FF, 0xFFD4E1F7);
    }

    public void renderStartMenuActionStrip(
            GuiGraphicsExtractor graphics,
            Font font,
            UiRect stripBounds,
            UiRect settingsButtonBounds,
            UiRect closeButtonBounds,
            boolean settingsHovered,
            boolean closeHovered) {
        graphics.text(font, "Choose an action", stripBounds.x() + 8, stripBounds.y() + 12, 0xFF214F8F, false);
        renderStartMenuActionButton(graphics, font, settingsButtonBounds, "Settings", settingsHovered);
        renderStartMenuActionButton(graphics, font, closeButtonBounds, "Close", closeHovered);
    }

    public void renderStartMenuCategoryRow(GuiGraphicsExtractor graphics, Font font, UiRect bounds, String text,
            boolean selected, boolean hovered) {
        if (selected || hovered) {
            assets.fillHorizontalGradient(graphics, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0xFF2A64C2,
                    0xFF4D95F2);
        }
        int color = (selected || hovered) ? XpShellAssets.TEXT_LIGHT : 0xFF27467E;
        graphics.text(font, text, bounds.x() + 6, bounds.y() + 5, color, false);
        graphics.text(font, ">", bounds.right() - 10, bounds.y() + 5, color, false);
    }

    public void renderStartMenuModuleRow(GuiGraphicsExtractor graphics, Font font, UiRect bounds, String text,
            boolean enabled, boolean highlighted) {
        int background = highlighted ? 0xFF2B67C8 : 0xFFFDFBF7;
        int textColor = highlighted ? XpShellAssets.TEXT_LIGHT : XpShellAssets.TEXT_DARK;
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), background);
        graphics.fill(bounds.x() + 4, bounds.y() + 4, bounds.x() + 10, bounds.bottom() - 4,
                enabled ? 0xFF36A12B : 0xFFAAB7CC);
        if (enabled) {
            graphics.fill(bounds.x() + 5, bounds.y() + 5, bounds.x() + 9, bounds.bottom() - 5, 0xFF7BD56A);
        }
        graphics.text(font, text, bounds.x() + 16, bounds.y() + 5, textColor, false);
    }

    public void renderSearchBox(GuiGraphicsExtractor graphics, Font font, UiRect bounds, String text, boolean focused) {
        int fill = focused ? 0xFFFFFFFF : 0xFFF9F7F2;
        assets.drawSunkenBox(graphics, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), fill);
        if (text.isEmpty()) {
            graphics.text(font, "Search modules...", bounds.x() + 5, bounds.y() + 5, 0xFF7A8EAD, false);
            return;
        }
        graphics.text(font, text, bounds.x() + 5, bounds.y() + 5, XpShellAssets.TEXT_DARK, false);
    }

    public void renderStartMenuFlyoutShell(GuiGraphicsExtractor graphics, Font font, UiRect bounds, String title) {
        graphics.fill(bounds.x() + 2, bounds.y() + 2, bounds.right() + 3, bounds.bottom() + 3, 0x33000000);
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0xFF0B3F96);
        assets.drawRaisedBox(graphics, bounds.x() + 1, bounds.y() + 1, bounds.right() - 1, bounds.bottom() - 1,
                0xFFF9F6EC);
        assets.fillHorizontalGradient(graphics, bounds.x() + 2, bounds.y() + 2, bounds.right() - 2, bounds.y() + 18,
                0xFF2B66C5, 0xFF4A8EEA);
        graphics.text(font, title, bounds.x() + 8, bounds.y() + 7, 0xFFFFFFFF, false);
    }

    public void renderStartMenuFlyoutRow(GuiGraphicsExtractor graphics, Font font, UiRect bounds, String text,
            boolean enabled, boolean hovered) {
        int background = hovered ? 0xFF2B67C8 : 0xFFFDFBF7;
        int textColor = hovered ? XpShellAssets.TEXT_LIGHT : XpShellAssets.TEXT_DARK;
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), background);
        graphics.fill(bounds.x() + 4, bounds.y() + 4, bounds.x() + 10, bounds.bottom() - 4,
                enabled ? 0xFF36A12B : 0xFFAAB7CC);
        graphics.text(font, text, bounds.x() + 16, bounds.y() + 5, textColor, false);
    }

    private void drawCaptionButtons(GuiGraphicsExtractor graphics, int x, int y) {
        assets.drawRaisedBox(graphics, x, y, x + 15, y + 14, 0xFF6EA0F9);
        assets.drawRaisedBox(graphics, x + 18, y, x + 33, y + 14, 0xFF6EA0F9);
        assets.drawRaisedBox(graphics, x + 36, y, x + 51, y + 14, XpShellAssets.XP_RED_BUTTON);

        graphics.fill(x + 4, y + 9, x + 11, y + 10, 0xFFFFFFFF);

        graphics.fill(x + 25, y + 3, x + 26, y + 11, 0xFFFFFFFF);
        graphics.fill(x + 22, y + 6, x + 29, y + 7, 0xFFFFFFFF);

        graphics.text(net.minecraft.client.Minecraft.getInstance().font, "X", x + 42, y + 3, 0xFFFFFFFF, false);
    }

    private void renderStartFlagIcon(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 4, y + 4, 0xFFD92727);
        graphics.fill(x + 4, y, x + 8, y + 4, 0xFF4BC13B);
        graphics.fill(x, y + 4, x + 4, y + 8, 0xFF2E5EDF);
        graphics.fill(x + 4, y + 4, x + 8, y + 8, 0xFFE8D84D);
        graphics.fill(x - 1, y + 1, x, y + 9, 0xFFFFFFFF);
    }

    private void renderStartMenuAvatar(GuiGraphicsExtractor graphics, UiRect avatarBounds, Identifier playerSkin) {
        assets.drawSunkenBox(graphics, avatarBounds.x() - 1, avatarBounds.y() - 1, avatarBounds.right() + 1,
                avatarBounds.bottom() + 1, 0xFFFFFFFF);
        if (playerSkin == null) {
            graphics.fill(avatarBounds.x(), avatarBounds.y(), avatarBounds.right(), avatarBounds.bottom(), 0xFFB7D0F6);
            graphics.fill(avatarBounds.x() + 6, avatarBounds.y() + 7, avatarBounds.x() + 9, avatarBounds.y() + 10,
                    0xFF1B386F);
            graphics.fill(avatarBounds.x() + 15, avatarBounds.y() + 7, avatarBounds.x() + 18, avatarBounds.y() + 10,
                    0xFF1B386F);
            graphics.fill(avatarBounds.x() + 7, avatarBounds.y() + 15, avatarBounds.x() + 17, avatarBounds.y() + 17,
                    0xFF1B386F);
            return;
        }

        float scaleX = avatarBounds.w() / 8.0F;
        float scaleY = avatarBounds.h() / 8.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(avatarBounds.x(), avatarBounds.y());
        graphics.pose().scale(scaleX, scaleY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, playerSkin, 0, 0, 8.0F, 8.0F, 8, 8, 64, 64);
        graphics.blit(RenderPipelines.GUI_TEXTURED, playerSkin, 0, 0, 40.0F, 8.0F, 8, 8, 64, 64);
        graphics.pose().popMatrix();
    }

    private void renderStartMenuActionButton(GuiGraphicsExtractor graphics, Font font, UiRect bounds, String text,
            boolean hovered) {
        int borderFill = hovered ? 0xFFD8E8FF : 0xFFE8F0FF;
        int top = hovered ? 0xFFF6FBFF : 0xFFFFFFFF;
        int bottom = hovered ? 0xFFBFD7FF : 0xFFD3E2FB;
        int textColor = hovered ? 0xFF113A77 : 0xFF214F8F;
        assets.drawRaisedBox(graphics, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), borderFill);
        assets.fillVerticalGradient(graphics, bounds.x() + 1, bounds.y() + 1, bounds.right() - 1, bounds.bottom() - 1,
                top, bottom);
        graphics.centeredText(font, text, bounds.x() + bounds.w() / 2, bounds.y() + 6, textColor);
    }

    public void renderColorPreview(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int argb) {
        assets.drawRaisedBox(graphics, x, y, x + width, y + height, 0xFFF4F6FD);

        int insetX1 = x + 2;
        int insetY1 = y + 2;
        int insetX2 = x + width - 2;
        int insetY2 = y + height - 2;

        graphics.fill(insetX1, insetY1, insetX2, insetY2, 0xFFFFFFFF);
        graphics.fill(insetX1, insetY1, insetX2, insetY2, argb);
    }

    public void renderColorPopupFrame(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            String title) {
        renderWindowFrame(graphics, x, y, width, height, title, font);
    }

    public void renderHueBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float selectedHue) {
        assets.drawSunkenBox(graphics, x, y, x + width, y + height, 0xFFFFFFFF);

        for (int yy = 0; yy < height - 4; yy++) {
            float hue = yy / (float) Math.max(1, (height - 5));
            int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f) | 0xFF000000;
            graphics.fill(x + 2, y + 2 + yy, x + width - 2, y + 3 + yy, rgb);
        }

        int markerY = y + 2 + Math.round(selectedHue * (height - 5));
        graphics.fill(x + 1, markerY, x + width - 1, markerY + 1, 0xFF000000);
        graphics.fill(x + 2, markerY + 1, x + width - 2, markerY + 2, 0xFFFFFFFF);
    }

    public void renderAlphaBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int rgb,
            float selectedAlpha) {
        assets.drawSunkenBox(graphics, x, y, x + width, y + height, 0xFFFFFFFF);

        for (int yy = 0; yy < height - 4; yy++) {
            float alpha = 1.0f - (yy / (float) Math.max(1, (height - 5)));
            int argb = ((int) (alpha * 255.0f) << 24) | (rgb & 0x00FFFFFF);
            graphics.fill(x + 2, y + 2 + yy, x + width - 2, y + 3 + yy, argb);
        }

        int markerY = y + 2 + Math.round((1.0f - selectedAlpha) * (height - 5));
        graphics.fill(x + 1, markerY, x + width - 1, markerY + 1, 0xFF000000);
        graphics.fill(x + 2, markerY + 1, x + width - 2, markerY + 2, 0xFFFFFFFF);
    }

    public void renderSatBrightSquare(GuiGraphicsExtractor graphics, int x, int y, int size, float hue, float sat,
            float bright) {
        assets.drawSunkenBox(graphics, x, y, x + size, y + size, 0xFFFFFFFF);

        for (int yy = 0; yy < size - 4; yy++) {
            float rowBright = 1.0f - (yy / (float) Math.max(1, (size - 5)));
            for (int xx = 0; xx < size - 4; xx++) {
                float rowSat = xx / (float) Math.max(1, (size - 5));
                int rgb = java.awt.Color.HSBtoRGB(hue, rowSat, rowBright) | 0xFF000000;
                graphics.fill(x + 2 + xx, y + 2 + yy, x + 3 + xx, y + 3 + yy, rgb);
            }
        }

        int markerX = x + 2 + Math.round(sat * (size - 5));
        int markerY = y + 2 + Math.round((1.0f - bright) * (size - 5));

        graphics.fill(markerX - 2, markerY, markerX + 3, markerY + 1, 0xFF000000);
        graphics.fill(markerX, markerY - 2, markerX + 1, markerY + 3, 0xFF000000);
        graphics.fill(markerX - 1, markerY, markerX + 2, markerY + 1, 0xFFFFFFFF);
        graphics.fill(markerX, markerY - 1, markerX + 1, markerY + 2, 0xFFFFFFFF);
    }
}
