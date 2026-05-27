package cn.kafei.TeamGlowing.persistence;

import java.util.ArrayList;
import java.util.List;

public class SavedParty
{
    public String name;
    public String color;
    public SavedBannerMarker bannerMarker;
    public List<SavedBannerMarker> locatorMarkers = new ArrayList<>();
    public List<String> hiddenLocatorMarkerIds = new ArrayList<>();
    public boolean hideAllLocatorMarkers;
    public String leaderName;
    public String leaderId;
    public List<String> admins = new ArrayList<>();
    public List<SavedMember> members = new ArrayList<>();
}
