package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.client.ClientServerTabOverlayConfigCache;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TabOverlayConfigMessageHandler {
    private TabOverlayConfigMessageHandler() {
    }

    public static void handle(TabOverlayConfigMessage message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> ClientServerTabOverlayConfigCache.update(message.state()));
    }
}
