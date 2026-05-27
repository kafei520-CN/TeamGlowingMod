package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Quaternionf;

/**
 * 用途：兼容 1.21.1 PoseStack 与 1.21.6+ Matrix3x2fStack 的 GUI 变换。
 */
public final class ClientGuiPoseCompat {
    private static final String MATRIX_3X2_STACK_NAME = "org.joml.Matrix3x2fStack";
    private static final ConcurrentMap<Class<?>, Method> POSE_METHODS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Class<?>, StackAccess> STACK_ACCESSES = new ConcurrentHashMap<>();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private ClientGuiPoseCompat() {
    }

    public static void push(GuiGraphics context) {
        withStack(context, StackAccess::push);
    }

    public static void pop(GuiGraphics context) {
        withStack(context, StackAccess::pop);
    }

    public static void translate(GuiGraphics context, float x, float y, float z) {
        withStack(context, (access, stack) -> access.translate(stack, x, y, z));
    }

    public static void scale(GuiGraphics context, float x, float y, float z) {
        withStack(context, (access, stack) -> access.scale(stack, x, y, z));
    }

    public static void rotateZDegrees(GuiGraphics context, float degrees) {
        withStack(context, (access, stack) -> access.rotateZDegrees(stack, degrees));
    }

    private static void withStack(GuiGraphics context, StackOperation operation) {
        try {
            Object stack = resolveStack(context);
            if (stack == null) {
                return;
            }
            StackAccess access = STACK_ACCESSES.computeIfAbsent(stack.getClass(), StackAccess::create);
            operation.apply(access, stack);
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            warnOnce("GUI 矩阵栈兼容调用失败", exception);
        }
    }

    private static Object resolveStack(GuiGraphics context) throws ReflectiveOperationException {
        Method method = POSE_METHODS.computeIfAbsent(context.getClass(), ClientGuiPoseCompat::findPoseMethod);
        return method == null ? null : method.invoke(context);
    }

    private static Method findPoseMethod(Class<?> contextClass) {
        Method named = findNoArgMethod(contextClass, "pose", "method_51448");
        if (named != null) {
            return named;
        }
        for (Method method : contextClass.getMethods()) {
            if (method.getParameterCount() == 0 && isSupportedStackType(method.getReturnType())) {
                return method;
            }
        }
        warnOnce("当前 Minecraft 版本缺少可用的 GUI 矩阵栈入口", null);
        return null;
    }

    private static boolean isSupportedStackType(Class<?> type) {
        return MATRIX_3X2_STACK_NAME.equals(type.getName()) || "net.minecraft.class_4587".equals(type.getName()) || type.getName().endsWith(".PoseStack");
    }

    private static Method findNoArgMethod(Class<?> owner, String... names) {
        for (String name : names) {
            try {
                return owner.getMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> owner, Class<?>[] parameterTypes, String... names) {
        for (String name : names) {
            try {
                return owner.getMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static void warnOnce(String message, Throwable throwable) {
        if (WARNED.compareAndSet(false, true)) {
            if (throwable == null) {
                TeamGlowingConstants.LOGGER.warn(message);
            } else {
                TeamGlowingConstants.LOGGER.warn(message, throwable);
            }
        }
    }

    @FunctionalInterface
    private interface StackOperation {
        void apply(StackAccess access, Object stack) throws ReflectiveOperationException;
    }

    private record StackAccess(
        Method push,
        Method pop,
        Method translate3Float,
        Method translate3Double,
        Method translate2Float,
        Method scale3Float,
        Method scale2Float,
        Method mulPose,
        Method rotate2d
    ) {
        private static StackAccess create(Class<?> stackClass) {
            return new StackAccess(
                findNoArgMethod(stackClass, "pushPose", "method_22903", "pushMatrix"),
                findNoArgMethod(stackClass, "popPose", "method_22909", "popMatrix"),
                findMethod(stackClass, new Class<?>[] {float.class, float.class, float.class}, "translate", "method_46416"),
                findMethod(stackClass, new Class<?>[] {double.class, double.class, double.class}, "translate", "method_22904"),
                findMethod(stackClass, new Class<?>[] {float.class, float.class}, "translate"),
                findMethod(stackClass, new Class<?>[] {float.class, float.class, float.class}, "scale", "method_22905"),
                findMethod(stackClass, new Class<?>[] {float.class, float.class}, "scale"),
                findMethod(stackClass, new Class<?>[] {Quaternionf.class}, "mulPose", "method_22907"),
                findMethod(stackClass, new Class<?>[] {float.class}, "rotate")
            );
        }

        private void push(Object stack) throws ReflectiveOperationException {
            invokeRequired(this.push, stack, "push");
        }

        private void pop(Object stack) throws ReflectiveOperationException {
            invokeRequired(this.pop, stack, "pop");
        }

        private void translate(Object stack, float x, float y, float z) throws ReflectiveOperationException {
            if (this.translate3Float != null) {
                this.translate3Float.invoke(stack, x, y, z);
                return;
            }
            if (this.translate3Double != null) {
                this.translate3Double.invoke(stack, (double) x, (double) y, (double) z);
                return;
            }
            if (this.translate2Float != null) {
                this.translate2Float.invoke(stack, x, y);
                return;
            }
            throw new NoSuchMethodException("translate");
        }

        private void scale(Object stack, float x, float y, float z) throws ReflectiveOperationException {
            if (this.scale3Float != null) {
                this.scale3Float.invoke(stack, x, y, z);
                return;
            }
            if (this.scale2Float != null) {
                this.scale2Float.invoke(stack, x, y);
                return;
            }
            throw new NoSuchMethodException("scale");
        }

        private void rotateZDegrees(Object stack, float degrees) throws ReflectiveOperationException {
            float radians = (float) Math.toRadians(degrees);
            if (this.mulPose != null) {
                this.mulPose.invoke(stack, new Quaternionf().rotationZ(radians));
                return;
            }
            if (this.rotate2d != null) {
                this.rotate2d.invoke(stack, radians);
                return;
            }
            throw new NoSuchMethodException("rotate");
        }

        private static void invokeRequired(Method method, Object stack, String name) throws ReflectiveOperationException {
            if (method == null) {
                throw new NoSuchMethodException(name);
            }
            method.invoke(stack);
        }
    }
}
