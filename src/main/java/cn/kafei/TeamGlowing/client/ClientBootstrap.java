package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import cn.kafei.TeamGlowing.network.TeamGlowingNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.util.Identifier;

public final class ClientBootstrap implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientToggleState.load();
        ClientMarkerController.initialize();
        TeamGlowingNetwork.registerClient();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> TeamGlowingClientCommand.register(dispatcher));
        HudElementRegistry.addLast(
            Identifier.of(TeamGlowingConstants.MODID, "hud"),
            (context, tickCounter) -> ClientHudRenderer.render(context)
        );
        HudElementRegistry.addLast(
            Identifier.of(TeamGlowingConstants.MODID, "shared_markers"),
            (context, tickCounter) -> ClientWorldMarkerRenderer.renderOverlay(context)
        );
        WorldRenderEvents.AFTER_ENTITIES.register(ClientWorldMarkerRenderer::render);
        ClientTickEvents.END_CLIENT_TICK.register(ClientToggleState::suppressTeammateGlow);
        ClientTickEvents.END_CLIENT_TICK.register(ClientMarkerController::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientPartyTabCache.clear();
            ClientServerTabOverlayConfigCache.clear();
            ClientLocatorCache.clear();
            ClientWorldMarkerCache.clear();
        });
    }
}
