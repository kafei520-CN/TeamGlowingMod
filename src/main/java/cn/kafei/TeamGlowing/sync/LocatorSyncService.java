package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.network.TeamGlowingNetwork;
import cn.kafei.TeamGlowing.network.TeamLocatorEntry;
import cn.kafei.TeamGlowing.network.TeamLocatorMessage;
import cn.kafei.TeamGlowing.party.PartyManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class LocatorSyncService
{
    public void syncToPlayer(EntityPlayerMP player, PartyManager partyManager)
    {
        List<EntityPlayerMP> teammates = partyManager.getOnlineTeammates(player);
        List<TeamLocatorEntry> entries = new ArrayList<>();
        for (EntityPlayerMP teammate : teammates)
        {
            if (this.shouldHideLocator(teammate))
            {
                continue;
            }
            entries.add(new TeamLocatorEntry(teammate.getName(), teammate.getName(), teammate.posX, teammate.posY, teammate.posZ));
        }
        TeamGlowingNetwork.CHANNEL.sendTo(new TeamLocatorMessage(entries), player);
    }

    private boolean shouldHideLocator(EntityPlayerMP player)
    {
        return player.isSneaking() || this.isWearingHiddenHelmet(player);
    }

    private boolean isWearingHiddenHelmet(EntityPlayerMP player)
    {
        ItemStack helmet = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (helmet.isEmpty())
        {
            return false;
        }

        Item item = helmet.getItem();
        return item == Item.getItemFromBlock(Blocks.PUMPKIN) || item == Item.getItemFromBlock(Blocks.LIT_PUMPKIN) || item == Items.SKULL;
    }
}
