package cn.kafei.TeamGlowing.persistence;

import java.util.ArrayList;
import java.util.List;

public class PartySaveData
{
    public List<SavedParty> parties = new ArrayList<>();
    public List<SavedInvite> invites = new ArrayList<>();
    public List<SavedBannerMarker> locatorMarkers = new ArrayList<>();
    public SavedBannerMarker partyBanner;
    public String partyBannerOwner;
    public boolean partyBannerHidden;
}
