package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.config.TabOverlayConfigState;
import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class TabOverlayConfigMessage implements CustomPacketPayload {
    public static final Type<TabOverlayConfigMessage> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(TeamGlowingConstants.MODID, "tab_overlay_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TabOverlayConfigMessage> CODEC = StreamCodec.ofMember(TabOverlayConfigMessage::write, TabOverlayConfigMessage::new);

    private final TabOverlayConfigState state;

    public TabOverlayConfigMessage(TabOverlayConfigState state) {
        this.state = state;
    }

    public TabOverlayConfigMessage(RegistryFriendlyByteBuf buf) {
        this.state = new TabOverlayConfigState(
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readUtf(),
            readLines(buf),
            readLines(buf),
            buf.readUtf(),
            buf.readVarInt(),
            buf.readVarLong(),
            buf.readBoolean(),
            buf.readUtf()
        );
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(this.state.enabled());
        buf.writeBoolean(this.state.topBorderEnabled());
        buf.writeUtf(this.state.topBorderText());
        writeLines(buf, this.state.headerLines());
        writeLines(buf, this.state.footerLines());
        buf.writeUtf(this.state.welcomeText());
        buf.writeVarInt(this.state.welcomeWidth());
        buf.writeVarLong(this.state.welcomeIntervalMs());
        buf.writeBoolean(this.state.bottomBorderEnabled());
        buf.writeUtf(this.state.bottomBorderText());
    }

    private static List<String> readLines(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> lines = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            lines.add(buf.readUtf());
        }
        return lines;
    }

    private static void writeLines(RegistryFriendlyByteBuf buf, List<String> lines) {
        buf.writeVarInt(lines.size());
        for (String line : lines) {
            buf.writeUtf(line == null ? "" : line);
        }
    }

    public TabOverlayConfigState state() {
        return this.state;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
