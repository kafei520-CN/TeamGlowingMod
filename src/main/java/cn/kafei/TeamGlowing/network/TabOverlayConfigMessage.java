package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.config.TabOverlayConfigState;
import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class TabOverlayConfigMessage implements CustomPayload {
    public static final Id<TabOverlayConfigMessage> ID = new Id<>(Identifier.of(TeamGlowingConstants.MODID, "tab_overlay_config"));
    public static final PacketCodec<RegistryByteBuf, TabOverlayConfigMessage> CODEC = PacketCodec.of(TabOverlayConfigMessage::write, TabOverlayConfigMessage::new);

    private final TabOverlayConfigState state;

    public TabOverlayConfigMessage(TabOverlayConfigState state) {
        this.state = state;
    }

    public TabOverlayConfigMessage(RegistryByteBuf buf) {
        this.state = new TabOverlayConfigState(
            buf.readBoolean(),
            buf.readString(),
            readLines(buf),
            readLines(buf),
            buf.readString(),
            buf.readVarInt(),
            buf.readVarLong(),
            buf.readBoolean(),
            buf.readString()
        );
    }

    private void write(RegistryByteBuf buf) {
        buf.writeBoolean(this.state.topBorderEnabled());
        buf.writeString(this.state.topBorderText());
        writeLines(buf, this.state.headerLines());
        writeLines(buf, this.state.footerLines());
        buf.writeString(this.state.welcomeText());
        buf.writeVarInt(this.state.welcomeWidth());
        buf.writeVarLong(this.state.welcomeIntervalMs());
        buf.writeBoolean(this.state.bottomBorderEnabled());
        buf.writeString(this.state.bottomBorderText());
    }

    private static List<String> readLines(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<String> lines = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            lines.add(buf.readString());
        }
        return lines;
    }

    private static void writeLines(RegistryByteBuf buf, List<String> lines) {
        buf.writeVarInt(lines.size());
        for (String line : lines) {
            buf.writeString(line == null ? "" : line);
        }
    }

    public TabOverlayConfigState state() {
        return this.state;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
