package cn.kafei.TeamGlowing.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class ClientBootstrap {
    private static boolean initialized;

    private ClientBootstrap() {
    }

    public static synchronized void initialize(KeyMapping markerKeyBinding) {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientToggleState.load();
        ClientMarkerController.initialize(markerKeyBinding);
    }

    public static void onEndClientTick(Minecraft client) {
        ClientToggleState.suppressTeammateGlow(client);
        ClientMarkerController.tick(client);
        PartyRespawnRequestScheduler.tick(client);
    }

    public static void onDisconnect() {
        ClientPartyTabCache.clear();
        ClientServerTabOverlayConfigCache.clear();
        ClientBannerVisibilityState.clear();
        ClientLocatorCache.clear();
        ClientWorldMarkerCache.clear();
        PartyRespawnRequestScheduler.clear();
    }
}
