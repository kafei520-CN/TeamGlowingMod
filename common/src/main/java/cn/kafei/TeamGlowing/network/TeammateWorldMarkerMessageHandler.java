package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.client.ClientWorldMarkerCache;

public final class TeammateWorldMarkerMessageHandler {
    private TeammateWorldMarkerMessageHandler() {
    }

    public static void handle(TeammateWorldMarkerMessage message) {
        ClientWorldMarkerCache.update(message.entries());
    }
}
