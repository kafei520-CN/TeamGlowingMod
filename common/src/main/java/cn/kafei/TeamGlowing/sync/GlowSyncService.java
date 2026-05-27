package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import cn.kafei.TeamGlowing.mixin.EntityAccessor;
import cn.kafei.TeamGlowing.party.PartyManager;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class GlowSyncService {
    private final EntityDataAccessor<Byte> entityFlags;

    public GlowSyncService() {
        this.entityFlags = EntityAccessor.teamglowing$getFlags();
    }

    public void syncVisibilityForPlayer(ServerPlayer player, PartyManager partyManager) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (viewer.getId() == player.getId()) {
                continue;
            }

            boolean shouldGlow = partyManager.areTeammates(PartyManager.getPlayerName(viewer), PartyManager.getPlayerName(player));
            this.sendGlowState(viewer, player, shouldGlow);
        }
    }

    public void syncSinglePlayer(ServerPlayer player, PartyManager partyManager) {
        this.syncVisibilityForPlayer(player, partyManager);
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        for (ServerPlayer otherPlayer : server.getPlayerList().getPlayers()) {
            if (otherPlayer.getId() != player.getId()) {
                this.syncVisibilityForPlayer(otherPlayer, partyManager);
            }
        }
    }

    private void sendGlowState(ServerPlayer viewer, Entity target, boolean glowing) {
        byte flags = target.getEntityData().get(this.entityFlags);
        byte updatedFlags = glowing
            ? (byte) (flags | TeamGlowingConstants.ENTITY_GLOWING_FLAG)
            : (byte) (flags & ~TeamGlowingConstants.ENTITY_GLOWING_FLAG);

        List<SynchedEntityData.DataValue<?>> entries = List.of(SynchedEntityData.DataValue.create(this.entityFlags, updatedFlags));
        viewer.connection.send(new ClientboundSetEntityDataPacket(target.getId(), entries));
    }
}
