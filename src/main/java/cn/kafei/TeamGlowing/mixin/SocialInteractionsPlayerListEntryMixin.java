package cn.kafei.TeamGlowing.mixin;

import cn.kafei.TeamGlowing.client.ClientPartyTabCache;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsPlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SocialInteractionsPlayerListEntry.class)
public abstract class SocialInteractionsPlayerListEntryMixin {
    @Unique
    private static final int TEAMGLOWING_INVITE_BUTTON_WIDTH = 20;
    @Unique
    private static final int TEAMGLOWING_INVITE_BUTTON_HEIGHT = 20;
    @Unique
    private static final int TEAMGLOWING_INVITE_BUTTON_MARGIN = 4;
    @Unique
    private static final Text TEAMGLOWING_INVITE_BUTTON_TEXT = Text.literal("+");

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    private String name;

    @Shadow
    @Final
    private UUID uuid;

    @Shadow
    @Final
    private List<ClickableWidget> buttons;

    @Shadow
    private boolean offline;

    @Unique
    private ButtonWidget teamglowing$inviteButton;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void teamglowing$initInviteButton(MinecraftClient client, net.minecraft.client.gui.screen.multiplayer.SocialInteractionsScreen parent, UUID uuid, String name, java.util.function.Supplier<?> skinTexture, boolean reportable, CallbackInfo ci) {
        this.teamglowing$inviteButton = ButtonWidget.builder(
                TEAMGLOWING_INVITE_BUTTON_TEXT,
                button -> this.teamglowing$sendInviteCommand()
            )
            .dimensions(0, 0, TEAMGLOWING_INVITE_BUTTON_WIDTH, TEAMGLOWING_INVITE_BUTTON_HEIGHT)
            .tooltip(Tooltip.of(Text.translatable("teamglowing.social.invite.tooltip", this.name)))
            .build();
        this.teamglowing$inviteButton.visible = false;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void teamglowing$renderInviteButton(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickProgress, CallbackInfo ci) {
        if (this.teamglowing$inviteButton == null) {
            return;
        }

        boolean visible = this.teamglowing$shouldShowInviteButton();
        this.teamglowing$inviteButton.visible = visible;
        this.teamglowing$inviteButton.active = visible;
        if (!visible) {
            return;
        }

        int visibleOtherButtons = 0;
        for (ClickableWidget button : this.buttons) {
            if (button.visible) {
                visibleOtherButtons++;
            }
        }

        int buttonX = x + entryWidth - 6 - TEAMGLOWING_INVITE_BUTTON_WIDTH - visibleOtherButtons * (TEAMGLOWING_INVITE_BUTTON_WIDTH + TEAMGLOWING_INVITE_BUTTON_MARGIN);
        int buttonY = y + (entryHeight - TEAMGLOWING_INVITE_BUTTON_HEIGHT) / 2;
        this.teamglowing$inviteButton.setDimensionsAndPosition(
            TEAMGLOWING_INVITE_BUTTON_WIDTH,
            TEAMGLOWING_INVITE_BUTTON_HEIGHT,
            buttonX,
            buttonY
        );
        this.teamglowing$inviteButton.render(context, mouseX, mouseY, tickProgress);
    }

    @Unique
    private boolean teamglowing$shouldShowInviteButton() {
        if (this.client.player == null || this.client.getNetworkHandler() == null || this.offline) {
            return false;
        }
        if (this.client.player.getUuid().equals(this.uuid)) {
            return false;
        }

        if (!this.teamglowing$hasOwnParty()) {
            return false;
        }
        if (ClientPartyTabCache.isSameParty(this.uuid.toString(), this.name)) {
            return false;
        }
        return !ClientPartyTabCache.hasParty(this.uuid.toString(), this.name);
    }

    @Unique
    private boolean teamglowing$hasOwnParty() {
        if (!ClientPartyTabCache.getOwnPartyName().isBlank()) {
            return true;
        }
        if (!ClientPartyTabCache.hasEntries()) {
            return true;
        }
        if (this.client.player == null) {
            return false;
        }
        return ClientPartyTabCache.hasParty(this.client.player.getUuidAsString(), this.client.player.getGameProfile().getName());
    }

    @Inject(method = "children", at = @At("RETURN"), cancellable = true)
    private void teamglowing$appendInviteButtonChildren(CallbackInfoReturnable<List<? extends Element>> cir) {
        if (this.teamglowing$inviteButton == null) {
            return;
        }
        List<Element> elements = new ArrayList<>(cir.getReturnValue());
        elements.add(this.teamglowing$inviteButton);
        cir.setReturnValue(elements);
    }

    @Inject(method = "selectableChildren", at = @At("RETURN"), cancellable = true)
    private void teamglowing$appendInviteButtonSelectableChildren(CallbackInfoReturnable<List<? extends Selectable>> cir) {
        if (this.teamglowing$inviteButton == null) {
            return;
        }
        List<Selectable> elements = new ArrayList<>(cir.getReturnValue());
        elements.add(this.teamglowing$inviteButton);
        cir.setReturnValue(elements);
    }

    @Unique
    private void teamglowing$sendInviteCommand() {
        if (this.client.getNetworkHandler() == null) {
            return;
        }
        this.client.getNetworkHandler().sendChatCommand("tg invite " + this.name);
    }
}
