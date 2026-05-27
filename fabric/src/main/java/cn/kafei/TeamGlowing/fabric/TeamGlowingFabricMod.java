package cn.kafei.TeamGlowing.fabric;

import cn.kafei.TeamGlowing.TeamGlowingCommon;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;

public final class TeamGlowingFabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        TeamGlowingCommon.initialize(FabricTeamGlowingPlatform.INSTANCE, "Fabric");
        FabricTeamGlowingNetwork.registerPayloadTypes();
        FabricTeamGlowingNetwork.registerServerReceivers();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            TeamGlowingCommon.registerCommands(dispatcher)
        );
        ServerLifecycleEvents.SERVER_STARTED.register(TeamGlowingCommon::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(TeamGlowingCommon::onServerStopping);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> TeamGlowingCommon.onPlayerJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> TeamGlowingCommon.onPlayerDisconnect(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> TeamGlowingCommon.afterRespawn(oldPlayer, newPlayer));
        ServerTickEvents.END_SERVER_TICK.register(TeamGlowingCommon::onEndServerTick);

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
            player instanceof ServerPlayer serverPlayer
                ? TeamGlowingCommon.handleUseBlock(serverPlayer, world, hand, hitResult)
                : InteractionResult.PASS
        );
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            InteractionResult result = player instanceof ServerPlayer serverPlayer
                ? TeamGlowingCommon.handleUseItem(serverPlayer, world, hand)
                : InteractionResult.PASS;
            return new InteractionResultHolder<>(result, stack);
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) ->
            TeamGlowingCommon.handleBlockBreak(world, pos, state, blockEntity)
        );
    }
}
