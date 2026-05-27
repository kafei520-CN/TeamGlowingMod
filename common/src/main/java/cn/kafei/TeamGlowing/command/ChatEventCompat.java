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
        new String[] {"net.minecraft.network.chat.ClickEvent$RunCommand", "net.minecraft.class_2558$class_10609"},
        String.class
    );
    private static final Constructor<?> MODERN_SHOW_TEXT = findConstructor(
        new String[] {"net.minecraft.network.chat.HoverEvent$ShowText", "net.minecraft.class_2568$class_10613"},
        Component.class
    );
    private static final Class<?> LEGACY_CLICK_ACTION = findClass(
        "net.minecraft.network.chat.ClickEvent$Action",
        "net.minecraft.class_2558$class_2559"
    );
    private static final Class<?> LEGACY_HOVER_ACTION = findClass(
        "net.minecraft.network.chat.HoverEvent$Action",
        "net.minecraft.class_2568$class_5247"
    );
    private static final Constructor<?> LEGACY_CLICK_EVENT = findConstructor(
        new String[] {"net.minecraft.network.chat.ClickEvent", "net.minecraft.class_2558"},
        LEGACY_CLICK_ACTION,
        String.class
    );
    private static final Constructor<?> LEGACY_HOVER_EVENT = findConstructor(
        new String[] {"net.minecraft.network.chat.HoverEvent", "net.minecraft.class_2568"},
        LEGACY_HOVER_ACTION,
        Object.class
    );
    private static final Object LEGACY_RUN_COMMAND_ACTION = findFieldValue(LEGACY_CLICK_ACTION, "RUN_COMMAND", "field_11750");
    private static final Object LEGACY_SHOW_TEXT_ACTION = findFieldValue(LEGACY_HOVER_ACTION, "SHOW_TEXT", "field_24342");
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

    private static Constructor<?> findConstructor(String[] classNames, Class<?>... parameterTypes) {
        for (Class<?> parameterType : parameterTypes) {
            if (parameterType == null) {
                return null;
            }
        }
        Class<?> owner = findClass(classNames);
        if (owner == null) {
            return null;
        }
        try {
            return owner.getConstructor(parameterTypes);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private static Class<?> findClass(String... classNames) {
        for (String className : classNames) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private static Object findFieldValue(Class<?> owner, String... fieldNames) {
        if (owner == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            try {
                Field field = owner.getField(fieldName);
                return field.get(null);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static void warnOnce() {
        if (WARNED.compareAndSet(false, true)) {
            TeamGlowingConstants.LOGGER.warn("当前 Minecraft 版本缺少可用的聊天点击/悬浮事件构造方法");
        }
    }
}
