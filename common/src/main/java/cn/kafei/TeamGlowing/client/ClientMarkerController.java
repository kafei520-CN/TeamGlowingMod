package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.marker.SharedMarkerKind;
import cn.kafei.TeamGlowing.network.SetSharedMarkerRequest;
import cn.kafei.TeamGlowing.network.TeamGlowingNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class ClientMarkerController {
    private static final double MARKER_RAYCAST_DISTANCE = 4096.0D;
    private static final double RAYCAST_STEP_EPSILON = 0.05D;
    private static final int MAX_IGNORED_BLOCK_STEPS = 256;
    private static KeyMapping markKey;

    private ClientMarkerController() {
    }

    public static KeyMapping createMarkKeyBinding() {
        return new KeyMapping(
            "key.teamglowing.share_marker",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            "category.teamglowing"
        );
    }

    public static void initialize(KeyMapping registeredKey) {
        if (markKey != null) {
            return;
        }
        markKey = registeredKey;
    }

    public static void tick(Minecraft client) {
        if (markKey == null) {
            return;
        }

        while (markKey.consumeClick()) {
            tryHandleMarkRequest(client);
        }
    }

    public static boolean tryHandleMarkRequest(Minecraft client) {
        if (!ClientToggleState.isEnabled()) {
            return false;
        }

        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.screen != null) {
            return false;
        }

        if (player.isShiftKeyDown()) {
            TeamGlowingNetwork.sendToServer(new SetSharedMarkerRequest(
                SharedMarkerKind.CLEAR,
                "",
                0.0D,
                0.0D,
                0.0D,
                ""
            ));
            player.displayClientMessage(Component.translatable("teamglowing.marker.cleared"), true);
            return true;
        }

        HitResult hitResult = findMarkerTarget(client, player);
        if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();
            if (entity instanceof ItemEntity itemEntity) {
                TeamGlowingNetwork.sendToServer(new SetSharedMarkerRequest(
                    SharedMarkerKind.ITEM,
                    client.level.dimension().location().toString(),
                    itemEntity.getX(),
                    itemEntity.getY(),
                    itemEntity.getZ(),
                    itemEntity.getStringUUID()
                ));
                player.displayClientMessage(Component.translatable("teamglowing.marker.shared_item"), true);
                return true;
            }

            if (isTrackableMarkerEntity(entity, player)) {
                TeamGlowingNetwork.sendToServer(new SetSharedMarkerRequest(
                    SharedMarkerKind.ENTITY,
                    client.level.dimension().location().toString(),
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    entity.getStringUUID()
                ));
                player.displayClientMessage(Component.translatable("teamglowing.marker.shared_entity"), true);
                return true;
            }
        }

        if (hitResult instanceof BlockHitResult blockHitResult) {
            Vec3 pos = Vec3.atCenterOf(blockHitResult.getBlockPos());
            TeamGlowingNetwork.sendToServer(new SetSharedMarkerRequest(
                SharedMarkerKind.WAYPOINT,
                client.level.dimension().location().toString(),
                pos.x,
                pos.y,
                pos.z,
                ""
            ));
            player.displayClientMessage(Component.translatable("teamglowing.marker.shared_waypoint"), true);
            return true;
        }

        if (hitResult != null) {
            Vec3 pos = hitResult.getLocation();
            TeamGlowingNetwork.sendToServer(new SetSharedMarkerRequest(
                SharedMarkerKind.WAYPOINT,
                client.level.dimension().location().toString(),
                pos.x,
                pos.y,
                pos.z,
                ""
            ));
            player.displayClientMessage(Component.translatable("teamglowing.marker.shared_waypoint"), true);
            return true;
        }

        player.displayClientMessage(Component.translatable("teamglowing.marker.invalid_target"), true);
        return true;
    }

    public static void discardPendingMarkClicks() {
        if (markKey == null) {
            return;
        }
        while (markKey.consumeClick()) {
            // 清掉同一次中键点击，避免 Mixin 抢先处理后客户端 tick 再处理一次。
        }
    }

    private static HitResult findMarkerTarget(Minecraft client, LocalPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getViewVector(1.0F);
        Vec3 end = start.add(direction.scale(MARKER_RAYCAST_DISTANCE));

        BlockHitResult blockHitResult = findBlockTarget(client, player, start, end, direction);

        double maxDistanceSquared = MARKER_RAYCAST_DISTANCE * MARKER_RAYCAST_DISTANCE;
        AABB searchBox = player.getBoundingBox().expandTowards(direction.scale(MARKER_RAYCAST_DISTANCE)).inflate(1.0D);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
            player,
            start,
            end,
            searchBox,
            ClientMarkerController::isValidMarkerEntity,
            maxDistanceSquared
        );

        if (entityHitResult != null) {
            double entityDistanceSquared = start.distanceToSqr(entityHitResult.getLocation());
            if (blockHitResult == null || blockHitResult.getType() == HitResult.Type.MISS) {
                return entityHitResult;
            }

            double blockDistanceSquared = start.distanceToSqr(blockHitResult.getLocation());
            if (entityDistanceSquared <= blockDistanceSquared) {
                return entityHitResult;
            }
            return blockHitResult;
        }

        if (blockHitResult != null && blockHitResult.getType() != HitResult.Type.MISS) {
            return blockHitResult;
        }

        return BlockHitResult.miss(end, getNearestDirection(direction), BlockPos.containing(end));
    }

    private static BlockHitResult findBlockTarget(
        Minecraft client,
        LocalPlayer player,
        Vec3 start,
        Vec3 end,
        Vec3 direction
    ) {
        Vec3 currentStart = start;
        for (int step = 0; step < MAX_IGNORED_BLOCK_STEPS; step++) {
            BlockHitResult hitResult = client.level.clip(new ClipContext(
                currentStart,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
            ));
            if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
                return BlockHitResult.miss(end, getNearestDirection(direction), BlockPos.containing(end));
            }
            if (!isIgnoredMarkerBlock(client, hitResult)) {
                return hitResult;
            }

            Vec3 nextStart = hitResult.getLocation().add(direction.scale(RAYCAST_STEP_EPSILON));
            if (nextStart.distanceToSqr(start) >= end.distanceToSqr(start)) {
                break;
            }
            currentStart = nextStart;
        }
        return BlockHitResult.miss(end, getNearestDirection(direction), BlockPos.containing(end));
    }

    private static boolean isIgnoredMarkerBlock(Minecraft client, BlockHitResult hitResult) {
        BlockPos blockPos = hitResult.getBlockPos();
        BlockState state = client.level.getBlockState(blockPos);
        return state.canBeReplaced() || state.is(BlockTags.FLOWERS);
    }

    private static Direction getNearestDirection(Vec3 direction) {
        double absX = Math.abs(direction.x);
        double absY = Math.abs(direction.y);
        double absZ = Math.abs(direction.z);
        if (absY >= absX && absY >= absZ) {
            return direction.y >= 0.0D ? Direction.UP : Direction.DOWN;
        }
        if (absZ >= absX) {
            return direction.z >= 0.0D ? Direction.SOUTH : Direction.NORTH;
        }
        return direction.x >= 0.0D ? Direction.EAST : Direction.WEST;
    }

    private static boolean isValidMarkerEntity(Entity entity) {
        return isItemMarkerEntity(entity) || isTrackableMarkerEntity(entity, Minecraft.getInstance().player);
    }

    private static boolean isItemMarkerEntity(Entity entity) {
        return entity instanceof ItemEntity && !entity.isSpectator() && entity.isAlive();
    }

    private static boolean isTrackableMarkerEntity(Entity entity, Player localPlayer) {
        if (entity == null || entity.isSpectator() || !entity.isAlive() || entity == localPlayer) {
            return false;
        }
        return entity instanceof Player || entity instanceof Mob;
    }
}
