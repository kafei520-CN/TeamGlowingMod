package cn.kafei.TeamGlowing.neoforge;

import cn.kafei.TeamGlowing.TeamGlowingCommon;
import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@SuppressWarnings("null")
@Mod(TeamGlowingConstants.MODID)
public final class TeamGlowingNeoForgeMod {
    public TeamGlowingNeoForgeMod(IEventBus modBus) {
        TeamGlowingCommon.initialize(NeoForgeTeamGlowingPlatform.INSTANCE, "NeoForge");
        modBus.addListener(NeoForgeTeamGlowingNetwork::registerPayloads);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            TeamGlowingNeoForgeClient.register(modBus, NeoForge.EVENT_BUS);
        }
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(this::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(this::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(this::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(this::onServerTickPost);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        TeamGlowingCommon.registerCommands(event.getDispatcher());
    }

    private void onServerStarted(ServerStartedEvent event) {
        TeamGlowingCommon.onServerStarted(event.getServer());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        TeamGlowingCommon.onServerStopping(event.getServer());
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeamGlowingCommon.onPlayerJoin(player);
        }
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeamGlowingCommon.onPlayerDisconnect(player);
        }
    }

    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeamGlowingCommon.afterRespawn(player, player);
        }
    }

    private void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        InteractionResult result = TeamGlowingCommon.handleUseBlock(player, event.getLevel(), event.getHand(), event.getHitVec());
        applyInteractionResult(event, result);
    }

    private void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        InteractionResult result = TeamGlowingCommon.handleUseItem(player, event.getLevel(), event.getHand());
        applyInteractionResult(event, result);
    }

    private void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level world) {
            TeamGlowingCommon.handleBlockBreak(world, event.getPos(), event.getState(), null);
        }
    }

    private void onServerTickPost(ServerTickEvent.Post event) {
        TeamGlowingCommon.onEndServerTick(event.getServer());
    }

    private static void applyInteractionResult(PlayerInteractEvent event, InteractionResult result) {
        if (result == InteractionResult.PASS) {
            return;
        }
        if (event instanceof PlayerInteractEvent.RightClickBlock rightClickBlock) {
            rightClickBlock.setCancellationResult(result);
            rightClickBlock.setCanceled(true);
            return;
        }
        if (event instanceof PlayerInteractEvent.RightClickItem rightClickItem) {
            rightClickItem.setCancellationResult(result);
            rightClickItem.setCanceled(true);
        }
    }
}
