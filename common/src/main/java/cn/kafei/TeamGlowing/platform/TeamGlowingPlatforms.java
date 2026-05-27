package cn.kafei.TeamGlowing.platform;

import java.util.Objects;

public final class TeamGlowingPlatforms {
    private static TeamGlowingPlatform platform;

    private TeamGlowingPlatforms() {
    }

    public static synchronized void initialize(TeamGlowingPlatform value) {
        if (platform != null) {
            return;
        }
        platform = Objects.requireNonNull(value, "value");
    }

    public static TeamGlowingPlatform get() {
        TeamGlowingPlatform current = platform;
        if (current == null) {
            throw new IllegalStateException("TeamGlowing platform is not initialized");
        }
        return current;
    }
}
