package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * 用途：兼容 1.21.x 中 ServerLevel 按 UUID 查找实体的映射变化。
 */
final class ServerEntityLookupCompat {
    private static final Method DIRECT_GET_ENTITY = findMethod(ServerLevel.class, "getEntity", "method_14190");
    private static final Method GET_ALL_ENTITIES = findMethod(ServerLevel.class, "getAllEntities", "method_27909");
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private ServerEntityLookupCompat() {
    }

    static Entity getEntity(ServerLevel world, UUID entityId) {
        if (world == null || entityId == null) {
            return null;
        }

        Entity directEntity = getDirect(world, entityId);
        if (directEntity != null) {
            return directEntity;
        }
        return findInAllEntities(world, entityId);
    }

    private static Entity getDirect(ServerLevel world, UUID entityId) {
        if (DIRECT_GET_ENTITY == null) {
            return null;
        }
        try {
            Object value = DIRECT_GET_ENTITY.invoke(world, entityId);
            return value instanceof Entity entity ? entity : null;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            warnOnce(exception);
            return null;
        }
    }

    private static Entity findInAllEntities(ServerLevel world, UUID entityId) {
        Iterable<?> entities = getAllEntities(world);
        if (entities == null) {
            return null;
        }
        for (Object value : entities) {
            if (value instanceof Entity entity && entityId.equals(entity.getUUID())) {
                return entity;
            }
        }
        return null;
    }

    private static Iterable<?> getAllEntities(ServerLevel world) {
        if (GET_ALL_ENTITIES == null) {
            warnOnce(null);
            return null;
        }
        try {
            Object value = GET_ALL_ENTITIES.invoke(world);
            return value instanceof Iterable<?> iterable ? iterable : null;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            warnOnce(exception);
            return null;
        }
    }

    private static Method findMethod(Class<?> owner, String namedName, String intermediaryName) {
        for (String name : new String[] {namedName, intermediaryName}) {
            try {
                return owner.getMethod(name, UUID.class);
            } catch (NoSuchMethodException ignored) {
            }
        }
        for (String name : new String[] {namedName, intermediaryName}) {
            try {
                return owner.getMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static void warnOnce(Throwable throwable) {
        if (!WARNED.compareAndSet(false, true)) {
            return;
        }
        if (throwable == null) {
            TeamGlowingConstants.LOGGER.warn("当前 Minecraft 版本缺少可用的实体 UUID 查询方法");
            return;
        }
        TeamGlowingConstants.LOGGER.warn("实体 UUID 查询兼容调用失败", throwable);
    }
}
