package cn.kafei.TeamGlowing.marker;

import java.util.UUID;

public record ServerSharedMarkerState(
    String ownerName,
    String ownerPlayerName,
    String ownerPlayerId,
    SharedMarkerKind kind,
    String dimensionId,
    double x,
    double y,
    double z,
    UUID trackedEntityId,
    long expiresAtMillis
) {
}
