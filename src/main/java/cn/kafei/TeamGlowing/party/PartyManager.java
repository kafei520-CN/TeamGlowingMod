package cn.kafei.TeamGlowing.party;

import cn.kafei.TeamGlowing.persistence.PartySaveData;
import cn.kafei.TeamGlowing.persistence.SavedInvite;
import cn.kafei.TeamGlowing.persistence.SavedMember;
import cn.kafei.TeamGlowing.persistence.SavedParty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.network.ServerPlayerEntity;

public class PartyManager {
    private final Map<String, Party> partiesByName = new HashMap<>();
    private final Map<String, String> playerPartyByName = new HashMap<>();
    private final Map<String, String> pendingInvites = new HashMap<>();

    public boolean areTeammates(String firstPlayerName, String secondPlayerName) {
        String firstParty = this.playerPartyByName.get(normalizePlayerName(firstPlayerName));
        if (firstParty == null) {
            return false;
        }
        return firstParty.equals(this.playerPartyByName.get(normalizePlayerName(secondPlayerName)));
    }

    public List<ServerPlayerEntity> getOnlineTeammates(ServerPlayerEntity player) {
        String playerNameKey = getPlayerNameKey(player);
        String partyName = this.playerPartyByName.get(playerNameKey);
        if (partyName == null || player.getServer() == null) {
            return Collections.emptyList();
        }

        List<ServerPlayerEntity> teammates = new ArrayList<>();
        for (ServerPlayerEntity onlinePlayer : player.getServer().getPlayerManager().getPlayerList()) {
            String onlinePlayerNameKey = getPlayerNameKey(onlinePlayer);
            if (!onlinePlayerNameKey.equals(playerNameKey) && partyName.equals(this.playerPartyByName.get(onlinePlayerNameKey))) {
                teammates.add(onlinePlayer);
            }
        }

        teammates.sort((first, second) -> Double.compare(player.squaredDistanceTo(first), player.squaredDistanceTo(second)));
        return teammates;
    }

    public Party createParty(String leaderName, String partyName) {
        String leaderNameKey = normalizePlayerName(leaderName);
        String normalizedName = normalizePartyName(partyName);
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new IllegalArgumentException("party.error.empty_name");
        }

        if (this.isInParty(leaderNameKey)) {
            throw new IllegalStateException("party.error.already_in_party");
        }

        if (this.partiesByName.containsKey(normalizedName)) {
            throw new IllegalStateException("party.error.party_exists");
        }

        Party party = new Party(normalizedName, leaderNameKey, leaderName);
        this.partiesByName.put(normalizedName, party);
        this.playerPartyByName.put(leaderNameKey, normalizedName);
        this.pendingInvites.remove(leaderNameKey);
        return party;
    }

    public void invite(String leaderName, String targetName) {
        String leaderNameKey = normalizePlayerName(leaderName);
        String targetNameKey = normalizePlayerName(targetName);
        Party party = this.getPartyByPlayerName(leaderNameKey);
        if (party == null) {
            throw new IllegalStateException("party.error.not_in_party");
        }

        if (!party.leaderNameKey.equals(leaderNameKey)) {
            throw new IllegalStateException("party.error.only_leader_invite");
        }

        if (this.isInParty(targetNameKey)) {
            throw new IllegalStateException("party.error.target_in_party");
        }

        party.playerNames.put(targetNameKey, targetName);
        this.pendingInvites.put(targetNameKey, party.name);
    }

    public String acceptInvite(String playerName) {
        String playerNameKey = normalizePlayerName(playerName);
        if (this.isInParty(playerNameKey)) {
            throw new IllegalStateException("party.error.already_in_party");
        }

        String partyName = this.pendingInvites.remove(playerNameKey);
        if (partyName == null) {
            throw new IllegalStateException("party.error.no_pending_invite");
        }

        Party party = this.partiesByName.get(partyName);
        if (party == null) {
            throw new IllegalStateException("party.error.party_missing");
        }

        party.memberNameKeys.add(playerNameKey);
        party.playerNames.put(playerNameKey, playerName);
        this.playerPartyByName.put(playerNameKey, party.name);
        return party.name;
    }

    public String rejectInvite(String playerName) {
        String playerNameKey = normalizePlayerName(playerName);
        String partyName = this.pendingInvites.remove(playerNameKey);
        if (partyName == null) {
            throw new IllegalStateException("party.error.no_pending_invite");
        }
        return partyName;
    }

    public LeaveResult leave(String playerName) {
        String playerNameKey = normalizePlayerName(playerName);
        Party party = this.getPartyByPlayerName(playerNameKey);
        if (party == null) {
            throw new IllegalStateException("party.error.not_in_party");
        }

        this.pendingInvites.remove(playerNameKey);
        this.playerPartyByName.remove(playerNameKey);
        party.memberNameKeys.remove(playerNameKey);
        party.playerNames.remove(playerNameKey);

        if (party.memberNameKeys.isEmpty()) {
            this.partiesByName.remove(party.name);
            return new LeaveResult(party.name, true, null);
        }

        if (party.leaderNameKey.equals(playerNameKey)) {
            String nextLeaderNameKey = party.memberNameKeys.iterator().next();
            party.leaderNameKey = nextLeaderNameKey;
            return new LeaveResult(party.name, false, party.playerNames.get(nextLeaderNameKey));
        }

        return new LeaveResult(party.name, false, null);
    }

    public PartyInfo getPartyInfo(String playerName) {
        Party party = this.getPartyByPlayerName(playerName);
        if (party == null) {
            return null;
        }

        List<String> memberNames = new ArrayList<>();
        for (String memberNameKey : party.memberNameKeys) {
            String name = party.playerNames.get(memberNameKey);
            memberNames.add(name == null ? memberNameKey : name);
        }
        Collections.sort(memberNames);

        String leaderName = party.playerNames.get(party.leaderNameKey);
        return new PartyInfo(party.name, leaderName == null ? party.leaderNameKey : leaderName, memberNames);
    }

    public Set<String> getKnownPartyNames() {
        return new HashSet<>(this.partiesByName.keySet());
    }

    public PartySaveData toSaveData() {
        PartySaveData saveData = new PartySaveData();
        for (Party party : this.partiesByName.values()) {
            SavedParty savedParty = new SavedParty();
            savedParty.name = party.name;
            savedParty.leaderName = party.playerNames.get(party.leaderNameKey);
            savedParty.members = new ArrayList<>();
            for (String memberNameKey : party.memberNameKeys) {
                SavedMember savedMember = new SavedMember();
                savedMember.playerName = party.playerNames.get(memberNameKey);
                savedMember.name = savedMember.playerName;
                savedParty.members.add(savedMember);
            }
            saveData.parties.add(savedParty);
        }

        for (Map.Entry<String, String> entry : this.pendingInvites.entrySet()) {
            SavedInvite invite = new SavedInvite();
            invite.playerName = this.findKnownPlayerName(entry.getKey());
            invite.partyName = entry.getValue();
            saveData.invites.add(invite);
        }
        return saveData;
    }

    public void load(PartySaveData saveData) {
        this.partiesByName.clear();
        this.playerPartyByName.clear();
        this.pendingInvites.clear();

        if (saveData == null) {
            return;
        }

        for (SavedParty savedParty : saveData.parties) {
            if (savedParty == null || savedParty.name == null) {
                continue;
            }

            String leaderName = resolveSavedName(savedParty.leaderName, savedParty.leaderId);
            if (leaderName == null) {
                continue;
            }

            String leaderNameKey = normalizePlayerName(leaderName);
            Party party = new Party(savedParty.name, leaderNameKey, leaderName);
            party.memberNameKeys.clear();
            party.playerNames.clear();
            party.leaderNameKey = leaderNameKey;

            for (SavedMember savedMember : savedParty.members == null ? Collections.<SavedMember>emptyList() : savedParty.members) {
                String memberName = savedMember == null ? null : resolveSavedName(savedMember.playerName, savedMember.name, savedMember.id);
                if (memberName == null) {
                    continue;
                }

                String memberNameKey = normalizePlayerName(memberName);
                party.memberNameKeys.add(memberNameKey);
                party.playerNames.put(memberNameKey, memberName);
                this.playerPartyByName.put(memberNameKey, savedParty.name);
            }

            if (!party.memberNameKeys.contains(leaderNameKey)) {
                party.memberNameKeys.add(leaderNameKey);
                party.playerNames.put(leaderNameKey, leaderName);
                this.playerPartyByName.put(leaderNameKey, savedParty.name);
            }

            this.partiesByName.put(savedParty.name, party);
        }

        for (SavedInvite savedInvite : saveData.invites) {
            if (savedInvite == null || savedInvite.partyName == null) {
                continue;
            }
            String playerName = resolveSavedName(savedInvite.playerName, savedInvite.playerId);
            if (playerName != null) {
                this.pendingInvites.put(normalizePlayerName(playerName), savedInvite.partyName);
            }
        }
    }

    private String findKnownPlayerName(String playerNameKey) {
        for (Party party : this.partiesByName.values()) {
            String playerName = party.playerNames.get(playerNameKey);
            if (playerName != null) {
                return playerName;
            }
        }
        return playerNameKey;
    }

    private boolean isInParty(String playerName) {
        return this.playerPartyByName.containsKey(normalizePlayerName(playerName));
    }

    private String getPartyName(String playerName) {
        return this.playerPartyByName.get(normalizePlayerName(playerName));
    }

    private Party getPartyByPlayerName(String playerName) {
        String partyName = this.getPartyName(playerName);
        return partyName == null ? null : this.partiesByName.get(partyName);
    }

    public static String getPlayerName(ServerPlayerEntity player) {
        return player.getGameProfile().getName();
    }

    public static String getPlayerNameKey(ServerPlayerEntity player) {
        return normalizePlayerName(getPlayerName(player));
    }

    private static String normalizePartyName(String partyName) {
        return partyName == null ? null : partyName.toLowerCase(Locale.ROOT);
    }

    private static String normalizePlayerName(String playerName) {
        return playerName == null ? null : playerName.toLowerCase(Locale.ROOT);
    }

    private static String resolveSavedName(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.trim().isEmpty()) {
                return candidate;
            }
        }
        return null;
    }
}
