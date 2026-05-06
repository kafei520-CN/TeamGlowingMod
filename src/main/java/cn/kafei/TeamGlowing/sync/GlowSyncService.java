package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import cn.kafei.TeamGlowing.party.PartyManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketEntityMetadata;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class GlowSyncService
{
    private Field dataEntriesField;
    private Field packetEntityIdField;
    private Field packetDataEntriesField;

    public void initReflection()
    {
        this.dataEntriesField = ReflectionHelper.findField(EntityDataManager.class, "entries", "field_187234_c");
        this.packetEntityIdField = ReflectionHelper.findField(SPacketEntityMetadata.class, "entityId", "field_149379_a");
        this.packetDataEntriesField = ReflectionHelper.findField(SPacketEntityMetadata.class, "dataManagerEntries", "field_149378_b");
    }

    public void syncVisibilityForPlayer(EntityPlayerMP player, PartyManager partyManager)
    {
        MinecraftServer server = player.getServer();
        if (server == null)
        {
            return;
        }

        for (EntityPlayerMP viewer : server.getPlayerList().getPlayers())
        {
            if (viewer.getEntityId() == player.getEntityId())
            {
                continue;
            }

            boolean shouldGlow = partyManager.areTeammates(PartyManager.getPlayerName(viewer), PartyManager.getPlayerName(player));
            this.sendGlowState(viewer, player, shouldGlow);
        }
    }

    public void syncSinglePlayer(EntityPlayerMP player, PartyManager partyManager)
    {
        this.syncVisibilityForPlayer(player, partyManager);
        MinecraftServer server = player.getServer();
        if (server == null)
        {
            return;
        }

        for (EntityPlayerMP otherPlayer : server.getPlayerList().getPlayers())
        {
            if (otherPlayer.getEntityId() != player.getEntityId())
            {
                this.syncVisibilityForPlayer(otherPlayer, partyManager);
            }
        }
    }

    private void sendGlowState(EntityPlayerMP viewer, Entity target, boolean glowing)
    {
        EntityDataManager dataManager = target.getDataManager();
        List<EntityDataManager.DataEntry<?>> copiedEntries = this.copyDataEntries(dataManager);
        if (copiedEntries == null || copiedEntries.isEmpty())
        {
            return;
        }

        EntityDataManager.DataEntry<?> flagsEntry = copiedEntries.get(0);
        Object rawValue = flagsEntry.getValue();
        if (!(rawValue instanceof Byte))
        {
            return;
        }

        byte flags = (Byte) rawValue;
        byte updatedFlags = glowing ? (byte) (flags | TeamGlowingConstants.ENTITY_GLOWING_FLAG) : (byte) (flags & ~TeamGlowingConstants.ENTITY_GLOWING_FLAG);
        @SuppressWarnings("unchecked")
        EntityDataManager.DataEntry<Byte> byteFlagsEntry = (EntityDataManager.DataEntry<Byte>) flagsEntry;
        byteFlagsEntry.setValue(updatedFlags);

        try
        {
            SPacketEntityMetadata packet = new SPacketEntityMetadata();
            this.packetEntityIdField.set(packet, target.getEntityId());
            this.packetDataEntriesField.set(packet, copiedEntries);
            viewer.connection.sendPacket(packet);
        }
        catch (IllegalAccessException exception)
        {
            throw new IllegalStateException("Failed to send selective glow metadata", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private List<EntityDataManager.DataEntry<?>> copyDataEntries(EntityDataManager dataManager)
    {
        try
        {
            Map<Integer, EntityDataManager.DataEntry<?>> entries = (Map<Integer, EntityDataManager.DataEntry<?>>) this.dataEntriesField.get(dataManager);
            if (entries == null || entries.isEmpty())
            {
                return Collections.emptyList();
            }

            List<EntityDataManager.DataEntry<?>> copiedEntries = new ArrayList<>(entries.size());
            for (EntityDataManager.DataEntry<?> entry : entries.values())
            {
                copiedEntries.add(entry.copy());
            }
            return copiedEntries;
        }
        catch (IllegalAccessException exception)
        {
            throw new IllegalStateException("Failed to read entity metadata entries", exception);
        }
    }
}
