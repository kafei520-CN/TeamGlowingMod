package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.client.ClientPartyTabCache;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class PartyTabMessageHandler {
    private PartyTabMessageHandler() {
    }

    public static void handle(PartyTabMessage message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> ClientPartyTabCache.update(message.partyName(), message.entries()));
    }
}
