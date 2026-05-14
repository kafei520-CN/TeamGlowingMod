package cn.kafei.TeamGlowing.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class TeamGlowingNetwork {
    private TeamGlowingNetwork() {
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(TeamLocatorMessage.ID, TeamLocatorMessage.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(TeamLocatorMessage.ID, TeamLocatorMessageHandler::handle);
    }

    public static void sendTo(ServerPlayerEntity player, TeamLocatorMessage message) {
        ServerPlayNetworking.send(player, message);
    }
}
