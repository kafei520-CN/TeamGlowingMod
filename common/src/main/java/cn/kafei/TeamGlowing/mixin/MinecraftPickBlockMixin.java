package cn.kafei.TeamGlowing.mixin;

import cn.kafei.TeamGlowing.client.ClientMarkerController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class, priority = Integer.MAX_VALUE)
public abstract class MinecraftPickBlockMixin {
    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void teamglowing$handleMarkerBeforePickBlock(CallbackInfo ci) {
        // 先处理 TeamGlowing 中键标记，成功后取消原版 Pick Block。
        if (ClientMarkerController.tryHandleMarkRequest((Minecraft) (Object) this)) {
            ClientMarkerController.discardPendingMarkClicks();
            ci.cancel();
        }
    }
}
