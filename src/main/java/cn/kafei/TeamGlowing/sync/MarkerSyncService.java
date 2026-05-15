package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.marker.ServerSharedMarkerState;
import cn.kafei.TeamGlowing.marker.SharedMarkerKind;
import cn.kafei.TeamGlowing.network.SetSharedMarkerRequest;
import cn.kafei.TeamGlowing.network.TeamGlowingNetwork;
import cn.kafei.TeamGlowing.network.TeammateWorldMarkerEntry;
import cn.kafei.TeamGlowing.network.TeammateWorldMarkerMessage;
import cn.kafei.TeamGlowing.party.PartyManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public final class MarkerSyncService {
    private static final long MARKER_DURATION_MILLIS = 60_000L;
    private final Map<UUID, ServerSharedMarkerState> markersByOwner = new ConcurrentHashMap<>();

    public void handleRequest(ServerPlayerEntity player, SetSharedMarkerRequest request) {
        if (player == null || player.getServer() == null) {
            return;
        }

        if (request.kind() == SharedMarkerKind.CLEAR) {
            this.clearMarker(player.getUuid());
            return;
        }

        if (request.kind() == SharedMarkerKind.ITEM) {
            this.setItemMarker(player, request);
            return;
        }

        this.setWaypoint(player, request);
    }

    public void clearMarker(UUID ownerId) {
        if (ownerId != null) {
            this.markersByOwner.remove(ownerId);
        }
    }

    public void syncToPlayer(ServerPlayerEntity viewer, PartyManager partyManager) {
        long now = System.currentTimeMillis();
        this.markersByOwner.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);

        List<TeammateWorldMarkerEntry> entries = new ArrayList<>();
        this.addMarkerEntry(entries, viewer, this.markersByOwner.get(viewer.getUuid()));
        for (ServerPlayerEntity teammate : partyManager.getOnlineTeammates(viewer)) {
            ServerSharedMarkerState state = this.markersByOwner.get(teammate.getUuid());
            this.addMarkerEntry(entries, viewer, state);
        }
        TeamGlowingNetwork.sendTo(viewer, new TeammateWorldMarkerMessage(entries));
    }

    private void addMarkerEntry(List<TeammateWorldMarkerEntry> entries, ServerPlayerEntity viewer, ServerSharedMarkerState state) {
        if (state == null) {
            return;
        }

        TeammateWorldMarkerEntry entry = this.resolveEntry(viewer, state);
        if (entry != null) {
            entries.add(entry);
        }
    }

    private void setWaypoint(ServerPlayerEntity player, SetSharedMarkerRequest request) {
        long expiresAtMillis = System.currentTimeMillis() + MARKER_DURATION_MILLIS;
        this.markersByOwner.put(player.getUuid(), new ServerSharedMarkerState(
            player.getName().getString(),
            player.getGameProfile().getName(),
            player.getUuidAsString(),
            SharedMarkerKind.WAYPOINT,
            request.dimensionId(),
            request.x(),
            request.y(),
            request.z(),
            null,
            expiresAtMillis
        ));
    }

    private void setItemMarker(ServerPlayerEntity player, SetSharedMarkerRequest request) {
        ServerWorld world = resolveWorld(player, request.dimensionId());
        if (world == null) {
            return;
        }

        UUID targetEntityId;
        try {
            targetEntityId = UUID.fromString(request.targetEntityId());
        } catch (IllegalArgumentException exception) {
            return;
        }

        Entity entity = world.getEntity(targetEntityId);
        if (!(entity instanceof ItemEntity itemEntity)) {
            return;
        }

        long expiresAtMillis = System.currentTimeMillis() + MARKER_DURATION_MILLIS;
        this.markersByOwner.put(player.getUuid(), new ServerSharedMarkerState(
            player.getName().getString(),
            player.getGameProfile().getName(),
            player.getUuidAsString(),
            SharedMarkerKind.ITEM,
            request.dimensionId(),
            itemEntity.getX(),
            itemEntity.getY(),
            itemEntity.getZ(),
            targetEntityId,
            expiresAtMillis
        ));
    }

    private TeammateWorldMarkerEntry resolveEntry(ServerPlayerEntity viewer, ServerSharedMarkerState state) {
        if (viewer.getWorld() == null) {
            return null;
        }
        if (!viewer.getWorld().getRegistryKey().getValue().toString().equals(state.dimensionId())) {
            return null;
        }

        if (state.kind() == SharedMarkerKind.ITEM) {
            return this.resolveItemEntry(viewer, state);
        }

        return new TeammateWorldMarkerEntry(
            state.ownerName(),
            state.ownerPlayerName(),
            state.ownerPlayerId(),
            SharedMarkerKind.WAYPOINT,
            state.dimensionId(),
            state.x(),
            state.y(),
            state.z(),
            "",
            state.expiresAtMillis()
        );
    }

    private TeammateWorldMarkerEntry resolveItemEntry(ServerPlayerEntity viewer, ServerSharedMarkerState state) {
        ServerWorld world = resolveWorld(viewer, state.dimensionId());
        if (world == null || state.trackedEntityId() == null) {
            return null;
        }

        Entity entity = world.getEntity(state.trackedEntityId());
        if (!(entity instanceof ItemEntity itemEntity)) {
            this.markersByOwner.remove(UUID.fromString(state.ownerPlayerId()));
            return null;
        }

        ItemStack stack = itemEntity.getStack();
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        return new TeammateWorldMarkerEntry(
            state.ownerName(),
            state.ownerPlayerName(),
            state.ownerPlayerId(),
            SharedMarkerKind.ITEM,
            state.dimensionId(),
            itemEntity.getX(),
            itemEntity.getY() + 0.25D,
            itemEntity.getZ(),
            itemId == null ? "" : itemId.toString(),
            state.expiresAtMillis()
        );
    }

    private static ServerWorld resolveWorld(ServerPlayerEntity player, String dimensionId) {
        if (player.getServer() == null || dimensionId == null || dimensionId.isBlank()) {
            return null;
        }
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(dimensionId));
        return player.getServer().getWorld(worldKey);
    }
}
