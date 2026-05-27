package cn.kafei.TeamGlowing.fabric;

import cn.kafei.TeamGlowing.TeamGlowingCommon;
import cn.kafei.TeamGlowing.network.ClientBannerVisibilityMessage;
import cn.kafei.TeamGlowing.network.PartyTabMessage;
import cn.kafei.TeamGlowing.network.RequestPartyRespawnMessage;
import cn.kafei.TeamGlowing.network.SetSharedMarkerRequest;
import cn.kafei.TeamGlowing.network.TabOverlayConfigMessage;
import cn.kafei.TeamGlowing.network.TeamLocatorMessage;
import cn.kafei.TeamGlowing.network.TeammateWorldMarkerMessage;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class FabricTeamGlowingNetwork {
    private FabricTeamGlowingNetwork() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playC2S().register(SetSharedMarkerRequest.ID, SetSharedMarkerRequest.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestPartyRespawnMessage.ID, RequestPartyRespawnMessage.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyTabMessage.ID, PartyTabMessage.CODEC);
        PayloadTypeRegistry.playS2C().register(TabOverlayConfigMessage.ID, TabOverlayConfigMessage.CODEC);
        PayloadTypeRegistry.playS2C().register(TeamLocatorMessage.ID, TeamLocatorMessage.CODEC);
        PayloadTypeRegistry.playS2C().register(TeammateWorldMarkerMessage.ID, TeammateWorldMarkerMessage.CODEC);
        PayloadTypeRegistry.playS2C().register(ClientBannerVisibilityMessage.ID, ClientBannerVisibilityMessage.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(SetSharedMarkerRequest.ID, (payload, context) ->
            context.server().execute(() -> TeamGlowingCommon.handleSetSharedMarker(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(RequestPartyRespawnMessage.ID, (payload, context) ->
            context.server().execute(() -> TeamGlowingCommon.handlePartyRespawnRequest(context.player(), payload))
        );
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}
