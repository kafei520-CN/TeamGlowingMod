package cn.kafei.TeamGlowing.config;

import cn.kafei.TeamGlowing.platform.TeamGlowingPlatforms;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class ServerTabOverlayConfig {
    private static final Path CONFIG_PATH = TeamGlowingPlatforms.get().getConfigDir().resolve("teamglowing-tab.properties");
    private static final long RELOAD_INTERVAL_MS = 1000L;
    private static final List<String> ORDERED_KEYS = List.of(
        "enabled",
        "top_border.enabled",
        "top_border.text",
        "header.line.1",
        "header.line.2",
        "header.line.3",
        "header.line.4",
        "header.line.5",
        "footer.line.1",
        "welcome.text",
        "welcome.width",
        "welcome.interval_ms",
        "bottom_border.enabled",
        "bottom_border.text"
    );

    private static TabOverlayConfigState state = TabOverlayConfigDefaults.createState();
    private static long lastCheckedAt = 0L;
    private static long lastModifiedAt = Long.MIN_VALUE;

    private ServerTabOverlayConfig() {
    }

    public static void load() {
        synchronized (ServerTabOverlayConfig.class) {
            ensureConfigFile();
            loadInternal();
        }
    }

    public static TabOverlayConfigState get() {
        synchronized (ServerTabOverlayConfig.class) {
            long now = System.currentTimeMillis();
            if (now - lastCheckedAt >= RELOAD_INTERVAL_MS) {
                lastCheckedAt = now;
                long modifiedTime = getModifiedTime();
                if (modifiedTime != lastModifiedAt) {
                    loadInternal();
                }
            }
            return state;
        }
    }

    private static void ensureConfigFile() {
        if (Files.exists(CONFIG_PATH)) {
            return;
        }
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            saveProperties(createDefaultProperties());
        } catch (IOException ignored) {
        }
    }

    private static void loadInternal() {
        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            boolean rewriteAsUtf8 = containsUnicodeEscapes();
            properties.load(reader);
            if (migrateLegacyDefaults(properties) || rewriteAsUtf8) {
                saveProperties(properties);
            }
            state = new TabOverlayConfigState(
                Boolean.parseBoolean(properties.getProperty("enabled", "true")),
                Boolean.parseBoolean(properties.getProperty("top_border.enabled", "true")),
                properties.getProperty("top_border.text", TabOverlayConfigDefaults.DEFAULT_BORDER),
                readIndexedLines(properties, "header.line."),
                readIndexedLines(properties, "footer.line."),
                properties.getProperty("welcome.text", "欢迎来到 TeamGlowing，%player%"),
                parseInt(properties.getProperty("welcome.width", "18"), 18),
                parseLong(properties.getProperty("welcome.interval_ms", "120"), 120L),
                Boolean.parseBoolean(properties.getProperty("bottom_border.enabled", "true")),
                properties.getProperty("bottom_border.text", TabOverlayConfigDefaults.DEFAULT_BORDER)
            );
        } catch (IOException ignored) {
            state = TabOverlayConfigDefaults.createState();
        }
        lastModifiedAt = getModifiedTime();
    }

    private static boolean containsUnicodeEscapes() {
        if (!Files.exists(CONFIG_PATH)) {
            return false;
        }
        try {
            return Files.readString(CONFIG_PATH, StandardCharsets.UTF_8).contains("\\u");
        } catch (IOException ignored) {
            return false;
        }
    }

    private static List<String> readIndexedLines(Properties properties, String prefix) {
        List<String> lines = new ArrayList<>();
        for (int i = 1; i <= 64; i++) {
            String key = prefix + i;
            if (!properties.containsKey(key)) {
                break;
            }
            lines.add(properties.getProperty(key, ""));
        }
        return lines;
    }

    private static long getModifiedTime() {
        try {
            FileTime time = Files.exists(CONFIG_PATH) ? Files.getLastModifiedTime(CONFIG_PATH) : FileTime.fromMillis(0L);
            return time.toMillis();
        } catch (IOException ignored) {
            return Long.MIN_VALUE;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Properties createDefaultProperties() {
        Properties properties = new Properties();
        TabOverlayConfigState defaults = TabOverlayConfigDefaults.createState();
        properties.setProperty("enabled", Boolean.toString(defaults.enabled()));
        properties.setProperty("top_border.enabled", Boolean.toString(defaults.topBorderEnabled()));
        properties.setProperty("top_border.text", defaults.topBorderText());
        int index = 1;
        for (String line : defaults.headerLines()) {
            properties.setProperty("header.line." + index, line);
            index++;
        }
        index = 1;
        for (String line : defaults.footerLines()) {
            properties.setProperty("footer.line." + index, line);
            index++;
        }
        properties.setProperty("welcome.text", defaults.welcomeText());
        properties.setProperty("welcome.width", Integer.toString(defaults.welcomeWidth()));
        properties.setProperty("welcome.interval_ms", Long.toString(defaults.welcomeIntervalMs()));
        properties.setProperty("bottom_border.enabled", Boolean.toString(defaults.bottomBorderEnabled()));
        properties.setProperty("bottom_border.text", defaults.bottomBorderText());
        return properties;
    }

    private static boolean migrateLegacyDefaults(Properties properties) {
        boolean changed = false;
        changed |= ensureProperty(properties, "enabled", "true");
        changed |= replaceIfEquals(properties, "top_border.text", "&f&m================================================", TabOverlayConfigDefaults.DEFAULT_BORDER);
        changed |= replaceIfEquals(properties, "top_border.text", "&8&m================================================", TabOverlayConfigDefaults.DEFAULT_BORDER);
        changed |= replaceIfEquals(properties, "top_border.text", "&6===&f&m==========================================&6===", TabOverlayConfigDefaults.DEFAULT_BORDER);
        changed |= replaceIfEquals(properties, "top_border.text", "&8&m==&6&m===&e&m====&f&m==============================&e&m====&6&m===&8&m==", TabOverlayConfigDefaults.DEFAULT_BORDER);
        changed |= replaceIfEquals(properties, "header.line.1", "&3&lTeamGlowing", "&b&lTeam&f&lGlowing");
        changed |= replaceIfEquals(properties, "header.line.1", "&b&lTeamGlowing", "&b&lTeam&f&lGlowing");
        changed |= replaceIfEquals(properties, "header.line.2", "&7>> &fWelcome, %player% &7<<", "&3&l>> &f欢迎你，%player% &3&l<<");
        changed |= replaceIfEquals(properties, "header.line.2", "&8[ &f欢迎你，%player% &8]", "&3&l>> &f欢迎你，%player% &3&l<<");
        changed |= replaceIfEquals(properties, "header.line.2", "&7>> &f欢迎你，%player% &7<<", "&3&l>> &f%welcome% &3&l<<");
        changed |= replaceIfEquals(properties, "header.line.2", "&7>> &f%welcome% &7<<", "&3&l>> &f%welcome% &3&l<<");
        changed |= replaceIfEquals(properties, "header.line.2", "&8[ %welcome% &8]", "&3&l>> &f%welcome% &3&l<<");
        changed |= replaceIfEquals(properties, "header.line.3", "&7Ping: &f%ping%  &7TPS: &f%tps%", "&7延迟: &f%ping%  &8|  &7TPS: &b%tps%  &8|  &7MSPT: &f%mspt%");
        changed |= replaceIfEquals(properties, "header.line.3", "&7延迟: &f%ping%  &7TPS: &f%tps%", "&7延迟: &f%ping%  &8|  &7TPS: &b%tps%  &8|  &7MSPT: &f%mspt%");
        changed |= replaceIfEquals(properties, "header.line.3", "&7延迟: &f%ping%  &8|  &7TPS: &b%tps%", "&7延迟: &f%ping%  &8|  &7TPS: &b%tps%  &8|  &7MSPT: &f%mspt%");
        changed |= replaceIfEquals(properties, "header.line.4", "&7Date: &f%date%  &7Time: &f%time%", "&7内存: &f%memory%");
        changed |= replaceIfEquals(properties, "header.line.4", "&7日期: &f%date%  &7时间: &f%time%", "&7内存: &f%memory%");
        changed |= replaceIfEquals(properties, "header.line.4", "&7日期: &f%date%  &8|  &7时间: &f%time%", "&7内存: &f%memory%");
        changed |= ensureProperty(properties, "header.line.5", "&7日期: &f%date%  &8|  &7时间: &f%time%");
        changed |= replaceIfEquals(properties, "footer.line.1", "&7Party: &f%party%  &7Online: &f%online%", "&7小队: &b%party%  &8|  &7在线: &f%online%");
        changed |= replaceIfEquals(properties, "footer.line.1", "&7小队: &f%party%  &7在线: &f%online%", "&7小队: &b%party%  &8|  &7在线: &f%online%");
        changed |= replaceIfEquals(properties, "welcome.text", "欢迎来到 TeamGlowing，%player%", "&f欢迎来到 &bTeamGlowing&f，%player%");
        changed |= ensureProperty(properties, "welcome.text", "&f欢迎来到 &bTeamGlowing&f，%player%");
        changed |= ensureProperty(properties, "welcome.width", "18");
        changed |= ensureProperty(properties, "welcome.interval_ms", "120");
        changed |= replaceIfEquals(properties, "bottom_border.text", "&f&m================================================", TabOverlayConfigDefaults.DEFAULT_BORDER);
        changed |= replaceIfEquals(properties, "bottom_border.text", "&8&m================================================", TabOverlayConfigDefaults.DEFAULT_BORDER);
        changed |= replaceIfEquals(properties, "bottom_border.text", "&6===&f&m==========================================&6===", TabOverlayConfigDefaults.DEFAULT_BORDER);
        changed |= replaceIfEquals(properties, "bottom_border.text", "&8&m==&6&m===&e&m====&f&m==============================&e&m====&6&m===&8&m==", TabOverlayConfigDefaults.DEFAULT_BORDER);
        return changed;
    }

    private static boolean replaceIfEquals(Properties properties, String key, String oldValue, String newValue) {
        String currentValue = properties.getProperty(key);
        if (currentValue == null || !currentValue.equals(oldValue) || currentValue.equals(newValue)) {
            return false;
        }
        properties.setProperty(key, newValue);
        return true;
    }

    private static boolean ensureProperty(Properties properties, String key, String value) {
        if (properties.containsKey(key)) {
            return false;
        }
        properties.setProperty(key, value);
        return true;
    }

    private static void saveProperties(Properties properties) throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
            writer.write("#TeamGlowing TAB overlay config");
            writer.newLine();
            for (String key : ORDERED_KEYS) {
                String value = properties.getProperty(key);
                if (value != null) {
                    writer.write(key);
                    writer.write('=');
                    writer.write(escapePropertyValue(value));
                    writer.newLine();
                }
            }
            List<String> extraKeys = new ArrayList<>();
            for (String key : properties.stringPropertyNames()) {
                if (!ORDERED_KEYS.contains(key)) {
                    extraKeys.add(key);
                }
            }
            Collections.sort(extraKeys);
            for (String key : extraKeys) {
                writer.write(escapePropertyKey(key));
                writer.write('=');
                writer.write(escapePropertyValue(properties.getProperty(key, "")));
                writer.newLine();
            }
        }
    }

    private static String escapePropertyKey(String value) {
        return escapeProperty(value, true);
    }

    private static String escapePropertyValue(String value) {
        return escapeProperty(value, false);
    }

    private static String escapeProperty(String value, boolean key) {
        StringBuilder builder = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '=':
                case ':':
                case '#':
                case '!':
                    if (key || i == 0) {
                        builder.append('\\');
                    }
                    builder.append(current);
                    break;
                case ' ':
                    if (i == 0) {
                        builder.append("\\ ");
                    } else {
                        builder.append(' ');
                    }
                    break;
                default:
                    builder.append(current);
                    break;
            }
        }
        return builder.toString();
    }
}
