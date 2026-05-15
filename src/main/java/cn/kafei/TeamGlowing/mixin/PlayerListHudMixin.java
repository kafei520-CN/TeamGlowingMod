package cn.kafei.TeamGlowing.mixin;

import cn.kafei.TeamGlowing.client.ClientPlayerColorHelper;
import cn.kafei.TeamGlowing.client.ClientPartyTabCache;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Unique
    private static final int TEAMGLOWING_TAB_TOP = 10;

    @Unique
    private static final int TEAMGLOWING_ROW_HEIGHT = 9;

    @Unique
    private static final int TEAMGLOWING_COLUMN_GAP = 5;

    @Unique
    private static final int TEAMGLOWING_ICON_SIZE = 8;

    @Unique
    private static final float TEAMGLOWING_DIAMOND_SIZE = 6.0F;

    @Unique
    private static final int TEAMGLOWING_ICON_GAP = 3;

    @Unique
    private static final int TEAMGLOWING_HEAD_WIDTH = 8;

    @Unique
    private static final String TEAMGLOWING_NAME_LEFT_PADDING = "   ";

    @Unique
    private static final String TEAMGLOWING_HEAD_NAME_PADDING = "    ";

    @Unique
    private static final int TEAMGLOWING_HEAD_X_SHIFT = TEAMGLOWING_ICON_SIZE + TEAMGLOWING_ICON_GAP;

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void teamglowing$decoratePlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        Text original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        cir.setReturnValue(teamglowing$getDecoratedName(entry, original));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void teamglowing$renderPartyIcons(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        if (this.client.player == null || this.client.getNetworkHandler() == null || !ClientPartyTabCache.hasEntries()) {
            return;
        }

        List<PlayerListEntry> entries = new ArrayList<>(this.client.getNetworkHandler().getListedPlayerListEntries());
        if (entries.isEmpty()) {
            return;
        }

        entries.sort(Comparator
            .comparingInt((PlayerListEntry entry) -> entry.getGameMode() == GameMode.SPECTATOR ? 1 : 0)
            .thenComparing(entry -> {
                Team team = entry.getScoreboardTeam();
                return team == null ? "" : team.getName();
            }, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(entry -> entry.getProfile().getName(), String.CASE_INSENSITIVE_ORDER)
        );

        int count = Math.min(entries.size(), 80);
        entries = entries.subList(0, count);

        int columns = 1;
        int rows = count;
        while (rows > 20) {
            columns++;
            rows = (count + columns - 1) / columns;
        }

        int maxNameWidth = 0;
        for (PlayerListEntry entry : entries) {
            maxNameWidth = Math.max(maxNameWidth, this.client.textRenderer.getWidth(teamglowing$getDecoratedName(entry, teamglowing$getDisplayName(entry))));
        }

        int cellWidth = maxNameWidth;
        int totalWidth = columns * cellWidth + (columns - 1) * TEAMGLOWING_COLUMN_GAP;
        int startX = scaledWindowWidth / 2 - totalWidth / 2;

        for (int index = 0; index < count; index++) {
            PlayerListEntry entry = entries.get(index);
            int column = index / rows;
            int row = index % rows;
            int cellX = startX + column * (cellWidth + TEAMGLOWING_COLUMN_GAP);
            int drawY = TEAMGLOWING_TAB_TOP + row * TEAMGLOWING_ROW_HEIGHT;
            int drawX = cellX + cellWidth - TEAMGLOWING_ICON_SIZE - 1 - TEAMGLOWING_ICON_SIZE;
            int color = teamglowing$getColor(entry);
            teamglowing$renderDiamond(context, drawX, drawY, color, teamglowing$isSelf(entry));
        }
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/PlayerSkinDrawer;draw(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/util/Identifier;IIIZZI)V"
        ),
        index = 2
    )
    private int teamglowing$shiftTabHeadX(int x) {
        return x + TEAMGLOWING_HEAD_X_SHIFT;
    }

    @Unique
    private Text teamglowing$getDisplayName(PlayerListEntry entry) {
        Text displayName = entry.getDisplayName();
        return displayName != null ? displayName.copy() : Text.literal(Team.decorateName(entry.getScoreboardTeam(), Text.literal(entry.getProfile().getName())).getString());
    }

    @Unique
    private Text teamglowing$getDecoratedName(PlayerListEntry entry, Text original) {
        MutableText decorated = Text.literal(teamglowing$hasHeadColumn() ? TEAMGLOWING_HEAD_NAME_PADDING : TEAMGLOWING_NAME_LEFT_PADDING)
            .append(original.copy());
        String partyName = teamglowing$getPartyName(entry);
        if (!partyName.isBlank()) {
            decorated.append(Text.literal(" [" + partyName + "]").formatted(Formatting.BLUE));
        }
        return decorated;
    }

    @Unique
    private boolean teamglowing$hasParty(PlayerListEntry entry) {
        GameProfile profile = entry.getProfile();
        return profile != null && ClientPartyTabCache.hasParty(
            profile.getId() == null ? null : profile.getId().toString(),
            profile.getName()
        );
    }

    @Unique
    private String teamglowing$getPartyName(PlayerListEntry entry) {
        GameProfile profile = entry.getProfile();
        if (profile == null) {
            return "";
        }
        return ClientPartyTabCache.getPartyName(
            profile.getId() == null ? null : profile.getId().toString(),
            profile.getName()
        );
    }

    @Unique
    private int teamglowing$getColor(PlayerListEntry entry) {
        GameProfile profile = entry.getProfile();
        if (profile == null) {
            return 0xFFFFFF;
        }
        
        String playerName = profile.getName();
        boolean self = this.client.player != null && profile.getId() != null && profile.getId().equals(this.client.player.getUuid());
        boolean hasParty = ClientPartyTabCache.hasParty(profile.getId() == null ? null : profile.getId().toString(), playerName);
        boolean sameParty = ClientPartyTabCache.isSameParty(profile.getId() == null ? null : profile.getId().toString(), playerName);
        
        if (sameParty || (self && hasParty)) {
            return ClientPlayerColorHelper.getPlayerColor(playerName) & 0xFFFFFF;
        }
        return 0xFFFFFF;
    }

    @Unique
    private boolean teamglowing$isSelf(PlayerListEntry entry) {
        GameProfile profile = entry.getProfile();
        return this.client.player != null && profile != null && profile.getId() != null && profile.getId().equals(this.client.player.getUuid());
    }

    @Unique
    private boolean teamglowing$hasHeadColumn() {
        return this.client.isInSingleplayer()
            || (this.client.getNetworkHandler() != null && this.client.getNetworkHandler().getConnection().isEncrypted());
    }

    @Unique
    private void teamglowing$renderDiamond(DrawContext context, int x, int y, int rgbColor, boolean self) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x + TEAMGLOWING_ICON_SIZE * 0.5F, y + TEAMGLOWING_ICON_SIZE * 0.5F);
        context.getMatrices().mul(new Matrix3x2f().rotateLocal((float) (Math.PI / 4.0D)));
        context.getMatrices().translate(-TEAMGLOWING_DIAMOND_SIZE * 0.5F, -TEAMGLOWING_DIAMOND_SIZE * 0.5F);
        if (self) {
            int color = 0xFF000000 | rgbColor;
            teamglowing$fillDiamondFrame(context, 0, 0, 6, color);
            context.fill(2, 2, 4, 4, color);
        } else {
            context.fill(0, 0, 6, 6, 0xFF000000 | rgbColor);
        }
        context.getMatrices().popMatrix();
    }

    @Unique
    private void teamglowing$fillDiamondFrame(DrawContext context, int x, int y, int size, int color) {
        context.fill(x, y, x + size, y + 1, color);
        context.fill(x, y + size - 1, x + size, y + size, color);
        context.fill(x, y + 1, x + 1, y + size - 1, color);
        context.fill(x + size - 1, y + 1, x + size, y + size - 1, color);
    }
}
