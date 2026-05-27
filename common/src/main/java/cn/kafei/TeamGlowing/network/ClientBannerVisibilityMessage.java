package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class ClientBannerVisibilityMessage implements CustomPacketPayload {
    public static final int TOGGLE_PARTY_BANNER = 0;
    public static final int TOGGLE_LOCATOR_BANNER = 1;
    public static final int TOGGLE_LOCATOR_BANNERS = 2;
    public static final Type<ClientBannerVisibilityMessage> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(TeamGlowingConstants.MODID, "client_banner_visibility"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientBannerVisibilityMessage> CODEC = StreamCodec.ofMember(ClientBannerVisibilityMessage::write, ClientBannerVisibilityMessage::new);

    private final int action;
    private final String displayName;
    private final List<String> entryIds = new ArrayList<>();

    private ClientBannerVisibilityMessage(int action, String displayName, List<String> entryIds) {
        this.action = action;
        this.displayName = displayName == null ? "" : displayName;
        this.entryIds.addAll(entryIds);
    }

    public ClientBannerVisibilityMessage(RegistryFriendlyByteBuf buf) {
        this.action = buf.readVarInt();
        this.displayName = buf.readUtf();
        int size = buf.readVarInt();
        for (int index = 0; index < size; index++) {
            this.entryIds.add(buf.readUtf());
        }
    }

    public static ClientBannerVisibilityMessage togglePartyBanner(String entryId, String displayName) {
        return new ClientBannerVisibilityMessage(
            TOGGLE_PARTY_BANNER,
            displayName,
            List.of(entryId)
        );
    }

    public static ClientBannerVisibilityMessage toggleLocatorBanner(String entryId, String displayName) {
        return new ClientBannerVisibilityMessage(TOGGLE_LOCATOR_BANNER, displayName, List.of(entryId));
    }

    public static ClientBannerVisibilityMessage toggleLocatorBanners(List<String> entryIds) {
        return new ClientBannerVisibilityMessage(TOGGLE_LOCATOR_BANNERS, "", entryIds);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.action);
        buf.writeUtf(this.displayName);
        buf.writeVarInt(this.entryIds.size());
        for (String entryId : this.entryIds) {
            buf.writeUtf(entryId);
        }
    }

    public int action() {
        return this.action;
    }

    public String displayName() {
        return this.displayName;
    }

    public List<String> entryIds() {
        return List.copyOf(this.entryIds);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
