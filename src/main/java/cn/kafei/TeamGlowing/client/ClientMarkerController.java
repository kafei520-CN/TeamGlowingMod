package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.marker.SharedMarkerKind;
import cn.kafei.TeamGlowing.network.SetSharedMarkerRequest;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import org.lwjgl.glfw.GLFW;

public final class ClientMarkerController {
    private static final double MARKER_RAYCAST_DISTANCE = 1024.0D;
    private static KeyBinding markKey;

    private ClientMarkerController() {
    }

    public static void initialize() {
        if (markKey != null) {
            return;
        }
        markKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.teamglowing.share_marker",
            InputUtil.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            "category.teamglowing"
        ));
    }

    public static void tick(MinecraftClient client) {
        if (markKey == null) {
            return;
        }

        while (markKey.wasPressed()) {
            handleMarkRequest(client);
        }
    }

    private static void handleMarkRequest(MinecraftClient client) {
        if (!ClientToggleState.isEnabled()) {
            return;
        }

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.currentScreen != null) {
            return;
        }

        if (player.isSneaking()) {
            ClientPlayNetworking.send(new SetSharedMarkerRequest(
                SharedMarkerKind.CLEAR,
                "",
                0.0D,
                0.0D,
                0.0D,
                ""
            ));
            player.sendMessage(Text.translatable("teamglowing.marker.cleared"), true);
            return;
        }

        HitResult hitResult = findMarkerTarget(client, player);
        if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof ItemEntity itemEntity) {
            ClientPlayNetworking.send(new SetSharedMarkerRequest(
                SharedMarkerKind.ITEM,
                client.world.getRegistryKey().getValue().toString(),
                itemEntity.getX(),
                itemEntity.getY(),
                itemEntity.getZ(),
                itemEntity.getUuidAsString()
            ));
            player.sendMessage(Text.translatable("teamglowing.marker.shared_item"), true);
            return;
        }

        if (hitResult instanceof BlockHitResult blockHitResult) {
            Vec3d pos = Vec3d.ofCenter(blockHitResult.getBlockPos()).add(0.0D, 0.35D, 0.0D);
            ClientPlayNetworking.send(new SetSharedMarkerRequest(
                SharedMarkerKind.WAYPOINT,
                client.world.getRegistryKey().getValue().toString(),
                pos.x,
                pos.y,
                pos.z,
                ""
            ));
            player.sendMessage(Text.translatable("teamglowing.marker.shared_waypoint"), true);
            return;
        }

        if (hitResult != null) {
            Vec3d pos = hitResult.getPos();
            ClientPlayNetworking.send(new SetSharedMarkerRequest(
                SharedMarkerKind.WAYPOINT,
                client.world.getRegistryKey().getValue().toString(),
                pos.x,
                pos.y,
                pos.z,
                ""
            ));
            player.sendMessage(Text.translatable("teamglowing.marker.shared_waypoint"), true);
            return;
        }

        player.sendMessage(Text.translatable("teamglowing.marker.invalid_target"), true);
    }

    private static HitResult findMarkerTarget(MinecraftClient client, ClientPlayerEntity player) {
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(MARKER_RAYCAST_DISTANCE));

        BlockHitResult blockHitResult = client.world.raycast(new RaycastContext(
            start,
            end,
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE,
            player
        ));

        double maxDistanceSquared = MARKER_RAYCAST_DISTANCE * MARKER_RAYCAST_DISTANCE;
        Box searchBox = player.getBoundingBox().stretch(direction.multiply(MARKER_RAYCAST_DISTANCE)).expand(1.0D);
        EntityHitResult entityHitResult = ProjectileUtil.raycast(
            player,
            start,
            end,
            searchBox,
            ClientMarkerController::isValidMarkerEntity,
            maxDistanceSquared
        );

        if (entityHitResult != null) {
            double entityDistanceSquared = start.squaredDistanceTo(entityHitResult.getPos());
            if (blockHitResult == null || blockHitResult.getType() == HitResult.Type.MISS) {
                return entityHitResult;
            }

            double blockDistanceSquared = start.squaredDistanceTo(blockHitResult.getPos());
            if (entityDistanceSquared <= blockDistanceSquared) {
                return entityHitResult;
            }
            return blockHitResult;
        }

        if (blockHitResult != null && blockHitResult.getType() != HitResult.Type.MISS) {
            return blockHitResult;
        }

        return BlockHitResult.createMissed(end, net.minecraft.util.math.Direction.getFacing(direction.x, direction.y, direction.z), net.minecraft.util.math.BlockPos.ofFloored(end));
    }

    private static boolean isValidMarkerEntity(Entity entity) {
        return entity instanceof ItemEntity && !entity.isSpectator();
    }
}
