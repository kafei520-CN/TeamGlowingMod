package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.client.ClientLocatorCache;

public final class TeamLocatorMessageHandler {
    private TeamLocatorMessageHandler() {
    }

    public static void handle(TeamLocatorMessage message) {
        ClientLocatorCache.update(message.entries());
    }
}
