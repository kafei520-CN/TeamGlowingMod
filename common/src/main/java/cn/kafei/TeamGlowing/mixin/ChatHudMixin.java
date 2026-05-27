package cn.kafei.TeamGlowing.mixin;

import cn.kafei.TeamGlowing.client.ClientPartyTabCache;
import cn.kafei.TeamGlowing.client.ClientPlayerColorHelper;
import cn.kafei.TeamGlowing.platform.TeamGlowingPlatforms;
import com.mojang.math.Axis;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatHudMixin {
    @Unique
    private static final int TEAMGLOWING_OTHER_PARTY_RIGHT_COLOR = 0x808080;

    @Unique
    private static final String TEAMGLOWING_CHAT_PADDING = "   ";

    @Unique
    private static final String TEAMGLOWING_CHAT_HEAD_PADDING = "   ";

    @Unique
    private static final int TEAMGLOWING_CHAT_ICON_SIZE = 8;

    @Unique
    private static final float TEAMGLOWING_CHAT_DIAMOND_SIZE = 6.0F;

    @Unique
    private static final int TEAMGLOWING_CHAT_HEAD_ICON_OFFSET = 1;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private java.util.List<GuiMessage.Line> trimmedMessages;

    @Shadow
    private int chatScrollbarPos;

    @Shadow
    public double getScale() {
        throw new AssertionError();
    }

    @Shadow
    private int getLineHeight() {
        throw new AssertionError();
    }

    @Shadow
    public int getLinesPerPage() {
        throw new AssertionError();
    }

    @Shadow
    private static double getTimeFactor(int age) {
        throw new AssertionError();
    }

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component teamglowing$decorateChatMessage(Component message) {
        return teamglowing$reformatChatMessage(message);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V", at = @At("TAIL"))
    private void teamglowing$renderChatDiamonds(GuiGraphics context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        if (this.minecraft.player == null || this.trimmedMessages.isEmpty()) {
            return;
        }

        int visibleLineCount = this.getLinesPerPage();
        if (visibleLineCount <= 0) {
            return;
        }

        double chatScale = this.getScale();
        if (chatScale <= 0.0D) {
            return;
        }

        double chatOpacity = this.minecraft.options.chatOpacity().get();
        boolean hasChatHeads = TeamGlowingPlatforms.get().isModLoaded("chat_heads");

        context.pose().pushPose();
        context.pose().translate(4.0F, this.minecraft.getWindow().getGuiScaledHeight() - 40.0F, 0.0F);
        context.pose().scale((float) chatScale, (float) chatScale, 1.0F);

        for (int lineIndex = 0; lineIndex + this.chatScrollbarPos < this.trimmedMessages.size() && lineIndex < visibleLineCount; lineIndex++) {
            GuiMessage.Line line = this.trimmedMessages.get(lineIndex + this.chatScrollbarPos);
            if (line == null) {
                continue;
            }

            int age = currentTick - line.addedTime();
            if (age >= 200 && !focused) {
                continue;
            }

            double opacityMultiplier = focused ? 1.0D : getTimeFactor(age);
            int alpha = Mth.ceil(255.0D * opacityMultiplier * chatOpacity);
            if (alpha <= 8) {
                continue;
            }

            TeamglowingChatMarker marker = teamglowing$resolveMarker(line.content());
            if (marker == null) {
                continue;
            }

            int y = -lineIndex * this.getLineHeight() - 8;
            int renderOffsetX = hasChatHeads ? TEAMGLOWING_CHAT_HEAD_ICON_OFFSET : 1;
            teamglowing$renderDiamond(context, renderOffsetX, y, marker.color(), marker.self(), marker.sameParty(), marker.hasParty(), alpha / 255.0F);
        }

        context.pose().popPose();
    }

    @Unique
    private Component teamglowing$reformatChatMessage(Component message) {
        ComponentContents content = message.getContents();
        if (!(content instanceof TranslatableContents translatable)) {
            return message;
        }
        if (!"chat.type.text".equals(translatable.getKey())) {
            return message;
        }

        Object[] args = translatable.getArgs();
        if (args.length < 2) {
            return message;
        }

        Component playerName = teamglowing$toText(args[0]);
        Component body = teamglowing$toText(args[1]);
        String cleanPlayerName = teamglowing$stripDecoratedPlayerName(playerName.getString());
        String partyName = ClientPartyTabCache.getPartyName(null, cleanPlayerName);
        boolean hasChatHeads = TeamGlowingPlatforms.get().isModLoaded("chat_heads");

        MutableComponent decorated = Component.literal(hasChatHeads ? TEAMGLOWING_CHAT_HEAD_PADDING : TEAMGLOWING_CHAT_PADDING)
            .append(Component.translatable("chat.type.text", Component.literal(cleanPlayerName), Component.empty()));
        if (!partyName.isBlank()) {
            int partyColor = ClientPartyTabCache.getPartyColor(null, cleanPlayerName);
            decorated.append(Component.literal("[" + partyName + "]").withStyle(style -> style.withColor(partyColor)))
                .append(Component.literal(" "));
        }
        return decorated.append(body.copy());
    }

    @Unique
    private void teamglowing$renderDiamond(GuiGraphics context, int x, int y, int rgbColor, boolean self, boolean sameParty, boolean hasParty, float alpha) {
        int fullColor = (Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24) | (rgbColor & 0xFFFFFF);

        if (hasParty && !sameParty && !self) {
            teamglowing$renderSplitDiamond(context, x, y, withAlpha(0xFFFFFF, alpha), withAlpha(TEAMGLOWING_OTHER_PARTY_RIGHT_COLOR, alpha));
            return;
        }

        context.pose().pushPose();
        context.pose().translate(x + TEAMGLOWING_CHAT_ICON_SIZE * 0.5F, y + TEAMGLOWING_CHAT_ICON_SIZE * 0.5F, 0.0F);
        context.pose().mulPose(Axis.ZP.rotationDegrees(45.0F));
        context.pose().translate(-TEAMGLOWING_CHAT_DIAMOND_SIZE * 0.5F, -TEAMGLOWING_CHAT_DIAMOND_SIZE * 0.5F, 0.0F);
        if (self) {
            teamglowing$fillDiamondFrame(context, 0, 0, 6, fullColor);
            context.fill(2, 2, 4, 4, fullColor);
        } else {
            context.fill(0, 0, 6, 6, fullColor);
        }
        context.pose().popPose();
    }

    @Unique
    private void teamglowing$renderSplitDiamond(GuiGraphics context, int x, int y, int leftColor, int rightColor) {
        context.pose().pushPose();
        context.pose().translate(x + TEAMGLOWING_CHAT_ICON_SIZE * 0.5F, y + TEAMGLOWING_CHAT_ICON_SIZE * 0.5F, 0.0F);
        context.pose().mulPose(Axis.ZP.rotationDegrees(45.0F));
        context.pose().translate(-TEAMGLOWING_CHAT_DIAMOND_SIZE * 0.5F, -TEAMGLOWING_CHAT_DIAMOND_SIZE * 0.5F, 0.0F);
        context.fill(0, 0, 3, 6, leftColor);
        context.fill(3, 0, 6, 6, rightColor);
        context.pose().popPose();
    }

    @Unique
    private void teamglowing$fillDiamondFrame(GuiGraphics context, int x, int y, int size, int color) {
        context.fill(x, y, x + size, y + 1, color);
        context.fill(x, y + size - 1, x + size, y + size, color);
        context.fill(x, y + 1, x + 1, y + size - 1, color);
        context.fill(x + size - 1, y + 1, x + size, y + size - 1, color);
    }

    @Unique
    private @Nullable TeamglowingChatMarker teamglowing$resolveMarker(FormattedCharSequence content) {
        String line = teamglowing$orderedTextToString(content).stripLeading();
        if (!line.startsWith("<")) {
            return null;
        }

        int nameEnd = line.indexOf('>');
        if (nameEnd <= 1) {
            return null;
        }

        String playerName = teamglowing$stripDecoratedPlayerName(line.substring(1, nameEnd));
        boolean self = teamglowing$isSelf(playerName);
        boolean hasParty = ClientPartyTabCache.hasParty(null, playerName);
        boolean sameParty = ClientPartyTabCache.isSameParty(null, playerName);
        int color = hasParty
            ? (ClientPlayerColorHelper.getPlayerColor(playerName) & 0xFFFFFF)
            : 0xFFFFFF;
        int nameWidth = this.minecraft.font.width("<" + playerName + ">");
        return new TeamglowingChatMarker(color, self, sameParty, hasParty, nameWidth);
    }

    @Unique
    private int withAlpha(int rgbColor, float alpha) {
        int alphaChannel = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        return (alphaChannel << 24) | (rgbColor & 0xFFFFFF);
    }

    @Unique
    private String teamglowing$orderedTextToString(FormattedCharSequence text) {
        StringBuilder builder = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
    }

    @Unique
    private Component teamglowing$toText(Object value) {
        if (value instanceof Component text) {
            return text;
        }
        return Component.literal(String.valueOf(value));
    }

    @Unique
    private boolean teamglowing$isSelf(String playerName) {
        return this.minecraft.player != null && this.minecraft.player.getGameProfile().getName().equalsIgnoreCase(playerName);
    }

    @Unique
    private String teamglowing$stripDecoratedPlayerName(String value) {
        if (value == null) {
            return "";
        }
        int suffixStart = value.lastIndexOf(" [");
        if (suffixStart > 0 && value.endsWith("]")) {
            return value.substring(0, suffixStart);
        }
        return value;
    }

    @Unique
    private record TeamglowingChatMarker(int color, boolean self, boolean sameParty, boolean hasParty, int nameWidth) {
    }
}
