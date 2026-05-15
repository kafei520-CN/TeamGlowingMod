package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.network.PartyTabEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClientPartyTabCache {
    private static String ownPartyName = "";
    private static Map<String, String> partyByPlayerId = Map.of();
    private static Map<String, String> partyByPlayerName = Map.of();

    private ClientPartyTabCache() {
    }

    public static void update(String updatedPartyName, List<PartyTabEntry> entries) {
        Map<String, String> ids = new HashMap<>();
        Map<String, String> names = new HashMap<>();
        for (PartyTabEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            String partyName = entry.partyName() == null ? "" : entry.partyName();
            if (entry.playerId() != null && !entry.playerId().isBlank()) {
                ids.put(entry.playerId(), partyName);
            }
            if (entry.playerName() != null && !entry.playerName().isBlank()) {
                names.put(entry.playerName().toLowerCase(Locale.ROOT), partyName);
            }
        }

        ownPartyName = updatedPartyName == null ? "" : updatedPartyName;
        partyByPlayerId = Map.copyOf(ids);
        partyByPlayerName = Map.copyOf(names);
    }

    public static void clear() {
        ownPartyName = "";
        partyByPlayerId = Map.of();
        partyByPlayerName = Map.of();
    }

    public static boolean hasEntries() {
        return !partyByPlayerId.isEmpty() || !partyByPlayerName.isEmpty();
    }

    public static String getOwnPartyName() {
        return ownPartyName;
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

    public static boolean isSameParty(String playerId, String playerName) {
        String partyName = getPartyName(playerId, playerName);
        return !partyName.isBlank() && partyName.equals(ownPartyName);
    }
}
