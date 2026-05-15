package cn.kafei.TeamGlowing;

import cn.kafei.TeamGlowing.command.TeamGlowingPartyCommand;
import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import cn.kafei.TeamGlowing.localization.Localization;
import cn.kafei.TeamGlowing.network.TeamGlowingNetwork;
import cn.kafei.TeamGlowing.party.PartyManager;
import cn.kafei.TeamGlowing.persistence.PartyPersistence;
import cn.kafei.TeamGlowing.sync.GlowSyncService;
import cn.kafei.TeamGlowing.sync.LocatorSyncService;
import cn.kafei.TeamGlowing.sync.MarkerSyncService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;

public class TeamGlowingMod implements ModInitializer {
    private static final PartyManager PARTY_MANAGER = new PartyManager();
    private static final Localization LOCALIZATION = new Localization();
    private static final PartyPersistence PERSISTENCE = new PartyPersistence();
    private static final GlowSyncService GLOW_SYNC_SERVICE = new GlowSyncService();
    private static final LocatorSyncService LOCATOR_SYNC_SERVICE = new LocatorSyncService();
    private static final MarkerSyncService MARKER_SYNC_SERVICE = new MarkerSyncService();

    @Override
    public void onInitialize() {
        TeamGlowingNetwork.register();
        TeamGlowingNetwork.registerServerReceiver((payload, context) ->
            context.server().execute(() -> MARKER_SYNC_SERVICE.handleRequest(context.player(), payload))
        );
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            TeamGlowingPartyCommand.register(dispatcher, PARTY_MANAGER, LOCALIZATION, PERSISTENCE)
        );
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> PERSISTENCE.save(PARTY_MANAGER));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> this.onPlayerJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> MARKER_SYNC_SERVICE.clearMarker(handler.player.getUuid()));
        ServerTickEvents.END_SERVER_TICK.register(this::onEndServerTick);
        TeamGlowingConstants.LOGGER.info("{} initialized for Fabric {}", TeamGlowingConstants.NAME, TeamGlowingConstants.VERSION);
    }

    private void onServerStarted(MinecraftServer server) {
        PERSISTENCE.setSaveFilePath(server.getSavePath(WorldSavePath.ROOT).resolve("teamglowing-parties.json"));
        PERSISTENCE.load(PARTY_MANAGER);
    }

    private void onPlayerJoin(ServerPlayerEntity player) {
        GLOW_SYNC_SERVICE.syncSinglePlayer(player, PARTY_MANAGER);
        LOCATOR_SYNC_SERVICE.syncToPlayer(player, PARTY_MANAGER);
        MARKER_SYNC_SERVICE.syncToPlayer(player, PARTY_MANAGER);
    }

    private void onEndServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            GLOW_SYNC_SERVICE.syncVisibilityForPlayer(player, PARTY_MANAGER);
            LOCATOR_SYNC_SERVICE.syncToPlayer(player, PARTY_MANAGER);
            MARKER_SYNC_SERVICE.syncToPlayer(player, PARTY_MANAGER);
        }
    }
}
