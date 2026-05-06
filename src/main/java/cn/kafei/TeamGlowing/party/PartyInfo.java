package cn.kafei.TeamGlowing.party;

import java.util.List;

public class PartyInfo
{
    public final String name;
    public final String leaderName;
    public final List<String> memberNames;

    public PartyInfo(String name, String leaderName, List<String> memberNames)
    {
        this.name = name;
        this.leaderName = leaderName;
        this.memberNames = memberNames;
    }
}
