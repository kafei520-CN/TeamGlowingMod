package cn.kafei.TeamGlowing.mixin;

import cn.kafei.TeamGlowing.client.ClientPartyTabCache;
import cn.kafei.TeamGlowing.client.ClientPlayerColorHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
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
    private MinecraftClient client;

    @Shadow
    @Final
    private java.util.List<ChatHudLine.Visible> visibleMessages;

    @Shadow
    private int scrolledLines;

    @Shadow
    public double getChatScale() {
        throw new AssertionError();
    }

    @Shadow
    private int getLineHeight() {
        throw new AssertionError();
    }

    @Shadow
    public int getVisibleLineCount() {
        throw new AssertionError();
    }

    @Shadow
    private static double getMessageOpacityMultiplier(int age) {
        throw new AssertionError();
    }

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Text teamglowing$decorateChatMessage(Text message) {
        return teamglowing$reformatChatMessage(message);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V", at = @At("TAIL"))
    private void teamglowing$renderChatDiamonds(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        if (this.client.player == null || this.visibleMessages.isEmpty()) {
            return;
        }

        int visibleLineCount = this.getVisibleLineCount();
        if (visibleLineCount <= 0) {
            return;
        }

        double chatScale = this.getChatScale();
        if (chatScale <= 0.0D) {
            return;
        }

        double chatOpacity = this.client.options.getChatOpacity().getValue();
        boolean hasChatHeads = FabricLoader.getInstance().isModLoaded("chat_heads");

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(4.0F, this.client.getWindow().getScaledHeight() - 40.0F);
        context.getMatrices().scale((float) chatScale, (float) chatScale);

        for (int lineIndex = 0; lineIndex + this.scrolledLines < this.visibleMessages.size() && lineIndex < visibleLineCount; lineIndex++) {
            ChatHudLine.Visible line = this.visibleMessages.get(lineIndex + this.scrolledLines);
            if (line == null) {
                continue;
            }

            int age = currentTick - line.addedTime();
            if (age >= 200 && !focused) {
                continue;
            }

            double opacityMultiplier = focused ? 1.0D : getMessageOpacityMultiplier(age);
            int alpha = MathHelper.ceil(255.0D * opacityMultiplier * chatOpacity);
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

        context.getMatrices().popMatrix();
    }

    @Unique
    private Text teamglowing$reformatChatMessage(Text message) {
        TextContent content = message.getContent();
        if (!(content instanceof TranslatableTextContent translatable)) {
            return message;
        }
        if (!"chat.type.text".equals(translatable.getKey())) {
            return message;
        }

        Object[] args = translatable.getArgs();
        if (args.length < 2) {
            return message;
        }

        Text playerName = teamglowing$toText(args[0]);
        Text body = teamglowing$toText(args[1]);
        String cleanPlayerName = teamglowing$stripDecoratedPlayerName(playerName.getString());
        String partyName = ClientPartyTabCache.getPartyName(null, cleanPlayerName);
        boolean hasChatHeads = FabricLoader.getInstance().isModLoaded("chat_heads");

        MutableText decorated = Text.literal(hasChatHeads ? TEAMGLOWING_CHAT_HEAD_PADDING : TEAMGLOWING_CHAT_PADDING)
            .append(Text.translatable("chat.type.text", Text.literal(cleanPlayerName), Text.empty()));
        if (!partyName.isBlank()) {
            int partyColor = ClientPartyTabCache.getPartyColor(null, cleanPlayerName);
            decorated.append(Text.literal("[" + partyName + "]").styled(style -> style.withColor(partyColor)))
                .append(Text.literal(" "));
        }
        return decorated.append(body.copy());
    }

    @Unique
    private void teamglowing$renderDiamond(DrawContext context, int x, int y, int rgbColor, boolean self, boolean sameParty, boolean hasParty, float alpha) {
        int fullColor = (MathHelper.clamp((int) (alpha * 255.0F), 0, 255) << 24) | (rgbColor & 0xFFFFFF);

        if (hasParty && !sameParty && !self) {
            teamglowing$renderSplitDiamond(context, x, y, withAlpha(0xFFFFFF, alpha), withAlpha(TEAMGLOWING_OTHER_PARTY_RIGHT_COLOR, alpha));
            return;
        }

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x + TEAMGLOWING_CHAT_ICON_SIZE * 0.5F, y + TEAMGLOWING_CHAT_ICON_SIZE * 0.5F);
        context.getMatrices().mul(new Matrix3x2f().rotateLocal((float) (Math.PI / 4.0D)));
        context.getMatrices().translate(-TEAMGLOWING_CHAT_DIAMOND_SIZE * 0.5F, -TEAMGLOWING_CHAT_DIAMOND_SIZE * 0.5F);
        if (self) {
            teamglowing$fillDiamondFrame(context, 0, 0, 6, fullColor);
            context.fill(2, 2, 4, 4, fullColor);
        } else {
            context.fill(0, 0, 6, 6, fullColor);
        }
        context.getMatrices().popMatrix();
    }

    @Unique
    private void teamglowing$renderSplitDiamond(DrawContext context, int x, int y, int leftColor, int rightColor) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x + TEAMGLOWING_CHAT_ICON_SIZE * 0.5F, y + TEAMGLOWING_CHAT_ICON_SIZE * 0.5F);
        context.getMatrices().mul(new Matrix3x2f().rotateLocal((float) (Math.PI / 4.0D)));
        context.getMatrices().translate(-TEAMGLOWING_CHAT_DIAMOND_SIZE * 0.5F, -TEAMGLOWING_CHAT_DIAMOND_SIZE * 0.5F);
        context.fill(0, 0, 3, 6, leftColor);
        context.fill(3, 0, 6, 6, rightColor);
        context.getMatrices().popMatrix();
    }

    @Unique
    private void teamglowing$fillDiamondFrame(DrawContext context, int x, int y, int size, int color) {
        context.fill(x, y, x + size, y + 1, color);
        context.fill(x, y + size - 1, x + size, y + size, color);
        context.fill(x, y + 1, x + 1, y + size - 1, color);
        context.fill(x + size - 1, y + 1, x + size, y + size - 1, color);
    }

    @Unique
    private @Nullable TeamglowingChatMarker teamglowing$resolveMarker(OrderedText content) {
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
        int nameWidth = this.client.textRenderer.getWidth("<" + playerName + ">");
        return new TeamglowingChatMarker(color, self, sameParty, hasParty, nameWidth);
    }

    @Unique
    private int withAlpha(int rgbColor, float alpha) {
        int alphaChannel = MathHelper.clamp((int) (alpha * 255.0F), 0, 255);
        return (alphaChannel << 24) | (rgbColor & 0xFFFFFF);
    }

    @Unique
    private String teamglowing$orderedTextToString(OrderedText text) {
        StringBuilder builder = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
    }

    @Unique
    private Text teamglowing$toText(Object value) {
        if (value instanceof Text text) {
            return text;
        }
        return Text.literal(String.valueOf(value));
    }

    @Unique
    private boolean teamglowing$isSelf(String playerName) {
        return this.client.player != null && this.client.player.getGameProfile().getName().equalsIgnoreCase(playerName);
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
