package cn.kafei.TeamGlowing.fabric;

import cn.kafei.TeamGlowing.TeamGlowingCommon;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class FabricUseItemCallbackCompat {
    private static final String CALLBACK_CLASS_NAME = "net.fabricmc.fabric.api.event.player.UseItemCallback";
    private static final String EVENT_CLASS_NAME = "net.fabricmc.fabric.api.event.Event";

    private FabricUseItemCallbackCompat() {
    }

    static void register() {
        try {
            Class<?> callbackType = Class.forName(CALLBACK_CLASS_NAME);
            Object event = callbackType.getField("EVENT").get(null);
            Object listener = Proxy.newProxyInstance(
                callbackType.getClassLoader(),
                new Class<?>[] {callbackType},
                new UseItemInvocationHandler()
            );
            Class.forName(EVENT_CLASS_NAME).getMethod("register", Object.class).invoke(event, listener);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to register Fabric use item callback", exception);
        }
    }

    private static final class UseItemInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return this.invokeObjectMethod(proxy, method, args);
            }
            if (!"interact".equals(method.getName()) || args == null || args.length != 3) {
                return defaultValue(method.getReturnType());
            }

            Player player = (Player) args[0];
            Level world = (Level) args[1];
            InteractionHand hand = (InteractionHand) args[2];
            ItemStack stack = player.getItemInHand(hand);
            InteractionResult result = player instanceof ServerPlayer serverPlayer
                ? TeamGlowingCommon.handleUseItem(serverPlayer, world, hand)
                : FabricInteractionResultCompat.pass();
            return adaptReturn(method.getReturnType(), result, stack);
        }

        private Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "toString" -> "TeamGlowingUseItemCallback";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> null;
            };
        }
    }

    private static Object adaptReturn(Class<?> returnType, InteractionResult result, ItemStack stack) throws ReflectiveOperationException {
        if (returnType.isInstance(result)) {
            return result;
        }
        Constructor<?> constructor = returnType.getConstructor(InteractionResult.class, Object.class);
        return constructor.newInstance(result, stack);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == Boolean.TYPE) {
            return Boolean.FALSE;
        }
        if (returnType == Integer.TYPE || returnType == Short.TYPE || returnType == Byte.TYPE || returnType == Character.TYPE) {
            return Integer.valueOf(0);
        }
        if (returnType == Long.TYPE) {
            return Long.valueOf(0L);
        }
        if (returnType == Float.TYPE) {
            return Float.valueOf(0.0F);
        }
        if (returnType == Double.TYPE) {
            return Double.valueOf(0.0D);
        }
        return null;
    }
}
