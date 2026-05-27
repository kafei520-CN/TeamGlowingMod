package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.network.TeamGlowingNetwork;
import cn.kafei.TeamGlowing.network.TeamLocatorEntry;
import cn.kafei.TeamGlowing.network.TeamLocatorMessage;
import cn.kafei.TeamGlowing.party.BannerMarker;
import cn.kafei.TeamGlowing.party.BannerMarkerEntryIds;
import cn.kafei.TeamGlowing.party.PartyManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class LocatorSyncService {
    private static final Set<Item> HIDDEN_HELMETS = Set.of(
        Blocks.CARVED_PUMPKIN.asItem(),
        Blocks.JACK_O_LANTERN.asItem(),
        Items.SKELETON_SKULL,
        Items.WITHER_SKELETON_SKULL,
        Items.ZOMBIE_HEAD,
        Items.CREEPER_HEAD,
        Items.DRAGON_HEAD,
        Items.PIGLIN_HEAD,
        Items.PLAYER_HEAD
    );

    public void syncToPlayer(ServerPlayer player, PartyManager partyManager) {
        if (player.getServer() != null) {
            partyManager.pruneMissingBanners(player.getServer());
        }

        List<ServerPlayer> teammates = partyManager.getOnlineTeammates(player);
        Map<String, TeamLocatorEntry> uniqueEntries = new LinkedHashMap<>();
        for (BannerMarker marker : partyManager.getLocatorBanners(player.getGameProfile().getName())) {
            this.addBannerEntry(player, uniqueEntries, BannerMarkerEntryIds.locatorBanner(marker), marker, false);
        }
        BannerMarker partyBanner = partyManager.getPartyBanner(player.getGameProfile().getName());
        if (partyBanner != null) {
            this.addBannerEntry(player, uniqueEntries, BannerMarkerEntryIds.partyBanner(partyBanner), partyBanner, true);
        }
        for (ServerPlayer teammate : teammates) {
            if (this.shouldHideLocator(teammate)) {
                continue;
            }

            TeamLocatorEntry entry = this.createEntry(player, teammate);
            if (entry == null) {
                continue;
            }

            uniqueEntries.putIfAbsent(normalizePlayerKey(entry.playerId(), entry.playerName(), entry.name()), entry);
        }

        List<TeamLocatorEntry> entries = new ArrayList<>(uniqueEntries.values());
        TeamGlowingNetwork.sendTo(player, new TeamLocatorMessage(entries));
    }

    private TeamLocatorEntry createEntry(ServerPlayer viewer, ServerPlayer teammate) {
        ResourceKey<Level> viewerDimension = viewer.level().dimension();
        ResourceKey<Level> teammateDimension = teammate.level().dimension();
        if (!canDisplayAcrossDimensions(viewerDimension, teammateDimension)) {
            return null;
        }

        double x = teammate.getX();
        double z = teammate.getZ();
        if (!viewerDimension.equals(teammateDimension)) {
            double scale = getCoordinateScale(teammateDimension, viewerDimension);
            x *= scale;
            z *= scale;
        }

        return new TeamLocatorEntry(
                teammate.getStringUUID(),
                teammate.getName().getString(),
                teammate.getGameProfile().getName(),
                teammate.getStringUUID(),
                teammateDimension.location().toString(),
                x,
                teammate.getY(),
                z,
                false,
                false,
                0
            );
    }

    private void addBannerEntry(ServerPlayer viewer, Map<String, TeamLocatorEntry> uniqueEntries, String entryId, BannerMarker marker, boolean partyBanner) {
        if (marker == null) {
            return;
        }
        ResourceKey<Level> viewerDimension = viewer.level().dimension();
        ResourceKey<Level> markerDimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(marker.dimensionId()));
        if (!canDisplayAcrossDimensions(viewerDimension, markerDimension)) {
            return;
        }
        double x = marker.x() + 0.5D;
        double z = marker.z() + 0.5D;
        if (!viewerDimension.equals(markerDimension)) {
            double scale = getCoordinateScale(markerDimension, viewerDimension);
            x *= scale;
            z *= scale;
        }
        TeamLocatorEntry entry = new TeamLocatorEntry(
            entryId,
            marker.name(),
            "",
            "",
            marker.dimensionId(),
            x,
            marker.y(),
            z,
            true,
            partyBanner,
            marker.bannerColorId()
        );
        uniqueEntries.putIfAbsent(entryId, entry);
    }

    private boolean shouldHideLocator(ServerPlayer player) {
        return player.isShiftKeyDown() || this.isWearingHiddenHelmet(player);
    }

    private boolean isWearingHiddenHelmet(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        return !helmet.isEmpty() && HIDDEN_HELMETS.contains(helmet.getItem());
    }

    private static String normalizePlayerKey(String playerId, String playerName, String fallbackName) {
        String value = playerId != null && !playerId.isBlank() ? playerId : (playerName != null && !playerName.isBlank() ? playerName : fallbackName);
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static boolean canDisplayAcrossDimensions(ResourceKey<Level> viewerDimension, ResourceKey<Level> teammateDimension) {
        if (viewerDimension.equals(teammateDimension)) {
            return true;
        }
        if (viewerDimension.equals(Level.END) || teammateDimension.equals(Level.END)) {
            return false;
        }
        return isOverworldNetherPair(viewerDimension, teammateDimension);
    }

    private static boolean isOverworldNetherPair(ResourceKey<Level> first, ResourceKey<Level> second) {
        return (first.equals(Level.OVERWORLD) && second.equals(Level.NETHER))
            || (first.equals(Level.NETHER) && second.equals(Level.OVERWORLD));
    }

    private static double getCoordinateScale(ResourceKey<Level> sourceDimension, ResourceKey<Level> targetDimension) {
        if (sourceDimension.equals(Level.NETHER) && targetDimension.equals(Level.OVERWORLD)) {
            return 8.0D;
        }
        if (sourceDimension.equals(Level.OVERWORLD) && targetDimension.equals(Level.NETHER)) {
            return 0.125D;
        }
        return 1.0D;
    }
}
