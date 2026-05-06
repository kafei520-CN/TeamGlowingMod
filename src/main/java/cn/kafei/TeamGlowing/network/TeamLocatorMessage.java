package cn.kafei.TeamGlowing.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class TeamLocatorMessage implements IMessage
{
    public final List<TeamLocatorEntry> entries = new ArrayList<>();

    public TeamLocatorMessage()
    {
    }

    public TeamLocatorMessage(List<TeamLocatorEntry> entries)
    {
        this.entries.addAll(entries);
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.entries.clear();
        int size = buf.readInt();
        for (int index = 0; index < size; index++)
        {
            String name = ByteBufUtils.readUTF8String(buf);
            String playerName = ByteBufUtils.readUTF8String(buf);
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            this.entries.add(new TeamLocatorEntry(name, playerName, x, y, z));
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(this.entries.size());
        for (TeamLocatorEntry entry : this.entries)
        {
            ByteBufUtils.writeUTF8String(buf, entry.name);
            ByteBufUtils.writeUTF8String(buf, entry.playerName);
            buf.writeDouble(entry.x);
            buf.writeDouble(entry.y);
            buf.writeDouble(entry.z);
        }
    }
}
