package cn.kafei.TeamGlowing.party;

import net.minecraft.util.math.BlockPos;

public record BannerMarker(String name, String dimensionId, int x, int y, int z, int bannerColorId) {
    public BlockPos toBlockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }
}
