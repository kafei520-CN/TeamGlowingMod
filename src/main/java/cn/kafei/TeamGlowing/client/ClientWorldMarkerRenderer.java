package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.marker.SharedMarkerKind;
import cn.kafei.TeamGlowing.network.TeammateWorldMarkerEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ClientWorldMarkerRenderer {
    private static final int ITEM_ICON_SIZE = 12;
    private static final int PING_ICON_SIZE = 8;
    private static final float PING_DIAMOND_SIZE = 6.0F;
    private static final int ITEM_PING_X_OFFSET = 7;
    private static final int ITEM_PING_Y_OFFSET = 7;
    private static final int LABEL_SPACING = 2;
    private static final int ENTITY_ICON_GAP = 1;
    private static final float DISTANCE_TEXT_SCALE = 0.55F;
    private static final float ENTITY_NAME_TEXT_SCALE = 0.75F;
    private static final float MIN_SCALE = 0.55F;
    private static final float MAX_SCALE = 0.85F;
    private static final float MIN_ALPHA = 0.35F;
    private static final List<ProjectedMarker> PROJECTED_MARKERS = new ArrayList<>();

    private ClientWorldMarkerRenderer() {
    }

    public static void render(WorldRenderContext context) {
        if (!ClientToggleState.isEnabled()) {
            PROJECTED_MARKERS.clear();
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            PROJECTED_MARKERS.clear();
            return;
        }

        PROJECTED_MARKERS.clear();
        Vec3d cameraPos = context.camera().getPos();
        Quaternionf inverseCameraRotation = new Quaternionf(context.camera().getRotation()).conjugate();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        float focalLength = getFocalLength(client, screenHeight);

        for (TeammateWorldMarkerEntry entry : ClientWorldMarkerCache.getEntries()) {
            if (!client.world.getRegistryKey().getValue().toString().equals(entry.dimensionId())) {
                continue;
            }

            ProjectedMarker projectedMarker = projectMarker(
                entry,
                client,
                cameraPos,
                inverseCameraRotation,
                screenWidth,
                screenHeight,
                focalLength
            );
            if (projectedMarker != null) {
                PROJECTED_MARKERS.add(projectedMarker);
            }
        }

        PROJECTED_MARKERS.sort(Comparator
            .comparingDouble(ProjectedMarker::depth)
            .thenComparingDouble(ProjectedMarker::distance));
    }

    public static void renderOverlay(DrawContext context) {
        if (!ClientToggleState.isEnabled() || PROJECTED_MARKERS.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        for (ProjectedMarker marker : PROJECTED_MARKERS) {
            renderProjectedMarker(context, client, marker);
        }
    }

    private static ProjectedMarker projectMarker(
        TeammateWorldMarkerEntry entry,
        MinecraftClient client,
        Vec3d cameraPos,
        Quaternionf inverseCameraRotation,
        int screenWidth,
        int screenHeight,
        float focalLength
    ) {
        Vector3f cameraSpace = new Vector3f(
            (float) (entry.x() - cameraPos.x),
            (float) (entry.y() - cameraPos.y),
            (float) (entry.z() - cameraPos.z)
        );
        cameraSpace.rotate(inverseCameraRotation);
        if (cameraSpace.z >= -0.05F) {
            return null;
        }

        double distance = Math.sqrt(cameraSpace.x * cameraSpace.x + cameraSpace.y * cameraSpace.y + cameraSpace.z * cameraSpace.z);
        float scale = MathHelper.clamp(
            (float) (Math.max(1.0D, 2.0D / Math.pow(distance, 0.3D)) * 0.5D),
            MIN_SCALE,
            MAX_SCALE
        );
        float screenX = screenWidth * 0.5F + cameraSpace.x * focalLength / -cameraSpace.z;
        float screenY = screenHeight * 0.5F - cameraSpace.y * focalLength / -cameraSpace.z;
        if (screenX < -32.0F || screenX > screenWidth + 32.0F || screenY < -32.0F || screenY > screenHeight + 32.0F) {
            return null;
        }

        boolean isSelfMarker = client.player != null && client.player.getUuidAsString().equals(entry.playerId());
        int color = isSelfMarker ? 0xFFFFFF : (ClientPlayerColorHelper.getPlayerColor(entry.playerName()) & 0xFFFFFF);
        return new ProjectedMarker(
            entry,
            screenX,
            screenY,
            distance,
            scale,
            color,
            isSelfMarker,
            resolveItemStack(entry.itemId()),
            -cameraSpace.z
        );
    }

    private static void renderProjectedMarker(DrawContext context, MinecraftClient client, ProjectedMarker marker) {
        if (marker.entry().kind() == SharedMarkerKind.ENTITY) {
            renderEntityMarker(context, client, marker);
            return;
        }

        boolean renderItem = marker.entry().kind() == SharedMarkerKind.ITEM && !marker.itemStack().isEmpty();
        int iconSize = renderItem ? ITEM_ICON_SIZE : PING_ICON_SIZE;
        float alpha = getAlpha(marker.distance());

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(marker.screenX(), marker.screenY());
        context.getMatrices().scale(marker.scale(), marker.scale());
        context.getMatrices().translate(-iconSize / 2.0F, -iconSize / 2.0F);

        if (renderItem) {
            context.drawItem(marker.itemStack(), 0, 0);
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(ITEM_PING_X_OFFSET, ITEM_PING_Y_OFFSET);
            renderPingIcon(context, marker.color(), marker.self(), alpha);
            context.getMatrices().popMatrix();
        } else {
            renderPingIcon(context, marker.color(), marker.self(), alpha);
        }
        context.getMatrices().popMatrix();

        String distanceText = MathHelper.floor(marker.distance()) + "m";
        int distanceWidth = client.textRenderer.getWidth(distanceText);
        int iconPixelSize = Math.round(iconSize * marker.scale());
        int drawY = Math.round(marker.screenY() - iconPixelSize / 2.0F);
        int distanceX = Math.round(marker.screenX() - (distanceWidth * DISTANCE_TEXT_SCALE) / 2.0F);
        int distanceY = drawY + iconPixelSize + LABEL_SPACING;
        drawScaledText(context, client, distanceText, distanceX, distanceY, withAlpha(0xFFFFFF, alpha), DISTANCE_TEXT_SCALE);
    }

    private static void renderEntityMarker(DrawContext context, MinecraftClient client, ProjectedMarker marker) {
        float alpha = getAlpha(marker.distance());
        String nameText = resolveEntityLabel(marker.entry());
        String distanceText = MathHelper.floor(marker.distance()) + "m";
        int nameWidth = client.textRenderer.getWidth(nameText);
        int distanceWidth = client.textRenderer.getWidth(distanceText);
        int pingPixelSize = Math.round(PING_ICON_SIZE * marker.scale());
        int nameY = Math.round(marker.screenY()) + pingPixelSize / 2 + ENTITY_ICON_GAP;
        int distanceY = nameY + Math.round(9.0F * ENTITY_NAME_TEXT_SCALE) + 1;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(marker.screenX(), marker.screenY() - pingPixelSize / 2.0F);
        context.getMatrices().scale(marker.scale(), marker.scale());
        context.getMatrices().translate(-PING_ICON_SIZE / 2.0F, -PING_ICON_SIZE / 2.0F);
        renderPingIcon(context, marker.color(), marker.self(), alpha);
        context.getMatrices().popMatrix();

        drawScaledText(
            context,
            client,
            nameText,
            Math.round(marker.screenX() - (nameWidth * ENTITY_NAME_TEXT_SCALE) / 2.0F),
            nameY,
            withAlpha(marker.color(), alpha),
            ENTITY_NAME_TEXT_SCALE
        );
        drawScaledText(
            context,
            client,
            distanceText,
            Math.round(marker.screenX() - (distanceWidth * DISTANCE_TEXT_SCALE) / 2.0F),
            distanceY,
            withAlpha(0xFFFFFF, alpha),
            DISTANCE_TEXT_SCALE
        );
    }

    private static void renderPingIcon(DrawContext context, int color, boolean self, float alpha) {
        int fullColor = withAlpha(color, alpha);
        context.getMatrices().translate(PING_ICON_SIZE * 0.5F, PING_ICON_SIZE * 0.5F);
        context.getMatrices().mul(new Matrix3x2f().rotateLocal((float) (Math.PI / 4.0D)));
        context.getMatrices().translate(-PING_DIAMOND_SIZE * 0.5F, -PING_DIAMOND_SIZE * 0.5F);
        if (self) {
            fillDiamondFrame(context, 0, 0, 6, fullColor);
            context.fill(2, 2, 4, 4, fullColor);
        } else {
            context.fill(0, 0, 6, 6, fullColor);
        }
    }

    private static void fillDiamondFrame(DrawContext context, int x, int y, int size, int color) {
        context.fill(x, y, x + size, y + 1, color);
        context.fill(x, y + size - 1, x + size, y + size, color);
        context.fill(x, y + 1, x + 1, y + size - 1, color);
        context.fill(x + size - 1, y + 1, x + size, y + size - 1, color);
    }

    private static ItemStack resolveItemStack(String itemId) {
        Identifier identifier = Identifier.tryParse(itemId);
        if (identifier == null || !Registries.ITEM.containsId(identifier)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(Registries.ITEM.get(identifier));
    }

    private static String resolveEntityLabel(TeammateWorldMarkerEntry entry) {
        if (entry.label() == null || entry.label().isBlank()) {
            return entry.playerName() == null || entry.playerName().isBlank() ? entry.name() : entry.playerName();
        }
        return entry.labelIsTranslationKey()
            ? Text.translatable(entry.label()).getString()
            : entry.label();
    }

    private static void drawScaledText(
        DrawContext context,
        MinecraftClient client,
        String text,
        int x,
        int y,
        int color,
        float scale
    ) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.drawText(client.textRenderer, text, 0, 0, color, true);
        context.getMatrices().popMatrix();
    }

    private static float getFocalLength(MinecraftClient client, int screenHeight) {
        float fov = client.options.getFov().getValue();
        double halfFovRadians = Math.toRadians(fov * 0.5D);
        return (float) (screenHeight / (2.0D * Math.tan(halfFovRadians)));
    }

    private static float getAlpha(double distance) {
        if (distance <= 24.0D) {
            return 1.0F;
        }
        if (distance >= 256.0D) {
            return MIN_ALPHA;
        }
        double normalized = (distance - 24.0D) / 232.0D;
        return (float) (1.0D - normalized * (1.0D - MIN_ALPHA));
    }

    private static int withAlpha(int rgbColor, float alpha) {
        int alphaChannel = MathHelper.clamp((int) (alpha * 255.0F), 0, 255);
        return (alphaChannel << 24) | (rgbColor & 0xFFFFFF);
    }

    private record ProjectedMarker(
        TeammateWorldMarkerEntry entry,
        float screenX,
        float screenY,
        double distance,
        float scale,
        int color,
        boolean self,
        ItemStack itemStack,
        double depth
    ) {
    }
}
