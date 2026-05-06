package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.client.ClientLocatorCache;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TeamLocatorMessageHandler implements IMessageHandler<TeamLocatorMessage, IMessage>
{
    @Override
    public IMessage onMessage(TeamLocatorMessage message, MessageContext ctx)
    {
        Minecraft.getMinecraft().addScheduledTask(() -> ClientLocatorCache.update(message.entries));
        return null;
    }
}
