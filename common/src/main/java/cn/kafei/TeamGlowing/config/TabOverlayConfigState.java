package cn.kafei.TeamGlowing.config;

import java.util.List;

public record TabOverlayConfigState(
    boolean enabled,
    boolean topBorderEnabled,
    String topBorderText,
    List<String> headerLines,
    List<String> footerLines,
    String welcomeText,
    int welcomeWidth,
    long welcomeIntervalMs,
    boolean bottomBorderEnabled,
    String bottomBorderText
) {
    public TabOverlayConfigState {
        headerLines = List.copyOf(headerLines);
        footerLines = List.copyOf(footerLines);
        welcomeWidth = Math.max(6, welcomeWidth);
        welcomeIntervalMs = Math.max(50L, welcomeIntervalMs);
    }
}
