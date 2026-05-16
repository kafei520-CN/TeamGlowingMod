package cn.kafei.TeamGlowing.command;

import cn.kafei.TeamGlowing.localization.Localization;
import cn.kafei.TeamGlowing.party.LeaveResult;
import cn.kafei.TeamGlowing.party.Party;
import cn.kafei.TeamGlowing.party.PartyColorHelper;
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
                .then(CommandManager.argument("input", StringArgumentType.greedyString())
                    .executes(context -> handleCreate(context, partyManager, localization, persistence))))
            .then(CommandManager.literal("rename")
                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                    .executes(context -> handleRename(context, partyManager, localization, persistence))))
            .then(CommandManager.literal("color")
                .then(CommandManager.argument("color", StringArgumentType.greedyString())
                    .suggests((context, builder) -> CommandSource.suggestMatching(PartyColorHelper.getSuggestedInputs(), builder))
                    .executes(context -> handleColor(context, partyManager, localization, persistence))))
            .then(CommandManager.literal("invite")
                .then(CommandManager.argument("player", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestMatching(
                        context.getSource().getServer().getPlayerManager().getPlayerNames(), builder))
                    .executes(context -> handleInvite(context, partyManager, localization, persistence))))
            .then(CommandManager.literal("admin")
                .then(CommandManager.literal("add")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> CommandSource.suggestMatching(
                            context.getSource().getServer().getPlayerManager().getPlayerNames(), builder))
                        .executes(context -> handleAdminAdd(context, partyManager, localization, persistence))))
                .then(CommandManager.literal("remove")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> CommandSource.suggestMatching(
                            context.getSource().getServer().getPlayerManager().getPlayerNames(), builder))
                        .executes(context -> handleAdminRemove(context, partyManager, localization, persistence)))))
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
        ParsedCreateInput parsedInput = parseCreateInput(StringArgumentType.getString(context, "input"));
        Integer partyColor = PartyColorHelper.parse(parsedInput.colorInput());
        if (partyColor == null) {
            throw error(localization.translate(player, "party.error.invalid_color"));
        }

        try {
            Party party = partyManager.createParty(PartyManager.getPlayerName(player), parsedInput.partyName(), partyColor.intValue());
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

    private static int handleRename(CommandContext<ServerCommandSource> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String newPartyName = StringArgumentType.getString(context, "name");
        try {
            String renamedParty = partyManager.renameParty(PartyManager.getPlayerName(player), newPartyName);
            persistence.save(partyManager);
            player.sendMessage(Text.literal(localization.translate(player, "party.renamed", renamedParty)), false);
            return 1;
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleColor(CommandContext<ServerCommandSource> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        Integer partyColor = PartyColorHelper.parse(StringArgumentType.getString(context, "color"));
        if (partyColor == null) {
            throw error(localization.translate(player, "party.error.invalid_color"));
        }

        try {
            int updatedColor = partyManager.setPartyColor(PartyManager.getPlayerName(player), partyColor.intValue());
            persistence.save(partyManager);
            player.sendMessage(Text.literal(localization.translate(player, "party.color.updated", PartyColorHelper.formatHex(updatedColor))), false);
            return 1;
        } catch (IllegalStateException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleAdminAdd(CommandContext<ServerCommandSource> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String targetName = StringArgumentType.getString(context, "player");
        try {
            partyManager.addAdmin(PartyManager.getPlayerName(player), targetName);
            persistence.save(partyManager);
            player.sendMessage(Text.literal(localization.translate(player, "party.admin.added", targetName)), false);
            return 1;
        } catch (IllegalStateException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleAdminRemove(CommandContext<ServerCommandSource> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String targetName = StringArgumentType.getString(context, "player");
        try {
            partyManager.removeAdmin(PartyManager.getPlayerName(player), targetName);
            persistence.save(partyManager);
            player.sendMessage(Text.literal(localization.translate(player, "party.admin.removed", targetName)), false);
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
        player.sendMessage(Text.literal(localization.translate(player, "party.info.color", PartyColorHelper.formatHex(info.color))), false);
        player.sendMessage(Text.literal(localization.translate(player, "party.info.leader", info.leaderName)), false);
        player.sendMessage(Text.literal(localization.translate(player, "party.info.admins", info.adminNames.isEmpty() ? "-" : String.join(", ", info.adminNames))), false);
        player.sendMessage(Text.literal(localization.translate(player, "party.info.members", String.join(", ", info.memberNames))), false);
        return 1;
    }

    private static CommandSyntaxException error(String message) {
        return new SimpleCommandExceptionType(Text.literal(message)).create();
    }

    private static ParsedCreateInput parseCreateInput(String rawInput) {
        String normalizedInput = rawInput == null ? "" : rawInput.strip();
        if (normalizedInput.isEmpty()) {
            return new ParsedCreateInput("", null);
        }

        String[] parts = normalizedInput.split("\\s+");
        if (parts.length < 2) {
            return new ParsedCreateInput(normalizedInput, null);
        }

        String lastPart = parts[parts.length - 1];
        if (PartyColorHelper.parse(lastPart) == null) {
            return new ParsedCreateInput(normalizedInput, null);
        }

        int colorStart = normalizedInput.lastIndexOf(lastPart);
        String partyName = colorStart <= 0 ? normalizedInput : normalizedInput.substring(0, colorStart).stripTrailing();
        if (partyName.isEmpty()) {
            return new ParsedCreateInput(normalizedInput, null);
        }
        return new ParsedCreateInput(partyName, lastPart);
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

    private record ParsedCreateInput(String partyName, String colorInput) {
    }
}
