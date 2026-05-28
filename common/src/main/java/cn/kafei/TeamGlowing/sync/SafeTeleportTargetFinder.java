package cn.kafei.TeamGlowing.sync;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * 用途：统一查找玩家传送和重生的安全站立点。
 */
final class SafeTeleportTargetFinder {
    static final int DEFAULT_SEARCH_RADIUS = 8;
    static final int DEFAULT_VERTICAL_RANGE = 16;

    private SafeTeleportTargetFinder() {
    }

    static Vec3 findAround(ServerLevel world, BlockPos centerPos) {
        return findAround(world, centerPos, Set.of());
    }

    static Vec3 findAround(ServerLevel world, BlockPos centerPos, BlockPos excludedPos) {
        return findAround(world, centerPos, excludedPos == null ? Set.of() : Set.of(excludedPos));
    }

    static Vec3 findAround(ServerLevel world, BlockPos centerPos, Set<BlockPos> excludedPositions) {
        if (world == null || centerPos == null) {
            return null;
        }
        Set<BlockPos> exclusions = excludedPositions == null ? Set.of() : excludedPositions;
        Vec3 scannedTarget = findByVerticalScan(world, centerPos, exclusions);
        if (scannedTarget != null) {
            return scannedTarget;
        }
        return findByHeightmap(world, centerPos, exclusions);
    }

    static boolean isSafe(ServerLevel world, BlockPos feetPos) {
        if (world == null || feetPos == null) {
            return false;
        }
        BlockPos headPos = feetPos.above();
        BlockPos groundPos = feetPos.below();
        return isPassable(world, feetPos)
            && isPassable(world, headPos)
            && isStandable(world, groundPos);
    }

    private static Vec3 findByVerticalScan(ServerLevel world, BlockPos centerPos, Set<BlockPos> excludedPositions) {
        for (int radius = 0; radius <= DEFAULT_SEARCH_RADIUS; radius++) {
            for (int verticalDistance = 0; verticalDistance <= DEFAULT_VERTICAL_RANGE; verticalDistance++) {
                Vec3 upperTarget = findAtOffset(world, centerPos, excludedPositions, radius, verticalDistance);
                if (upperTarget != null) {
                    return upperTarget;
                }
                if (verticalDistance == 0) {
                    continue;
                }
                Vec3 lowerTarget = findAtOffset(world, centerPos, excludedPositions, radius, -verticalDistance);
                if (lowerTarget != null) {
                    return lowerTarget;
                }
            }
        }
        return null;
    }

    private static Vec3 findAtOffset(ServerLevel world, BlockPos centerPos, Set<BlockPos> excludedPositions, int radius, int yOffset) {
        for (int xOffset = -radius; xOffset <= radius; xOffset++) {
            for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                if (radius > 0 && Math.abs(xOffset) != radius && Math.abs(zOffset) != radius) {
                    continue;
                }
                BlockPos feetPos = centerPos.offset(xOffset, yOffset, zOffset);
                if (excludedPositions.contains(feetPos)) {
                    continue;
                }
                if (isSafe(world, feetPos)) {
                    return centerFeet(feetPos);
                }
            }
        }
        return null;
    }

    private static Vec3 findByHeightmap(ServerLevel world, BlockPos centerPos, Set<BlockPos> excludedPositions) {
        for (int radius = 0; radius <= DEFAULT_SEARCH_RADIUS; radius++) {
            for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                    if (radius > 0 && Math.abs(xOffset) != radius && Math.abs(zOffset) != radius) {
                        continue;
                    }
                    int x = centerPos.getX() + xOffset;
                    int z = centerPos.getZ() + zOffset;
                    BlockPos feetPos = new BlockPos(x, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                    if (excludedPositions.contains(feetPos)) {
                        continue;
                    }
                    if (isSafe(world, feetPos)) {
                        return centerFeet(feetPos);
                    }
                }
            }
        }
        return null;
    }

    private static Vec3 centerFeet(BlockPos feetPos) {
        return new Vec3(feetPos.getX() + 0.5D, feetPos.getY(), feetPos.getZ() + 0.5D);
    }

    private static boolean isPassable(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getCollisionShape(world, pos).isEmpty() && world.getFluidState(pos).isEmpty();
    }

    private static boolean isStandable(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.getCollisionShape(world, pos).isEmpty() && world.getFluidState(pos).isEmpty();
    }
}
