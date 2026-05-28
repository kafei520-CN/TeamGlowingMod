package cn.kafei.TeamGlowing.neoforge;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

@SuppressWarnings("null")
final class NeoForgeClientPacketSender {
    private static final Method SEND_TO_SERVER = findSendToServerMethod();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private NeoForgeClientPacketSender() {
    }

    static void send(CustomPacketPayload payload) {
        if (SEND_TO_SERVER == null) {
            warnOnce("当前 NeoForge 版本缺少可用的客户端发包 API，无法发送 TeamGlowing 数据包", null);
            return;
        }

        try {
            SEND_TO_SERVER.invoke(null, payload, new CustomPacketPayload[0]);
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException exception) {
            warnOnce("NeoForge 客户端发包兼容调用失败", exception);
        }
    }

    private static Method findSendToServerMethod() {
        Method modernMethod = findSendToServerMethod("net.neoforged.neoforge.client.network.ClientPacketDistributor");
        if (modernMethod != null) {
            return modernMethod;
        }
        return findSendToServerMethod("net.neoforged.neoforge.network.PacketDistributor");
    }

    private static Method findSendToServerMethod(String className) {
        try {
            Class<?> distributorClass = Class.forName(className);
            return distributorClass.getMethod("sendToServer", CustomPacketPayload.class, CustomPacketPayload[].class);
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            return null;
        }
    }

    private static void warnOnce(String message, Throwable throwable) {
        if (!WARNED.compareAndSet(false, true)) {
            return;
        }
        if (throwable == null) {
            TeamGlowingConstants.LOGGER.warn(message);
        } else {
            TeamGlowingConstants.LOGGER.warn(message, throwable);
        }
    }
}
