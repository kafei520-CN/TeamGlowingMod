package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.client.ClientWorldMarkerCache;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TeammateWorldMarkerMessageHandler {
    private TeammateWorldMarkerMessageHandler() {
    }

    public static void handle(TeammateWorldMarkerMessage message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> ClientWorldMarkerCache.update(message.entries()));
    }
}
