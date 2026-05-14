package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import cn.kafei.TeamGlowing.mixin.EntityAccessor;
import cn.kafei.TeamGlowing.party.PartyManager;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class GlowSyncService {
    private final TrackedData<Byte> entityFlags;

    public GlowSyncService() {
        this.entityFlags = EntityAccessor.teamglowing$getFlags();
    }

    public void syncVisibilityForPlayer(ServerPlayerEntity player, PartyManager partyManager) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
            if (viewer.getId() == player.getId()) {
                continue;
            }

            boolean shouldGlow = partyManager.areTeammates(PartyManager.getPlayerName(viewer), PartyManager.getPlayerName(player));
            this.sendGlowState(viewer, player, shouldGlow);
        }
    }

    public void syncSinglePlayer(ServerPlayerEntity player, PartyManager partyManager) {
        this.syncVisibilityForPlayer(player, partyManager);
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        for (ServerPlayerEntity otherPlayer : server.getPlayerManager().getPlayerList()) {
            if (otherPlayer.getId() != player.getId()) {
                this.syncVisibilityForPlayer(otherPlayer, partyManager);
            }
        }
    }

    private void sendGlowState(ServerPlayerEntity viewer, Entity target, boolean glowing) {
        byte flags = target.getDataTracker().get(this.entityFlags);
        byte updatedFlags = glowing
            ? (byte) (flags | TeamGlowingConstants.ENTITY_GLOWING_FLAG)
            : (byte) (flags & ~TeamGlowingConstants.ENTITY_GLOWING_FLAG);

        List<DataTracker.SerializedEntry<?>> entries = List.of(DataTracker.SerializedEntry.of(this.entityFlags, updatedFlags));
        viewer.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(target.getId(), entries));
    }
}
