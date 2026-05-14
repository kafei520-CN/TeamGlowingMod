package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.network.TeamLocatorEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class ClientHudRenderer {
    private static final int BAR_WIDTH = 182;
    private static final int BAR_Y_OFFSET = 29;
    private static final int ICON_SIZE = 9;
    private static final int ARROW_SIZE = 9;
    private static final int ARROW_TEXTURE_SIZE = 18;
    private static final int MARKER_Y_OFFSET = 2;
    private static final int MAX_DISPLAY = 4;
    private static final double MAX_ANGLE = 90.0D;
    private static final double HEIGHT_THRESHOLD = 3.0D;
    private static final int EDGE_FADE_MARGIN = 18;
    private static final Map<String, Float> SMOOTH_POSITIONS = new HashMap<>();

    private ClientHudRenderer() {
    }

    public static void render(DrawContext context) {
        if (!ClientToggleState.isEnabled()) {
            SMOOTH_POSITIONS.clear();
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
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

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int barLeft = (screenWidth - BAR_WIDTH) / 2;
        int barY = screenHeight - BAR_Y_OFFSET;
        boolean showNames = client.options.playerListKey.isPressed();

        context.fill(barLeft, barY + 3, barLeft + BAR_WIDTH, barY + 4, 0xC0101010);

        int rendered = 0;
        for (TeamLocatorEntry entry : entries) {
            if (rendered >= MAX_DISPLAY) {
                break;
            }

            double distance = player.squaredDistanceTo(entry.x(), entry.y(), entry.z());
            double relativeAngle = getRelativeAngleDegrees(camera.getYaw(), camera.getX(), camera.getZ(), entry.x(), entry.z());
            if (Math.abs(relativeAngle) > MAX_ANGLE) {
                SMOOTH_POSITIONS.remove(getEntryKey(entry));
                continue;
            }

            int targetX = projectAngleToX(relativeAngle, barLeft, BAR_WIDTH);
            float currentX = getSmoothedX(getEntryKey(entry), targetX);
            float alpha = getEdgeAlpha(currentX, barLeft, barLeft + BAR_WIDTH);
            int textureIndex = getTextureIndexFromDistance(Math.sqrt(distance));
            int playerColor = generateColorFromPlayerName(entry.playerName());
            int markerY = barY + MARKER_Y_OFFSET;
            drawMarker(context, currentX, markerY, textureIndex, playerColor, alpha);
            drawHeightArrow(context, currentX, markerY, entry.y() - camera.getY(), alpha);
            if (showNames) {
                drawPlayerName(context, entry.name(), currentX, markerY, alpha);
            }
            rendered++;
        }
    }

    private static void drawMarker(DrawContext context, float x, int barY, int textureIndex, int color, float alpha) {
        if (alpha <= 0.02F) {
            return;
        }

        int drawX = (int) x - ICON_SIZE / 2;
        int drawY = barY - ICON_SIZE / 2;
        int darkerColor = darkerColoring(color);
        
        // Convert to ARGB color format with alpha
        int outlineColor = ((int) (alpha * 255.0F) << 24) | (darkerColor & 0xFFFFFF);
        int dotColor = ((int) (alpha * 255.0F) << 24) | (color & 0xFFFFFF);
        
        // Draw outline with darker color
        context.drawTexture(RenderPipelines.GUI_TEXTURED, ClientHudTextures.PLAYER_DOT_OUTLINES[textureIndex], drawX, drawY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, outlineColor);
        
        // Draw dot with normal color
        context.drawTexture(RenderPipelines.GUI_TEXTURED, ClientHudTextures.PLAYER_DOTS[textureIndex], drawX, drawY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, dotColor);
    }

    private static void drawHeightArrow(DrawContext context, float x, int barY, double deltaY, float alpha) {
        if (Math.abs(deltaY) < HEIGHT_THRESHOLD || alpha <= 0.02F) {
            return;
        }

        int iconTop = barY - ICON_SIZE / 2;
        int iconBottom = iconTop + ICON_SIZE;
        int arrowX = Math.round(x) - ARROW_SIZE / 2;
        
        // White color with alpha
        int arrowColor = ((int) (alpha * 255.0F) << 24) | 0xFFFFFF;
        
        if (deltaY < 0.0D) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, ClientHudTextures.ARROW, arrowX, iconBottom, ARROW_SIZE, 0, ARROW_SIZE, ARROW_SIZE, ARROW_TEXTURE_SIZE, ARROW_TEXTURE_SIZE, arrowColor);
        } else {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, ClientHudTextures.ARROW, arrowX, iconTop - ARROW_SIZE, 0, 0, ARROW_SIZE, ARROW_SIZE, ARROW_TEXTURE_SIZE, ARROW_TEXTURE_SIZE, arrowColor);
        }
    }

    private static void drawPlayerName(DrawContext context, String name, float x, int barY, float alpha) {
        if (alpha <= 0.02F) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int textWidth = client.textRenderer.getWidth(name);
        int color = ((int) (alpha * 255.0F) << 24) | 0xFFFFFF;
        context.drawText(client.textRenderer, Text.literal(name), (int) x - textWidth / 2, barY - 17, color, true);
    }

    private static float getSmoothedX(String key, int targetX) {
        Float current = SMOOTH_POSITIONS.get(key);
        if (current == null) {
            current = (float) targetX;
        } else {
            current += (targetX - current) * 0.35F;
        }
        SMOOTH_POSITIONS.put(key, current);
        return current;
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

    private static int projectAngleToX(double relativeAngle, int barLeft, int barWidth) {
        double clampedAngle = Math.max(-MAX_ANGLE, Math.min(MAX_ANGLE, relativeAngle));
        double normalized = (clampedAngle + MAX_ANGLE) / (MAX_ANGLE * 2.0D);
        return barLeft + MathHelper.floor(normalized * (barWidth - 1));
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

    private static int generateColorFromPlayerName(String playerName) {
        Random random = new Random(playerName.toLowerCase().hashCode());
        return 0xFF000000 | ((random.nextInt(206) + 50) << 16) | ((random.nextInt(206) + 50) << 8) | (random.nextInt(206) + 50);
    }

    private static int darkerColoring(int color) {
        int red = (int) (((color >> 16) & 0xFF) * 0.55F);
        int green = (int) (((color >> 8) & 0xFF) * 0.55F);
        int blue = (int) ((color & 0xFF) * 0.55F);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
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
        if (entry.playerId() != null && !entry.playerId().isBlank()) {
            return entry.playerId();
        }
        if (entry.playerName() != null && !entry.playerName().isBlank()) {
            return entry.playerName().toLowerCase();
        }
        return entry.name().toLowerCase();
    }
}
