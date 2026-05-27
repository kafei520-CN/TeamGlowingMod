package cn.kafei.TeamGlowing.party;

import java.util.List;

public class PartyInfo
{
    public final String name;
    public final int color;
    public final String leaderName;
    public final List<String> adminNames;
    public final List<String> memberNames;

    public PartyInfo(String name, int color, String leaderName, List<String> adminNames, List<String> memberNames)
    {
        this.name = name;
        this.color = color;
        this.leaderName = leaderName;
        this.adminNames = adminNames;
        this.memberNames = memberNames;
    }
}
