package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class RequestPartyRespawnMessage implements CustomPacketPayload {
    public static final Type<RequestPartyRespawnMessage> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(TeamGlowingConstants.MODID, "request_party_respawn"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestPartyRespawnMessage> CODEC = StreamCodec.ofMember(RequestPartyRespawnMessage::write, RequestPartyRespawnMessage::new);

    public RequestPartyRespawnMessage() {
    }

    public RequestPartyRespawnMessage(RegistryFriendlyByteBuf buf) {
    }

    private void write(RegistryFriendlyByteBuf buf) {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
