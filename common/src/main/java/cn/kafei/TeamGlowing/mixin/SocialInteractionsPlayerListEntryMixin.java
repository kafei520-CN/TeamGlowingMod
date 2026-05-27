package cn.kafei.TeamGlowing.mixin;

import cn.kafei.TeamGlowing.client.ClientPartyTabCache;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.social.PlayerEntry;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntry.class)
public abstract class SocialInteractionsPlayerListEntryMixin {
    @Unique
    private static final int TEAMGLOWING_INVITE_BUTTON_WIDTH = 20;
    @Unique
    private static final int TEAMGLOWING_INVITE_BUTTON_HEIGHT = 20;
    @Unique
    private static final int TEAMGLOWING_INVITE_BUTTON_MARGIN = 4;
    @Unique
    private static final Component TEAMGLOWING_INVITE_BUTTON_TEXT = Component.literal("+");

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private String playerName;

    @Shadow
    @Final
    private UUID id;

    @Shadow
    @Final
    private List<AbstractWidget> children;

    @Shadow
    private boolean isRemoved;

    @Unique
    private Button teamglowing$inviteButton;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void teamglowing$initInviteButton(Minecraft client, net.minecraft.client.gui.screens.social.SocialInteractionsScreen parent, UUID uuid, String name, java.util.function.Supplier<?> skinTexture, boolean reportable, CallbackInfo ci) {
        this.teamglowing$inviteButton = Button.builder(
                TEAMGLOWING_INVITE_BUTTON_TEXT,
                button -> this.teamglowing$sendInviteCommand()
            )
            .bounds(0, 0, TEAMGLOWING_INVITE_BUTTON_WIDTH, TEAMGLOWING_INVITE_BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("teamglowing.social.invite.tooltip", this.playerName)))
            .build();
        this.teamglowing$inviteButton.visible = false;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void teamglowing$renderInviteButton(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickProgress, CallbackInfo ci) {
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
        for (AbstractWidget button : this.children) {
            if (button.visible) {
                visibleOtherButtons++;
            }
        }

        int buttonX = x + entryWidth - 6 - TEAMGLOWING_INVITE_BUTTON_WIDTH - visibleOtherButtons * (TEAMGLOWING_INVITE_BUTTON_WIDTH + TEAMGLOWING_INVITE_BUTTON_MARGIN);
        int buttonY = y + (entryHeight - TEAMGLOWING_INVITE_BUTTON_HEIGHT) / 2;
        this.teamglowing$inviteButton.setRectangle(
            TEAMGLOWING_INVITE_BUTTON_WIDTH,
            TEAMGLOWING_INVITE_BUTTON_HEIGHT,
            buttonX,
            buttonY
        );
        this.teamglowing$inviteButton.render(context, mouseX, mouseY, tickProgress);
    }

    @Unique
    private boolean teamglowing$shouldShowInviteButton() {
        if (this.minecraft.player == null || this.minecraft.getConnection() == null || this.isRemoved) {
            return false;
        }
        if (this.minecraft.player.getUUID().equals(this.id)) {
            return false;
        }

        if (!this.teamglowing$hasOwnParty()) {
            return false;
        }
        if (ClientPartyTabCache.isSameParty(this.id.toString(), this.playerName)) {
            return false;
        }
        return !ClientPartyTabCache.hasParty(this.id.toString(), this.playerName);
    }

    @Unique
    private boolean teamglowing$hasOwnParty() {
        if (!ClientPartyTabCache.getOwnPartyName().isBlank()) {
            return true;
        }
        if (!ClientPartyTabCache.hasEntries()) {
            return true;
        }
        if (this.minecraft.player == null) {
            return false;
        }
        return ClientPartyTabCache.hasParty(this.minecraft.player.getStringUUID(), this.minecraft.player.getGameProfile().getName());
    }

    @Inject(method = "children", at = @At("RETURN"), cancellable = true)
    private void teamglowing$appendInviteButtonChildren(CallbackInfoReturnable<List<? extends GuiEventListener>> cir) {
        if (this.teamglowing$inviteButton == null) {
            return;
        }
        List<GuiEventListener> elements = new ArrayList<>(cir.getReturnValue());
        elements.add(this.teamglowing$inviteButton);
        cir.setReturnValue(elements);
    }

    @Inject(method = "narratables", at = @At("RETURN"), cancellable = true)
    private void teamglowing$appendInviteButtonSelectableChildren(CallbackInfoReturnable<List<? extends NarratableEntry>> cir) {
        if (this.teamglowing$inviteButton == null) {
            return;
        }
        List<NarratableEntry> elements = new ArrayList<>(cir.getReturnValue());
        elements.add(this.teamglowing$inviteButton);
        cir.setReturnValue(elements);
    }

    @Unique
    private void teamglowing$sendInviteCommand() {
        if (this.minecraft.getConnection() == null) {
            return;
        }
        this.minecraft.getConnection().sendCommand("tg invite " + this.playerName);
    }
}
