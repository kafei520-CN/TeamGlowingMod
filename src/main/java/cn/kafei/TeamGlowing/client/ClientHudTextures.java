package cn.kafei.TeamGlowing.client;

import net.minecraft.util.ResourceLocation;

public final class ClientHudTextures
{
    public static final ResourceLocation[] PLAYER_DOTS = new ResourceLocation[] {
        new ResourceLocation("teamglowing", "textures/gui/bplb/player_dot_0.png"),
        new ResourceLocation("teamglowing", "textures/gui/bplb/player_dot_1.png"),
        new ResourceLocation("teamglowing", "textures/gui/bplb/player_dot_2.png"),
        new ResourceLocation("teamglowing", "textures/gui/bplb/player_dot_3.png")
    };
    public static final ResourceLocation[] PLAYER_DOT_OUTLINES = new ResourceLocation[] {
        new ResourceLocation("teamglowing", "textures/gui/bplb/player_dot_outline_0.png"),
        new ResourceLocation("teamglowing", "textures/gui/bplb/player_dot_outline_1.png"),
        new ResourceLocation("teamglowing", "textures/gui/bplb/player_dot_outline_2.png"),
        new ResourceLocation("teamglowing", "textures/gui/bplb/player_dot_outline_3.png")
    };
    public static final ResourceLocation ARROW = new ResourceLocation("teamglowing", "textures/gui/bplb/arrow.png");

    private ClientHudTextures()
    {
    }
}
