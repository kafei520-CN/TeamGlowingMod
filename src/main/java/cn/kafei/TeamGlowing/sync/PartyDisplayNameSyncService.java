package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.party.PartyInfo;
import cn.kafei.TeamGlowing.party.PartyManager;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class PartyDisplayNameSyncService {
    private static final String DISPLAY_TEAM_PREFIX = "tgd_";
    private static final String NO_TEAM = "__none__";
    private static final Map<String, String> PREVIOUS_TEAMS = new ConcurrentHashMap<>();

    public void syncPlayer(MinecraftServer server, ServerPlayerEntity player, PartyManager partyManager) {
        PartyInfo partyInfo = partyManager.getPartyInfo(player.getGameProfile().getName());
        if (partyInfo == null || partyInfo.name == null || partyInfo.name.isBlank()) {
            this.releasePlayerDisplay(server, player);
            return;
        }

        Scoreboard scoreboard = server.getScoreboard();
        Team displayTeam = this.ensureOwnedDisplayTeam(scoreboard, player);
        int color = partyInfo.color & 0xFFFFFF;
        displayTeam.setPrefix(Text.empty());
        displayTeam.setSuffix(Text.literal(" [" + partyInfo.name + "]").styled(style -> style.withColor(color)));
    }

    public void releasePlayerDisplay(MinecraftServer server, ServerPlayerEntity player) {
        Scoreboard scoreboard = server.getScoreboard();
        String holder = this.getScoreHolder(player);
        Team currentTeam = scoreboard.getScoreHolderTeam(holder);
        if (currentTeam != null && this.isModDisplayTeam(currentTeam.getName())) {
            scoreboard.removeScoreHolderFromTeam(holder, currentTeam);
        }

        String previousTeamName = PREVIOUS_TEAMS.remove(holder);
        if (previousTeamName != null && !NO_TEAM.equals(previousTeamName)) {
            Team previousTeam = scoreboard.getTeam(previousTeamName);
            if (previousTeam != null) {
                scoreboard.addScoreHolderToTeam(holder, previousTeam);
            }
        }
    }

    public void cleanup(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        for (Team team : new ArrayList<>(scoreboard.getTeams())) {
            if (this.isModDisplayTeam(team.getName())) {
                scoreboard.removeTeam(team);
            }
        }
        PREVIOUS_TEAMS.clear();
    }

    private Team ensureOwnedDisplayTeam(Scoreboard scoreboard, ServerPlayerEntity player) {
        String holder = this.getScoreHolder(player);
        Team currentTeam = scoreboard.getScoreHolderTeam(holder);
        Team expectedTeam = scoreboard.getTeam(this.getDisplayTeamId(player));
        if (expectedTeam == null) {
            expectedTeam = scoreboard.addTeam(this.getDisplayTeamId(player));
            expectedTeam.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.ALWAYS);
            expectedTeam.setPrefix(Text.empty());
            expectedTeam.setSuffix(Text.empty());
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
            scoreboard.removeScoreHolderFromTeam(holder, currentTeam);
        }
        scoreboard.addScoreHolderToTeam(holder, expectedTeam);
        return expectedTeam;
    }

    private String getDisplayTeamId(ServerPlayerEntity player) {
        long mixed = player.getUuid().getMostSignificantBits() ^ player.getUuid().getLeastSignificantBits();
        String hex = Long.toHexString(mixed).toLowerCase();
        if (hex.length() > 12) {
            hex = hex.substring(0, 12);
        }
        String id = DISPLAY_TEAM_PREFIX + hex;
        return id.length() > 16 ? id.substring(0, 16) : id;
    }

    private String getScoreHolder(ServerPlayerEntity player) {
        return player.getNameForScoreboard();
    }

    private boolean isModDisplayTeam(String teamName) {
        return teamName != null && teamName.startsWith(DISPLAY_TEAM_PREFIX);
    }
}
