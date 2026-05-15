package cn.kafei.TeamGlowing.marker;

public enum SharedMarkerKind {
    WAYPOINT,
    ITEM,
    ENTITY,
    CLEAR;

    public static SharedMarkerKind fromOrdinal(int ordinal) {
        SharedMarkerKind[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return WAYPOINT;
        }
        return values[ordinal];
    }
}
