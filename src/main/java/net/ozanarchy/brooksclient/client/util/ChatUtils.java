package net.ozanarchy.brooksclient.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ChatUtils {
    private static final String PREFIX = "&7[&bXPHack&7]&f ";

    private ChatUtils() {
    }

    public static void message(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        mc.player.sendSystemMessage(Component.literal(colorize(PREFIX + message)));
    }

    private static String colorize(String input) {
        return input.replace('&', '\u00A7');
    }
}
