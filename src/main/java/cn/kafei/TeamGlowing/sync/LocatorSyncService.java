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
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

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

            TeamLocatorEntry entry = new TeamLocatorEntry(
                teammate.getName().getString(),
                teammate.getGameProfile().getName(),
                teammate.getUuidAsString(),
                teammate.getX(),
                teammate.getY(),
                teammate.getZ()
            );
            uniqueEntries.putIfAbsent(normalizePlayerKey(entry.playerId(), entry.playerName(), entry.name()), entry);
        }

        List<TeamLocatorEntry> entries = new ArrayList<>(uniqueEntries.values());
        TeamGlowingNetwork.sendTo(player, new TeamLocatorMessage(entries));
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
}
