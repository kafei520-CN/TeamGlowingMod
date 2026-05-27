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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TeamGlowingPartyCommand {
    private TeamGlowingPartyCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, PartyManager partyManager, Localization localization, PartyPersistence persistence) {
        registerRoot(dispatcher, "teamglow", partyManager, localization, persistence);
        registerRoot(dispatcher, "tg", partyManager, localization, persistence);
    }

    private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher, String literal, PartyManager partyManager, Localization localization, PartyPersistence persistence) {
        dispatcher.register(Commands.literal(literal)
            .requires(source -> true)
            .then(Commands.literal("create")
                .then(Commands.argument("input", StringArgumentType.greedyString())
                    .executes(context -> handleCreate(context, partyManager, localization, persistence))))
            .then(Commands.literal("rename")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                    .executes(context -> handleRename(context, partyManager, localization, persistence))))
            .then(Commands.literal("color")
                .then(Commands.argument("color", StringArgumentType.greedyString())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(PartyColorHelper.getSuggestedInputs(), builder))
                    .executes(context -> handleColor(context, partyManager, localization, persistence))))
            .then(Commands.literal("invite")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        context.getSource().getServer().getPlayerList().getPlayerNamesArray(), builder))
                    .executes(context -> handleInvite(context, partyManager, localization, persistence))))
            .then(Commands.literal("admin")
                .then(Commands.literal("add")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                            context.getSource().getServer().getPlayerList().getPlayerNamesArray(), builder))
                        .executes(context -> handleAdminAdd(context, partyManager, localization, persistence))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                            context.getSource().getServer().getPlayerList().getPlayerNamesArray(), builder))
                        .executes(context -> handleAdminRemove(context, partyManager, localization, persistence)))))
            .then(Commands.literal("accept")
                .executes(context -> handleAccept(context, partyManager, localization, persistence)))
            .then(Commands.literal("reject")
                .executes(context -> handleReject(context, partyManager, localization, persistence)))
            .then(Commands.literal("leave")
                .executes(context -> handleLeave(context, partyManager, localization, persistence)))
            .then(Commands.literal("info")
                .executes(context -> handleInfo(context, partyManager, localization))));
    }

    private static int handleCreate(CommandContext<CommandSourceStack> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ParsedCreateInput parsedInput = parseCreateInput(StringArgumentType.getString(context, "input"));
        Integer partyColor = PartyColorHelper.parse(parsedInput.colorInput());
        if (partyColor == null) {
            throw error(localization.translate(player, "party.error.invalid_color"));
        }

        try {
            Party party = partyManager.createParty(PartyManager.getPlayerName(player), parsedInput.partyName(), partyColor.intValue());
            persistence.save(partyManager);
            player.displayClientMessage(Component.literal(localization.translate(player, "party.created", party.name)), false);
            return 1;
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleInvite(CommandContext<CommandSourceStack> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String targetName = StringArgumentType.getString(context, "player");
        ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayerByName(targetName);
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
            player.displayClientMessage(Component.literal(localization.translate(player, "party.invited_sender", target.getName().getString(), info.name)), false);
            sendInviteMessage(target, localization, player.getName().getString(), info.name);
            return 1;
        } catch (IllegalStateException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleRename(CommandContext<CommandSourceStack> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String newPartyName = StringArgumentType.getString(context, "name");
        try {
            String renamedParty = partyManager.renameParty(PartyManager.getPlayerName(player), newPartyName);
            persistence.save(partyManager);
            player.displayClientMessage(Component.literal(localization.translate(player, "party.renamed", renamedParty)), false);
            return 1;
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleColor(CommandContext<CommandSourceStack> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Integer partyColor = PartyColorHelper.parse(StringArgumentType.getString(context, "color"));
        if (partyColor == null) {
            throw error(localization.translate(player, "party.error.invalid_color"));
        }

        try {
            int updatedColor = partyManager.setPartyColor(PartyManager.getPlayerName(player), partyColor.intValue());
            persistence.save(partyManager);
            player.displayClientMessage(Component.literal(localization.translate(player, "party.color.updated", PartyColorHelper.formatHex(updatedColor))), false);
            return 1;
        } catch (IllegalStateException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleAdminAdd(CommandContext<CommandSourceStack> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String targetName = StringArgumentType.getString(context, "player");
        try {
            partyManager.addAdmin(PartyManager.getPlayerName(player), targetName);
            persistence.save(partyManager);
            player.displayClientMessage(Component.literal(localization.translate(player, "party.admin.added", targetName)), false);
            return 1;
        } catch (IllegalStateException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleAdminRemove(CommandContext<CommandSourceStack> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String targetName = StringArgumentType.getString(context, "player");
        try {
            partyManager.removeAdmin(PartyManager.getPlayerName(player), targetName);
            persistence.save(partyManager);
            player.displayClientMessage(Component.literal(localization.translate(player, "party.admin.removed", targetName)), false);
            return 1;
        } catch (IllegalStateException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleAccept(CommandContext<CommandSourceStack> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        try {
            String partyName = partyManager.acceptInvite(PartyManager.getPlayerName(player));
            persistence.save(partyManager);
            player.displayClientMessage(Component.literal(localization.translate(player, "party.joined", partyName)), false);
            return 1;
        } catch (IllegalStateException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleReject(CommandContext<CommandSourceStack> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        try {
            String partyName = partyManager.rejectInvite(PartyManager.getPlayerName(player));
            persistence.save(partyManager);
            player.displayClientMessage(Component.literal(localization.translate(player, "party.rejected", partyName)), false);
            return 1;
        } catch (IllegalStateException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleLeave(CommandContext<CommandSourceStack> context, PartyManager partyManager, Localization localization, PartyPersistence persistence) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        try {
            LeaveResult leaveResult = partyManager.leave(PartyManager.getPlayerName(player));
            persistence.save(partyManager);
            if (leaveResult.disbanded) {
                player.displayClientMessage(Component.literal(localization.translate(player, "party.left_disbanded", leaveResult.partyName)), false);
                return 1;
            }

            if (leaveResult.newLeaderName != null) {
                player.displayClientMessage(Component.literal(localization.translate(player, "party.left_new_leader", leaveResult.partyName, leaveResult.newLeaderName)), false);
                return 1;
            }

            player.displayClientMessage(Component.literal(localization.translate(player, "party.left", leaveResult.partyName)), false);
            return 1;
        } catch (IllegalStateException exception) {
            throw error(localization.translate(player, exception.getMessage()));
        }
    }

    private static int handleInfo(CommandContext<CommandSourceStack> context, PartyManager partyManager, Localization localization) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PartyInfo info = partyManager.getPartyInfo(PartyManager.getPlayerName(player));
        if (info == null) {
            player.displayClientMessage(Component.literal(localization.translate(player, "party.info.none")), false);
            return 0;
        }

        player.displayClientMessage(Component.literal(localization.translate(player, "party.info.name", info.name)), false);
        player.displayClientMessage(Component.literal(localization.translate(player, "party.info.color", PartyColorHelper.formatHex(info.color))), false);
        player.displayClientMessage(Component.literal(localization.translate(player, "party.info.leader", info.leaderName)), false);
        player.displayClientMessage(Component.literal(localization.translate(player, "party.info.admins", info.adminNames.isEmpty() ? "-" : String.join(", ", info.adminNames))), false);
        player.displayClientMessage(Component.literal(localization.translate(player, "party.info.members", String.join(", ", info.memberNames))), false);
        return 1;
    }

    private static CommandSyntaxException error(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
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

    private static void sendInviteMessage(ServerPlayer target, Localization localization, String inviterName, String partyName) {
        Component message = Component.literal(localization.translate(target, "party.invited_target", inviterName, partyName) + " ")
            .append(createActionButton(
                localization.translate(target, "party.invite_action.accept"),
                "/tg accept",
                localization.translate(target, "party.invite_action.accept.hover")
            ))
            .append(Component.literal(" "))
            .append(createActionButton(
                localization.translate(target, "party.invite_action.reject"),
                "/tg reject",
                localization.translate(target, "party.invite_action.reject.hover")
            ));
        target.displayClientMessage(message, false);
    }

    private static Component createActionButton(String label, String command, String hoverText) {
        return Component.literal(label).withStyle(style -> style
            .withColor(command.endsWith("accept") ? ChatFormatting.GREEN : ChatFormatting.RED)
            .withBold(true)
            .withClickEvent(ChatEventCompat.runCommand(command))
            .withHoverEvent(ChatEventCompat.showText(Component.literal(hoverText))));
    }

    private record ParsedCreateInput(String partyName, String colorInput) {
    }
}
