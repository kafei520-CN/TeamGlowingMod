package cn.kafei.TeamGlowing.network;

public class TeamLocatorEntry
{
    public final String name;
    public final String playerName;
    public final double x;
    public final double y;
    public final double z;

    public TeamLocatorEntry(String name, String playerName, double x, double y, double z)
    {
        this.name = name;
        this.playerName = playerName;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
