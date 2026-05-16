package cn.kafei.TeamGlowing.mixin;

import cn.kafei.TeamGlowing.client.ClientPartyTabCache;
import cn.kafei.TeamGlowing.client.ClientPlayerColorHelper;
import cn.kafei.TeamGlowing.client.ClientServerTabOverlayConfigCache;
import cn.kafei.TeamGlowing.config.TabOverlayConfigDefaults;
import cn.kafei.TeamGlowing.config.TabOverlayConfigState;
import com.mojang.authlib.GameProfile;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Unique
    private static final int TEAMGLOWING_OTHER_PARTY_RIGHT_COLOR = 0x808080;

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    private InGameHud inGameHud;

    @Shadow
    private Text footer;

    @Shadow
    private Text header;

    @Shadow
    protected abstract void renderLatencyIcon(DrawContext context, int width, int x, int y, PlayerListEntry entry);

    @Shadow
    public abstract Text getPlayerName(PlayerListEntry entry);

    @Shadow
    private List<PlayerListEntry> collectPlayerEntries() {
        throw new AssertionError();
    }

    @Unique
    private static final int TEAMGLOWING_TOP = 13;

    @Unique
    private static final int TEAMGLOWING_ROW_HEIGHT = 9;

    @Unique
    private static final int TEAMGLOWING_COLUMN_GAP = 2;

    @Unique
    private static final int TEAMGLOWING_CELL_PADDING_X = 2;

    @Unique
    private static final int TEAMGLOWING_CELL_PADDING_Y = 1;

    @Unique
    private static final int TEAMGLOWING_ICON_SIZE = 8;

    @Unique
    private static final int TEAMGLOWING_HEAD_SIZE = 8;

    @Unique
    private static final int TEAMGLOWING_MAX_PARTY_NAME_CHARACTERS = 5;

    @Unique
    private static final int TEAMGLOWING_PART_GAP = 1;

    @Unique
    private static final int TEAMGLOWING_PING_TEXT_GAP = 2;

    @Unique
    private static final int TEAMGLOWING_MAX_PING_TEXT_WIDTH = 28;

    @Unique
    private static final DateTimeFormatter TEAMGLOWING_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Unique
    private static final DateTimeFormatter TEAMGLOWING_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Unique
    private static final int TEAMGLOWING_MAX_NAME_WIDTH = 136;

    @Unique
    private static final int TEAMGLOWING_MAX_PLAYER_NAME_WIDTH = 104;

    @Unique
    private static final int TEAMGLOWING_MAX_PARTY_SUFFIX_WIDTH = 56;

    @Unique
    private static final int TEAMGLOWING_PARTY_AREA_GAP = 4;

    @Unique
    private static final int TEAMGLOWING_BACKGROUND = 0x66101824;

    @Unique
    private static final int TEAMGLOWING_TEXT_COLOR = 0xFFFFFFFF;

    @Unique
    private static final int TEAMGLOWING_SECONDARY_TEXT_COLOR = 0xFFB7C4D6;

    @Unique
    private static final int TEAMGLOWING_WELCOME_BAND_BACKGROUND = 0x90101F31;

    @Unique
    private static final int TEAMGLOWING_WELCOME_BAND_ACCENT = 0xFF3FA7D6;

    @Unique
    private static final int TEAMGLOWING_DIAMOND_BODY_SIZE = 6;

    @Unique
    private static final int TEAMGLOWING_BORDER_LENGTH = 48;

    @Unique
    private static final int TEAMGLOWING_BORDER_HIGHLIGHT_LENGTH = 5;

    @Unique
    private static final long TEAMGLOWING_BORDER_INTERVAL_MS = 90L;

    @Unique
    private static final int TEAMGLOWING_BOTTOM_BORDER_SHIFT = 3;

    @Unique
    private static final int TEAMGLOWING_TITLE_TOP_PADDING = 3;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void teamglowing$renderCustomTab(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        if (this.client.player == null || this.client.getNetworkHandler() == null) {
            return;
        }

        List<PlayerListEntry> entries = new ArrayList<>(this.collectPlayerEntries());
        if (entries.isEmpty()) {
            ci.cancel();
            return;
        }
        entries.sort(Comparator
            .comparingInt(this::teamglowing$getSortOrder)
            .thenComparing(this::teamglowing$getPartyName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(this::teamglowing$getProfileName, String.CASE_INSENSITIVE_ORDER));

        int count = entries.size();
        int columns = 1;
        int rows = count;
        while (rows > PlayerListHud.MAX_ROWS) {
            columns++;
            rows = (count + columns - 1) / columns;
        }

        boolean showHeads = teamglowing$shouldRenderHeads();

        int maxTextWidth = 0;
        int maxPingTextWidth = 0;
        for (PlayerListEntry entry : entries) {
            maxTextWidth = Math.max(maxTextWidth, teamglowing$getReservedNameAreaWidth(entry));
            maxPingTextWidth = Math.max(maxPingTextWidth, Math.min(TEAMGLOWING_MAX_PING_TEXT_WIDTH, this.client.textRenderer.getWidth(teamglowing$getLatencyText(entry))));
        }

        int cellWidth = TEAMGLOWING_CELL_PADDING_X * 2
            + TEAMGLOWING_ICON_SIZE
            + TEAMGLOWING_PART_GAP
            + (showHeads ? TEAMGLOWING_HEAD_SIZE + TEAMGLOWING_PART_GAP : 0)
            + maxTextWidth
            + TEAMGLOWING_PING_TEXT_GAP
            + Math.max(maxPingTextWidth, TEAMGLOWING_MAX_PING_TEXT_WIDTH)
            + TEAMGLOWING_PING_TEXT_GAP;

        int totalWidth = columns * cellWidth + (columns - 1) * TEAMGLOWING_COLUMN_GAP;
        List<Text> headerLines = teamglowing$getConfiguredHeaderLines();
        List<Text> footerLines = teamglowing$getConfiguredFooterLines();
        int headerWidth = teamglowing$getMaxLineWidth(headerLines);
        int footerWidth = teamglowing$getMaxLineWidth(footerLines);
        int panelWidth = Math.max(totalWidth, Math.max(headerWidth, footerWidth));
        int panelX = scaledWindowWidth / 2 - panelWidth / 2;
        int x = scaledWindowWidth / 2 - totalWidth / 2;
        int y = TEAMGLOWING_TOP;
        int headerHeight = teamglowing$getHeaderHeight(headerLines);
        int headerBottom = headerLines.isEmpty() ? y : y + headerHeight + 1;
        int listTop = headerBottom;
        int entriesBottom = listTop + rows * TEAMGLOWING_ROW_HEIGHT;
        int footerHeight = footerLines.isEmpty() ? 0 : footerLines.size() * this.client.textRenderer.fontHeight + 1;
        int panelBottom = entriesBottom + footerHeight;
        context.fill(panelX - 1, y - 1, panelX + panelWidth + 1, panelBottom, TEAMGLOWING_BACKGROUND);

        y = teamglowing$renderHeader(context, scaledWindowWidth, panelWidth, panelX, y, headerLines);

        for (int index = 0; index < count; index++) {
            int column = index / rows;
            int row = index % rows;
            int cellX = x + column * (cellWidth + TEAMGLOWING_COLUMN_GAP);
            int cellY = y + row * TEAMGLOWING_ROW_HEIGHT;
            PlayerListEntry entry = entries.get(index);
            teamglowing$renderEntry(context, scoreboard, objective, entry, cellX, cellY, cellWidth, showHeads);
        }

        int bottom = entriesBottom;
        teamglowing$renderFooter(context, scaledWindowWidth, panelWidth, panelX, bottom, footerLines);
        ci.cancel();
    }

    @Unique
    private int teamglowing$renderHeader(DrawContext context, int scaledWindowWidth, int totalWidth, int x, int y, List<Text> lines) {
        if (lines.isEmpty()) {
            return y;
        }

        int headerHeight = teamglowing$getHeaderHeight(lines);
        for (int i = 0; i < lines.size(); i++) {
            int lineY = y + i * this.client.textRenderer.fontHeight;
            if (i > 0) {
                lineY += TEAMGLOWING_TITLE_TOP_PADDING;
            }
            if (i == 1) {
                teamglowing$renderHeaderBand(context, x + 6, Math.max(0, totalWidth - 12), lineY, TEAMGLOWING_WELCOME_BAND_BACKGROUND, TEAMGLOWING_WELCOME_BAND_ACCENT);
            }
            Text line = lines.get(i);
            context.drawTextWithShadow(this.client.textRenderer, line, scaledWindowWidth / 2 - this.client.textRenderer.getWidth(line) / 2, lineY, TEAMGLOWING_TEXT_COLOR);
        }
        return y + headerHeight + 1;
    }

    @Unique
    private int teamglowing$getHeaderHeight(List<Text> lines) {
        if (lines.isEmpty()) {
            return 0;
        }
        int height = lines.size() * this.client.textRenderer.fontHeight;
        if (lines.size() > 1) {
            height += TEAMGLOWING_TITLE_TOP_PADDING;
        }
        return height;
    }

    @Unique
    private void teamglowing$renderHeaderBand(DrawContext context, int x, int width, int y, int backgroundColor, int accentColor) {
        if (width <= 0) {
            return;
        }
        int bandTop = y - 1;
        int bandBottom = y + this.client.textRenderer.fontHeight + 1;
        context.fill(x, bandTop, x + width, bandBottom, backgroundColor);
        context.fill(x, bandTop, x + width, bandTop + 1, accentColor);
        context.fill(x, bandBottom - 1, x + width, bandBottom, accentColor & 0x99FFFFFF);
    }

    @Unique
    private void teamglowing$renderFooter(DrawContext context, int scaledWindowWidth, int totalWidth, int x, int y, List<Text> lines) {
        if (lines.isEmpty()) {
            return;
        }

        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            context.drawTextWithShadow(this.client.textRenderer, line, scaledWindowWidth / 2 - this.client.textRenderer.getWidth(line) / 2, y + i * this.client.textRenderer.fontHeight, TEAMGLOWING_TEXT_COLOR);
        }
    }

    @Unique
    private void teamglowing$renderEntry(
        DrawContext context,
        Scoreboard scoreboard,
        ScoreboardObjective objective,
        PlayerListEntry entry,
        int cellX,
        int cellY,
        int cellWidth,
        boolean showHeads
    ) {
        int left = cellX + TEAMGLOWING_CELL_PADDING_X;
        int innerHeight = TEAMGLOWING_ROW_HEIGHT;
        int centerY = cellY + (innerHeight - TEAMGLOWING_ICON_SIZE) / 2;
        int iconX = left;
        int color = teamglowing$getColor(entry);
        boolean self = teamglowing$isSelf(entry);
        boolean hasParty = teamglowing$hasParty(entry);
        boolean sameParty = teamglowing$isSameParty(entry);
        teamglowing$renderDiamond(context, iconX, centerY, color, self, sameParty, hasParty);

        int cursorX = iconX + TEAMGLOWING_ICON_SIZE + TEAMGLOWING_PART_GAP;
        if (showHeads) {
            SkinTextures textures = entry.getSkinTextures();
            PlayerSkinDrawer.draw(context, textures, cursorX, centerY, TEAMGLOWING_HEAD_SIZE);
            cursorX += TEAMGLOWING_HEAD_SIZE + TEAMGLOWING_PART_GAP;
        }

        int pingAreaRight = cellX + cellWidth - TEAMGLOWING_CELL_PADDING_X;
        String latencyText = teamglowing$getLatencyText(entry);
        int nameRight = pingAreaRight - TEAMGLOWING_PING_TEXT_GAP - TEAMGLOWING_MAX_PING_TEXT_WIDTH;
        int nameMaxWidth = Math.max(0, nameRight - cursorX);

        int nameY = cellY + (TEAMGLOWING_ROW_HEIGHT - this.client.textRenderer.fontHeight) / 2;
        String partyName = teamglowing$getPartyName(entry);
        Text baseName = Text.literal(teamglowing$getProfileName(entry));
        int partyWidth = partyName.isBlank() ? 0 : this.client.textRenderer.getWidth(" [" + partyName + "]");
        int reservedPartyWidth = Math.min(nameMaxWidth, partyWidth);
        int baseNameWidth = Math.min(Math.max(0, nameMaxWidth - reservedPartyWidth), TEAMGLOWING_MAX_NAME_WIDTH);
        if (partyName.isBlank()) {
            baseNameWidth = Math.min(nameMaxWidth, TEAMGLOWING_MAX_NAME_WIDTH);
        }
        Text trimmedBaseName = teamglowing$trimStyledText(baseName, baseNameWidth);
        if (!trimmedBaseName.getString().isEmpty()) {
            context.drawTextWithShadow(this.client.textRenderer, trimmedBaseName, cursorX, nameY, TEAMGLOWING_TEXT_COLOR);
        }
        if (!partyName.isBlank()) {
            int partyX = cursorX + this.client.textRenderer.getWidth(trimmedBaseName);
            int remainingPartyWidth = Math.max(0, nameMaxWidth - this.client.textRenderer.getWidth(trimmedBaseName) - this.client.textRenderer.getWidth(" "));
            Text trimmedPartySuffix = teamglowing$getTrimmedPartySuffix(partyName, remainingPartyWidth, teamglowing$getPartyColor(entry));
            if (!trimmedPartySuffix.getString().isEmpty()) {
                context.drawTextWithShadow(this.client.textRenderer, Text.literal(" ").append(trimmedPartySuffix), partyX, nameY, TEAMGLOWING_TEXT_COLOR);
            }
        }

        if (!latencyText.isEmpty()) {
            String trimmedLatency = teamglowing$trimStringToWidth(latencyText, TEAMGLOWING_MAX_PING_TEXT_WIDTH);
            if (!trimmedLatency.isEmpty()) {
                int pingTextX = pingAreaRight - this.client.textRenderer.getWidth(trimmedLatency);
                context.drawTextWithShadow(this.client.textRenderer, trimmedLatency, pingTextX, nameY, TEAMGLOWING_SECONDARY_TEXT_COLOR);
            }
        }
    }

    @Unique
    private List<Text> teamglowing$getConfiguredHeaderLines() {
        return teamglowing$buildConfiguredLines(true);
    }

    @Unique
    private List<Text> teamglowing$getConfiguredFooterLines() {
        return teamglowing$buildConfiguredLines(false);
    }

    @Unique
    private int teamglowing$getMaxLineWidth(List<Text> lines) {
        int maxWidth = 0;
        for (Text line : lines) {
            maxWidth = Math.max(maxWidth, this.client.textRenderer.getWidth(line));
        }
        return maxWidth;
    }

    @Unique
    private List<Text> teamglowing$buildConfiguredLines(boolean headerSection) {
        TabOverlayConfigState config = ClientServerTabOverlayConfigCache.getOrFallback(TabOverlayConfigDefaults.createState());
        List<Text> lines = new ArrayList<>();
        if (headerSection && config.topBorderEnabled()) {
            lines.add(teamglowing$parseConfiguredText(teamglowing$replaceConfiguredText(config.topBorderText(), 0)));
        }

        List<String> rawLines = headerSection ? config.headerLines() : config.footerLines();
        for (String rawLine : rawLines) {
            lines.add(teamglowing$parseConfiguredText(teamglowing$replaceConfiguredText(rawLine, 0)));
        }

        if (!headerSection && config.bottomBorderEnabled()) {
            lines.add(teamglowing$parseConfiguredText(teamglowing$replaceConfiguredText(config.bottomBorderText(), TEAMGLOWING_BOTTOM_BORDER_SHIFT)));
        }
        return lines;
    }

    @Unique
    private String teamglowing$getSelfLatencyText() {
        if (this.client.player == null || this.client.getNetworkHandler() == null) {
            return "--ms";
        }

        PlayerListEntry entry = this.client.getNetworkHandler().getPlayerListEntry(this.client.player.getUuid());
        if (entry == null) {
            return "--ms";
        }
        return teamglowing$getLatencyText(entry);
    }

    @Unique
    private String teamglowing$getServerTpsText() {
        return teamglowing$getServerStatText("TPS", "--");
    }

    @Unique
    private String teamglowing$getServerMsptText() {
        return teamglowing$getServerStatText("MSPT", "--");
    }

    @Unique
    private String teamglowing$getServerMemoryText() {
        return teamglowing$getServerStatText("MEM", "--");
    }

    @Unique
    private String teamglowing$getServerStatText(String key, String fallback) {
        if (this.header == null) {
            return fallback;
        }
        String raw = this.header.getString();
        if (raw.isBlank()) {
            return fallback;
        }
        String prefix = key + ":";
        for (String line : raw.split("\\R")) {
            if (line.startsWith(prefix)) {
                String value = line.substring(prefix.length()).trim();
                return value.isEmpty() ? fallback : value;
            }
        }
        return fallback;
    }

    @Unique
    private String teamglowing$getLatencyText(PlayerListEntry entry) {
        if (entry == null) {
            return "";
        }
        return Math.max(0, entry.getLatency()) + "ms";
    }

    @Unique
    private String teamglowing$replaceTabPlaceholders(String input) {
        String playerName = this.client.player == null ? "-" : this.client.player.getGameProfile().getName();
        String partyName = ClientPartyTabCache.getOwnPartyName().isBlank() ? "无" : ClientPartyTabCache.getOwnPartyName();
        String onlineCount = this.client.getNetworkHandler() == null ? "0" : Integer.toString(this.client.getNetworkHandler().getPlayerList().size());
        LocalDateTime now = LocalDateTime.now();
        TabOverlayConfigState config = ClientServerTabOverlayConfigCache.getOrFallback(TabOverlayConfigDefaults.createState());
        return input
            .replace("%player%", playerName)
            .replace("%ping%", teamglowing$getSelfLatencyText())
            .replace("%tps%", teamglowing$getServerTpsText())
            .replace("%mspt%", teamglowing$getServerMsptText())
            .replace("%memory%", teamglowing$getServerMemoryText())
            .replace("%mem%", teamglowing$getServerMemoryText())
            .replace("%date%", now.format(TEAMGLOWING_DATE_FORMATTER))
            .replace("%time%", now.format(TEAMGLOWING_TIME_FORMATTER))
            .replace("%party%", partyName)
            .replace("%online%", onlineCount)
            .replace("%welcome%", teamglowing$getWelcomeMarquee(config, playerName));
    }

    @Unique
    private String teamglowing$replaceConfiguredText(String input, int borderShift) {
        return teamglowing$replaceTabPlaceholders(input)
            .replace("%border%", teamglowing$getScrollingBorder(borderShift));
    }

    @Unique
    private String teamglowing$getScrollingBorder(int shift) {
        int length = Math.max(8, TEAMGLOWING_BORDER_LENGTH);
        int highlightLength = Math.max(1, Math.min(TEAMGLOWING_BORDER_HIGHLIGHT_LENGTH, length));
        int offset = (int) ((System.currentTimeMillis() / TEAMGLOWING_BORDER_INTERVAL_MS) % length);
        offset = Math.floorMod(offset + shift, length);
        StringBuilder border = new StringBuilder(length * 4);
        border.append("&e&m");
        boolean goldActive = true;
        for (int i = 0; i < length; i++) {
            boolean highlight = teamglowing$isBorderHighlightIndex(i, offset, highlightLength, length);
            if (highlight && goldActive) {
                border.append("&f");
                goldActive = false;
            } else if (!highlight && !goldActive) {
                border.append("&e");
                goldActive = true;
            }
            border.append('=');
        }
        return border.toString();
    }

    @Unique
    private boolean teamglowing$isBorderHighlightIndex(int index, int offset, int highlightLength, int totalLength) {
        int end = offset + highlightLength;
        if (end <= totalLength) {
            return index >= offset && index < end;
        }
        return index >= offset || index < end - totalLength;
    }

    @Unique
    private String teamglowing$getWelcomeMarquee(TabOverlayConfigState config, String playerName) {
        String template = config.welcomeText()
            .replace("%player%", playerName)
            .replace("%ping%", teamglowing$getSelfLatencyText())
            .replace("%tps%", teamglowing$getServerTpsText())
            .replace("%mspt%", teamglowing$getServerMsptText())
            .replace("%memory%", teamglowing$getServerMemoryText())
            .replace("%mem%", teamglowing$getServerMemoryText());
        String padded = "    " + template + "    ";
        int width = Math.max(6, config.welcomeWidth());
        long intervalMs = Math.max(50L, config.welcomeIntervalMs());
        String scrollingSource = padded + padded;
        int length = padded.length();
        if (length == 0) {
            return "";
        }

        int offset = (int) ((System.currentTimeMillis() / intervalMs) % length);
        String window = scrollingSource.substring(offset, offset + Math.min(width, length));
        if (window.length() >= width) {
            return window;
        }
        return window + " ".repeat(width - window.length());
    }

    @Unique
    private Text teamglowing$parseConfiguredText(String value) {
        MutableText result = Text.empty();
        StringBuilder current = new StringBuilder();
        Formatting activeColor = null;
        List<Formatting> modifiers = new ArrayList<>();

        for (int i = 0; i < value.length(); i++) {
            char currentChar = value.charAt(i);
            if (currentChar == '&' && i + 1 < value.length()) {
                Formatting formatting = Formatting.byCode(value.charAt(i + 1));
                if (formatting != null) {
                    if (!current.isEmpty()) {
                        result.append(teamglowing$styledLiteral(current.toString(), activeColor, modifiers));
                        current.setLength(0);
                    }
                    if (formatting == Formatting.RESET) {
                        activeColor = null;
                        modifiers = new ArrayList<>();
                    } else if (formatting.isColor()) {
                        activeColor = formatting;
                    } else {
                        modifiers.add(formatting);
                    }
                    i++;
                    continue;
                }
            }
            current.append(currentChar);
        }

        if (!current.isEmpty()) {
            result.append(teamglowing$styledLiteral(current.toString(), activeColor, modifiers));
        }
        return result;
    }

    @Unique
    private Text teamglowing$styledLiteral(String value, Formatting color, List<Formatting> modifiers) {
        MutableText text = Text.literal(value);
        if (color != null) {
            text.formatted(color);
        }
        if (!modifiers.isEmpty()) {
            text.formatted(modifiers.toArray(Formatting[]::new));
        }
        return text;
    }

    @Unique
    private Text teamglowing$getDecoratedName(PlayerListEntry entry) {
        MutableText decorated = Text.literal(teamglowing$getProfileName(entry));
        String partyName = teamglowing$getPartyName(entry);
        if (!partyName.isBlank()) {
            decorated.append(Text.literal(" [" + partyName + "]").styled(style -> style.withColor(teamglowing$getPartyColor(entry))));
        }
        return decorated;
    }

    @Unique
    private int teamglowing$getReservedNameAreaWidth(PlayerListEntry entry) {
        int baseWidth = this.client.textRenderer.getWidth(teamglowing$getProfileName(entry));
        String partyName = teamglowing$getPartyName(entry);
        if (!partyName.isBlank()) {
            baseWidth += this.client.textRenderer.getWidth(" [" + partyName + "]");
        }
        return Math.min(TEAMGLOWING_MAX_NAME_WIDTH, baseWidth);
    }

    @Unique
    private Text teamglowing$getTabDisplayName(PlayerListEntry entry) {
        String raw = this.getPlayerName(entry).getString();
        if (raw.startsWith("\u25c6 ")) {
            raw = raw.substring(2);
        }
        return Text.literal(raw);
    }

    @Unique
    private int teamglowing$getPartyAreaWidth() {
        return Math.max(TEAMGLOWING_MAX_PARTY_SUFFIX_WIDTH, this.client.textRenderer.getWidth("[WWWWW]"));
    }

    @Unique
    private Text teamglowing$getTrimmedPartySuffix(String partyName, int maxWidth) {
        return teamglowing$getTrimmedPartySuffix(partyName, maxWidth, 0xFFFFFF);
    }

    @Unique
    private Text teamglowing$getTrimmedPartySuffix(String partyName, int maxWidth, int color) {
        if (partyName == null || partyName.isBlank() || maxWidth <= 0) {
            return Text.empty();
        }

        String limitedPartyName = teamglowing$limitPartyName(partyName, TEAMGLOWING_MAX_PARTY_NAME_CHARACTERS);
        String fullSuffix = "[" + limitedPartyName + "]";
        String trimmedSuffix = teamglowing$trimStringToWidthWithoutEllipsis(fullSuffix, maxWidth);
        if (trimmedSuffix.isEmpty()) {
            return Text.empty();
        }
        return Text.literal(trimmedSuffix).styled(style -> style.withColor(color));
    }

    @Unique
    private Text teamglowing$trimStyledText(Text text, int maxWidth) {
        String trimmed = teamglowing$trimStringToWidth(text.getString(), maxWidth);
        if (trimmed.isEmpty()) {
            return Text.empty();
        }
        return Text.literal(trimmed).setStyle(text.getStyle());
    }

    @Unique
    private String teamglowing$trimStringToWidth(String value, int maxWidth) {
        if (value == null || value.isEmpty() || maxWidth <= 0) {
            return "";
        }

        if (this.client.textRenderer.getWidth(value) <= maxWidth) {
            return value;
        }

        String ellipsis = "...";
        int ellipsisWidth = this.client.textRenderer.getWidth(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return "";
        }

        int end = value.length();
        while (end > 0) {
            String candidate = value.substring(0, end) + ellipsis;
            if (this.client.textRenderer.getWidth(candidate) <= maxWidth) {
                return candidate;
            }
            end--;
        }
        return "";
    }

    @Unique
    private String teamglowing$trimStringToWidthWithoutEllipsis(String value, int maxWidth) {
        if (value == null || value.isEmpty() || maxWidth <= 0) {
            return "";
        }

        if (this.client.textRenderer.getWidth(value) <= maxWidth) {
            return value;
        }

        int end = value.length();
        while (end > 0) {
            String candidate = value.substring(0, end);
            if (this.client.textRenderer.getWidth(candidate) <= maxWidth) {
                return candidate;
            }
            end--;
        }
        return "";
    }

    @Unique
    private String teamglowing$limitPartyName(String value, int maxCharacters) {
        if (value == null || value.isEmpty() || maxCharacters <= 0) {
            return "";
        }

        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= maxCharacters) {
            return value;
        }

        int endIndex = value.offsetByCodePoints(0, maxCharacters);
        return value.substring(0, endIndex);
    }

    @Unique
    private String teamglowing$getPartyName(PlayerListEntry entry) {
        GameProfile profile = entry.getProfile();
        if (profile == null) {
            return "";
        }
        return ClientPartyTabCache.getPartyName(profile.getId() == null ? null : profile.getId().toString(), profile.getName());
    }

    @Unique
    private int teamglowing$getPartyColor(PlayerListEntry entry) {
        GameProfile profile = entry.getProfile();
        if (profile == null) {
            return 0xFFFFFF;
        }
        return ClientPartyTabCache.getPartyColor(profile.getId() == null ? null : profile.getId().toString(), profile.getName()) & 0xFFFFFF;
    }

    @Unique
    private String teamglowing$getProfileName(PlayerListEntry entry) {
        GameProfile profile = entry.getProfile();
        return profile == null || profile.getName() == null ? "" : profile.getName();
    }

    @Unique
    private int teamglowing$getSortOrder(PlayerListEntry entry) {
        GameProfile profile = entry.getProfile();
        if (profile == null) {
            return Integer.MAX_VALUE;
        }
        return ClientPartyTabCache.getSortOrder(profile.getId() == null ? null : profile.getId().toString(), profile.getName());
    }

    @Unique
    private int teamglowing$getColor(PlayerListEntry entry) {
        GameProfile profile = entry.getProfile();
        if (profile == null) {
            return 0xFFFFFF;
        }

        String playerName = profile.getName();
        boolean hasParty = ClientPartyTabCache.hasParty(profile.getId() == null ? null : profile.getId().toString(), playerName);
        if (hasParty) {
            return ClientPlayerColorHelper.getPlayerColor(playerName) & 0xFFFFFF;
        }
        return 0xFFFFFF;
    }

    @Unique
    private boolean teamglowing$hasParty(PlayerListEntry entry) {
        GameProfile profile = entry.getProfile();
        if (profile == null) {
            return false;
        }
        return ClientPartyTabCache.hasParty(profile.getId() == null ? null : profile.getId().toString(), profile.getName());
    }

    @Unique
    private boolean teamglowing$isSameParty(PlayerListEntry entry) {
        GameProfile profile = entry.getProfile();
        if (profile == null) {
            return false;
        }
        return ClientPartyTabCache.isSameParty(profile.getId() == null ? null : profile.getId().toString(), profile.getName());
    }

    @Unique
    private boolean teamglowing$isSelf(PlayerListEntry entry) {
        GameProfile profile = entry.getProfile();
        return this.client.player != null && profile != null && profile.getId() != null && profile.getId().equals(this.client.player.getUuid());
    }

    @Unique
    private boolean teamglowing$shouldRenderHeads() {
        return this.client.isInSingleplayer()
            || (this.client.getNetworkHandler() != null && this.client.getNetworkHandler().getConnection().isEncrypted());
    }

    @Unique
    private void teamglowing$renderDiamond(DrawContext context, int x, int y, int rgbColor, boolean self, boolean sameParty, boolean hasParty) {
        if (hasParty && !sameParty && !self) {
            teamglowing$renderSplitDiamond(context, x, y, 0xFFFFFFFF, 0xFF808080);
            return;
        }
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x + TEAMGLOWING_ICON_SIZE * 0.5F, y + TEAMGLOWING_ICON_SIZE * 0.5F);
        context.getMatrices().mul(new Matrix3x2f().rotateLocal((float)(Math.PI / 4.0D)));
        context.getMatrices().translate(-TEAMGLOWING_DIAMOND_BODY_SIZE * 0.5F, -TEAMGLOWING_DIAMOND_BODY_SIZE * 0.5F);
        int color = 0xFF000000 | rgbColor;
        if (self) {
            teamglowing$fillDiamondFrame(context, 0, 0, TEAMGLOWING_DIAMOND_BODY_SIZE, color);
            context.fill(2, 2, 4, 4, color);
        } else {
            context.fill(0, 0, TEAMGLOWING_DIAMOND_BODY_SIZE, TEAMGLOWING_DIAMOND_BODY_SIZE, color);
        }
        context.getMatrices().popMatrix();
    }

    @Unique
    private void teamglowing$renderSplitDiamond(DrawContext context, int x, int y, int leftColor, int rightColor) {
        teamglowing$drawSplitDiamondRow(context, x + 2, y, 2, leftColor, rightColor);
        teamglowing$drawSplitDiamondRow(context, x + 1, y + 1, 4, leftColor, rightColor);
        teamglowing$drawSplitDiamondRow(context, x, y + 2, 6, leftColor, rightColor);
        teamglowing$drawSplitDiamondRow(context, x, y + 3, 6, leftColor, rightColor);
        teamglowing$drawSplitDiamondRow(context, x + 1, y + 4, 4, leftColor, rightColor);
        teamglowing$drawSplitDiamondRow(context, x + 2, y + 5, 2, leftColor, rightColor);
    }

    @Unique
    private void teamglowing$drawSplitDiamondRow(DrawContext context, int x, int y, int width, int leftColor, int rightColor) {
        int leftWidth = (width + 1) / 2;
        int rightWidth = width - leftWidth;
        context.fill(x, y, x + leftWidth, y + 1, leftColor);
        if (rightWidth > 0) {
            context.fill(x + leftWidth, y, x + width, y + 1, rightColor);
        }
    }

    @Unique
    private void teamglowing$fillDiamondFrame(DrawContext context, int x, int y, int size, int color) {
        context.fill(x, y, x + size, y + 1, color);
        context.fill(x, y + size - 1, x + size, y + size, color);
        context.fill(x, y + 1, x + 1, y + size - 1, color);
        context.fill(x + size - 1, y + 1, x + size, y + size - 1, color);
    }
}
