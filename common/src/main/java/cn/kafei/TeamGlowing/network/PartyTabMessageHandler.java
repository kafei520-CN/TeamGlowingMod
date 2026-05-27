package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.client.ClientPartyTabCache;

public final class PartyTabMessageHandler {
    private PartyTabMessageHandler() {
    }

    public static void handle(PartyTabMessage message) {
        ClientPartyTabCache.update(message.partyName(), message.entries());
    }
}
