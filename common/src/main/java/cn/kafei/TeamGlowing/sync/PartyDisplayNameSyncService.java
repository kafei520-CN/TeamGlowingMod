package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.party.PartyInfo;
import cn.kafei.TeamGlowing.party.PartyManager;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public final class PartyDisplayNameSyncService {
    private static final String DISPLAY_TEAM_PREFIX = "tgd_";
    private static final String NO_TEAM = "__none__";
    private static final Map<String, String> PREVIOUS_TEAMS = new ConcurrentHashMap<>();

    public void syncPlayer(MinecraftServer server, ServerPlayer player, PartyManager partyManager) {
        PartyInfo partyInfo = partyManager.getPartyInfo(player.getGameProfile().getName());
        if (partyInfo == null || partyInfo.name == null || partyInfo.name.isBlank()) {
            this.releasePlayerDisplay(server, player);
            return;
        }

        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam displayTeam = this.ensureOwnedDisplayTeam(scoreboard, player);
        int color = partyInfo.color & 0xFFFFFF;
        displayTeam.setPlayerPrefix(Component.empty());
        displayTeam.setPlayerSuffix(Component.literal(" [" + partyInfo.name + "]").withStyle(style -> style.withColor(color)));
    }

    public void releasePlayerDisplay(MinecraftServer server, ServerPlayer player) {
        if (server == null || player == null) {
            return;
        }
        Scoreboard scoreboard = server.getScoreboard();
        String holder = this.getScoreHolder(player);
        PlayerTeam currentTeam = scoreboard.getPlayersTeam(holder);
        if (currentTeam != null && this.isModDisplayTeam(currentTeam.getName())) {
            scoreboard.removePlayerFromTeam(holder, currentTeam);
        }

        String previousTeamName = PREVIOUS_TEAMS.remove(holder);
        if (previousTeamName != null && !NO_TEAM.equals(previousTeamName)) {
            PlayerTeam previousTeam = scoreboard.getPlayerTeam(previousTeamName);
            if (previousTeam != null) {
                scoreboard.addPlayerToTeam(holder, previousTeam);
            }
        }
    }

    public void cleanup(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        for (PlayerTeam team : new ArrayList<>(scoreboard.getPlayerTeams())) {
            if (this.isModDisplayTeam(team.getName())) {
                scoreboard.removePlayerTeam(team);
            }
        }
        PREVIOUS_TEAMS.clear();
    }

    private PlayerTeam ensureOwnedDisplayTeam(Scoreboard scoreboard, ServerPlayer player) {
        String holder = this.getScoreHolder(player);
        PlayerTeam currentTeam = scoreboard.getPlayersTeam(holder);
        PlayerTeam expectedTeam = scoreboard.getPlayerTeam(this.getDisplayTeamId(player));
        if (expectedTeam == null) {
            expectedTeam = scoreboard.addPlayerTeam(this.getDisplayTeamId(player));
            expectedTeam.setNameTagVisibility(Team.Visibility.ALWAYS);
            expectedTeam.setPlayerPrefix(Component.empty());
            expectedTeam.setPlayerSuffix(Component.empty());
        }

        if (currentTeam == expectedTeam) {
            return expectedTeam;
        }

        if (currentTeam != null && !this.isModDisplayTeam(currentTeam.getName())) {
            PREVIOUS_TEAMS.put(holder, currentTeam.getName());
        } else if (!PREVIOUS_TEAMS.containsKey(holder)) {
            PREVIOUS_TEAMS.put(holder, NO_TEAM);
        }

        if (currentTeam != null) {
            scoreboard.removePlayerFromTeam(holder, currentTeam);
        }
        scoreboard.addPlayerToTeam(holder, expectedTeam);
        return expectedTeam;
    }

    private String getDisplayTeamId(ServerPlayer player) {
        long mixed = player.getUUID().getMostSignificantBits() ^ player.getUUID().getLeastSignificantBits();
        String hex = Long.toHexString(mixed).toLowerCase();
        if (hex.length() > 12) {
            hex = hex.substring(0, 12);
        }
        String id = DISPLAY_TEAM_PREFIX + hex;
        return id.length() > 16 ? id.substring(0, 16) : id;
    }

    private String getScoreHolder(ServerPlayer player) {
        return player.getScoreboardName();
    }

    private boolean isModDisplayTeam(String teamName) {
        return teamName != null && teamName.startsWith(DISPLAY_TEAM_PREFIX);
    }
}
