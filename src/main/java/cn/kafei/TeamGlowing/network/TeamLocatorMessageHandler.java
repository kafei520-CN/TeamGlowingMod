package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.client.ClientLocatorCache;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TeamLocatorMessageHandler {
    private TeamLocatorMessageHandler() {
    }

    public static void handle(TeamLocatorMessage message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> ClientLocatorCache.update(message.entries()));
    }
}
