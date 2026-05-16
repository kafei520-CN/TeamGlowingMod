package cn.kafei.TeamGlowing.client;

import net.minecraft.client.MinecraftClient;

public final class PartyRespawnRequestScheduler {
    private static int ticksUntilRespawn = -1;

    private PartyRespawnRequestScheduler() {
    }

    public static void schedule() {
        ticksUntilRespawn = 1;
    }

    public static void clear() {
        ticksUntilRespawn = -1;
    }

    public static void tick(MinecraftClient client) {
        if (ticksUntilRespawn < 0) {
            return;
        }
        if (client == null || client.player == null) {
            ticksUntilRespawn = -1;
            return;
        }
        if (ticksUntilRespawn > 0) {
            ticksUntilRespawn--;
            return;
        }
        ticksUntilRespawn = -1;
        client.player.requestRespawn();
    }
}
