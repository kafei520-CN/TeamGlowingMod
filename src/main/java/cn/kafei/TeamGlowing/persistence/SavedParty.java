package cn.kafei.TeamGlowing.persistence;

import java.util.ArrayList;
import java.util.List;

public class SavedParty
{
    public String name;
    public String color;
    public String leaderName;
    public String leaderId;
    public List<String> admins = new ArrayList<>();
    public List<SavedMember> members = new ArrayList<>();
}
