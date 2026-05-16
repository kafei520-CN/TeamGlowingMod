package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class PartyTabMessage implements CustomPayload {
    public static final Id<PartyTabMessage> ID = new Id<>(Identifier.of(TeamGlowingConstants.MODID, "party_tab"));
    public static final PacketCodec<RegistryByteBuf, PartyTabMessage> CODEC = PacketCodec.of(PartyTabMessage::write, PartyTabMessage::new);

    private final String partyName;
    private final List<PartyTabEntry> entries = new ArrayList<>();

    public PartyTabMessage(String partyName, List<PartyTabEntry> entries) {
        this.partyName = partyName == null ? "" : partyName;
        this.entries.addAll(entries);
    }

    public PartyTabMessage(RegistryByteBuf buf) {
        this.partyName = buf.readString();
        int size = buf.readVarInt();
        for (int index = 0; index < size; index++) {
            this.entries.add(new PartyTabEntry(
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readVarInt()
            ));
        }
    }

    private void write(RegistryByteBuf buf) {
        buf.writeString(this.partyName);
        buf.writeVarInt(this.entries.size());
        for (PartyTabEntry entry : this.entries) {
            buf.writeString(entry.playerId());
            buf.writeString(entry.playerName());
            buf.writeString(entry.partyName() == null ? "" : entry.partyName());
            buf.writeVarInt(entry.sortOrder());
        }
    }

    public String partyName() {
        return this.partyName;
    }

    public List<PartyTabEntry> entries() {
        return List.copyOf(this.entries);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
