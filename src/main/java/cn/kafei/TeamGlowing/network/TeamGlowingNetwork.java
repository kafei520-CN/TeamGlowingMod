package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class TeamGlowingNetwork
{
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(TeamGlowingConstants.MODID);

    private TeamGlowingNetwork()
    {
    }

    public static void register()
    {
        CHANNEL.registerMessage(TeamLocatorMessageHandler.class, TeamLocatorMessage.class, 0, Side.CLIENT);
    }
}
