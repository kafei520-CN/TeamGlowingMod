package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.platform.TeamGlowingPlatforms;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class TeamGlowingNetwork {
    private TeamGlowingNetwork() {
    }

    public static void sendToServer(CustomPacketPayload payload) {
        TeamGlowingPlatforms.get().sendToServer(payload);
    }

    public static void sendTo(ServerPlayer player, PartyTabMessage message) {
        sendToPlayer(player, message);
    }

    public static void sendTo(ServerPlayer player, TabOverlayConfigMessage message) {
        sendToPlayer(player, message);
    }

    public static void sendTo(ServerPlayer player, TeamLocatorMessage message) {
        sendToPlayer(player, message);
    }

    public static void sendTo(ServerPlayer player, TeammateWorldMarkerMessage message) {
        sendToPlayer(player, message);
    }

    public static void sendTo(ServerPlayer player, ClientBannerVisibilityMessage message) {
        sendToPlayer(player, message);
    }

    private static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        TeamGlowingPlatforms.get().sendToPlayer(player, payload);
    }
}
