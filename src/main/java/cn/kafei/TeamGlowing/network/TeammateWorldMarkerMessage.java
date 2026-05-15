package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import cn.kafei.TeamGlowing.marker.SharedMarkerKind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class TeammateWorldMarkerMessage implements CustomPayload {
    public static final Id<TeammateWorldMarkerMessage> ID = new Id<>(Identifier.of(TeamGlowingConstants.MODID, "teammate_world_markers"));
    public static final PacketCodec<RegistryByteBuf, TeammateWorldMarkerMessage> CODEC = PacketCodec.of(TeammateWorldMarkerMessage::write, TeammateWorldMarkerMessage::new);

    private final List<TeammateWorldMarkerEntry> entries = new ArrayList<>();

    public TeammateWorldMarkerMessage(List<TeammateWorldMarkerEntry> entries) {
        this.entries.addAll(entries);
    }

    public TeammateWorldMarkerMessage(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        for (int index = 0; index < size; index++) {
            this.entries.add(new TeammateWorldMarkerEntry(
                buf.readString(),
                buf.readString(),
                buf.readString(),
                SharedMarkerKind.fromOrdinal(buf.readVarInt()),
                buf.readString(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readString(),
                buf.readString(),
                buf.readBoolean(),
                buf.readLong()
            ));
        }
    }

    private void write(RegistryByteBuf buf) {
        buf.writeVarInt(this.entries.size());
        for (TeammateWorldMarkerEntry entry : this.entries) {
            buf.writeString(entry.name());
            buf.writeString(entry.playerName());
            buf.writeString(entry.playerId());
            buf.writeVarInt(entry.kind().ordinal());
            buf.writeString(entry.dimensionId());
            buf.writeDouble(entry.x());
            buf.writeDouble(entry.y());
            buf.writeDouble(entry.z());
            buf.writeString(entry.itemId() == null ? "" : entry.itemId());
            buf.writeString(entry.label() == null ? "" : entry.label());
            buf.writeBoolean(entry.labelIsTranslationKey());
            buf.writeLong(entry.expiresAtMillis());
        }
    }

    public List<TeammateWorldMarkerEntry> entries() {
        return List.copyOf(this.entries);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
