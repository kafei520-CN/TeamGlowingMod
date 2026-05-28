package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

/**
 * 用途：兼容 1.21.x 中 ItemStack 数据组件读取方法的映射名变化。
 */
final class ItemStackComponentCompat {
    private static final Method GET_COMPONENT = findGetComponentMethod();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private ItemStackComponentCompat() {
    }

    @SuppressWarnings("unchecked")
    static <T> T get(ItemStack stack, DataComponentType<? extends T> type) {
        if (stack == null || type == null || GET_COMPONENT == null) {
            return null;
        }
        try {
            Object value = GET_COMPONENT.invoke(stack, type);
            return value == null ? null : (T) value;
        } catch (ReflectiveOperationException | ClassCastException exception) {
            warnOnce(exception);
            return null;
        }
    }

    private static Method findGetComponentMethod() {
        for (String name : new String[] {"get", "method_57824", "method_58694"}) {
            try {
                return ItemStack.class.getMethod(name, DataComponentType.class);
            } catch (NoSuchMethodException ignored) {
            }
        }
        warnOnce(null);
        return null;
    }

    private static void warnOnce(Throwable throwable) {
        if (WARNED.compareAndSet(false, true)) {
            if (throwable == null) {
                TeamGlowingConstants.LOGGER.warn("当前 Minecraft 版本缺少可用的物品组件读取方法");
            } else {
                TeamGlowingConstants.LOGGER.warn("物品组件读取兼容调用失败", throwable);
            }
        }
    }
}
