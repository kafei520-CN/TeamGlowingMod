package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.network.PartyTabEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClientPartyTabCache {
    private static String ownPartyName = "";
    private static int ownPartyColor = 0xFFFFFF;
    private static Map<String, String> partyByPlayerId = Map.of();
    private static Map<String, String> partyByPlayerName = Map.of();
    private static Map<String, Integer> partyColorByPlayerId = Map.of();
    private static Map<String, Integer> partyColorByPlayerName = Map.of();
    private static Map<String, Integer> partyColorByPartyName = Map.of();
    private static Map<String, Integer> sortOrderByPlayerId = Map.of();
    private static Map<String, Integer> sortOrderByPlayerName = Map.of();

    private ClientPartyTabCache() {
    }

    public static void update(String updatedPartyName, List<PartyTabEntry> entries) {
        Map<String, String> ids = new HashMap<>();
        Map<String, String> names = new HashMap<>();
        Map<String, Integer> colorsById = new HashMap<>();
        Map<String, Integer> colorsByName = new HashMap<>();
        Map<String, Integer> colorsByPartyName = new HashMap<>();
        Map<String, Integer> sortOrdersById = new HashMap<>();
        Map<String, Integer> sortOrdersByName = new HashMap<>();
        for (PartyTabEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            String partyName = entry.partyName() == null ? "" : entry.partyName();
            int partyColor = entry.partyColor() & 0xFFFFFF;
            if (entry.playerId() != null && !entry.playerId().isBlank()) {
                ids.put(entry.playerId(), partyName);
                colorsById.put(entry.playerId(), Integer.valueOf(partyColor));
                sortOrdersById.put(entry.playerId(), Integer.valueOf(entry.sortOrder()));
            }
            if (entry.playerName() != null && !entry.playerName().isBlank()) {
                String loweredName = entry.playerName().toLowerCase(Locale.ROOT);
                names.put(loweredName, partyName);
                colorsByName.put(loweredName, Integer.valueOf(partyColor));
                sortOrdersByName.put(loweredName, Integer.valueOf(entry.sortOrder()));
            }
            if (!partyName.isBlank()) {
                colorsByPartyName.put(partyName, Integer.valueOf(partyColor));
            }
        }

        ownPartyName = updatedPartyName == null ? "" : updatedPartyName;
        ownPartyColor = colorsByPartyName.getOrDefault(ownPartyName, Integer.valueOf(0xFFFFFF)).intValue();
        partyByPlayerId = Map.copyOf(ids);
        partyByPlayerName = Map.copyOf(names);
        partyColorByPlayerId = Map.copyOf(colorsById);
        partyColorByPlayerName = Map.copyOf(colorsByName);
        partyColorByPartyName = Map.copyOf(colorsByPartyName);
        sortOrderByPlayerId = Map.copyOf(sortOrdersById);
        sortOrderByPlayerName = Map.copyOf(sortOrdersByName);
    }

    public static void clear() {
        ownPartyName = "";
        ownPartyColor = 0xFFFFFF;
        partyByPlayerId = Map.of();
        partyByPlayerName = Map.of();
        partyColorByPlayerId = Map.of();
        partyColorByPlayerName = Map.of();
        partyColorByPartyName = Map.of();
        sortOrderByPlayerId = Map.of();
        sortOrderByPlayerName = Map.of();
    }

    public static boolean hasEntries() {
        return !partyByPlayerId.isEmpty() || !partyByPlayerName.isEmpty();
    }

    public static String getOwnPartyName() {
        return ownPartyName;
    }

    public static int getOwnPartyColor() {
        return ownPartyColor;
    }

    public static String getPartyName(String playerId, String playerName) {
        if (playerId != null) {
            String partyName = partyByPlayerId.get(playerId);
            if (partyName != null) {
                return partyName;
            }
        }
        if (playerName == null) {
            return "";
        }
        return partyByPlayerName.getOrDefault(playerName.toLowerCase(Locale.ROOT), "");
    }

    public static boolean hasParty(String playerId, String playerName) {
        return !getPartyName(playerId, playerName).isBlank();
    }

    public static int getPartyColor(String playerId, String playerName) {
        if (playerId != null) {
            Integer color = partyColorByPlayerId.get(playerId);
            if (color != null) {
                return color.intValue();
            }
        }
        if (playerName == null) {
            return 0xFFFFFF;
        }
        return partyColorByPlayerName.getOrDefault(playerName.toLowerCase(Locale.ROOT), Integer.valueOf(0xFFFFFF)).intValue();
    }

    public static int getPartyColorByPartyName(String partyName) {
        if (partyName == null || partyName.isBlank()) {
            return 0xFFFFFF;
        }
        return partyColorByPartyName.getOrDefault(partyName, Integer.valueOf(0xFFFFFF)).intValue();
    }

    public static boolean isSameParty(String playerId, String playerName) {
        String partyName = getPartyName(playerId, playerName);
        return !partyName.isBlank() && partyName.equals(ownPartyName);
    }

    public static int getSortOrder(String playerId, String playerName) {
        if (playerId != null) {
            Integer sortOrder = sortOrderByPlayerId.get(playerId);
            if (sortOrder != null) {
                return sortOrder.intValue();
            }
        }
        if (playerName == null) {
            return Integer.MAX_VALUE;
        }
        return sortOrderByPlayerName.getOrDefault(playerName.toLowerCase(Locale.ROOT), Integer.valueOf(Integer.MAX_VALUE)).intValue();
    }
}
