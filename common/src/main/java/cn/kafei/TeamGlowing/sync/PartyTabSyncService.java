package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.network.PartyTabEntry;
import cn.kafei.TeamGlowing.network.PartyTabMessage;
import cn.kafei.TeamGlowing.network.TeamGlowingNetwork;
import cn.kafei.TeamGlowing.party.PartyInfo;
import cn.kafei.TeamGlowing.party.PartyManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

public final class PartyTabSyncService {
    public void syncToPlayer(ServerPlayer viewer, PartyManager partyManager) {
        PartyInfo partyInfo = partyManager.getPartyInfo(viewer.getGameProfile().getName());
        String ownPartyName = partyInfo == null ? "" : partyInfo.name;
        List<PartyTabEntry> entries = new ArrayList<>();
        if (viewer.getServer() != null) {
            for (ServerPlayer onlinePlayer : viewer.getServer().getPlayerList().getPlayers()) {
                PartyInfo onlinePartyInfo = partyManager.getPartyInfo(onlinePlayer.getGameProfile().getName());
                String onlinePartyName = onlinePartyInfo == null ? "" : onlinePartyInfo.name;
                int onlinePartyColor = onlinePartyInfo == null ? 0xFFFFFF : onlinePartyInfo.color;
                entries.add(new PartyTabEntry(
                    onlinePlayer.getStringUUID(),
                    onlinePlayer.getGameProfile().getName(),
                    onlinePartyName,
                    onlinePartyColor,
                    this.getSortOrder(viewer, onlinePlayer, ownPartyName, onlinePartyName, partyManager)
                ));
            }
        }
        TeamGlowingNetwork.sendTo(viewer, new PartyTabMessage(ownPartyName, entries));
    }

    private int getSortOrder(ServerPlayer viewer, ServerPlayer target, String ownPartyName, String targetPartyName, PartyManager partyManager) {
        if (viewer.getUUID().equals(target.getUUID())) {
            return 0;
        }

        int baseOrder;
        if (!ownPartyName.isBlank() && ownPartyName.equals(targetPartyName)) {
            baseOrder = 10 + partyManager.getTabRoleOrder(target.getGameProfile().getName());
        } else if (!targetPartyName.isBlank()) {
            baseOrder = 20 + partyManager.getTabRoleOrder(target.getGameProfile().getName());
        } else {
            baseOrder = 30;
        }

        if (target.isSpectator()) {
            baseOrder += 100;
        }
        return baseOrder;
    }
}
