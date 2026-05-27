package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.client.ClientServerTabOverlayConfigCache;

public final class TabOverlayConfigMessageHandler {
    private TabOverlayConfigMessageHandler() {
    }

    public static void handle(TabOverlayConfigMessage message) {
        ClientServerTabOverlayConfigCache.update(message.state());
    }
}
