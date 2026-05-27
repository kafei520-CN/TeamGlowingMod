package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.network.TeammateWorldMarkerEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClientWorldMarkerCache {
    private static List<TeammateWorldMarkerEntry> entries = List.of();

    private ClientWorldMarkerCache() {
    }

    public static void update(List<TeammateWorldMarkerEntry> updatedEntries) {
        long now = System.currentTimeMillis();
        Map<String, TeammateWorldMarkerEntry> uniqueEntries = new LinkedHashMap<>();
        for (TeammateWorldMarkerEntry entry : updatedEntries) {
            if (entry == null || isExpired(entry, now)) {
                continue;
            }
            uniqueEntries.putIfAbsent(normalizePlayerKey(entry), entry);
        }
        entries = List.copyOf(uniqueEntries.values());
    }

    public static List<TeammateWorldMarkerEntry> getEntries() {
        pruneExpired();
        return entries;
    }

    public static void clear() {
        entries = new ArrayList<>();
    }

    private static void pruneExpired() {
        long now = System.currentTimeMillis();
        List<TeammateWorldMarkerEntry> currentEntries = entries;
        if (currentEntries.isEmpty()) {
            return;
        }

        List<TeammateWorldMarkerEntry> filteredEntries = new ArrayList<>(currentEntries.size());
        boolean changed = false;
        for (TeammateWorldMarkerEntry entry : currentEntries) {
            if (isExpired(entry, now)) {
                changed = true;
                continue;
            }
            filteredEntries.add(entry);
        }

        if (changed) {
            entries = List.copyOf(filteredEntries);
        }
    }

    private static boolean isExpired(TeammateWorldMarkerEntry entry, long now) {
        return entry.expiresAtMillis() > 0L && entry.expiresAtMillis() <= now;
    }

    private static String normalizePlayerKey(TeammateWorldMarkerEntry entry) {
        String value = entry.playerId() != null && !entry.playerId().isBlank()
            ? entry.playerId()
            : (entry.playerName() != null && !entry.playerName().isBlank() ? entry.playerName() : entry.name());
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
