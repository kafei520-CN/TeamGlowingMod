package cn.kafei.TeamGlowing.mixin;

import cn.kafei.TeamGlowing.client.ClientPartyTabCache;
import cn.kafei.TeamGlowing.client.PartyRespawnRequestScheduler;
import cn.kafei.TeamGlowing.network.RequestPartyRespawnMessage;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
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
    private List<ButtonWidget> buttons;

    @Shadow
    private boolean isHardcore;

    @Unique
    private ButtonWidget teamglowing$partyRespawnButton;

    protected DeathScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void teamglowing$addPartyRespawnButton(CallbackInfo ci) {
        if (this.isHardcore || this.client == null || this.client.player == null) {
            return;
        }
        if (ClientPartyTabCache.getOwnPartyName().isBlank()) {
            return;
        }

        this.teamglowing$partyRespawnButton = ButtonWidget.builder(
                Text.translatable("teamglowing.death_screen.party_respawn"),
                button -> this.teamglowing$requestPartyRespawn()
            )
            .dimensions(this.width / 2 - 100, this.height / 4 + 120, 200, 20)
            .build();
        this.teamglowing$partyRespawnButton.active = false;
        this.addDrawableChild(this.teamglowing$partyRespawnButton);
        this.buttons.add(this.teamglowing$partyRespawnButton);
    }

    @Unique
    private void teamglowing$requestPartyRespawn() {
        if (this.client == null || this.client.player == null) {
            return;
        }
        ClientPlayNetworking.send(new RequestPartyRespawnMessage());
        PartyRespawnRequestScheduler.schedule();
    }
}
