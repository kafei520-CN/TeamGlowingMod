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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MarkerSyncService {
    private static final long MARKER_DURATION_MILLIS = 60_000L;
    private final Map<UUID, ServerSharedMarkerState> markersByOwner = new ConcurrentHashMap<>();

    public void handleRequest(ServerPlayer player, SetSharedMarkerRequest request) {
        if (player == null || player.getServer() == null) {
            return;
        }

        if (request.kind() == SharedMarkerKind.CLEAR) {
            this.clearMarker(player.getUUID());
            return;
        }

        if (request.kind() == SharedMarkerKind.ITEM) {
            this.setItemMarker(player, request);
            return;
        }

        if (request.kind() == SharedMarkerKind.ENTITY) {
            this.setEntityMarker(player, request);
            return;
        }

        this.setWaypoint(player, request);
    }

    public void clearMarker(UUID ownerId) {
        if (ownerId != null) {
            this.markersByOwner.remove(ownerId);
        }
    }

    public void syncToPlayer(ServerPlayer viewer, PartyManager partyManager) {
        long now = System.currentTimeMillis();
        this.markersByOwner.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);

        List<TeammateWorldMarkerEntry> entries = new ArrayList<>();
        this.addMarkerEntry(entries, viewer, this.markersByOwner.get(viewer.getUUID()));
        for (ServerPlayer teammate : partyManager.getOnlineTeammates(viewer)) {
            ServerSharedMarkerState state = this.markersByOwner.get(teammate.getUUID());
            this.addMarkerEntry(entries, viewer, state);
        }
        TeamGlowingNetwork.sendTo(viewer, new TeammateWorldMarkerMessage(entries));
    }

    private void addMarkerEntry(List<TeammateWorldMarkerEntry> entries, ServerPlayer viewer, ServerSharedMarkerState state) {
        if (state == null) {
            return;
        }

        TeammateWorldMarkerEntry entry = this.resolveEntry(viewer, state);
        if (entry != null) {
            entries.add(entry);
        }
    }

    private void setWaypoint(ServerPlayer player, SetSharedMarkerRequest request) {
        long expiresAtMillis = System.currentTimeMillis() + MARKER_DURATION_MILLIS;
        this.markersByOwner.put(player.getUUID(), new ServerSharedMarkerState(
            player.getName().getString(),
            player.getGameProfile().getName(),
            player.getStringUUID(),
            SharedMarkerKind.WAYPOINT,
            request.dimensionId(),
            request.x(),
            request.y(),
            request.z(),
            null,
            expiresAtMillis
        ));
    }

    private void setItemMarker(ServerPlayer player, SetSharedMarkerRequest request) {
        ServerLevel world = resolveWorld(player, request.dimensionId());
        if (world == null) {
            return;
        }

        UUID targetEntityId;
        try {
            targetEntityId = UUID.fromString(request.targetEntityId());
        } catch (IllegalArgumentException exception) {
            return;
        }

        Entity entity = ServerEntityLookupCompat.getEntity(world, targetEntityId);
        if (!(entity instanceof ItemEntity itemEntity)) {
            return;
        }

        long expiresAtMillis = System.currentTimeMillis() + MARKER_DURATION_MILLIS;
        this.markersByOwner.put(player.getUUID(), new ServerSharedMarkerState(
            player.getName().getString(),
            player.getGameProfile().getName(),
            player.getStringUUID(),
            SharedMarkerKind.ITEM,
            request.dimensionId(),
            itemEntity.getX(),
            itemEntity.getY(),
            itemEntity.getZ(),
            targetEntityId,
            expiresAtMillis
        ));
    }

    private void setEntityMarker(ServerPlayer player, SetSharedMarkerRequest request) {
        ServerLevel world = resolveWorld(player, request.dimensionId());
        if (world == null) {
            return;
        }

        UUID targetEntityId;
        try {
            targetEntityId = UUID.fromString(request.targetEntityId());
        } catch (IllegalArgumentException exception) {
            return;
        }

        Entity entity = ServerEntityLookupCompat.getEntity(world, targetEntityId);
        if (!isTrackableEntity(entity)) {
            return;
        }

        long expiresAtMillis = System.currentTimeMillis() + MARKER_DURATION_MILLIS;
        this.markersByOwner.put(player.getUUID(), new ServerSharedMarkerState(
            player.getName().getString(),
            player.getGameProfile().getName(),
            player.getStringUUID(),
            SharedMarkerKind.ENTITY,
            request.dimensionId(),
            entity.getX(),
            entity.getY(),
            entity.getZ(),
            targetEntityId,
            expiresAtMillis
        ));
    }

    private TeammateWorldMarkerEntry resolveEntry(ServerPlayer viewer, ServerSharedMarkerState state) {
        if (viewer.level() == null) {
            return null;
        }
        if (!viewer.level().dimension().location().toString().equals(state.dimensionId())) {
            return null;
        }

        if (state.kind() == SharedMarkerKind.ITEM) {
            return this.resolveItemEntry(viewer, state);
        }

        if (state.kind() == SharedMarkerKind.ENTITY) {
            return this.resolveEntityEntry(viewer, state);
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
            "",
            false,
            state.expiresAtMillis()
        );
    }

    private TeammateWorldMarkerEntry resolveItemEntry(ServerPlayer viewer, ServerSharedMarkerState state) {
        ServerLevel world = resolveWorld(viewer, state.dimensionId());
        if (world == null || state.trackedEntityId() == null) {
            return null;
        }

        Entity entity = ServerEntityLookupCompat.getEntity(world, state.trackedEntityId());
        if (!(entity instanceof ItemEntity itemEntity)) {
            this.markersByOwner.remove(UUID.fromString(state.ownerPlayerId()));
            return null;
        }

        ItemStack stack = itemEntity.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
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
            "",
            false,
            state.expiresAtMillis()
        );
    }

    private TeammateWorldMarkerEntry resolveEntityEntry(ServerPlayer viewer, ServerSharedMarkerState state) {
        ServerLevel world = resolveWorld(viewer, state.dimensionId());
        if (world == null || state.trackedEntityId() == null) {
            return null;
        }

        Entity entity = ServerEntityLookupCompat.getEntity(world, state.trackedEntityId());
        if (!isTrackableEntity(entity)) {
            this.markersByOwner.remove(UUID.fromString(state.ownerPlayerId()));
            return null;
        }

        String label = entity instanceof Player playerEntity
            ? playerEntity.getDisplayName().getString()
            : entity.getType().getDescriptionId();
        boolean labelIsTranslationKey = !(entity instanceof Player);
        return new TeammateWorldMarkerEntry(
            state.ownerName(),
            state.ownerPlayerName(),
            state.ownerPlayerId(),
            SharedMarkerKind.ENTITY,
            state.dimensionId(),
            entity.getX(),
            entity.getY() + entity.getBbHeight() + 0.25D,
            entity.getZ(),
            "",
            label,
            labelIsTranslationKey,
            state.expiresAtMillis()
        );
    }

    private static boolean isTrackableEntity(Entity entity) {
        if (entity == null || entity.isSpectator() || !entity.isAlive()) {
            return false;
        }
        return entity instanceof Player || entity instanceof Mob;
    }

    private static ServerLevel resolveWorld(ServerPlayer player, String dimensionId) {
        if (player.getServer() == null || dimensionId == null || dimensionId.isBlank()) {
            return null;
        }
        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimensionId));
        return player.getServer().getLevel(worldKey);
    }
}
