package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import cn.kafei.TeamGlowing.marker.SharedMarkerKind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class TeammateWorldMarkerMessage implements CustomPacketPayload {
    public static final Type<TeammateWorldMarkerMessage> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(TeamGlowingConstants.MODID, "teammate_world_markers"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TeammateWorldMarkerMessage> CODEC = StreamCodec.ofMember(TeammateWorldMarkerMessage::write, TeammateWorldMarkerMessage::new);

    private final List<TeammateWorldMarkerEntry> entries = new ArrayList<>();

    public TeammateWorldMarkerMessage(List<TeammateWorldMarkerEntry> entries) {
        this.entries.addAll(entries);
    }

    public TeammateWorldMarkerMessage(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        for (int index = 0; index < size; index++) {
            this.entries.add(new TeammateWorldMarkerEntry(
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                SharedMarkerKind.fromOrdinal(buf.readVarInt()),
                buf.readUtf(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readLong()
            ));
        }
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entries.size());
        for (TeammateWorldMarkerEntry entry : this.entries) {
            buf.writeUtf(entry.name());
            buf.writeUtf(entry.playerName());
            buf.writeUtf(entry.playerId());
            buf.writeVarInt(entry.kind().ordinal());
            buf.writeUtf(entry.dimensionId());
            buf.writeDouble(entry.x());
            buf.writeDouble(entry.y());
            buf.writeDouble(entry.z());
            buf.writeUtf(entry.itemId() == null ? "" : entry.itemId());
            buf.writeUtf(entry.label() == null ? "" : entry.label());
            buf.writeBoolean(entry.labelIsTranslationKey());
            buf.writeLong(entry.expiresAtMillis());
        }
    }

    public List<TeammateWorldMarkerEntry> entries() {
        return List.copyOf(this.entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
