package cn.kafei.TeamGlowing.party;

public final class BannerMarkerEntryIds {
    private BannerMarkerEntryIds() {
    }

    public static String partyBanner(BannerMarker marker) {
        return "party-banner:" + PartyManager.getMarkerKey(marker);
    }

    public static String locatorBanner(BannerMarker marker) {
        return "locator-banner:" + PartyManager.getMarkerKey(marker);
    }
}
