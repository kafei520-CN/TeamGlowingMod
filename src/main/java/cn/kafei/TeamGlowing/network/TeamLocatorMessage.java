package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class TeamLocatorMessage implements CustomPayload {
    public static final Id<TeamLocatorMessage> ID = new Id<>(Identifier.of(TeamGlowingConstants.MODID, "team_locator"));
    public static final PacketCodec<RegistryByteBuf, TeamLocatorMessage> CODEC = PacketCodec.of(TeamLocatorMessage::write, TeamLocatorMessage::new);

    private final List<TeamLocatorEntry> entries = new ArrayList<>();

    public TeamLocatorMessage(List<TeamLocatorEntry> entries) {
        this.entries.addAll(entries);
    }

    public TeamLocatorMessage(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        for (int index = 0; index < size; index++) {
            String entryId = buf.readString();
            String name = buf.readString();
            String playerName = buf.readString();
            String playerId = buf.readString();
            String dimensionId = buf.readString();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            boolean banner = buf.readBoolean();
            boolean partyBanner = buf.readBoolean();
            int bannerColorId = buf.readVarInt();
            this.entries.add(new TeamLocatorEntry(entryId, name, playerName, playerId, dimensionId, x, y, z, banner, partyBanner, bannerColorId));
        }
    }

    private void write(RegistryByteBuf buf) {
        buf.writeVarInt(this.entries.size());
        for (TeamLocatorEntry entry : this.entries) {
            buf.writeString(entry.entryId());
            buf.writeString(entry.name());
            buf.writeString(entry.playerName());
            buf.writeString(entry.playerId());
            buf.writeString(entry.dimensionId());
            buf.writeDouble(entry.x());
            buf.writeDouble(entry.y());
            buf.writeDouble(entry.z());
            buf.writeBoolean(entry.banner());
            buf.writeBoolean(entry.partyBanner());
            buf.writeVarInt(entry.bannerColorId());
        }
    }

    public List<TeamLocatorEntry> entries() {
        return List.copyOf(this.entries);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
