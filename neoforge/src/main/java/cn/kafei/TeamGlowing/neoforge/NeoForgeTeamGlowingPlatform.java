package cn.kafei.TeamGlowing.neoforge;

import cn.kafei.TeamGlowing.platform.TeamGlowingPlatform;
import java.nio.file.Path;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

@SuppressWarnings("null")
public enum NeoForgeTeamGlowingPlatform implements TeamGlowingPlatform {
    INSTANCE;

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        NeoForgeClientPacketSender.send(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
