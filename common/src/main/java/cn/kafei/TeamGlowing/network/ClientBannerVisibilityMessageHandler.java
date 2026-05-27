package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.client.ClientBannerVisibilityState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class ClientBannerVisibilityMessageHandler {
    private ClientBannerVisibilityMessageHandler() {
    }

    public static void handle(ClientBannerVisibilityMessage message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (message.action() == ClientBannerVisibilityMessage.TOGGLE_PARTY_BANNER) {
            boolean hidden = ClientBannerVisibilityState.togglePartyBanner(message.entryIds().isEmpty() ? "" : message.entryIds().get(0));
            show(player, hidden ? "party.banner.hidden" : "party.banner.shown", message.displayName());
            return;
        }
        if (message.action() == ClientBannerVisibilityMessage.TOGGLE_LOCATOR_BANNER) {
            boolean hidden = ClientBannerVisibilityState.toggleLocatorBanner(message.entryIds().isEmpty() ? "" : message.entryIds().get(0));
            show(player, hidden ? "party.locator_banner.hidden_single" : "party.locator_banner.shown_single", message.displayName());
            return;
        }
        if (message.action() == ClientBannerVisibilityMessage.TOGGLE_LOCATOR_BANNERS) {
            boolean hidden = ClientBannerVisibilityState.toggleLocatorBanners(message.entryIds());
            player.displayClientMessage(Component.translatable(hidden ? "party.locator_banner.hidden_all" : "party.locator_banner.shown_all"), true);
        }
    }

    private static void show(LocalPlayer player, String translationKey, String displayName) {
        player.displayClientMessage(Component.translatable(translationKey, displayName), true);
    }
}
