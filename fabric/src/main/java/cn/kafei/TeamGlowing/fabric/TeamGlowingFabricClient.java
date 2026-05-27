package cn.kafei.TeamGlowing.fabric;

import cn.kafei.TeamGlowing.client.ClientBootstrap;
import cn.kafei.TeamGlowing.client.ClientHudRenderer;
import cn.kafei.TeamGlowing.client.ClientMarkerController;
import cn.kafei.TeamGlowing.client.ClientWorldMarkerRenderer;
import cn.kafei.TeamGlowing.client.TeamGlowingClientCommand;
import cn.kafei.TeamGlowing.network.ClientBannerVisibilityMessage;
import cn.kafei.TeamGlowing.network.ClientBannerVisibilityMessageHandler;
import cn.kafei.TeamGlowing.network.PartyTabMessage;
import cn.kafei.TeamGlowing.network.PartyTabMessageHandler;
import cn.kafei.TeamGlowing.network.TabOverlayConfigMessage;
import cn.kafei.TeamGlowing.network.TabOverlayConfigMessageHandler;
import cn.kafei.TeamGlowing.network.TeamLocatorMessage;
import cn.kafei.TeamGlowing.network.TeamLocatorMessageHandler;
import cn.kafei.TeamGlowing.network.TeammateWorldMarkerMessage;
import cn.kafei.TeamGlowing.network.TeammateWorldMarkerMessageHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

public final class TeamGlowingFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMapping markerKeyBinding = KeyBindingHelper.registerKeyBinding(ClientMarkerController.createMarkKeyBinding());
        ClientBootstrap.initialize(markerKeyBinding);
        registerClientReceivers();

        ClientTickEvents.END_CLIENT_TICK.register(ClientBootstrap::onEndClientTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientBootstrap.onDisconnect());
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            ClientHudRenderer.render(context);
            ClientWorldMarkerRenderer.renderOverlay(context);
        });
        WorldRenderEvents.AFTER_ENTITIES.register(context -> ClientWorldMarkerRenderer.render(context.camera()));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            TeamGlowingClientCommand.register(dispatcher, source -> Minecraft.getInstance(), (source, feedback) -> source.sendFeedback(feedback))
        );
    }

    private static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(PartyTabMessage.ID, (payload, context) ->
            context.client().execute(() -> PartyTabMessageHandler.handle(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(TabOverlayConfigMessage.ID, (payload, context) ->
            context.client().execute(() -> TabOverlayConfigMessageHandler.handle(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(TeamLocatorMessage.ID, (payload, context) ->
            context.client().execute(() -> TeamLocatorMessageHandler.handle(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(TeammateWorldMarkerMessage.ID, (payload, context) ->
            context.client().execute(() -> TeammateWorldMarkerMessageHandler.handle(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(ClientBannerVisibilityMessage.ID, (payload, context) ->
            context.client().execute(() -> ClientBannerVisibilityMessageHandler.handle(payload))
        );
    }
}
