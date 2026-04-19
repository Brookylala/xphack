package net.ozanarchy.brooksclient.client.module.modules;

import net.ozanarchy.brooksclient.client.module.Category;
import net.ozanarchy.brooksclient.client.module.Module;
import net.ozanarchy.brooksclient.client.setting.BooleanSetting;
import net.ozanarchy.brooksclient.client.setting.ColorSetting;
import net.ozanarchy.brooksclient.client.setting.SliderSetting;

public final class ChestESPModule extends Module {
    private final BooleanSetting showChests = addSetting(new BooleanSetting("Chests", true));
    private final BooleanSetting showTrappedChests = addSetting(new BooleanSetting("Trapped Chests", true));
    private final BooleanSetting showEnderChests = addSetting(new BooleanSetting("Ender Chests", true));
    private final BooleanSetting showBarrels = addSetting(new BooleanSetting("Barrels", true));
    private final BooleanSetting showShulkers = addSetting(new BooleanSetting("Shulkers", true));

    private final SliderSetting range = addSetting(new SliderSetting("Range", 64.0D, 8.0D, 256.0D, 4.0D));

    private final ColorSetting chestColor = addSetting(new ColorSetting("Chest Color", 0x90FFFFFF));
    private final ColorSetting trappedChestColor = addSetting(new ColorSetting("Trapped Chest Color", 0x90FF5555));
    private final ColorSetting enderChestColor = addSetting(new ColorSetting("Ender Chest Color", 0x906A4CFF));
    private final ColorSetting barrelColor = addSetting(new ColorSetting("Barrel Color", 0x90C68642));
    private final ColorSetting shulkerColor = addSetting(new ColorSetting("Shulker Color", 0x90FF55FF));

    public ChestESPModule() {
        super("ChestESP", "Highlights nearby containers.", Category.RENDER, -1);
    }

    public boolean showChests() {
        return showChests.getValue();
    }

    public boolean showTrappedChests() {
        return showTrappedChests.getValue();
    }

    public boolean showEnderChests() {
        return showEnderChests.getValue();
    }

    public boolean showBarrels() {
        return showBarrels.getValue();
    }

    public boolean showShulkers() {
        return showShulkers.getValue();
    }

    public double getRange() {
        return range.getValue();
    }

    public int getChestColor() {
        return chestColor.getArgb();
    }

    public int getTrappedChestColor() {
        return trappedChestColor.getArgb();
    }

    public int getEnderChestColor() {
        return enderChestColor.getArgb();
    }

    public int getBarrelColor() {
        return barrelColor.getArgb();
    }

    public int getShulkerColor() {
        return shulkerColor.getArgb();
    }
}