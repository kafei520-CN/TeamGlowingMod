package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.marker.SharedMarkerKind;

public record TeammateWorldMarkerEntry(
    String name,
    String playerName,
    String playerId,
    SharedMarkerKind kind,
    String dimensionId,
    double x,
    double y,
    double z,
    String itemId,
    long expiresAtMillis
) {
}
