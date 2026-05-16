package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class RequestPartyRespawnMessage implements CustomPayload {
    public static final Id<RequestPartyRespawnMessage> ID = new Id<>(Identifier.of(TeamGlowingConstants.MODID, "request_party_respawn"));
    public static final PacketCodec<RegistryByteBuf, RequestPartyRespawnMessage> CODEC = PacketCodec.of(RequestPartyRespawnMessage::write, RequestPartyRespawnMessage::new);

    public RequestPartyRespawnMessage() {
    }

    public RequestPartyRespawnMessage(RegistryByteBuf buf) {
    }

    private void write(RegistryByteBuf buf) {
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
