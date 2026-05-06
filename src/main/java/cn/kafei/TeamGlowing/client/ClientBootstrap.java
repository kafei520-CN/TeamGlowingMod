package cn.kafei.TeamGlowing.client;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class ClientBootstrap
{
    private static boolean initialized;

    private ClientBootstrap()
    {
    }

    public static void init()
    {
        if (initialized)
        {
            return;
        }
        initialized = true;
        MinecraftForge.EVENT_BUS.register(new ClientHudRenderer());
        MinecraftForge.EVENT_BUS.register(new ClientBootstrap());
    }

    @SubscribeEvent
    public void onClientLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        ClientLocatorCache.clear();
    }
}
