package cn.kafei.TeamGlowing;

import cn.kafei.TeamGlowing.command.TeamGlowingPartyCommand;
import cn.kafei.TeamGlowing.config.ServerTabOverlayConfig;
import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import cn.kafei.TeamGlowing.localization.Localization;
import cn.kafei.TeamGlowing.network.RequestPartyRespawnMessage;
import cn.kafei.TeamGlowing.network.SetSharedMarkerRequest;
import cn.kafei.TeamGlowing.party.PartyManager;
import cn.kafei.TeamGlowing.persistence.PartyPersistence;
import cn.kafei.TeamGlowing.platform.TeamGlowingPlatform;
import cn.kafei.TeamGlowing.platform.TeamGlowingPlatforms;
import cn.kafei.TeamGlowing.sync.BannerBindingService;
import cn.kafei.TeamGlowing.sync.GlowSyncService;
import cn.kafei.TeamGlowing.sync.LocatorSyncService;
import cn.kafei.TeamGlowing.sync.MarkerSyncService;
import cn.kafei.TeamGlowing.sync.PartyDisplayNameSyncService;
import cn.kafei.TeamGlowing.sync.PartyRespawnService;
import cn.kafei.TeamGlowing.sync.PartyTabSyncService;
import cn.kafei.TeamGlowing.sync.TabHeaderFooterSyncService;
import cn.kafei.TeamGlowing.sync.VanillaLocatorBarRuleService;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;

public final class TeamGlowingCommon {
    private static final PartyManager PARTY_MANAGER = new PartyManager();
    private static final Localization LOCALIZATION = new Localization();
    private static final PartyPersistence PERSISTENCE = new PartyPersistence();
    private static final GlowSyncService GLOW_SYNC_SERVICE = new GlowSyncService();
    private static final LocatorSyncService LOCATOR_SYNC_SERVICE = new LocatorSyncService();
    private static final MarkerSyncService MARKER_SYNC_SERVICE = new MarkerSyncService();
    private static final BannerBindingService BANNER_BINDING_SERVICE = new BannerBindingService(PARTY_MANAGER, LOCALIZATION, PERSISTENCE);
    private static final PartyRespawnService PARTY_RESPAWN_SERVICE = new PartyRespawnService(PARTY_MANAGER, LOCALIZATION);
    private static final PartyDisplayNameSyncService PARTY_DISPLAY_NAME_SYNC_SERVICE = new PartyDisplayNameSyncService();
    private static final PartyTabSyncService PARTY_TAB_SYNC_SERVICE = new PartyTabSyncService();
    private static final TabHeaderFooterSyncService TAB_HEADER_FOOTER_SYNC_SERVICE = new TabHeaderFooterSyncService();
    private static final VanillaLocatorBarRuleService VANILLA_LOCATOR_BAR_RULE_SERVICE = new VanillaLocatorBarRuleService();
    private static boolean initialized;

    private TeamGlowingCommon() {
    }

    public static synchronized void initialize(TeamGlowingPlatform platform, String loaderName) {
        TeamGlowingPlatforms.initialize(platform);
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTabOverlayConfig.load();
        TeamGlowingConstants.LOGGER.info("{} initialized for {}", TeamGlowingConstants.NAME, loaderName);
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        TeamGlowingPartyCommand.register(dispatcher, PARTY_MANAGER, LOCALIZATION, PERSISTENCE);
    }

    public static InteractionResult handleUseBlock(ServerPlayer player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        return BANNER_BINDING_SERVICE.handleUseBlock(player, world, hand, hitResult);
    }

    public static InteractionResult handleUseItem(ServerPlayer player, Level world, InteractionHand hand) {
        return BANNER_BINDING_SERVICE.handleUseItem(player, world, hand);
    }

    public static void handleBlockBreak(Level world, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (state != null && state.getBlock() instanceof AbstractBannerBlock) {
            BANNER_BINDING_SERVICE.handleBannerBroken(world, pos);
        }
    }

    public static void onServerStarted(MinecraftServer server) {
        VANILLA_LOCATOR_BAR_RULE_SERVICE.disableIfAvailable(server);
        PERSISTENCE.setSaveFilePath(server.getWorldPath(LevelResource.ROOT).resolve("teamglowing-parties.json"));
        TeamGlowingConstants.LOGGER.info("Loading party data from: {}", server.getWorldPath(LevelResource.ROOT).resolve("teamglowing-parties.json"));
        PERSISTENCE.load(PARTY_MANAGER);
        PARTY_DISPLAY_NAME_SYNC_SERVICE.cleanup(server);
    }

    public static void onServerStopping(MinecraftServer server) {
        PERSISTENCE.save(PARTY_MANAGER);
        PARTY_DISPLAY_NAME_SYNC_SERVICE.cleanup(server);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        TeamGlowingConstants.LOGGER.info("Player joined: {}, syncing party info", player.getGameProfile().getName());
        VANILLA_LOCATOR_BAR_RULE_SERVICE.disableIfAvailable(player.getServer());
        GLOW_SYNC_SERVICE.syncSinglePlayer(player, PARTY_MANAGER);
        syncPartyDisplayName(player);
        PARTY_TAB_SYNC_SERVICE.syncToPlayer(player, PARTY_MANAGER);
        TAB_HEADER_FOOTER_SYNC_SERVICE.syncToPlayer(player, PARTY_MANAGER);
        LOCATOR_SYNC_SERVICE.syncToPlayer(player, PARTY_MANAGER);
        MARKER_SYNC_SERVICE.syncToPlayer(player, PARTY_MANAGER);
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        if (player == null) {
            return;
        }
        MARKER_SYNC_SERVICE.clearMarker(player.getUUID());
        PARTY_RESPAWN_SERVICE.clear(player);
        TAB_HEADER_FOOTER_SYNC_SERVICE.clear(player);
        PARTY_DISPLAY_NAME_SYNC_SERVICE.releasePlayerDisplay(player.getServer(), player);
    }

    public static void afterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        PARTY_RESPAWN_SERVICE.afterRespawn(oldPlayer, newPlayer);
    }

    public static void onEndServerTick(MinecraftServer server) {
        BANNER_BINDING_SERVICE.tick(server);
        PARTY_RESPAWN_SERVICE.tick(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            GLOW_SYNC_SERVICE.syncVisibilityForPlayer(player, PARTY_MANAGER);
            syncPartyDisplayName(player);
            PARTY_TAB_SYNC_SERVICE.syncToPlayer(player, PARTY_MANAGER);
            TAB_HEADER_FOOTER_SYNC_SERVICE.syncToPlayer(player, PARTY_MANAGER);
            LOCATOR_SYNC_SERVICE.syncToPlayer(player, PARTY_MANAGER);
            MARKER_SYNC_SERVICE.syncToPlayer(player, PARTY_MANAGER);
        }
    }

    public static void handleSetSharedMarker(ServerPlayer player, SetSharedMarkerRequest payload) {
        MARKER_SYNC_SERVICE.handleRequest(player, payload);
    }

    public static void handlePartyRespawnRequest(ServerPlayer player, RequestPartyRespawnMessage payload) {
        PARTY_RESPAWN_SERVICE.requestPartyRespawn(player);
    }

    private static void syncPartyDisplayName(ServerPlayer player) {
        if (ServerTabOverlayConfig.get().enabled()) {
            PARTY_DISPLAY_NAME_SYNC_SERVICE.syncPlayer(player.getServer(), player, PARTY_MANAGER);
            return;
        }
        PARTY_DISPLAY_NAME_SYNC_SERVICE.releasePlayerDisplay(player.getServer(), player);
    }
}
