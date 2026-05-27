package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.config.ServerTabOverlayConfig;
import cn.kafei.TeamGlowing.config.TabOverlayConfigState;
import cn.kafei.TeamGlowing.network.TabOverlayConfigMessage;
import cn.kafei.TeamGlowing.network.TeamGlowingNetwork;
import cn.kafei.TeamGlowing.party.PartyInfo;
import cn.kafei.TeamGlowing.party.PartyManager;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class TabHeaderFooterSyncService {
    private final Map<UUID, String> lastContentByPlayer = new HashMap<>();
    private final Map<UUID, String> lastConfigSignatureByPlayer = new HashMap<>();

    public void syncToPlayer(ServerPlayer player, PartyManager partyManager) {
        TabOverlayConfigState config = ServerTabOverlayConfig.get();
        this.syncConfigToPlayer(player, config);
        if (!config.enabled()) {
            this.clearHeaderFooter(player);
            return;
        }

        PartyInfo partyInfo = partyManager.getPartyInfo(player.getGameProfile().getName());
        Component header = this.buildHeader(player, partyInfo);
        Component footer = this.buildFooter(partyInfo);
        String signature = header.getString() + "\n" + footer.getString();
        UUID playerId = player.getUUID();
        if (signature.equals(this.lastContentByPlayer.get(playerId))) {
            return;
        }

        player.connection.send(new ClientboundTabListPacket(header, footer));
        this.lastContentByPlayer.put(playerId, signature);
    }

    public void clear(ServerPlayer player) {
        this.lastContentByPlayer.remove(player.getUUID());
        this.lastConfigSignatureByPlayer.remove(player.getUUID());
    }

    private Component buildHeader(ServerPlayer player, PartyInfo partyInfo) {
        return Component.literal(
            "TPS:" + this.getServerTps(player.getServer())
                + "\nMSPT:" + this.getServerMspt(player.getServer())
                + "\nMEM:" + this.getServerMemoryUsage()
        );
    }

    private Component buildFooter(PartyInfo partyInfo) {
        return Component.empty();
    }

    private void syncConfigToPlayer(ServerPlayer player, TabOverlayConfigState config) {
        String signature = this.getConfigSignature(config);
        UUID playerId = player.getUUID();
        if (signature.equals(this.lastConfigSignatureByPlayer.get(playerId))) {
            return;
        }
        TeamGlowingNetwork.sendTo(player, new TabOverlayConfigMessage(config));
        this.lastConfigSignatureByPlayer.put(playerId, signature);
    }

    private void clearHeaderFooter(ServerPlayer player) {
        UUID playerId = player.getUUID();
        String signature = "__disabled__";
        if (signature.equals(this.lastContentByPlayer.get(playerId))) {
            return;
        }
        player.connection.send(new ClientboundTabListPacket(Component.empty(), Component.empty()));
        this.lastContentByPlayer.put(playerId, signature);
    }

    private String getConfigSignature(TabOverlayConfigState config) {
        return config.enabled()
            + "|" + config.topBorderEnabled()
            + "|" + config.topBorderText()
            + "|" + String.join("\n", config.headerLines())
            + "|" + String.join("\n", config.footerLines())
            + "|" + config.welcomeText()
            + "|" + config.welcomeWidth()
            + "|" + config.welcomeIntervalMs()
            + "|" + config.bottomBorderEnabled()
            + "|" + config.bottomBorderText();
    }

    private String getServerTps(MinecraftServer server) {
        if (server == null) {
            return "--";
        }
        double mspt = server.getAverageTickTimeNanos() / 1000000.0D;
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
        double mspt = server.getAverageTickTimeNanos() / 1000000.0D;
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
