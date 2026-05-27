package cn.kafei.TeamGlowing.mixin;

import cn.kafei.TeamGlowing.client.ClientToggleState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowSuppressionMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void teamglowing$suppressClientGlow(CallbackInfoReturnable<Boolean> cir) {
        if (ClientToggleState.shouldSuppressGlow((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
