package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.localization.Localization;
import cn.kafei.TeamGlowing.party.BannerMarker;
import cn.kafei.TeamGlowing.party.PartyManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class PartyRespawnService {
    private final PartyManager partyManager;
    private final Localization localization;
    private final Set<UUID> pendingPartyRespawns = ConcurrentHashMap.newKeySet();
    private final Map<UUID, QueuedRespawnTeleport> queuedTeleports = new ConcurrentHashMap<>();

    public PartyRespawnService(PartyManager partyManager, Localization localization) {
        this.partyManager = partyManager;
        this.localization = localization;
    }

    public void requestPartyRespawn(ServerPlayerEntity player) {
        if (player != null) {
            this.pendingPartyRespawns.add(player.getUuid());
        }
    }

    public void clear(ServerPlayerEntity player) {
        if (player != null) {
            this.pendingPartyRespawns.remove(player.getUuid());
            this.queuedTeleports.remove(player.getUuid());
        }
    }

    public void afterRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer) {
        if (oldPlayer == null || newPlayer == null || !this.pendingPartyRespawns.remove(oldPlayer.getUuid())) {
            return;
        }

        BannerMarker marker = this.partyManager.getOwnedPartyBanner(newPlayer.getGameProfile().getName());
        if (marker == null || newPlayer.getServer() == null) {
            newPlayer.sendMessage(Text.literal(this.localization.translate(newPlayer, "party.respawn.failed")), false);
            return;
        }

        RegistryKey<net.minecraft.world.World> worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(marker.dimensionId()));
        ServerWorld world = newPlayer.getServer().getWorld(worldKey);
        if (world == null) {
            newPlayer.sendMessage(Text.literal(this.localization.translate(newPlayer, "party.respawn.failed")), false);
            return;
        }

        Vec3d destination = findRespawnTarget(world, marker.toBlockPos());
        if (destination == null) {
            newPlayer.sendMessage(Text.literal(this.localization.translate(newPlayer, "party.respawn.failed")), false);
            return;
        }

        this.queuedTeleports.put(newPlayer.getUuid(), new QueuedRespawnTeleport(
            world.getRegistryKey(),
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
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) {
                iterator.remove();
                continue;
            }

            QueuedRespawnTeleport queued = entry.getValue();
            ServerWorld world = server.getWorld(queued.worldKey());
            if (world == null) {
                player.sendMessage(Text.literal(this.localization.translate(player, "party.respawn.failed")), false);
                iterator.remove();
                continue;
            }

            player.teleport(
                world,
                queued.destination().x,
                queued.destination().y,
                queued.destination().z,
                java.util.Set.<PositionFlag>of(),
                player.getYaw(),
                player.getPitch(),
                false
            );
            player.sendMessage(Text.literal(this.localization.translate(player, "party.respawn.completed", queued.bannerName())), false);
            iterator.remove();
        }
    }

    private static Vec3d findRespawnTarget(ServerWorld world, BlockPos bannerPos) {
        for (int radius = 0; radius <= 2; radius++) {
            for (int yOffset = -1; yOffset <= 1; yOffset++) {
                for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                    for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                        if (radius > 0 && Math.abs(xOffset) != radius && Math.abs(zOffset) != radius) {
                            continue;
                        }
                        BlockPos feetPos = bannerPos.add(xOffset, yOffset, zOffset);
                        if (feetPos.equals(bannerPos)) {
                            continue;
                        }
                        if (isSafeRespawnPos(world, feetPos)) {
                            return new Vec3d(feetPos.getX() + 0.5D, feetPos.getY(), feetPos.getZ() + 0.5D);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafeRespawnPos(ServerWorld world, BlockPos feetPos) {
        BlockPos headPos = feetPos.up();
        BlockPos groundPos = feetPos.down();
        return isPassable(world, feetPos) && isPassable(world, headPos) && isStandable(world, groundPos);
    }

    private static boolean isPassable(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getCollisionShape(world, pos).isEmpty() && world.getFluidState(pos).isEmpty();
    }

    private static boolean isStandable(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.getCollisionShape(world, pos).isEmpty() && world.getFluidState(pos).isEmpty();
    }

    private record QueuedRespawnTeleport(
        RegistryKey<net.minecraft.world.World> worldKey,
        Vec3d destination,
        String bannerName
    ) {
    }
}
