package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.network.TeamLocatorEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientLocatorCache
{
    private static List<TeamLocatorEntry> entries = new ArrayList<>();

    private ClientLocatorCache()
    {
    }

    public static void update(List<TeamLocatorEntry> updatedEntries)
    {
        entries = new ArrayList<>(updatedEntries);
    }

    public static List<TeamLocatorEntry> getEntries()
    {
        return Collections.unmodifiableList(entries);
    }

    public static void clear()
    {
        entries.clear();
    }
}
