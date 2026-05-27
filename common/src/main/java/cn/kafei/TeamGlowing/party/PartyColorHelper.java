package cn.kafei.TeamGlowing.party;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PartyColorHelper {
    public static final int DEFAULT_COLOR = 0x55AAFF;
    private static final Map<String, Integer> NAMED_COLORS = createNamedColors();
    private static final List<String> SUGGESTED_INPUTS = List.of(
        "blue",
        "aqua",
        "green",
        "yellow",
        "gold",
        "red",
        "purple",
        "#55AAFF"
    );

    private PartyColorHelper() {
    }

    public static Integer parse(String input) {
        if (input == null) {
            return Integer.valueOf(DEFAULT_COLOR);
        }

        String normalized = input.strip();
        if (normalized.isEmpty()) {
            return Integer.valueOf(DEFAULT_COLOR);
        }

        Integer namedColor = NAMED_COLORS.get(normalized.toLowerCase(Locale.ROOT));
        if (namedColor != null) {
            return namedColor;
        }

        String hex = normalized;
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        } else if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }

        if (hex.length() != 6) {
            return null;
        }

        try {
            return Integer.valueOf(Integer.parseInt(hex, 16) & 0xFFFFFF);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static int normalize(int color) {
        return color & 0xFFFFFF;
    }

    public static String formatHex(int color) {
        return String.format(Locale.ROOT, "#%06X", normalize(color));
    }

    public static List<String> getSuggestedInputs() {
        return SUGGESTED_INPUTS;
    }

    private static Map<String, Integer> createNamedColors() {
        Map<String, Integer> colors = new LinkedHashMap<>();
        register(colors, 0xFFFFFF, "white", "white_color", "白", "白色");
        register(colors, 0x000000, "black", "黑", "黑色");
        register(colors, 0xFF5555, "red", "红", "红色");
        register(colors, 0x55FF55, "green", "绿", "绿色");
        register(colors, 0x5555FF, "blue", "蓝", "蓝色");
        register(colors, 0x55FFFF, "aqua", "cyan", "青", "青色", "水蓝", "天蓝");
        register(colors, 0xFFFF55, "yellow", "黄", "黄色");
        register(colors, 0xFFAA00, "gold", "orange", "橙", "橙色", "金", "金色");
        register(colors, 0xAA00AA, "purple", "magenta", "紫", "紫色", "粉", "粉色");
        register(colors, 0xAAAAAA, "gray", "grey", "灰", "灰色");
        return Map.copyOf(colors);
    }

    private static void register(Map<String, Integer> colors, int rgb, String... aliases) {
        Integer value = Integer.valueOf(rgb);
        for (String alias : aliases) {
            colors.put(alias.toLowerCase(Locale.ROOT), value);
        }
    }
}
