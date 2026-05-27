package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import net.minecraft.resources.ResourceLocation;

public final class ClientHudTextures {
    public static final ResourceLocation[] PLAYER_DOTS = new ResourceLocation[] {
        ResourceLocation.fromNamespaceAndPath(TeamGlowingConstants.MODID, "textures/gui/bplb/player_dot_0.png"),
        ResourceLocation.fromNamespaceAndPath(TeamGlowingConstants.MODID, "textures/gui/bplb/player_dot_1.png"),
        ResourceLocation.fromNamespaceAndPath(TeamGlowingConstants.MODID, "textures/gui/bplb/player_dot_2.png"),
        ResourceLocation.fromNamespaceAndPath(TeamGlowingConstants.MODID, "textures/gui/bplb/player_dot_3.png")
    };
    public static final ResourceLocation[] PLAYER_DOT_OUTLINES = new ResourceLocation[] {
        ResourceLocation.fromNamespaceAndPath(TeamGlowingConstants.MODID, "textures/gui/bplb/player_dot_outline_0.png"),
        ResourceLocation.fromNamespaceAndPath(TeamGlowingConstants.MODID, "textures/gui/bplb/player_dot_outline_1.png"),
        ResourceLocation.fromNamespaceAndPath(TeamGlowingConstants.MODID, "textures/gui/bplb/player_dot_outline_2.png"),
        ResourceLocation.fromNamespaceAndPath(TeamGlowingConstants.MODID, "textures/gui/bplb/player_dot_outline_3.png")
    };
    public static final ResourceLocation ARROW = ResourceLocation.fromNamespaceAndPath(TeamGlowingConstants.MODID, "textures/gui/bplb/arrow.png");

    private ClientHudTextures() {
    }
}
