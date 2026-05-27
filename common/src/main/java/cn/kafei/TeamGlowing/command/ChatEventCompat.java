package cn.kafei.TeamGlowing.command;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

final class ChatEventCompat {
    private static final Constructor<?> MODERN_RUN_COMMAND = findConstructor(
        "net.minecraft.network.chat.ClickEvent$RunCommand",
        String.class
    );
    private static final Constructor<?> MODERN_SHOW_TEXT = findConstructor(
        "net.minecraft.network.chat.HoverEvent$ShowText",
        Component.class
    );
    private static final Class<?> LEGACY_CLICK_ACTION = findClass("net.minecraft.network.chat.ClickEvent$Action");
    private static final Class<?> LEGACY_HOVER_ACTION = findClass("net.minecraft.network.chat.HoverEvent$Action");
    private static final Constructor<?> LEGACY_CLICK_EVENT = findConstructor(
        "net.minecraft.network.chat.ClickEvent",
        LEGACY_CLICK_ACTION,
        String.class
    );
    private static final Constructor<?> LEGACY_HOVER_EVENT = findConstructor(
        "net.minecraft.network.chat.HoverEvent",
        LEGACY_HOVER_ACTION,
        Object.class
    );
    private static final Object LEGACY_RUN_COMMAND_ACTION = findFieldValue(LEGACY_CLICK_ACTION, "RUN_COMMAND");
    private static final Object LEGACY_SHOW_TEXT_ACTION = findFieldValue(LEGACY_HOVER_ACTION, "SHOW_TEXT");
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private ChatEventCompat() {
    }

    static ClickEvent runCommand(String command) {
        Object event = instantiate(MODERN_RUN_COMMAND, command);
        if (event instanceof ClickEvent clickEvent) {
            return clickEvent;
        }

        event = instantiate(LEGACY_CLICK_EVENT, LEGACY_RUN_COMMAND_ACTION, command);
        if (event instanceof ClickEvent clickEvent) {
            return clickEvent;
        }

        warnOnce();
        return null;
    }

    static HoverEvent showText(Component text) {
        Object event = instantiate(MODERN_SHOW_TEXT, text);
        if (event instanceof HoverEvent hoverEvent) {
            return hoverEvent;
        }

        event = instantiate(LEGACY_HOVER_EVENT, LEGACY_SHOW_TEXT_ACTION, text);
        if (event instanceof HoverEvent hoverEvent) {
            return hoverEvent;
        }

        warnOnce();
        return null;
    }

    private static Object instantiate(Constructor<?> constructor, Object... args) {
        if (constructor == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg == null) {
                return null;
            }
        }
        try {
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            warnOnce();
            return null;
        }
    }

    private static Constructor<?> findConstructor(String className, Class<?>... parameterTypes) {
        for (Class<?> parameterType : parameterTypes) {
            if (parameterType == null) {
                return null;
            }
        }
        Class<?> owner = findClass(className);
        if (owner == null) {
            return null;
        }
        try {
            return owner.getConstructor(parameterTypes);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private static Class<?> findClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    private static Object findFieldValue(Class<?> owner, String fieldName) {
        if (owner == null) {
            return null;
        }
        try {
            Field field = owner.getField(fieldName);
            return field.get(null);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static void warnOnce() {
        if (WARNED.compareAndSet(false, true)) {
            TeamGlowingConstants.LOGGER.warn("当前 Minecraft 版本缺少可用的聊天点击/悬浮事件构造方法");
        }
    }
}
