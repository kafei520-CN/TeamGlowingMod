package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.network.TeamLocatorEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClientLocatorCache {
    private static List<TeamLocatorEntry> entries = List.of();

    private ClientLocatorCache() {
    }

    public static void update(List<TeamLocatorEntry> updatedEntries) {
        Map<String, TeamLocatorEntry> uniqueEntries = new LinkedHashMap<>();
        for (TeamLocatorEntry entry : updatedEntries) {
            uniqueEntries.putIfAbsent(normalizeEntryKey(entry), entry);
        }
        entries = List.copyOf(uniqueEntries.values());
    }

    public static List<TeamLocatorEntry> getEntries() {
        if (!ClientBannerVisibilityState.hasHiddenEntries()) {
            return entries;
        }
        return entries.stream()
            .filter(entry -> !ClientBannerVisibilityState.isHidden(entry))
            .toList();
    }

    public static boolean hasPartyBanner() {
        for (TeamLocatorEntry entry : getEntries()) {
            if (entry.banner() && entry.partyBanner()) {
                return true;
            }
        }
        return false;
    }

    public static void clear() {
        entries = new ArrayList<>();
    }

    private static String normalizeEntryKey(TeamLocatorEntry entry) {
        String value = entry.entryId() != null && !entry.entryId().isBlank()
            ? entry.entryId()
            : (entry.playerId() != null && !entry.playerId().isBlank()
            ? entry.playerId()
            : (entry.playerName() != null && !entry.playerName().isBlank() ? entry.playerName() : entry.name()));
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
