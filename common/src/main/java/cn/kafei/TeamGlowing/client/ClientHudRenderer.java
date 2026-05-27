package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.network.TeamLocatorEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;

public final class ClientHudRenderer {
    private static final int BAR_WIDTH = 182;
    private static final int BAR_Y_OFFSET = 29;
    private static final int ICON_SIZE = 9;
    private static final int ARROW_SIZE = 9;
    private static final int ARROW_TEXTURE_SIZE = 18;
    private static final int MARKER_Y_OFFSET = 2;
    private static final int PARTY_BANNER_ACCENT_COLOR = 0xFFD54A;
    private static final int MAX_DISPLAY = 4;
    private static final double MAX_ANGLE = 90.0D;
    private static final double HEIGHT_THRESHOLD = 3.0D;
    private static final int EDGE_FADE_MARGIN = 18;
    private static final float POSITION_SMOOTHING_TIME_MS = 48.0F;
    private static final float POSITION_SNAP_DISTANCE = 72.0F;
    private static final Map<String, SmoothedPosition> SMOOTH_POSITIONS = new HashMap<>();

    private ClientHudRenderer() {
    }

    public static void render(GuiGraphics context) {
        if (!ClientToggleState.isEnabled()) {
            SMOOTH_POSITIONS.clear();
            return;
        }

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            SMOOTH_POSITIONS.clear();
            return;
        }

        if (player.getVehicle() instanceof PlayerRideableJumping) {
            SMOOTH_POSITIONS.clear();
            return;
        }

        List<TeamLocatorEntry> entries = ClientLocatorCache.getEntries();
        if (entries.isEmpty()) {
            SMOOTH_POSITIONS.clear();
            return;
        }

        entries = dedupeEntries(entries);
        pruneUnusedSmoothPositions(entries);

        Entity camera = client.getCameraEntity();
        if (camera == null) {
            camera = player;
        }

        int screenWidth = context.guiWidth();
        int screenHeight = context.guiHeight();
        int barLeft = (screenWidth - BAR_WIDTH) / 2;
        int barY = screenHeight - BAR_Y_OFFSET;
        boolean showNames = client.options.keyPlayerList.isDown();

        int rendered = 0;
        for (TeamLocatorEntry entry : entries) {
            if (rendered >= MAX_DISPLAY) {
                break;
            }

            double distance = player.distanceToSqr(entry.x(), entry.y(), entry.z());
            double relativeAngle = getRelativeAngleDegrees(camera.getYRot(), camera.getX(), camera.getZ(), entry.x(), entry.z());
            if (Math.abs(relativeAngle) > MAX_ANGLE) {
                SMOOTH_POSITIONS.remove(getEntryKey(entry));
                continue;
            }

            float targetX = projectAngleToX(relativeAngle, barLeft, BAR_WIDTH);
            float currentX = getSmoothedX(getEntryKey(entry), targetX);
            float alpha = getEdgeAlpha(currentX, barLeft, barLeft + BAR_WIDTH);
            int textureIndex = getTextureIndexFromDistance(Math.sqrt(distance));
            int markerY = barY + MARKER_Y_OFFSET;
            if (entry.banner()) {
                drawBannerMarker(context, currentX, markerY, entry.bannerColorId(), entry.partyBanner(), alpha);
            } else {
                int playerColor = ClientPlayerColorHelper.getPlayerColor(entry.playerName());
                drawMarker(context, currentX, markerY, textureIndex, playerColor, alpha);
                drawHeightArrow(context, currentX, markerY, entry.y() - camera.getY(), alpha);
            }
            if (showNames) {
                drawPlayerName(context, getDisplayName(client, entry), currentX, markerY, alpha);
            }
            rendered++;
        }
    }

    private static void drawMarker(GuiGraphics context, float x, int barY, int textureIndex, int color, float alpha) {
        if (alpha <= 0.02F) {
            return;
        }

        int drawX = (int) x - ICON_SIZE / 2;
        int drawY = barY - ICON_SIZE / 2;
        int darkerColor = ClientPlayerColorHelper.getDarkerColor(color);
        
        // 转成带透明度的 ARGB，兼容层会按当前 MC 版本选择绘制 API。
        int outlineColor = ((int) (alpha * 255.0F) << 24) | (darkerColor & 0xFFFFFF);
        int dotColor = ((int) (alpha * 255.0F) << 24) | (color & 0xFFFFFF);

        ClientGuiRenderCompat.blitTexture(context, ClientHudTextures.PLAYER_DOT_OUTLINES[textureIndex], drawX, drawY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, outlineColor);
        ClientGuiRenderCompat.blitTexture(context, ClientHudTextures.PLAYER_DOTS[textureIndex], drawX, drawY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, dotColor);
    }

    private static void drawHeightArrow(GuiGraphics context, float x, int barY, double deltaY, float alpha) {
        if (Math.abs(deltaY) < HEIGHT_THRESHOLD || alpha <= 0.02F) {
            return;
        }

        int iconTop = barY - ICON_SIZE / 2;
        int iconBottom = iconTop + ICON_SIZE;
        int arrowX = Math.round(x) - ARROW_SIZE / 2;
        
        // 白色箭头只叠加透明度。
        int arrowColor = ((int) (alpha * 255.0F) << 24) | 0xFFFFFF;
        
        if (deltaY < 0.0D) {
            ClientGuiRenderCompat.blitTexture(context, ClientHudTextures.ARROW, arrowX, iconBottom, ARROW_SIZE, 0.0F, ARROW_SIZE, ARROW_SIZE, ARROW_TEXTURE_SIZE, ARROW_TEXTURE_SIZE, arrowColor);
        } else {
            ClientGuiRenderCompat.blitTexture(context, ClientHudTextures.ARROW, arrowX, iconTop - ARROW_SIZE, 0.0F, 0.0F, ARROW_SIZE, ARROW_SIZE, ARROW_TEXTURE_SIZE, ARROW_TEXTURE_SIZE, arrowColor);
        }
    }

    private static void drawBannerMarker(GuiGraphics context, float x, int barY, int bannerColorId, boolean partyBanner, float alpha) {
        if (alpha <= 0.02F) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Holder<MapDecorationType> type = getBannerDecorationType(bannerColorId);
        if (type == null) {
            return;
        }
        MapDecoration decoration = new MapDecoration(type, (byte) 0, (byte) 0, (byte) 0, Optional.empty());
        TextureAtlasSprite sprite = client.getMapDecorationTextures().get(decoration);
        int drawX = Math.round(x) - ICON_SIZE / 2;
        int drawY = barY - ICON_SIZE / 2;
        int color = ((int) (alpha * 255.0F) << 24) | 0xFFFFFF;
        ClientGuiRenderCompat.blitSprite(context, sprite, drawX, drawY, ICON_SIZE, ICON_SIZE, color);
        if (partyBanner) {
            drawPartyBannerAccent(context, drawX, drawY, alpha);
        }
    }

    private static void drawPartyBannerAccent(GuiGraphics context, int drawX, int drawY, float alpha) {
        int accentColor = ((int) (alpha * 255.0F) << 24) | PARTY_BANNER_ACCENT_COLOR;
        context.fill(drawX - 1, drawY - 1, drawX + ICON_SIZE + 1, drawY, accentColor);
        context.fill(drawX - 1, drawY + ICON_SIZE, drawX + ICON_SIZE + 1, drawY + ICON_SIZE + 1, accentColor);
        context.fill(drawX - 1, drawY, drawX, drawY + ICON_SIZE, accentColor);
        context.fill(drawX + ICON_SIZE, drawY, drawX + ICON_SIZE + 1, drawY + ICON_SIZE, accentColor);
    }

    private static void drawPlayerName(GuiGraphics context, String name, float x, int barY, float alpha) {
        if (alpha <= 0.02F) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        int textWidth = client.font.width(name);
        int color = ((int) (alpha * 255.0F) << 24) | 0xFFFFFF;
        context.drawString(client.font, Component.literal(name), (int) x - textWidth / 2, barY - 17, color, true);
    }

    private static float getSmoothedX(String key, float targetX) {
        long now = System.currentTimeMillis();
        SmoothedPosition current = SMOOTH_POSITIONS.get(key);
        if (current == null || Math.abs(targetX - current.x()) >= POSITION_SNAP_DISTANCE) {
            SmoothedPosition updated = new SmoothedPosition(targetX, now);
            SMOOTH_POSITIONS.put(key, updated);
            return targetX;
        }

        float deltaMs = Math.max(1.0F, now - current.updatedAtMillis());
        float smoothing = 1.0F - (float) Math.exp(-deltaMs / POSITION_SMOOTHING_TIME_MS);
        float smoothedX = Mth.lerp(smoothing, current.x(), targetX);
        SMOOTH_POSITIONS.put(key, new SmoothedPosition(smoothedX, now));
        return smoothedX;
    }

    private static float getEdgeAlpha(float currentX, int barLeft, int barRight) {
        float leftDistance = currentX - barLeft;
        float rightDistance = barRight - currentX;
        float minDistance = Math.min(leftDistance, rightDistance);
        if (minDistance >= EDGE_FADE_MARGIN) {
            return 1.0F;
        }
        if (minDistance <= 0.0F) {
            return 0.0F;
        }
        return minDistance / EDGE_FADE_MARGIN;
    }

    private static double getRelativeAngleDegrees(float playerYaw, double playerX, double playerZ, double targetX, double targetZ) {
        double dx = targetX - playerX;
        double dz = targetZ - playerZ;
        if (Math.abs(dx) < 0.0001D && Math.abs(dz) < 0.0001D) {
            return 0.0D;
        }

        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double relative = targetYaw - playerYaw;
        while (relative <= -180.0D) {
            relative += 360.0D;
        }
        while (relative > 180.0D) {
            relative -= 360.0D;
        }
        return relative;
    }

    private static float projectAngleToX(double relativeAngle, int barLeft, int barWidth) {
        double clampedAngle = Math.max(-MAX_ANGLE, Math.min(MAX_ANGLE, relativeAngle));
        double normalized = (clampedAngle + MAX_ANGLE) / (MAX_ANGLE * 2.0D);
        return (float) (barLeft + normalized * (barWidth - 1));
    }

    private static int getTextureIndexFromDistance(double distance) {
        if (distance < 128.0D) {
            return 0;
        }
        if (distance < 230.0D) {
            return 1;
        }
        if (distance < 332.0D) {
            return 2;
        }
        return 3;
    }

    private static String getDisplayName(Minecraft client, TeamLocatorEntry entry) {
        String name = entry.name();
        if (client.level == null || entry.dimensionId() == null || entry.dimensionId().isBlank()) {
            return name;
        }

        String currentDimensionId = client.level.dimension().location().toString();
        if (entry.dimensionId().equals(currentDimensionId)) {
            return name;
        }

        return name + I18n.get(getDimensionSuffixKey(entry.dimensionId()));
    }

    private static String getDimensionSuffixKey(String dimensionId) {
        if ("minecraft:the_nether".equals(dimensionId)) {
            return "teamglowing.dimension_suffix.nether";
        }
        if ("minecraft:overworld".equals(dimensionId)) {
            return "teamglowing.dimension_suffix.overworld";
        }
        if ("minecraft:the_end".equals(dimensionId)) {
            return "teamglowing.dimension_suffix.end";
        }
        return "teamglowing.dimension_suffix.other";
    }

    private static List<TeamLocatorEntry> dedupeEntries(List<TeamLocatorEntry> entries) {
        Map<String, TeamLocatorEntry> uniqueEntries = new LinkedHashMap<>();
        for (TeamLocatorEntry entry : entries) {
            uniqueEntries.putIfAbsent(getEntryKey(entry), entry);
        }
        return List.copyOf(uniqueEntries.values());
    }

    private static void pruneUnusedSmoothPositions(List<TeamLocatorEntry> entries) {
        Set<String> validKeys = new HashSet<>();
        for (TeamLocatorEntry entry : entries) {
            validKeys.add(getEntryKey(entry));
        }
        SMOOTH_POSITIONS.keySet().removeIf(key -> !validKeys.contains(key));
    }

    private static String getEntryKey(TeamLocatorEntry entry) {
        if (entry.entryId() != null && !entry.entryId().isBlank()) {
            return entry.entryId().toLowerCase();
        }
        if (entry.playerId() != null && !entry.playerId().isBlank()) {
            return entry.playerId();
        }
        if (entry.playerName() != null && !entry.playerName().isBlank()) {
            return entry.playerName().toLowerCase();
        }
        return entry.name().toLowerCase();
    }

    private static Holder<MapDecorationType> getBannerDecorationType(int bannerColorId) {
        DyeColor[] colors = DyeColor.values();
        DyeColor color = colors[Math.floorMod(bannerColorId, colors.length)];
        return switch (color) {
            case WHITE -> MapDecorationTypes.WHITE_BANNER;
            case ORANGE -> MapDecorationTypes.ORANGE_BANNER;
            case MAGENTA -> MapDecorationTypes.MAGENTA_BANNER;
            case LIGHT_BLUE -> MapDecorationTypes.LIGHT_BLUE_BANNER;
            case YELLOW -> MapDecorationTypes.YELLOW_BANNER;
            case LIME -> MapDecorationTypes.LIME_BANNER;
            case PINK -> MapDecorationTypes.PINK_BANNER;
            case GRAY -> MapDecorationTypes.GRAY_BANNER;
            case LIGHT_GRAY -> MapDecorationTypes.LIGHT_GRAY_BANNER;
            case CYAN -> MapDecorationTypes.CYAN_BANNER;
            case PURPLE -> MapDecorationTypes.PURPLE_BANNER;
            case BLUE -> MapDecorationTypes.BLUE_BANNER;
            case BROWN -> MapDecorationTypes.BROWN_BANNER;
            case GREEN -> MapDecorationTypes.GREEN_BANNER;
            case RED -> MapDecorationTypes.RED_BANNER;
            case BLACK -> MapDecorationTypes.BLACK_BANNER;
        };
    }

    private record SmoothedPosition(float x, long updatedAtMillis) {
    }
}
