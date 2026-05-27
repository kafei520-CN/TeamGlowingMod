package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.network.TeamLocatorEntry;
import cn.kafei.TeamGlowing.platform.TeamGlowingPlatforms;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;

public final class ClientToggleState {
    private static final String ENABLED_KEY = "enabled";
    private static final Path CONFIG_PATH = TeamGlowingPlatforms.get().getConfigDir().resolve("teamglowing-client.properties");
    private static boolean enabled = true;

    private ClientToggleState() {
    }

    public static void load() {
        enabled = true;
        if (!Files.exists(CONFIG_PATH)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(CONFIG_PATH)) {
            properties.load(inputStream);
            enabled = Boolean.parseBoolean(properties.getProperty(ENABLED_KEY, "true"));
        } catch (IOException ignored) {
            enabled = true;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean setEnabled(boolean value) {
        if (enabled == value) {
            return false;
        }

        enabled = value;
        save();
        return true;
    }

    public static void suppressTeammateGlow(Minecraft client) {
        if (enabled || client.level == null || client.player == null) {
            return;
        }

        List<TeamLocatorEntry> entries = ClientLocatorCache.getEntries();
        if (entries.isEmpty()) {
            return;
        }

        Set<String> teammateIds = entries.stream()
            .map(TeamLocatorEntry::playerId)
            .filter(id -> id != null && !id.isBlank())
            .map(id -> id.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());

        Set<String> teammateNames = entries.stream()
            .map(TeamLocatorEntry::playerName)
            .filter(name -> name != null && !name.isBlank())
            .map(name -> name.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());

        for (AbstractClientPlayer otherPlayer : client.level.players()) {
            if (!isTrackedTeammate(client, otherPlayer, teammateIds, teammateNames)) {
                continue;
            }

            if (otherPlayer.isCurrentlyGlowing()) {
                otherPlayer.setGlowingTag(false);
            }
        }
    }

    public static boolean shouldSuppressGlow(Entity entity) {
        if (enabled || !(entity instanceof AbstractClientPlayer player)) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return false;
        }

        List<TeamLocatorEntry> entries = ClientLocatorCache.getEntries();
        if (entries.isEmpty()) {
            return false;
        }

        Set<String> teammateIds = entries.stream()
            .map(TeamLocatorEntry::playerId)
            .filter(id -> id != null && !id.isBlank())
            .map(id -> id.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());

        Set<String> teammateNames = entries.stream()
            .map(TeamLocatorEntry::playerName)
            .filter(name -> name != null && !name.isBlank())
            .map(name -> name.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());

        return isTrackedTeammate(client, player, teammateIds, teammateNames);
    }

    private static boolean isTrackedTeammate(Minecraft client, AbstractClientPlayer otherPlayer, Set<String> teammateIds, Set<String> teammateNames) {
        if (otherPlayer == client.player) {
            return false;
        }

        return teammateIds.contains(otherPlayer.getStringUUID().toLowerCase(Locale.ROOT))
            || teammateNames.contains(otherPlayer.getGameProfile().getName().toLowerCase(Locale.ROOT));
    }

    private static void save() {
        Properties properties = new Properties();
        properties.setProperty(ENABLED_KEY, Boolean.toString(enabled));
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream outputStream = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(outputStream, "TeamGlowing client settings");
            }
        } catch (IOException ignored) {
        }
    }
}
