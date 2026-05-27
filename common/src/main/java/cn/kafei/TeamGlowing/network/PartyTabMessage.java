package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class PartyTabMessage implements CustomPacketPayload {
    public static final Type<PartyTabMessage> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(TeamGlowingConstants.MODID, "party_tab"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PartyTabMessage> CODEC = StreamCodec.ofMember(PartyTabMessage::write, PartyTabMessage::new);

    private final String partyName;
    private final List<PartyTabEntry> entries = new ArrayList<>();

    public PartyTabMessage(String partyName, List<PartyTabEntry> entries) {
        this.partyName = partyName == null ? "" : partyName;
        this.entries.addAll(entries);
    }

    public PartyTabMessage(RegistryFriendlyByteBuf buf) {
        this.partyName = buf.readUtf();
        int size = buf.readVarInt();
        for (int index = 0; index < size; index++) {
            this.entries.add(new PartyTabEntry(
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readInt(),
                buf.readVarInt()
            ));
        }
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(this.partyName);
        buf.writeVarInt(this.entries.size());
        for (PartyTabEntry entry : this.entries) {
            buf.writeUtf(entry.playerId());
            buf.writeUtf(entry.playerName());
            buf.writeUtf(entry.partyName() == null ? "" : entry.partyName());
            buf.writeInt(entry.partyColor());
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
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
