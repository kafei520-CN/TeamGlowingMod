package cn.kafei.TeamGlowing.party;

import cn.kafei.TeamGlowing.persistence.PartySaveData;
import cn.kafei.TeamGlowing.persistence.SavedBannerMarker;
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
import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PartyManager {
    public static final int MAX_PARTY_NAME_CHARACTERS = 5;

    private final Map<String, Party> partiesByName = new HashMap<>();
    private final Map<String, String> playerPartyByName = new HashMap<>();
    private final Map<String, String> pendingInvites = new HashMap<>();
    private BannerMarker partyBannerMarker;
    private String partyBannerOwnerParty;
    private boolean partyBannerHidden;

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
        return this.createParty(leaderName, partyName, PartyColorHelper.DEFAULT_COLOR);
    }

    public Party createParty(String leaderName, String partyName, int color) {
        String leaderNameKey = normalizePlayerName(leaderName);
        String normalizedName = normalizePartyName(partyName);
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new IllegalArgumentException("party.error.empty_name");
        }
        if (countCodePoints(normalizedName) > MAX_PARTY_NAME_CHARACTERS) {
            throw new IllegalArgumentException("party.error.name_too_long");
        }

        if (this.isInParty(leaderNameKey)) {
            throw new IllegalStateException("party.error.already_in_party");
        }

        if (this.partiesByName.containsKey(normalizedName)) {
            throw new IllegalStateException("party.error.party_exists");
        }

        Party party = new Party(normalizedName, color, leaderNameKey, leaderName);
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

        if (!this.canInvite(party, leaderNameKey)) {
            throw new IllegalStateException("party.error.only_leader_invite");
        }

        if (this.isInParty(targetNameKey)) {
            throw new IllegalStateException("party.error.target_in_party");
        }

        party.playerNames.put(targetNameKey, targetName);
        this.pendingInvites.put(targetNameKey, party.name);
    }

    public void addAdmin(String leaderName, String targetName) {
        String leaderNameKey = normalizePlayerName(leaderName);
        String targetNameKey = normalizePlayerName(targetName);
        Party party = this.getPartyByPlayerName(leaderNameKey);
        if (party == null) {
            throw new IllegalStateException("party.error.not_in_party");
        }
        if (!party.leaderNameKey.equals(leaderNameKey)) {
            throw new IllegalStateException("party.error.only_leader_manage_admin");
        }
        if (!party.memberNameKeys.contains(targetNameKey)) {
            throw new IllegalStateException("party.error.target_not_in_party");
        }
        if (party.leaderNameKey.equals(targetNameKey)) {
            throw new IllegalStateException("party.error.leader_cannot_be_admin");
        }
        if (!party.adminNameKeys.add(targetNameKey)) {
            throw new IllegalStateException("party.error.already_admin");
        }
    }

    public void removeAdmin(String leaderName, String targetName) {
        String leaderNameKey = normalizePlayerName(leaderName);
        String targetNameKey = normalizePlayerName(targetName);
        Party party = this.getPartyByPlayerName(leaderNameKey);
        if (party == null) {
            throw new IllegalStateException("party.error.not_in_party");
        }
        if (!party.leaderNameKey.equals(leaderNameKey)) {
            throw new IllegalStateException("party.error.only_leader_manage_admin");
        }
        if (party.leaderNameKey.equals(targetNameKey)) {
            throw new IllegalStateException("party.error.leader_cannot_be_admin");
        }
        if (!party.adminNameKeys.remove(targetNameKey)) {
            throw new IllegalStateException("party.error.not_admin");
        }
    }

    public String renameParty(String leaderName, String newPartyName) {
        String leaderNameKey = normalizePlayerName(leaderName);
        Party party = this.getPartyByPlayerName(leaderNameKey);
        if (party == null) {
            throw new IllegalStateException("party.error.not_in_party");
        }
        if (!party.leaderNameKey.equals(leaderNameKey)) {
            throw new IllegalStateException("party.error.only_leader_manage_party");
        }

        String normalizedName = normalizePartyName(newPartyName);
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new IllegalArgumentException("party.error.empty_name");
        }
        if (countCodePoints(normalizedName) > MAX_PARTY_NAME_CHARACTERS) {
            throw new IllegalArgumentException("party.error.name_too_long");
        }
        if (!party.name.equals(normalizedName) && this.partiesByName.containsKey(normalizedName)) {
            throw new IllegalStateException("party.error.party_exists");
        }
        if (party.name.equals(normalizedName)) {
            throw new IllegalStateException("party.error.same_name");
        }

        String oldPartyName = party.name;
        this.partiesByName.remove(oldPartyName);
        party.name = normalizedName;
        this.partiesByName.put(normalizedName, party);

        for (String memberNameKey : party.memberNameKeys) {
            this.playerPartyByName.put(memberNameKey, normalizedName);
        }
        for (Map.Entry<String, String> entry : this.pendingInvites.entrySet()) {
            if (oldPartyName.equals(entry.getValue())) {
                entry.setValue(normalizedName);
            }
        }
        if (oldPartyName.equals(this.partyBannerOwnerParty)) {
            this.partyBannerOwnerParty = normalizedName;
        }
        return party.name;
    }

    public int setPartyColor(String leaderName, int color) {
        String leaderNameKey = normalizePlayerName(leaderName);
        Party party = this.getPartyByPlayerName(leaderNameKey);
        if (party == null) {
            throw new IllegalStateException("party.error.not_in_party");
        }
        if (!party.leaderNameKey.equals(leaderNameKey)) {
            throw new IllegalStateException("party.error.only_leader_manage_party");
        }

        int normalizedColor = PartyColorHelper.normalize(color);
        if (party.color == normalizedColor) {
            throw new IllegalStateException("party.error.same_color");
        }
        party.color = normalizedColor;
        return party.color;
    }

    public BannerMarker setPartyBanner(String playerName, BannerMarker marker) {
        Party party = this.getPartyByPlayerName(playerName);
        if (party == null) {
            throw new IllegalStateException("party.error.not_in_party");
        }
        this.partyBannerMarker = normalizeMarker(marker);
        this.partyBannerOwnerParty = party.name;
        this.partyBannerHidden = false;
        return this.partyBannerMarker;
    }

    public BannerMarker getPartyBanner(String playerName) {
        Party party = this.getPartyByPlayerName(playerName);
        if (party == null || this.partyBannerMarker == null || this.partyBannerHidden) {
            return null;
        }
        return party.name.equals(this.partyBannerOwnerParty) ? this.partyBannerMarker : null;
    }

    public BannerMarker setLocatorBanner(String playerName, BannerMarker marker) {
        Party party = this.getPartyByPlayerName(playerName);
        if (party == null) {
            throw new IllegalStateException("party.error.not_in_party");
        }
        BannerMarker normalizedMarker = normalizeMarker(marker);
        if (normalizedMarker == null) {
            throw new IllegalStateException("party.error.invalid_banner");
        }
        String markerKey = getMarkerKey(normalizedMarker);
        party.locatorMarkers.put(markerKey, normalizedMarker);
        party.hiddenLocatorMarkerKeys.remove(markerKey);
        party.locatorMarkersHidden = false;
        return normalizedMarker;
    }

    public List<BannerMarker> getLocatorBanners(String playerName) {
        Party party = this.getPartyByPlayerName(playerName);
        if (party == null || party.locatorMarkersHidden || party.locatorMarkers.isEmpty()) {
            return Collections.emptyList();
        }
        List<BannerMarker> markers = new ArrayList<>();
        for (Map.Entry<String, BannerMarker> entry : party.locatorMarkers.entrySet()) {
            if (!party.hiddenLocatorMarkerKeys.contains(entry.getKey())) {
                markers.add(entry.getValue());
            }
        }
        return markers;
    }

    public List<BannerMarker> getAllLocatorBanners(String playerName) {
        Party party = this.getPartyByPlayerName(playerName);
        if (party == null || party.locatorMarkers.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(party.locatorMarkers.values());
    }

    public BannerMarker getOwnedPartyBanner(String playerName) {
        Party party = this.getPartyByPlayerName(playerName);
        if (party == null || this.partyBannerMarker == null) {
            return null;
        }
        return party.name.equals(this.partyBannerOwnerParty) ? this.partyBannerMarker : null;
    }

    public BannerMarker findLocatorBannerForLodestone(String playerName, String dimensionId, BlockPos lodestonePos) {
        if (dimensionId == null || lodestonePos == null) {
            return null;
        }
        BlockPos bannerPos = lodestonePos.up();
        for (BannerMarker marker : this.getAllLocatorBanners(playerName)) {
            if (dimensionId.equals(marker.dimensionId())
                && marker.x() == bannerPos.getX()
                && marker.y() == bannerPos.getY()
                && marker.z() == bannerPos.getZ()) {
                return marker;
            }
        }
        return null;
    }

    public boolean isPartyBanner(BannerMarker marker) {
        return marker != null && this.partyBannerMarker != null && getMarkerKey(marker).equals(getMarkerKey(this.partyBannerMarker));
    }

    public boolean isTrackedLocatorBanner(String playerName, BannerMarker marker) {
        Party party = this.getPartyByPlayerName(playerName);
        return party != null && marker != null && party.locatorMarkers.containsKey(getMarkerKey(marker));
    }

    public boolean togglePartyBannerHidden(String playerName) {
        Party party = this.getPartyByPlayerName(playerName);
        if (party == null || this.partyBannerMarker == null || !party.name.equals(this.partyBannerOwnerParty)) {
            return false;
        }
        this.partyBannerHidden = !this.partyBannerHidden;
        return this.partyBannerHidden;
    }

    public boolean toggleAllLocatorBannersHidden(String playerName) {
        Party party = this.getPartyByPlayerName(playerName);
        if (party == null || party.locatorMarkers.isEmpty()) {
            return false;
        }
        party.locatorMarkersHidden = !party.locatorMarkersHidden;
        return party.locatorMarkersHidden;
    }

    public boolean toggleLocatorBannerHidden(String playerName, BannerMarker marker) {
        Party party = this.getPartyByPlayerName(playerName);
        if (party == null || marker == null) {
            return false;
        }
        String markerKey = getMarkerKey(marker);
        if (!party.locatorMarkers.containsKey(markerKey)) {
            return false;
        }
        party.locatorMarkersHidden = false;
        if (party.hiddenLocatorMarkerKeys.contains(markerKey)) {
            party.hiddenLocatorMarkerKeys.remove(markerKey);
            return false;
        }
        party.hiddenLocatorMarkerKeys.add(markerKey);
        return true;
    }

    public boolean clearBrokenBanner(String dimensionId, BlockPos pos) {
        boolean changed = false;
        if (matchesMarker(this.partyBannerMarker, dimensionId, pos)) {
            this.partyBannerMarker = null;
            this.partyBannerOwnerParty = null;
            this.partyBannerHidden = false;
            changed = true;
        }
        for (Party party : this.partiesByName.values()) {
            changed |= removeLocatorMarker(party, dimensionId, pos);
        }
        return changed;
    }

    public boolean pruneMissingBanners(MinecraftServer server) {
        boolean changed = false;
        if (this.partyBannerMarker != null && !isBannerPresent(server, this.partyBannerMarker)) {
            this.partyBannerMarker = null;
            this.partyBannerOwnerParty = null;
            this.partyBannerHidden = false;
            changed = true;
        }
        for (Party party : this.partiesByName.values()) {
            List<String> missingKeys = new ArrayList<>();
            for (Map.Entry<String, BannerMarker> entry : party.locatorMarkers.entrySet()) {
                if (!isBannerPresent(server, entry.getValue())) {
                    missingKeys.add(entry.getKey());
                }
            }
            if (!missingKeys.isEmpty()) {
                changed = true;
                for (String missingKey : missingKeys) {
                    party.locatorMarkers.remove(missingKey);
                    party.hiddenLocatorMarkerKeys.remove(missingKey);
                }
            }
        }
        return changed;
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
        party.adminNameKeys.remove(playerNameKey);
        party.playerNames.remove(playerNameKey);

        if (party.memberNameKeys.isEmpty()) {
            if (party.name.equals(this.partyBannerOwnerParty)) {
                this.partyBannerMarker = null;
                this.partyBannerOwnerParty = null;
                this.partyBannerHidden = false;
            }
            this.partiesByName.remove(party.name);
            return new LeaveResult(party.name, true, null);
        }

        if (party.leaderNameKey.equals(playerNameKey)) {
            String nextLeaderNameKey = party.memberNameKeys.iterator().next();
            party.leaderNameKey = nextLeaderNameKey;
            party.adminNameKeys.remove(nextLeaderNameKey);
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
        List<String> adminNames = new ArrayList<>();
        for (String memberNameKey : party.memberNameKeys) {
            String name = party.playerNames.get(memberNameKey);
            memberNames.add(name == null ? memberNameKey : name);
        }
        for (String adminNameKey : party.adminNameKeys) {
            String name = party.playerNames.get(adminNameKey);
            adminNames.add(name == null ? adminNameKey : name);
        }
        Collections.sort(adminNames);
        Collections.sort(memberNames);

        String leaderName = party.playerNames.get(party.leaderNameKey);
        return new PartyInfo(party.name, party.color, leaderName == null ? party.leaderNameKey : leaderName, adminNames, memberNames);
    }

    public int getTabRoleOrder(String playerName) {
        Party party = this.getPartyByPlayerName(playerName);
        if (party == null) {
            return 3;
        }

        String playerNameKey = normalizePlayerName(playerName);
        if (party.leaderNameKey.equals(playerNameKey)) {
            return 0;
        }
        if (party.adminNameKeys.contains(playerNameKey)) {
            return 1;
        }
        return 2;
    }

    public Set<String> getKnownPartyNames() {
        return new HashSet<>(this.partiesByName.keySet());
    }

    public PartySaveData toSaveData() {
        PartySaveData saveData = new PartySaveData();
        saveData.partyBanner = toSavedMarker("party-banner", null, this.partyBannerMarker);
        saveData.partyBannerOwner = this.partyBannerOwnerParty;
        saveData.partyBannerHidden = this.partyBannerHidden;
        for (Party party : this.partiesByName.values()) {
            SavedParty savedParty = new SavedParty();
            savedParty.name = party.name;
            savedParty.color = PartyColorHelper.formatHex(party.color);
            savedParty.bannerMarker = toSavedMarker(null, null, party.bannerMarker);
            savedParty.hideAllLocatorMarkers = party.locatorMarkersHidden;
            savedParty.hiddenLocatorMarkerIds = new ArrayList<>(party.hiddenLocatorMarkerKeys);
            savedParty.leaderName = party.playerNames.get(party.leaderNameKey);
            savedParty.admins = new ArrayList<>(party.adminNameKeys);
            savedParty.members = new ArrayList<>();
            for (String memberNameKey : party.memberNameKeys) {
                SavedMember savedMember = new SavedMember();
                savedMember.playerName = party.playerNames.get(memberNameKey);
                savedMember.name = savedMember.playerName;
                savedParty.members.add(savedMember);
            }
            for (Map.Entry<String, BannerMarker> entry : party.locatorMarkers.entrySet()) {
                SavedBannerMarker savedMarker = toSavedMarker(entry.getKey(), null, entry.getValue());
                if (savedMarker != null) {
                    savedParty.locatorMarkers.add(savedMarker);
                }
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
        if (saveData == null || saveData.parties == null) {
            return;
        }

        this.partiesByName.clear();
        this.playerPartyByName.clear();
        this.pendingInvites.clear();
        this.partyBannerMarker = fromSavedMarker(saveData.partyBanner);
        this.partyBannerOwnerParty = normalizePartyName(saveData.partyBannerOwner);
        this.partyBannerHidden = saveData.partyBannerHidden;

        for (SavedParty savedParty : saveData.parties) {
            if (savedParty == null || savedParty.name == null) {
                continue;
            }

            String leaderName = resolveSavedName(savedParty.leaderName, savedParty.leaderId);
            if (leaderName == null) {
                continue;
            }

            String leaderNameKey = normalizePlayerName(leaderName);
            String normalizedPartyName = normalizePartyName(savedParty.name);
            Integer parsedColor = PartyColorHelper.parse(savedParty.color);
            
            Party party = new Party(normalizedPartyName, parsedColor == null ? PartyColorHelper.DEFAULT_COLOR : parsedColor.intValue(), leaderNameKey, leaderName);
            party.memberNameKeys.clear();
            party.adminNameKeys.clear();
            party.playerNames.clear();
            party.leaderNameKey = leaderNameKey;
            party.bannerMarker = fromSavedMarker(savedParty.bannerMarker);
            party.locatorMarkersHidden = savedParty.hideAllLocatorMarkers;

            if (savedParty.members != null) {
                for (SavedMember savedMember : savedParty.members) {
                    String memberName = savedMember == null ? null : resolveSavedName(savedMember.playerName, savedMember.name, savedMember.id);
                    if (memberName == null) {
                        continue;
                    }

                    String memberNameKey = normalizePlayerName(memberName);
                    party.memberNameKeys.add(memberNameKey);
                    party.playerNames.put(memberNameKey, memberName);
                    this.playerPartyByName.put(memberNameKey, normalizedPartyName);
                }
            }

            if (!party.memberNameKeys.contains(leaderNameKey)) {
                party.memberNameKeys.add(leaderNameKey);
                party.playerNames.put(leaderNameKey, leaderName);
                this.playerPartyByName.put(leaderNameKey, normalizedPartyName);
            }

            if (savedParty.admins != null) {
                for (String adminName : savedParty.admins) {
                    if (adminName == null || adminName.isBlank()) {
                        continue;
                    }
                    String adminNameKey = normalizePlayerName(adminName);
                    if (!adminNameKey.equals(leaderNameKey) && party.memberNameKeys.contains(adminNameKey)) {
                        party.adminNameKeys.add(adminNameKey);
                    }
                }
            }

            if (savedParty.locatorMarkers != null) {
                for (SavedBannerMarker savedMarker : savedParty.locatorMarkers) {
                    BannerMarker marker = fromSavedMarker(savedMarker);
                    if (marker == null) {
                        continue;
                    }
                    party.locatorMarkers.put(getSavedMarkerKey(savedMarker, marker), marker);
                }
            }
            if (savedParty.hiddenLocatorMarkerIds != null) {
                for (String hiddenKey : savedParty.hiddenLocatorMarkerIds) {
                    if (hiddenKey != null && !hiddenKey.isBlank() && party.locatorMarkers.containsKey(hiddenKey)) {
                        party.hiddenLocatorMarkerKeys.add(hiddenKey);
                    }
                }
            }

            this.partiesByName.put(normalizedPartyName, party);
            if (this.partyBannerMarker == null && party.bannerMarker != null) {
                this.partyBannerMarker = party.bannerMarker;
                this.partyBannerOwnerParty = normalizedPartyName;
            }
        }

        if (saveData.invites != null) {
            for (SavedInvite savedInvite : saveData.invites) {
                if (savedInvite == null || savedInvite.partyName == null) {
                    continue;
                }
                String playerName = resolveSavedName(savedInvite.playerName, savedInvite.playerId);
                if (playerName != null) {
                    this.pendingInvites.put(normalizePlayerName(playerName), normalizePartyName(savedInvite.partyName));
                }
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

    private boolean canInvite(Party party, String playerNameKey) {
        return party.leaderNameKey.equals(playerNameKey) || party.adminNameKeys.contains(playerNameKey);
    }

    public static String getPlayerName(ServerPlayerEntity player) {
        return player.getGameProfile().getName();
    }

    public static String getPlayerNameKey(ServerPlayerEntity player) {
        return normalizePlayerName(getPlayerName(player));
    }

    private static String normalizePartyName(String partyName) {
        if (partyName == null) {
            return null;
        }
        String stripped = partyName.strip();
        return stripped.isEmpty() ? "" : stripped.toLowerCase(Locale.ROOT);
    }

    private static int countCodePoints(String value) {
        return value == null ? 0 : value.codePointCount(0, value.length());
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

    private static BannerMarker normalizeMarker(BannerMarker marker) {
        if (marker == null || marker.dimensionId() == null || marker.dimensionId().isBlank()) {
            return null;
        }
        return new BannerMarker(
            normalizeMarkerName(marker.name()),
            marker.dimensionId(),
            marker.x(),
            marker.y(),
            marker.z(),
            Math.max(0, marker.bannerColorId())
        );
    }

    private static String normalizeMarkerName(String name) {
        if (name == null || name.isBlank()) {
            return "Banner";
        }
        String trimmed = name.trim();
        if (trimmed.startsWith("block.minecraft.") || trimmed.startsWith("item.minecraft.")) {
            return "Banner";
        }
        return trimmed;
    }

    private static SavedBannerMarker toSavedMarker(String id, String owner, BannerMarker marker) {
        BannerMarker normalizedMarker = normalizeMarker(marker);
        if (normalizedMarker == null) {
            return null;
        }
        SavedBannerMarker savedMarker = new SavedBannerMarker();
        savedMarker.id = id;
        savedMarker.owner = owner;
        savedMarker.name = normalizedMarker.name();
        savedMarker.dimensionId = normalizedMarker.dimensionId();
        savedMarker.x = normalizedMarker.x();
        savedMarker.y = normalizedMarker.y();
        savedMarker.z = normalizedMarker.z();
        savedMarker.bannerColorId = normalizedMarker.bannerColorId();
        return savedMarker;
    }

    private static BannerMarker fromSavedMarker(SavedBannerMarker savedMarker) {
        if (savedMarker == null || savedMarker.dimensionId == null || savedMarker.dimensionId.isBlank()) {
            return null;
        }
        return new BannerMarker(
            savedMarker.name == null || savedMarker.name.isBlank() ? "Banner" : savedMarker.name,
            savedMarker.dimensionId,
            savedMarker.x,
            savedMarker.y,
            savedMarker.z,
            savedMarker.bannerColorId
        );
    }

    private static boolean matchesMarker(BannerMarker marker, String dimensionId, BlockPos pos) {
        return marker != null
            && dimensionId != null
            && marker.dimensionId().equals(dimensionId)
            && pos != null
            && marker.x() == pos.getX()
            && marker.y() == pos.getY()
            && marker.z() == pos.getZ();
    }

    private static boolean removeLocatorMarker(Party party, String dimensionId, BlockPos pos) {
        List<String> removedKeys = new ArrayList<>();
        for (Map.Entry<String, BannerMarker> entry : party.locatorMarkers.entrySet()) {
            if (matchesMarker(entry.getValue(), dimensionId, pos)) {
                removedKeys.add(entry.getKey());
            }
        }
        if (removedKeys.isEmpty()) {
            return false;
        }
        for (String removedKey : removedKeys) {
            party.locatorMarkers.remove(removedKey);
            party.hiddenLocatorMarkerKeys.remove(removedKey);
        }
        return true;
    }

    private static String getSavedMarkerKey(SavedBannerMarker savedMarker, BannerMarker marker) {
        if (savedMarker != null && savedMarker.id != null && !savedMarker.id.isBlank()) {
            return savedMarker.id;
        }
        return getMarkerKey(marker);
    }

    public static String getMarkerKey(BannerMarker marker) {
        BannerMarker normalizedMarker = normalizeMarker(marker);
        if (normalizedMarker == null) {
            return "";
        }
        return normalizedMarker.dimensionId() + ":" + normalizedMarker.x() + ":" + normalizedMarker.y() + ":" + normalizedMarker.z();
    }

    private static boolean isBannerPresent(MinecraftServer server, BannerMarker marker) {
        if (server == null || marker == null || marker.dimensionId() == null || marker.dimensionId().isBlank()) {
            return false;
        }
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(marker.dimensionId()));
        ServerWorld world = server.getWorld(worldKey);
        if (world == null) {
            return false;
        }
        return world.getBlockState(marker.toBlockPos()).getBlock() instanceof AbstractBannerBlock;
    }
}
