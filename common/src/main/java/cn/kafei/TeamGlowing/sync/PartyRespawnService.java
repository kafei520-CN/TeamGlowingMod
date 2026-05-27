package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.localization.Localization;
import cn.kafei.TeamGlowing.party.BannerMarker;
import cn.kafei.TeamGlowing.party.PartyManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class PartyRespawnService {
    private final PartyManager partyManager;
    private final Localization localization;
    private final Set<UUID> pendingPartyRespawns = ConcurrentHashMap.newKeySet();
    private final Map<UUID, QueuedRespawnTeleport> queuedTeleports = new ConcurrentHashMap<>();

    public PartyRespawnService(PartyManager partyManager, Localization localization) {
        this.partyManager = partyManager;
        this.localization = localization;
    }

    public void requestPartyRespawn(ServerPlayer player) {
        if (player != null) {
            this.pendingPartyRespawns.add(player.getUUID());
        }
    }

    public void clear(ServerPlayer player) {
        if (player != null) {
            this.pendingPartyRespawns.remove(player.getUUID());
            this.queuedTeleports.remove(player.getUUID());
        }
    }

    public void afterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        if (oldPlayer == null || newPlayer == null || !this.pendingPartyRespawns.remove(oldPlayer.getUUID())) {
            return;
        }

        BannerMarker marker = this.partyManager.getOwnedPartyBanner(newPlayer.getGameProfile().getName());
        if (marker == null || newPlayer.getServer() == null) {
            newPlayer.displayClientMessage(Component.literal(this.localization.translate(newPlayer, "party.respawn.failed")), false);
            return;
        }

        ResourceKey<net.minecraft.world.level.Level> worldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(marker.dimensionId()));
        ServerLevel world = newPlayer.getServer().getLevel(worldKey);
        if (world == null) {
            newPlayer.displayClientMessage(Component.literal(this.localization.translate(newPlayer, "party.respawn.failed")), false);
            return;
        }

        Vec3 destination = findRespawnTarget(world, marker.toBlockPos());
        if (destination == null) {
            newPlayer.displayClientMessage(Component.literal(this.localization.translate(newPlayer, "party.respawn.failed")), false);
            return;
        }

        this.queuedTeleports.put(newPlayer.getUUID(), new QueuedRespawnTeleport(
            world.dimension(),
            destination,
            marker.name()
        ));
    }

    public void tick(MinecraftServer server) {
        if (server == null || this.queuedTeleports.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, QueuedRespawnTeleport>> iterator = this.queuedTeleports.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, QueuedRespawnTeleport> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) {
                iterator.remove();
                continue;
            }

            QueuedRespawnTeleport queued = entry.getValue();
            ServerLevel world = server.getLevel(queued.worldKey());
            if (world == null) {
                player.displayClientMessage(Component.literal(this.localization.translate(player, "party.respawn.failed")), false);
                iterator.remove();
                continue;
            }

            if (ServerTeleportCompat.teleport(player, world, queued.destination())) {
                player.displayClientMessage(Component.literal(this.localization.translate(player, "party.respawn.completed", queued.bannerName())), false);
            } else {
                player.displayClientMessage(Component.literal(this.localization.translate(player, "party.respawn.failed")), false);
            }
            iterator.remove();
        }
    }

    private static Vec3 findRespawnTarget(ServerLevel world, BlockPos bannerPos) {
        for (int radius = 0; radius <= 2; radius++) {
            for (int yOffset = -1; yOffset <= 1; yOffset++) {
                for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                    for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                        if (radius > 0 && Math.abs(xOffset) != radius && Math.abs(zOffset) != radius) {
                            continue;
                        }
                        BlockPos feetPos = bannerPos.offset(xOffset, yOffset, zOffset);
                        if (feetPos.equals(bannerPos)) {
                            continue;
                        }
                        if (isSafeRespawnPos(world, feetPos)) {
                            return new Vec3(feetPos.getX() + 0.5D, feetPos.getY(), feetPos.getZ() + 0.5D);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafeRespawnPos(ServerLevel world, BlockPos feetPos) {
        BlockPos headPos = feetPos.above();
        BlockPos groundPos = feetPos.below();
        return isPassable(world, feetPos) && isPassable(world, headPos) && isStandable(world, groundPos);
    }

    private static boolean isPassable(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getCollisionShape(world, pos).isEmpty() && world.getFluidState(pos).isEmpty();
    }

    private static boolean isStandable(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.getCollisionShape(world, pos).isEmpty() && world.getFluidState(pos).isEmpty();
    }

    private record QueuedRespawnTeleport(
        ResourceKey<net.minecraft.world.level.Level> worldKey,
        Vec3 destination,
        String bannerName
    ) {
    }
}
