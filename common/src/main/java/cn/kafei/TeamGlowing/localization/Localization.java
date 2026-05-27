package cn.kafei.TeamGlowing.localization;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public class Localization {
    private static final Gson GSON = new Gson();
    private static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();
    private final Map<String, Map<String, String>> bundles = new HashMap<>();

    public Localization() {
        this.load("zh_cn");
        this.load("en_us");
    }

    private void load(String locale) {
        String path = "assets/" + TeamGlowingConstants.MODID + "/lang/" + locale + ".json";
        Map<String, String> values = new HashMap<>();

        try (InputStream inputStream = Localization.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream != null) {
                try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    Map<String, String> loaded = GSON.fromJson(reader, STRING_MAP_TYPE);
                    if (loaded != null) {
                        values.putAll(loaded);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        this.bundles.put(locale, values);
    }

    public String translate(CommandSourceStack source, String key, Object... args) {
        ServerPlayer player = source != null && source.getEntity() instanceof ServerPlayer serverPlayer
            ? serverPlayer
            : null;
        return this.translate(player, key, args);
    }

    public String translate(ServerPlayer player, String key, Object... args) {
        String locale = player == null ? "en_us" : "zh_cn";
        String pattern = this.lookup(locale, key);
        return MessageFormat.format(pattern, args);
    }

    private String lookup(String locale, String key) {
        Map<String, String> localized = this.bundles.get(locale);
        if (localized != null && localized.containsKey(key)) {
            return localized.get(key);
        }

        Map<String, String> fallback = this.bundles.get("en_us");
        if (fallback != null && fallback.containsKey(key)) {
            return fallback.get(key);
        }

        return key;
    }
}
