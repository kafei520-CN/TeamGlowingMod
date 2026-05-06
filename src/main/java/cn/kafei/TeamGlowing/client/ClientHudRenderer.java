package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.network.TeamLocatorEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClientHudRenderer
{
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
    private final Map<String, Float> smoothPositions = new HashMap<>();

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event)
    {
        if (event.getType() != RenderGameOverlayEvent.ElementType.EXPERIENCE)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.world == null)
        {
            this.smoothPositions.clear();
            return;
        }

        List<TeamLocatorEntry> entries = ClientLocatorCache.getEntries();
        if (entries.isEmpty())
        {
            this.smoothPositions.clear();
            return;
        }

        ScaledResolution resolution = new ScaledResolution(minecraft);
        Entity camera = minecraft.getRenderViewEntity();
        if (camera == null)
        {
            camera = minecraft.player;
        }
        int barLeft = (resolution.getScaledWidth() - BAR_WIDTH) / 2;
        int barY = resolution.getScaledHeight() - BAR_Y_OFFSET;
        boolean showNames = minecraft.gameSettings.keyBindPlayerList.isKeyDown();

        Gui.drawRect(barLeft, barY + 3, barLeft + BAR_WIDTH, barY + 4, 0xC0101010);

        int rendered = 0;
        for (TeamLocatorEntry entry : entries)
        {
            if (rendered >= MAX_DISPLAY)
            {
                break;
            }

            double distance = minecraft.player.getDistance(entry.x, entry.y, entry.z);
            double relativeAngle = this.getRelativeAngleDegrees(camera.rotationYaw, camera.posX, camera.posZ, entry.x, entry.z);
            if (Math.abs(relativeAngle) > MAX_ANGLE)
            {
                this.smoothPositions.remove(entry.name);
                continue;
            }
            int targetX = this.projectAngleToX(relativeAngle, barLeft, BAR_WIDTH);
            float currentX = this.getSmoothedX(entry.name, targetX);
            float alpha = this.getEdgeAlpha(currentX, barLeft, barLeft + BAR_WIDTH);
            int textureIndex = this.getTextureIndexFromDistance(distance);
            int playerColor = this.generateColorFromPlayerName(entry.playerName);
            int markerY = barY + MARKER_Y_OFFSET;
            this.drawMarker(minecraft, currentX, markerY, textureIndex, playerColor, alpha);
            this.drawHeightArrow(minecraft, currentX, markerY, entry.y - camera.posY, alpha);
            if (showNames)
            {
                this.drawPlayerName(minecraft, entry.name, currentX, markerY, alpha);
            }
            rendered++;
        }
    }

    private void drawMarker(Minecraft minecraft, float x, int barY, int textureIndex, int color, float alpha)
    {
        if (alpha <= 0.02F)
        {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        minecraft.getTextureManager().bindTexture(ClientHudTextures.PLAYER_DOT_OUTLINES[textureIndex]);
        this.setColor(this.darkerColoring(color), alpha);
        Gui.drawModalRectWithCustomSizedTexture((int) x - ICON_SIZE / 2, barY - ICON_SIZE / 2, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        minecraft.getTextureManager().bindTexture(ClientHudTextures.PLAYER_DOTS[textureIndex]);
        this.setColor(color, alpha);
        Gui.drawModalRectWithCustomSizedTexture((int) x - ICON_SIZE / 2, barY - ICON_SIZE / 2, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private void drawHeightArrow(Minecraft minecraft, float x, int barY, double deltaY, float alpha)
    {
        if (Math.abs(deltaY) < HEIGHT_THRESHOLD || alpha <= 0.02F)
        {
            return;
        }

        int iconTop = barY - ICON_SIZE / 2;
        int iconBottom = iconTop + ICON_SIZE;
        int arrowX = Math.round(x) - ARROW_SIZE / 2;
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        minecraft.getTextureManager().bindTexture(ClientHudTextures.ARROW);
        if (deltaY < 0.0D)
        {
            this.drawArrowFrame(arrowX, iconBottom, false);
        }
        else
        {
            this.drawArrowFrame(arrowX, iconTop - ARROW_SIZE, true);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private void drawPlayerName(Minecraft minecraft, String name, float x, int barY, float alpha)
    {
        if (alpha <= 0.02F)
        {
            return;
        }

        int textWidth = minecraft.fontRenderer.getStringWidth(name);
        int color = ((int) (alpha * 255.0F) << 24) | 0xFFFFFF;
        minecraft.fontRenderer.drawStringWithShadow(name, (int) x - textWidth / 2, barY - 17, color);
    }

    private void drawArrowFrame(int x, int y, boolean up)
    {
        int textureX = up ? 0 : ARROW_SIZE;
        Gui.drawModalRectWithCustomSizedTexture(x, y, textureX, 0, ARROW_SIZE, ARROW_SIZE, ARROW_TEXTURE_SIZE, ARROW_TEXTURE_SIZE);
    }

    private float getSmoothedX(String key, int targetX)
    {
        Float current = this.smoothPositions.get(key);
        if (current == null)
        {
            current = (float) targetX;
        }
        else
        {
            current += (targetX - current) * 0.35F;
        }
        this.smoothPositions.put(key, current);
        return current;
    }

    private float getEdgeAlpha(float currentX, int barLeft, int barRight)
    {
        float leftDistance = currentX - barLeft;
        float rightDistance = barRight - currentX;
        float minDistance = Math.min(leftDistance, rightDistance);
        if (minDistance >= EDGE_FADE_MARGIN)
        {
            return 1.0F;
        }
        if (minDistance <= 0.0F)
        {
            return 0.0F;
        }
        return minDistance / EDGE_FADE_MARGIN;
    }

    private double getRelativeAngleDegrees(float playerYaw, double playerX, double playerZ, double targetX, double targetZ)
    {
        double dx = targetX - playerX;
        double dz = targetZ - playerZ;
        if (Math.abs(dx) < 0.0001D && Math.abs(dz) < 0.0001D)
        {
            return 0.0D;
        }

        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double relative = targetYaw - playerYaw;
        while (relative <= -180.0D)
        {
            relative += 360.0D;
        }
        while (relative > 180.0D)
        {
            relative -= 360.0D;
        }
        return relative;
    }

    private int projectAngleToX(double relativeAngle, int barLeft, int barWidth)
    {
        double clampedAngle = Math.max(-MAX_ANGLE, Math.min(MAX_ANGLE, relativeAngle));
        double normalized = (clampedAngle + MAX_ANGLE) / (MAX_ANGLE * 2.0D);
        return barLeft + MathHelper.floor(normalized * (barWidth - 1));
    }

    private int getTextureIndexFromDistance(double distance)
    {
        if (distance < 128.0D)
        {
            return 0;
        }
        if (distance < 230.0D)
        {
            return 1;
        }
        if (distance < 332.0D)
        {
            return 2;
        }
        return 3;
    }

    private int generateColorFromPlayerName(String playerName)
    {
        Random random = new Random(playerName.toLowerCase().hashCode());
        return 0xFF000000 | ((random.nextInt(206) + 50) << 16) | ((random.nextInt(206) + 50) << 8) | (random.nextInt(206) + 50);
    }

    private int darkerColoring(int color)
    {
        int red = (int) (Math.max(0, ((color >> 16) & 0xFF) * 0.55F));
        int green = (int) (Math.max(0, ((color >> 8) & 0xFF) * 0.55F));
        int blue = (int) (Math.max(0, (color & 0xFF) * 0.55F));
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private void setColor(int color, float alpha)
    {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        GlStateManager.color(red, green, blue, alpha);
    }
}
