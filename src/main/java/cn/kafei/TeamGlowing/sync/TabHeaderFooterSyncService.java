package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.party.PartyInfo;
import cn.kafei.TeamGlowing.party.PartyManager;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class TabHeaderFooterSyncService {
    private final Map<UUID, String> lastContentByPlayer = new HashMap<>();

    public void syncToPlayer(ServerPlayerEntity player, PartyManager partyManager) {
        PartyInfo partyInfo = partyManager.getPartyInfo(player.getGameProfile().getName());
        Text header = this.buildHeader(player, partyInfo);
        Text footer = this.buildFooter(partyInfo);
        String signature = header.getString() + "\n" + footer.getString();
        UUID playerId = player.getUuid();
        if (signature.equals(this.lastContentByPlayer.get(playerId))) {
            return;
        }

        player.networkHandler.sendPacket(new PlayerListHeaderS2CPacket(header, footer));
        this.lastContentByPlayer.put(playerId, signature);
    }

    public void clear(ServerPlayerEntity player) {
        this.lastContentByPlayer.remove(player.getUuid());
    }

    private Text buildHeader(ServerPlayerEntity player, PartyInfo partyInfo) {
        return Text.literal(
            "TPS:" + this.getServerTps(player.getServer())
                + "\nMSPT:" + this.getServerMspt(player.getServer())
                + "\nMEM:" + this.getServerMemoryUsage()
        );
    }

    private Text buildFooter(PartyInfo partyInfo) {
        return Text.empty();
    }

    private String getServerTps(MinecraftServer server) {
        if (server == null) {
            return "--";
        }
        double mspt = server.getAverageNanosPerTick() / 1000000.0D;
        if (mspt <= 0.0D) {
            return "20.0";
        }
        double tps = Math.min(20.0D, 1000.0D / mspt);
        return String.format(Locale.ROOT, "%.1f", Double.valueOf(tps));
    }

    private String getServerMspt(MinecraftServer server) {
        if (server == null) {
            return "--";
        }
        double mspt = server.getAverageNanosPerTick() / 1000000.0D;
        if (mspt <= 0.0D) {
            return "0.0";
        }
        return String.format(Locale.ROOT, "%.1f", Double.valueOf(mspt));
    }

    private String getServerMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        long maxMb = runtime.maxMemory() / (1024L * 1024L);
        return usedMb + "/" + maxMb + "MB";
    }
}
