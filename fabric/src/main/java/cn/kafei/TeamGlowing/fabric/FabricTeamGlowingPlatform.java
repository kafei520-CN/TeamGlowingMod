package cn.kafei.TeamGlowing.fabric;

import cn.kafei.TeamGlowing.platform.TeamGlowingPlatform;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public enum FabricTeamGlowingPlatform implements TeamGlowingPlatform {
    INSTANCE;

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        FabricClientPacketSender.send(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        FabricTeamGlowingNetwork.sendToPlayer(player, payload);
    }
}
