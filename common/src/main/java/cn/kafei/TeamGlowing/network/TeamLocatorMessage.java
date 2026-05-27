package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class TeamLocatorMessage implements CustomPacketPayload {
    public static final Type<TeamLocatorMessage> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(TeamGlowingConstants.MODID, "team_locator"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TeamLocatorMessage> CODEC = StreamCodec.ofMember(TeamLocatorMessage::write, TeamLocatorMessage::new);

    private final List<TeamLocatorEntry> entries = new ArrayList<>();

    public TeamLocatorMessage(List<TeamLocatorEntry> entries) {
        this.entries.addAll(entries);
    }

    public TeamLocatorMessage(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        for (int index = 0; index < size; index++) {
            String entryId = buf.readUtf();
            String name = buf.readUtf();
            String playerName = buf.readUtf();
            String playerId = buf.readUtf();
            String dimensionId = buf.readUtf();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            boolean banner = buf.readBoolean();
            boolean partyBanner = buf.readBoolean();
            int bannerColorId = buf.readVarInt();
            this.entries.add(new TeamLocatorEntry(entryId, name, playerName, playerId, dimensionId, x, y, z, banner, partyBanner, bannerColorId));
        }
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entries.size());
        for (TeamLocatorEntry entry : this.entries) {
            buf.writeUtf(entry.entryId());
            buf.writeUtf(entry.name());
            buf.writeUtf(entry.playerName());
            buf.writeUtf(entry.playerId());
            buf.writeUtf(entry.dimensionId());
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
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
