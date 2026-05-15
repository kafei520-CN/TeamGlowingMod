package cn.kafei.TeamGlowing.client;

import java.util.Locale;
import java.util.Random;

public final class ClientPlayerColorHelper {
    private ClientPlayerColorHelper() {
    }

    public static int getPlayerColor(String playerName) {
        String normalizedName = playerName == null ? "" : playerName.toLowerCase(Locale.ROOT);
        Random random = new Random(normalizedName.hashCode());
        return 0xFF000000
            | ((random.nextInt(206) + 50) << 16)
            | ((random.nextInt(206) + 50) << 8)
            | (random.nextInt(206) + 50);
    }

    public static int getDarkerColor(int color) {
        int red = (int) (((color >> 16) & 0xFF) * 0.55F);
        int green = (int) (((color >> 8) & 0xFF) * 0.55F);
        int blue = (int) ((color & 0xFF) * 0.55F);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }
}
