package cn.kafei.TeamGlowing.tracker;

public class TrackerMarker
{
    public final String name;
    public final double distance;
    public final int slot;
    public final char symbol;
    public final String color;

    public TrackerMarker(String name, double distance, int slot, char symbol, String color)
    {
        this.name = name;
        this.distance = distance;
        this.slot = slot;
        this.symbol = symbol;
        this.color = color;
    }
}
