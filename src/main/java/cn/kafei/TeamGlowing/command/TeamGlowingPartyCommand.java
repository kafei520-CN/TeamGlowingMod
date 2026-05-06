package cn.kafei.TeamGlowing.command;

import cn.kafei.TeamGlowing.localization.Localization;
import cn.kafei.TeamGlowing.party.LeaveResult;
import cn.kafei.TeamGlowing.party.Party;
import cn.kafei.TeamGlowing.party.PartyInfo;
import cn.kafei.TeamGlowing.party.PartyManager;
import cn.kafei.TeamGlowing.persistence.PartyPersistence;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public class TeamGlowingPartyCommand extends CommandBase
{
    private final PartyManager partyManager;
    private final Localization localization;
    private final PartyPersistence persistence;

    public TeamGlowingPartyCommand(PartyManager partyManager, Localization localization, PartyPersistence persistence)
    {
        this.partyManager = partyManager;
        this.localization = localization;
        this.persistence = persistence;
    }

    @Override
    public String getName()
    {
        return "teamglow";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/teamglow <create|invite|accept|leave|info> ...";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (args.length == 0)
        {
            throw new WrongUsageException(this.getUsage(sender));
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if ("create".equals(action))
        {
            this.handleCreate(player, args);
            return;
        }

        if ("invite".equals(action))
        {
            this.handleInvite(server, player, args);
            return;
        }

        if ("accept".equals(action))
        {
            this.handleAccept(player);
            return;
        }

        if ("leave".equals(action))
        {
            this.handleLeave(player);
            return;
        }

        if ("info".equals(action))
        {
            this.handleInfo(player);
            return;
        }

        throw new WrongUsageException(this.getUsage(sender));
    }

    private void handleCreate(EntityPlayerMP player, String[] args) throws CommandException
    {
        if (args.length < 2)
        {
            throw new WrongUsageException("/teamglow create <partyName>");
        }

        try
        {
            Party party = this.partyManager.createParty(PartyManager.getPlayerName(player), args[1]);
            this.persistence.save(this.partyManager);
            player.sendMessage(new TextComponentString(this.localization.translate(player, "party.created", party.name)));
        }
        catch (IllegalStateException | IllegalArgumentException exception)
        {
            throw new CommandException(this.localization.translate(player, exception.getMessage()));
        }
    }

    private void handleInvite(MinecraftServer server, EntityPlayerMP player, String[] args) throws CommandException
    {
        if (args.length < 2)
        {
            throw new WrongUsageException("/teamglow invite <player>");
        }

        EntityPlayerMP target = getPlayer(server, player, args[1]);
        if (PartyManager.getPlayerName(target).equals(PartyManager.getPlayerName(player)))
        {
            throw new CommandException(this.localization.translate(player, "party.error.cannot_invite_self"));
        }

        try
        {
            this.partyManager.invite(PartyManager.getPlayerName(player), PartyManager.getPlayerName(target));
            this.persistence.save(this.partyManager);
            PartyInfo info = this.partyManager.getPartyInfo(PartyManager.getPlayerName(player));
            player.sendMessage(new TextComponentString(this.localization.translate(player, "party.invited_sender", target.getName(), info.name)));
            target.sendMessage(new TextComponentString(this.localization.translate(target, "party.invited_target", player.getName(), info.name)));
        }
        catch (IllegalStateException exception)
        {
            throw new CommandException(this.localization.translate(player, exception.getMessage()));
        }
    }

    private void handleAccept(EntityPlayerMP player) throws CommandException
    {
        try
        {
            String partyName = this.partyManager.acceptInvite(PartyManager.getPlayerName(player));
            this.persistence.save(this.partyManager);
            player.sendMessage(new TextComponentString(this.localization.translate(player, "party.joined", partyName)));
        }
        catch (IllegalStateException exception)
        {
            throw new CommandException(this.localization.translate(player, exception.getMessage()));
        }
    }

    private void handleLeave(EntityPlayerMP player) throws CommandException
    {
        try
        {
            LeaveResult leaveResult = this.partyManager.leave(PartyManager.getPlayerName(player));
            this.persistence.save(this.partyManager);
            if (leaveResult.disbanded)
            {
                player.sendMessage(new TextComponentString(this.localization.translate(player, "party.left_disbanded", leaveResult.partyName)));
                return;
            }

            if (leaveResult.newLeaderName != null)
            {
                player.sendMessage(new TextComponentString(this.localization.translate(player, "party.left_new_leader", leaveResult.partyName, leaveResult.newLeaderName)));
                return;
            }

            player.sendMessage(new TextComponentString(this.localization.translate(player, "party.left", leaveResult.partyName)));
        }
        catch (IllegalStateException exception)
        {
            throw new CommandException(this.localization.translate(player, exception.getMessage()));
        }
    }

    private void handleInfo(EntityPlayerMP player)
    {
        PartyInfo info = this.partyManager.getPartyInfo(PartyManager.getPlayerName(player));
        if (info == null)
        {
            player.sendMessage(new TextComponentString(this.localization.translate(player, "party.info.none")));
            return;
        }

        player.sendMessage(new TextComponentString(this.localization.translate(player, "party.info.name", info.name)));
        player.sendMessage(new TextComponentString(this.localization.translate(player, "party.info.leader", info.leaderName)));
        player.sendMessage(new TextComponentString(this.localization.translate(player, "party.info.members", String.join(", ", info.memberNames))));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos)
    {
        if (args.length == 1)
        {
            return getListOfStringsMatchingLastWord(args, "create", "invite", "accept", "leave", "info");
        }

        if (args.length == 2 && "invite".equalsIgnoreCase(args[0]))
        {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }

        if (args.length == 2 && "create".equalsIgnoreCase(args[0]))
        {
            return getListOfStringsMatchingLastWord(args, this.partyManager.getKnownPartyNames());
        }

        return Collections.emptyList();
    }
}
