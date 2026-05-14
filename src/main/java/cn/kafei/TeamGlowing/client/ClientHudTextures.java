package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import net.minecraft.util.Identifier;

public final class ClientHudTextures {
    public static final Identifier[] PLAYER_DOTS = new Identifier[] {
        Identifier.of(TeamGlowingConstants.MODID, "textures/gui/bplb/player_dot_0.png"),
        Identifier.of(TeamGlowingConstants.MODID, "textures/gui/bplb/player_dot_1.png"),
        Identifier.of(TeamGlowingConstants.MODID, "textures/gui/bplb/player_dot_2.png"),
        Identifier.of(TeamGlowingConstants.MODID, "textures/gui/bplb/player_dot_3.png")
    };
    public static final Identifier[] PLAYER_DOT_OUTLINES = new Identifier[] {
        Identifier.of(TeamGlowingConstants.MODID, "textures/gui/bplb/player_dot_outline_0.png"),
        Identifier.of(TeamGlowingConstants.MODID, "textures/gui/bplb/player_dot_outline_1.png"),
        Identifier.of(TeamGlowingConstants.MODID, "textures/gui/bplb/player_dot_outline_2.png"),
        Identifier.of(TeamGlowingConstants.MODID, "textures/gui/bplb/player_dot_outline_3.png")
    };
    public static final Identifier ARROW = Identifier.of(TeamGlowingConstants.MODID, "textures/gui/bplb/arrow.png");

    private ClientHudTextures() {
    }
}
