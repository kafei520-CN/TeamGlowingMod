package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.network.PartyTabEntry;
import cn.kafei.TeamGlowing.network.PartyTabMessage;
import cn.kafei.TeamGlowing.network.TeamGlowingNetwork;
import cn.kafei.TeamGlowing.party.PartyInfo;
import cn.kafei.TeamGlowing.party.PartyManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PartyTabSyncService {
    public void syncToPlayer(ServerPlayerEntity viewer, PartyManager partyManager) {
        PartyInfo partyInfo = partyManager.getPartyInfo(viewer.getGameProfile().getName());
        List<PartyTabEntry> entries = new ArrayList<>();
        if (viewer.getServer() != null) {
            for (ServerPlayerEntity onlinePlayer : viewer.getServer().getPlayerManager().getPlayerList()) {
                PartyInfo onlinePartyInfo = partyManager.getPartyInfo(onlinePlayer.getGameProfile().getName());
                entries.add(new PartyTabEntry(
                    onlinePlayer.getUuidAsString(),
                    onlinePlayer.getGameProfile().getName(),
                    onlinePartyInfo == null ? "" : onlinePartyInfo.name
                ));
            }
        }
        TeamGlowingNetwork.sendTo(viewer, new PartyTabMessage(partyInfo == null ? "" : partyInfo.name, entries));
    }
}
