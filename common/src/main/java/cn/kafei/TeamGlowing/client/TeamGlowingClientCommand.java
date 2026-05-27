package cn.kafei.TeamGlowing.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class TeamGlowingClientCommand {
    private TeamGlowingClientCommand() {
    }

    public static <S> void register(
        CommandDispatcher<S> dispatcher,
        Function<S, Minecraft> clientGetter,
        BiConsumer<S, Component> feedbackSender
    ) {
        registerRoot(dispatcher, "teamglowclient", clientGetter, feedbackSender);
        registerRoot(dispatcher, "tgc", clientGetter, feedbackSender);
    }

    private static <S> void registerRoot(
        CommandDispatcher<S> dispatcher,
        String literal,
        Function<S, Minecraft> clientGetter,
        BiConsumer<S, Component> feedbackSender
    ) {
        dispatcher.register(LiteralArgumentBuilder.<S>literal(literal)
            .then(LiteralArgumentBuilder.<S>literal("on")
                .executes(context -> setEnabled(context.getSource(), true, clientGetter, feedbackSender)))
            .then(LiteralArgumentBuilder.<S>literal("off")
                .executes(context -> setEnabled(context.getSource(), false, clientGetter, feedbackSender)))
            .then(LiteralArgumentBuilder.<S>literal("status")
                .executes(context -> sendStatus(context.getSource(), feedbackSender))));
    }

    private static <S> int setEnabled(
        S source,
        boolean enabled,
        Function<S, Minecraft> clientGetter,
        BiConsumer<S, Component> feedbackSender
    ) {
        boolean changed = ClientToggleState.setEnabled(enabled);
        if (!enabled) {
            ClientToggleState.suppressTeammateGlow(clientGetter.apply(source));
        }

        if (changed) {
            feedbackSender.accept(source, Component.translatable(enabled ? "teamglowing.client.enabled" : "teamglowing.client.disabled"));
        } else {
            feedbackSender.accept(source, Component.translatable(enabled ? "teamglowing.client.already_enabled" : "teamglowing.client.already_disabled"));
        }
        return 1;
    }

    private static <S> int sendStatus(S source, BiConsumer<S, Component> feedbackSender) {
        feedbackSender.accept(source, Component.translatable(
            ClientToggleState.isEnabled() ? "teamglowing.client.status.enabled" : "teamglowing.client.status.disabled"
        ));
        return 1;
    }
}
