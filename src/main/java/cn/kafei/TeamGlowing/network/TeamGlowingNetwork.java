package cn.kafei.TeamGlowing.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class TeamGlowingNetwork {
    private TeamGlowingNetwork() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(SetSharedMarkerRequest.ID, SetSharedMarkerRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(PartyTabMessage.ID, PartyTabMessage.CODEC);
        PayloadTypeRegistry.playS2C().register(TabOverlayConfigMessage.ID, TabOverlayConfigMessage.CODEC);
        PayloadTypeRegistry.playS2C().register(TeamLocatorMessage.ID, TeamLocatorMessage.CODEC);
        PayloadTypeRegistry.playS2C().register(TeammateWorldMarkerMessage.ID, TeammateWorldMarkerMessage.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(PartyTabMessage.ID, PartyTabMessageHandler::handle);
        ClientPlayNetworking.registerGlobalReceiver(TabOverlayConfigMessage.ID, TabOverlayConfigMessageHandler::handle);
        ClientPlayNetworking.registerGlobalReceiver(TeamLocatorMessage.ID, TeamLocatorMessageHandler::handle);
        ClientPlayNetworking.registerGlobalReceiver(TeammateWorldMarkerMessage.ID, TeammateWorldMarkerMessageHandler::handle);
    }

    public static void sendTo(ServerPlayerEntity player, PartyTabMessage message) {
        ServerPlayNetworking.send(player, message);
    }

    public static void sendTo(ServerPlayerEntity player, TabOverlayConfigMessage message) {
        ServerPlayNetworking.send(player, message);
    }

    public static void sendTo(ServerPlayerEntity player, TeamLocatorMessage message) {
        ServerPlayNetworking.send(player, message);
    }

    public static void sendTo(ServerPlayerEntity player, TeammateWorldMarkerMessage message) {
        ServerPlayNetworking.send(player, message);
    }

    public static void registerServerReceiver(ServerPlayNetworking.PlayPayloadHandler<SetSharedMarkerRequest> handler) {
        ServerPlayNetworking.registerGlobalReceiver(SetSharedMarkerRequest.ID, handler);
    }
}
