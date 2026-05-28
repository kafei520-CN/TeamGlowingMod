package cn.kafei.TeamGlowing.neoforge;

import cn.kafei.TeamGlowing.client.ClientBootstrap;
import cn.kafei.TeamGlowing.client.ClientHudRenderer;
import cn.kafei.TeamGlowing.client.ClientMarkerController;
import cn.kafei.TeamGlowing.client.ClientWorldMarkerRenderer;
import cn.kafei.TeamGlowing.client.TeamGlowingClientCommand;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@SuppressWarnings("null")
public final class TeamGlowingNeoForgeClient {
    private static KeyMapping markerKeyBinding;

    private TeamGlowingNeoForgeClient() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(TeamGlowingNeoForgeClient::registerKeyMappings);
        gameBus.addListener(TeamGlowingNeoForgeClient::registerClientCommands);
        gameBus.addListener(TeamGlowingNeoForgeClient::onClientTickPost);
        gameBus.addListener(TeamGlowingNeoForgeClient::onRenderGuiPost);
        registerRenderLevelListener(gameBus);
        gameBus.addListener(TeamGlowingNeoForgeClient::onLoggingOut);
    }

    private static void registerRenderLevelListener(IEventBus gameBus) {
        if (Modifier.isAbstract(RenderLevelStageEvent.class.getModifiers())) {
            Class<? extends Event> afterEntitiesEvent = findRenderLevelStageEvent("AfterEntities");
            if (afterEntitiesEvent != null) {
                addRenderLevelListener(gameBus, afterEntitiesEvent);
                return;
            }
        }
        gameBus.addListener(TeamGlowingNeoForgeClient::onRenderLevelStage);
    }

    private static Class<? extends Event> findRenderLevelStageEvent(String eventName) {
        try {
            Class<?> eventClass = Class.forName(RenderLevelStageEvent.class.getName() + "$" + eventName);
            if (RenderLevelStageEvent.class.isAssignableFrom(eventClass) && Event.class.isAssignableFrom(eventClass)) {
                return eventClass.asSubclass(Event.class);
            }
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addRenderLevelListener(IEventBus gameBus, Class<? extends Event> eventClass) {
        Consumer listener = event -> onRenderAfterEntitiesCompat((RenderLevelStageEvent) event);
        gameBus.addListener((Class) eventClass, listener);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        markerKeyBinding = ClientMarkerController.createMarkKeyBinding();
        event.register(markerKeyBinding);
        ClientBootstrap.initialize(markerKeyBinding);
    }

    private static void registerClientCommands(RegisterClientCommandsEvent event) {
        TeamGlowingClientCommand.register(
            event.getDispatcher(),
            source -> Minecraft.getInstance(),
            (source, text) -> sendFeedback(text)
        );
    }

    private static void onClientTickPost(ClientTickEvent.Post event) {
        ClientBootstrap.onEndClientTick(Minecraft.getInstance());
    }

    private static void onRenderGuiPost(RenderGuiEvent.Post event) {
        ClientHudRenderer.render(event.getGuiGraphics());
        ClientWorldMarkerRenderer.renderOverlay(event.getGuiGraphics());
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            ClientWorldMarkerRenderer.render(event.getCamera());
        }
    }

    private static void onRenderAfterEntitiesCompat(RenderLevelStageEvent event) {
        ClientWorldMarkerRenderer.render(event.getCamera());
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBootstrap.onDisconnect();
    }

    private static void sendFeedback(Component text) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(text, false);
        }
    }
}
