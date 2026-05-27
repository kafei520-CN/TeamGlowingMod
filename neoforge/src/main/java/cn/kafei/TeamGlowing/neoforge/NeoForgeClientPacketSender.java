package cn.kafei.TeamGlowing.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

final class NeoForgeClientPacketSender {
    private NeoForgeClientPacketSender() {
    }

    static void send(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
