package cn.kafei.TeamGlowing.neoforge;

import cn.kafei.TeamGlowing.TeamGlowingCommon;
import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import cn.kafei.TeamGlowing.network.ClientBannerVisibilityMessage;
import cn.kafei.TeamGlowing.network.ClientBannerVisibilityMessageHandler;
import cn.kafei.TeamGlowing.network.PartyTabMessage;
import cn.kafei.TeamGlowing.network.PartyTabMessageHandler;
import cn.kafei.TeamGlowing.network.RequestPartyRespawnMessage;
import cn.kafei.TeamGlowing.network.SetSharedMarkerRequest;
import cn.kafei.TeamGlowing.network.TabOverlayConfigMessage;
import cn.kafei.TeamGlowing.network.TabOverlayConfigMessageHandler;
import cn.kafei.TeamGlowing.network.TeamLocatorMessage;
import cn.kafei.TeamGlowing.network.TeamLocatorMessageHandler;
import cn.kafei.TeamGlowing.network.TeammateWorldMarkerMessage;
import cn.kafei.TeamGlowing.network.TeammateWorldMarkerMessageHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@SuppressWarnings("null")
public final class NeoForgeTeamGlowingNetwork {
    private NeoForgeTeamGlowingNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TeamGlowingConstants.MODID).optional();
        registrar.playToServer(SetSharedMarkerRequest.ID, SetSharedMarkerRequest.CODEC, (payload, context) ->
            context.enqueueWork(() -> TeamGlowingCommon.handleSetSharedMarker((ServerPlayer) context.player(), payload))
        );
        registrar.playToServer(RequestPartyRespawnMessage.ID, RequestPartyRespawnMessage.CODEC, (payload, context) ->
            context.enqueueWork(() -> TeamGlowingCommon.handlePartyRespawnRequest((ServerPlayer) context.player(), payload))
        );
        registrar.playToClient(PartyTabMessage.ID, PartyTabMessage.CODEC, (payload, context) ->
            context.enqueueWork(() -> PartyTabMessageHandler.handle(payload))
        );
        registrar.playToClient(TabOverlayConfigMessage.ID, TabOverlayConfigMessage.CODEC, (payload, context) ->
            context.enqueueWork(() -> TabOverlayConfigMessageHandler.handle(payload))
        );
        registrar.playToClient(TeamLocatorMessage.ID, TeamLocatorMessage.CODEC, (payload, context) ->
            context.enqueueWork(() -> TeamLocatorMessageHandler.handle(payload))
        );
        registrar.playToClient(TeammateWorldMarkerMessage.ID, TeammateWorldMarkerMessage.CODEC, (payload, context) ->
            context.enqueueWork(() -> TeammateWorldMarkerMessageHandler.handle(payload))
        );
        registrar.playToClient(ClientBannerVisibilityMessage.ID, ClientBannerVisibilityMessage.CODEC, (payload, context) ->
            context.enqueueWork(() -> ClientBannerVisibilityMessageHandler.handle(payload))
        );
    }
}
