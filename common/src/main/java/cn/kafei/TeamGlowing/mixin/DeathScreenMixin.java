package cn.kafei.TeamGlowing.mixin;

import cn.kafei.TeamGlowing.client.ClientPartyTabCache;
import cn.kafei.TeamGlowing.client.PartyRespawnRequestScheduler;
import cn.kafei.TeamGlowing.network.RequestPartyRespawnMessage;
import cn.kafei.TeamGlowing.network.TeamGlowingNetwork;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {
    @Shadow
    @Final
    private List<Button> exitButtons;

    @Shadow
    private boolean hardcore;

    @Unique
    private Button teamglowing$partyRespawnButton;

    protected DeathScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void teamglowing$addPartyRespawnButton(CallbackInfo ci) {
        if (this.hardcore || this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        if (ClientPartyTabCache.getOwnPartyName().isBlank()) {
            return;
        }

        this.teamglowing$partyRespawnButton = Button.builder(
                Component.translatable("teamglowing.death_screen.party_respawn"),
                button -> this.teamglowing$requestPartyRespawn()
            )
            .bounds(this.width / 2 - 100, this.height / 4 + 120, 200, 20)
            .build();
        this.teamglowing$partyRespawnButton.active = false;
        this.addRenderableWidget(this.teamglowing$partyRespawnButton);
        this.exitButtons.add(this.teamglowing$partyRespawnButton);
    }

    @Unique
    private void teamglowing$requestPartyRespawn() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        TeamGlowingNetwork.sendToServer(new RequestPartyRespawnMessage());
        PartyRespawnRequestScheduler.schedule();
    }
}
