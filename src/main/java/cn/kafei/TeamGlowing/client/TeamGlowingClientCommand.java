package cn.kafei.TeamGlowing.client;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class TeamGlowingClientCommand {
    private TeamGlowingClientCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("teamglowclient")
            .then(ClientCommandManager.literal("on")
                .executes(context -> setEnabled(context.getSource(), true)))
            .then(ClientCommandManager.literal("off")
                .executes(context -> setEnabled(context.getSource(), false)))
            .then(ClientCommandManager.literal("status")
                .executes(context -> sendStatus(context.getSource()))));
    }

    private static int setEnabled(FabricClientCommandSource source, boolean enabled) {
        boolean changed = ClientToggleState.setEnabled(enabled);
        if (!enabled) {
            ClientToggleState.suppressTeammateGlow(source.getClient());
        }

        if (changed) {
            source.sendFeedback(Text.translatable(enabled ? "teamglowing.client.enabled" : "teamglowing.client.disabled"));
        } else {
            source.sendFeedback(Text.translatable(enabled ? "teamglowing.client.already_enabled" : "teamglowing.client.already_disabled"));
        }
        return 1;
    }

    private static int sendStatus(FabricClientCommandSource source) {
        source.sendFeedback(Text.translatable(
            ClientToggleState.isEnabled() ? "teamglowing.client.status.enabled" : "teamglowing.client.status.disabled"
        ));
        return 1;
    }
}
