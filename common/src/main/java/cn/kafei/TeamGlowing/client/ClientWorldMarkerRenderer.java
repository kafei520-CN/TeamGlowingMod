package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.marker.SharedMarkerKind;
import cn.kafei.TeamGlowing.network.TeammateWorldMarkerEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
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
    private static final float MAX_SCALE = 0.55F;
    private static final float MIN_ALPHA = 0.35F;
    private static final List<ProjectedMarker> PROJECTED_MARKERS = new ArrayList<>();

    private ClientWorldMarkerRenderer() {
    }

    public static void render(Camera camera) {
        if (!ClientToggleState.isEnabled()) {
            PROJECTED_MARKERS.clear();
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            PROJECTED_MARKERS.clear();
            return;
        }

        PROJECTED_MARKERS.clear();
        Vec3 cameraPos = camera.getPosition();
        Quaternionf inverseCameraRotation = new Quaternionf(camera.rotation()).conjugate();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        float focalLength = getFocalLength(client, screenHeight);

        for (TeammateWorldMarkerEntry entry : ClientWorldMarkerCache.getEntries()) {
            if (!client.level.dimension().location().toString().equals(entry.dimensionId())) {
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

    public static void renderOverlay(GuiGraphics context) {
        if (!ClientToggleState.isEnabled() || PROJECTED_MARKERS.isEmpty()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        for (ProjectedMarker marker : PROJECTED_MARKERS) {
            renderProjectedMarker(context, client, marker);
        }
    }

    private static ProjectedMarker projectMarker(
        TeammateWorldMarkerEntry entry,
        Minecraft client,
        Vec3 cameraPos,
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
        float scale = Mth.clamp(
            (float) (Math.max(1.0D, 2.0D / Math.pow(distance, 0.3D)) * 0.5D),
            MIN_SCALE,
            MAX_SCALE
        );
        float screenX = screenWidth * 0.5F + cameraSpace.x * focalLength / -cameraSpace.z;
        float screenY = screenHeight * 0.5F - cameraSpace.y * focalLength / -cameraSpace.z;
        if (screenX < -32.0F || screenX > screenWidth + 32.0F || screenY < -32.0F || screenY > screenHeight + 32.0F) {
            return null;
        }

        boolean isSelfMarker = client.player != null && client.player.getStringUUID().equals(entry.playerId());
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

    private static void renderProjectedMarker(GuiGraphics context, Minecraft client, ProjectedMarker marker) {
        if (marker.entry().kind() == SharedMarkerKind.ENTITY) {
            renderEntityMarker(context, client, marker);
            return;
        }

        boolean renderItem = marker.entry().kind() == SharedMarkerKind.ITEM && !marker.itemStack().isEmpty();
        int iconSize = renderItem ? ITEM_ICON_SIZE : PING_ICON_SIZE;
        float alpha = getAlpha(marker.distance());

        ClientGuiPoseCompat.push(context);
        ClientGuiPoseCompat.translate(context, marker.screenX(), marker.screenY(), 0.0F);
        ClientGuiPoseCompat.scale(context, marker.scale(), marker.scale(), 1.0F);
        ClientGuiPoseCompat.translate(context, -iconSize / 2.0F, -iconSize / 2.0F, 0.0F);

        if (renderItem) {
            context.renderItem(marker.itemStack(), 0, 0);
            ClientGuiPoseCompat.push(context);
            ClientGuiPoseCompat.translate(context, ITEM_PING_X_OFFSET, ITEM_PING_Y_OFFSET, 0.0F);
            renderPingIcon(context, marker.color(), marker.self(), alpha);
            ClientGuiPoseCompat.pop(context);
        } else {
            renderPingIcon(context, marker.color(), marker.self(), alpha);
        }
        ClientGuiPoseCompat.pop(context);

        String distanceText = Mth.floor(marker.distance()) + "m";
        int distanceWidth = client.font.width(distanceText);
        int iconPixelSize = Math.round(iconSize * marker.scale());
        int drawY = Math.round(marker.screenY() - iconPixelSize / 2.0F);
        int distanceX = Math.round(marker.screenX() - (distanceWidth * DISTANCE_TEXT_SCALE) / 2.0F);
        int distanceY = drawY + iconPixelSize + LABEL_SPACING;
        drawScaledText(context, client, distanceText, distanceX, distanceY, withAlpha(0xFFFFFF, alpha), DISTANCE_TEXT_SCALE);
    }

    private static void renderEntityMarker(GuiGraphics context, Minecraft client, ProjectedMarker marker) {
        float alpha = getAlpha(marker.distance());
        String nameText = resolveEntityLabel(marker.entry());
        String distanceText = Mth.floor(marker.distance()) + "m";
        int nameWidth = client.font.width(nameText);
        int distanceWidth = client.font.width(distanceText);
        int pingPixelSize = Math.round(PING_ICON_SIZE * marker.scale());
        int nameY = Math.round(marker.screenY()) + pingPixelSize / 2 + ENTITY_ICON_GAP;
        int distanceY = nameY + Math.round(9.0F * ENTITY_NAME_TEXT_SCALE) + 1;

        ClientGuiPoseCompat.push(context);
        ClientGuiPoseCompat.translate(context, marker.screenX(), marker.screenY() - pingPixelSize / 2.0F, 0.0F);
        ClientGuiPoseCompat.scale(context, marker.scale(), marker.scale(), 1.0F);
        ClientGuiPoseCompat.translate(context, -PING_ICON_SIZE / 2.0F, -PING_ICON_SIZE / 2.0F, 0.0F);
        renderPingIcon(context, marker.color(), marker.self(), alpha);
        ClientGuiPoseCompat.pop(context);

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

    private static void renderPingIcon(GuiGraphics context, int color, boolean self, float alpha) {
        int fullColor = withAlpha(color, alpha);
        ClientGuiPoseCompat.translate(context, PING_ICON_SIZE * 0.5F, PING_ICON_SIZE * 0.5F, 0.0F);
        ClientGuiPoseCompat.rotateZDegrees(context, 45.0F);
        ClientGuiPoseCompat.translate(context, -PING_DIAMOND_SIZE * 0.5F, -PING_DIAMOND_SIZE * 0.5F, 0.0F);
        if (self) {
            fillDiamondFrame(context, 0, 0, 6, fullColor);
            context.fill(2, 2, 4, 4, fullColor);
        } else {
            context.fill(0, 0, 6, 6, fullColor);
        }
    }

    private static void fillDiamondFrame(GuiGraphics context, int x, int y, int size, int color) {
        context.fill(x, y, x + size, y + 1, color);
        context.fill(x, y + size - 1, x + size, y + size, color);
        context.fill(x, y + 1, x + 1, y + size - 1, color);
        context.fill(x + size - 1, y + 1, x + size, y + size - 1, color);
    }

    private static ItemStack resolveItemStack(String itemId) {
        ResourceLocation identifier = ResourceLocation.tryParse(itemId);
        if (identifier == null || !BuiltInRegistries.ITEM.containsKey(identifier)) {
            return ItemStack.EMPTY;
        }
        return BuiltInRegistries.ITEM.getOptional(identifier).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    private static String resolveEntityLabel(TeammateWorldMarkerEntry entry) {
        if (entry.label() == null || entry.label().isBlank()) {
            return entry.playerName() == null || entry.playerName().isBlank() ? entry.name() : entry.playerName();
        }
        return entry.labelIsTranslationKey()
            ? Component.translatable(entry.label()).getString()
            : entry.label();
    }

    private static void drawScaledText(
        GuiGraphics context,
        Minecraft client,
        String text,
        int x,
        int y,
        int color,
        float scale
    ) {
        ClientGuiPoseCompat.push(context);
        ClientGuiPoseCompat.translate(context, x, y, 0.0F);
        ClientGuiPoseCompat.scale(context, scale, scale, 1.0F);
        ClientGuiRenderCompat.drawString(context, client.font, text, 0, 0, color, true);
        ClientGuiPoseCompat.pop(context);
    }

    private static float getFocalLength(Minecraft client, int screenHeight) {
        float fov = client.options.fov().get();
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
        int alphaChannel = Mth.clamp((int) (alpha * 255.0F), 0, 255);
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
