package cn.kafei.TeamGlowing.network;

public record TeamLocatorEntry(
    String entryId,
    String name,
    String playerName,
    String playerId,
    String dimensionId,
    double x,
    double y,
    double z,
    boolean banner,
    boolean partyBanner,
    int bannerColorId
) {
}
