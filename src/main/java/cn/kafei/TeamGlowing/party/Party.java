package cn.kafei.TeamGlowing.party;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Party
{
    public final String name;
    public final int color;
    public final Set<String> memberNameKeys = new LinkedHashSet<>();
    public final Set<String> adminNameKeys = new LinkedHashSet<>();
    public final Map<String, String> playerNames = new HashMap<>();
    public String leaderNameKey;

    public Party(String name, int color, String leaderNameKey, String leaderName)
    {
        this.name = name;
        this.color = PartyColorHelper.normalize(color);
        this.leaderNameKey = leaderNameKey;
        this.memberNameKeys.add(leaderNameKey);
        this.playerNames.put(leaderNameKey, leaderName);
    }
}
