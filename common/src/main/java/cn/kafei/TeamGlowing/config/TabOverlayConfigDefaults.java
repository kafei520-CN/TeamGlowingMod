package cn.kafei.TeamGlowing.config;

import java.util.List;

public final class TabOverlayConfigDefaults {
    public static final String DEFAULT_BORDER = "%border%";

    private TabOverlayConfigDefaults() {
    }

    public static TabOverlayConfigState createState() {
        return new TabOverlayConfigState(
            true,
            true,
            DEFAULT_BORDER,
            List.of(
                "&b&lTeam&f&lGlowing",
                "&3&l>> &f%welcome% &3&l<<",
                "&7延迟: &f%ping%  &8|  &7TPS: &b%tps%  &8|  &7MSPT: &f%mspt%  &8|  &7内存: &f%memory%",
                "&7日期: &f%date%  &8|  &7时间: &f%time%"
            ),
            List.of("&7小队: &b%party%  &8|  &7在线: &f%online%"),
            "&f欢迎来到 &bTeamGlowing&f，%player%",
            18,
            120L,
            true,
            DEFAULT_BORDER
        );
    }
}
