package cn.kafei.TeamGlowing.command;

import cn.kafei.TeamGlowing.localization.Localization;
import cn.kafei.TeamGlowing.party.LeaveResult;
import cn.kafei.TeamGlowing.party.Party;
import cn.kafei.TeamGlowing.party.PartyInfo;
import cn.kafei.TeamGlowing.party.PartyManager;
import cn.kafei.TeamGlowing.persistence.PartyPersistence;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class TeamGlowingPartyCommand {
    private TeamGlowingPartyCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, PartyManager partyManager, Localization localization, PartyPersistence persistence) {
        registerRoot(dispatcher, "teamglow", partyManager, localization, persistence);
        registerRoot(dispatcher, "tg", partyManager, localization, persistence);
    }

    private static void registerRoot(CommandDispatcher<ServerCommandSource> dispatcher, String literal, PartyManager partyManager, Localization localization, PartyPersistence persistence) {
        dispatcher.register(CommandManager.literal(literal)
            .requires(source -> true)
            .then(CommandManager.literal("create")
                .then(CommandManager.argument("partyName", StringArgumentType.word())
                    .executes(context -> handleCreate(context, partyManager, localization, persistence))))
            .then(CommandManager.literal("invite")
                .then(CommandManager.argument("player", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestMatching(
                        context.getSource().getServer().getPlayerManager().getPlayerNames(), builder))
                    .executes(context -> handleInvite(context, partyManager, localization, persistence))))
            .then(CommandManager.literal("accept")
                .executes(context -> handleAccept(context, partyManager, localization, persistence)))
            .then(CommandManager.literal("reject")
                .executes(context -> handleReject(context, partyManager, localization, persistence)))
            .then(CommandManager.literal("leave")
                .executes(context -> handleLeave(context, partyManager, localization, persistence)))
            .then(CommandManager.literal("info")
                .executes(context -> handleInfo(context, partyManager, localization))));
    }

    private static int handleCreate(CommandContext<ServerCommandSource> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String partyName = StringArgumentType.getString(context, "partyName");

        try {
            Party party = partyManager.createParty(PartyManager.getPlayerName(player), partyName);
            persistence.save(partyManager);
            player.sendMessage(Text.literal(localization.translate(player, "party.created", party.name)), false);
            return 1;
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleInvite(CommandContext<ServerCommandSource> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String targetName = StringArgumentType.getString(context, "player");
        ServerPlayerEntity target = context.getSource().getServer().getPlayerManager().getPlayer(targetName);
        if (target == null) {
            throw error("Player not found");
        }

        if (PartyManager.getPlayerName(target).equalsIgnoreCase(PartyManager.getPlayerName(player))) {
            throw error(localization.translate(player, "party.error.cannot_invite_self"));
        }

        try {
            partyManager.invite(PartyManager.getPlayerName(player), PartyManager.getPlayerName(target));
            persistence.save(partyManager);
            PartyInfo info = partyManager.getPartyInfo(PartyManager.getPlayerName(player));
            player.sendMessage(Text.literal(localization.translate(player, "party.invited_sender", target.getName().getString(), info.name)), false);
            sendInviteMessage(target, localization, player.getName().getString(), info.name);
            return 1;
        } catch (IllegalStateException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleAccept(CommandContext<ServerCommandSource> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();

        try {
            String partyName = partyManager.acceptInvite(PartyManager.getPlayerName(player));
            persistence.save(partyManager);
            player.sendMessage(Text.literal(localization.translate(player, "party.joined", partyName)), false);
            return 1;
        } catch (IllegalStateException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleReject(CommandContext<ServerCommandSource> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();

        try {
            String partyName = partyManager.rejectInvite(PartyManager.getPlayerName(player));
            persistence.save(partyManager);
            player.sendMessage(Text.literal(localization.translate(player, "party.rejected", partyName)), false);
            return 1;
        } catch (IllegalStateException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleLeave(CommandContext<ServerCommandSource> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();

        try {
            LeaveResult leaveResult = partyManager.leave(PartyManager.getPlayerName(player));
            persistence.save(partyManager);
            if (leaveResult.disbanded) {
                player.sendMessage(Text.literal(localization.translate(player, "party.left_disbanded", leaveResult.partyName)), false);
                return 1;
            }

            if (leaveResult.newLeaderName != null) {
                player.sendMessage(Text.literal(localization.translate(player, "party.left_new_leader", leaveResult.partyName, leaveResult.newLeaderName)), false);
                return 1;
            }

            player.sendMessage(Text.literal(localization.translate(player, "party.left", leaveResult.partyName)), false);
            return 1;
        } catch (IllegalStateException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleInfo(CommandContext<ServerCommandSource> context, PartyManager partyManager, Localization localization) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        PartyInfo info = partyManager.getPartyInfo(PartyManager.getPlayerName(player));
        if (info == null) {
            player.sendMessage(Text.literal(localization.translate(player, "party.info.none")), false);
            return 0;
        }

        player.sendMessage(Text.literal(localization.translate(player, "party.info.name", info.name)), false);
        player.sendMessage(Text.literal(localization.translate(player, "party.info.leader", info.leaderName)), false);
        player.sendMessage(Text.literal(localization.translate(player, "party.info.members", String.join(", ", info.memberNames))), false);
        return 1;
    }

    private static CommandSyntaxException error(String message) {
        return new SimpleCommandExceptionType(Text.literal(message)).create();
    }

    private static void sendInviteMessage(ServerPlayerEntity target, Localization localization, String inviterName, String partyName) {
        Text message = Text.literal(localization.translate(target, "party.invited_target", inviterName, partyName) + " ")
            .append(createActionButton(
                localization.translate(target, "party.invite_action.accept"),
                "/tg accept",
                localization.translate(target, "party.invite_action.accept.hover")
            ))
            .append(Text.literal(" "))
            .append(createActionButton(
                localization.translate(target, "party.invite_action.reject"),
                "/tg reject",
                localization.translate(target, "party.invite_action.reject.hover")
            ));
        target.sendMessage(message, false);
    }

    private static Text createActionButton(String label, String command, String hoverText) {
        return Text.literal(label).styled(style -> style
            .withColor(command.endsWith("accept") ? Formatting.GREEN : Formatting.RED)
            .withBold(true)
            .withClickEvent(new ClickEvent.RunCommand(command))
            .withHoverEvent(new HoverEvent.ShowText(Text.literal(hoverText))));
    }
}
