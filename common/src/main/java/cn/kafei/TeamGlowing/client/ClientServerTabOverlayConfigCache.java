package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.config.TabOverlayConfigState;

public final class ClientServerTabOverlayConfigCache {
    private static volatile TabOverlayConfigState state;

    private ClientServerTabOverlayConfigCache() {
    }

    public static void update(TabOverlayConfigState updatedState) {
        state = updatedState;
    }

    public static TabOverlayConfigState getOrFallback(TabOverlayConfigState fallback) {
        TabOverlayConfigState current = state;
        return current == null ? fallback : current;
    }

    public static void clear() {
        state = null;
    }
}
