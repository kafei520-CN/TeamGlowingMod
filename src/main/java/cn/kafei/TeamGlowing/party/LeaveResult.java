package cn.kafei.TeamGlowing.party;

public class LeaveResult
{
    public final String partyName;
    public final boolean disbanded;
    public final String newLeaderName;

    public LeaveResult(String partyName, boolean disbanded, String newLeaderName)
    {
        this.partyName = partyName;
        this.disbanded = disbanded;
        this.newLeaderName = newLeaderName;
    }
}
