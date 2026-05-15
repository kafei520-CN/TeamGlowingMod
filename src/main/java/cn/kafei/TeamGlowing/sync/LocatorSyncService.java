package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.network.TeamGlowingNetwork;
import cn.kafei.TeamGlowing.network.TeamLocatorEntry;
import cn.kafei.TeamGlowing.network.TeamLocatorMessage;
import cn.kafei.TeamGlowing.party.PartyManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.registry.RegistryKey;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

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

    public void syncToPlayer(ServerPlayerEntity player, PartyManager partyManager) {
        List<ServerPlayerEntity> teammates = partyManager.getOnlineTeammates(player);
        Map<String, TeamLocatorEntry> uniqueEntries = new LinkedHashMap<>();
        for (ServerPlayerEntity teammate : teammates) {
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

    private TeamLocatorEntry createEntry(ServerPlayerEntity viewer, ServerPlayerEntity teammate) {
        RegistryKey<World> viewerDimension = viewer.getWorld().getRegistryKey();
        RegistryKey<World> teammateDimension = teammate.getWorld().getRegistryKey();
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
                teammate.getName().getString(),
                teammate.getGameProfile().getName(),
                teammate.getUuidAsString(),
                teammateDimension.getValue().toString(),
                x,
                teammate.getY(),
                z
            );
    }

    private boolean shouldHideLocator(ServerPlayerEntity player) {
        return player.isSneaking() || this.isWearingHiddenHelmet(player);
    }

    private boolean isWearingHiddenHelmet(ServerPlayerEntity player) {
        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        return !helmet.isEmpty() && HIDDEN_HELMETS.contains(helmet.getItem());
    }

    private static String normalizePlayerKey(String playerId, String playerName, String fallbackName) {
        String value = playerId != null && !playerId.isBlank() ? playerId : (playerName != null && !playerName.isBlank() ? playerName : fallbackName);
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static boolean canDisplayAcrossDimensions(RegistryKey<World> viewerDimension, RegistryKey<World> teammateDimension) {
        if (viewerDimension.equals(teammateDimension)) {
            return true;
        }
        if (viewerDimension.equals(World.END) || teammateDimension.equals(World.END)) {
            return false;
        }
        return isOverworldNetherPair(viewerDimension, teammateDimension);
    }

    private static boolean isOverworldNetherPair(RegistryKey<World> first, RegistryKey<World> second) {
        return (first.equals(World.OVERWORLD) && second.equals(World.NETHER))
            || (first.equals(World.NETHER) && second.equals(World.OVERWORLD));
    }

    private static double getCoordinateScale(RegistryKey<World> sourceDimension, RegistryKey<World> targetDimension) {
        if (sourceDimension.equals(World.NETHER) && targetDimension.equals(World.OVERWORLD)) {
            return 8.0D;
        }
        if (sourceDimension.equals(World.OVERWORLD) && targetDimension.equals(World.NETHER)) {
            return 0.125D;
        }
        return 1.0D;
    }
}
