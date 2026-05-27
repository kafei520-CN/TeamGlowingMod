package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

final class ServerTeleportCompat {
    private static final Set<?> ABSOLUTE_MOVEMENT = Set.of();
    private static final Method TELEPORT_WITH_DISMOUNT = findMethod(
        ServerPlayer.class,
        "teleportTo",
        ServerLevel.class,
        double.class,
        double.class,
        double.class,
        Set.class,
        float.class,
        float.class,
        boolean.class
    );
    private static final Method TELEPORT_LEGACY = findMethod(
        ServerPlayer.class,
        "teleportTo",
        ServerLevel.class,
        double.class,
        double.class,
        double.class,
        Set.class,
        float.class,
        float.class
    );
    private static final Method TELEPORT_SIMPLE = findMethod(
        ServerPlayer.class,
        "teleportTo",
        ServerLevel.class,
        double.class,
        double.class,
        double.class,
        float.class,
        float.class
    );
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private ServerTeleportCompat() {
    }

    static boolean teleport(ServerPlayer player, ServerLevel level, Vec3 destination) {
        return teleport(player, level, destination.x, destination.y, destination.z, player.getYRot(), player.getXRot());
    }

    static boolean teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        try {
            if (TELEPORT_WITH_DISMOUNT != null) {
                return asBoolean(TELEPORT_WITH_DISMOUNT.invoke(player, level, x, y, z, ABSOLUTE_MOVEMENT, yaw, pitch, false));
            }
            if (TELEPORT_LEGACY != null) {
                return asBoolean(TELEPORT_LEGACY.invoke(player, level, x, y, z, ABSOLUTE_MOVEMENT, yaw, pitch));
            }
            if (TELEPORT_SIMPLE != null) {
                TELEPORT_SIMPLE.invoke(player, level, x, y, z, yaw, pitch);
                return true;
            }
        } catch (IllegalAccessException | InvocationTargetException exception) {
            warnOnce(exception);
            return false;
        }
        warnOnce(null);
        return false;
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private static boolean asBoolean(Object result) {
        return !(result instanceof Boolean value) || value;
    }

    private static void warnOnce(Throwable throwable) {
        if (!WARNED.compareAndSet(false, true)) {
            return;
        }
        if (throwable == null) {
            TeamGlowingConstants.LOGGER.warn("当前 Minecraft 版本缺少可用的跨维度传送方法");
            return;
        }
        TeamGlowingConstants.LOGGER.warn("跨维度传送兼容调用失败", throwable);
    }
}
