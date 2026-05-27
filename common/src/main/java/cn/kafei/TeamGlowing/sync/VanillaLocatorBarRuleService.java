package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

public final class VanillaLocatorBarRuleService {
    private static final String DISABLE_LOCATOR_BAR_COMMAND = "gamerule locatorBar false";
    private volatile boolean unavailable;

    public void disableIfAvailable(MinecraftServer server) {
        if (server == null || this.unavailable) {
            return;
        }

        CommandSourceStack source = server.createCommandSourceStack()
            .withPermission(4)
            .withSuppressedOutput();
        try {
            server.getCommands().getDispatcher().execute(DISABLE_LOCATOR_BAR_COMMAND, source);
        } catch (CommandSyntaxException | RuntimeException exception) {
            this.unavailable = true;
            TeamGlowingConstants.LOGGER.debug("Vanilla locator bar gamerule is unavailable, skipping future attempts.", exception);
        }
    }
}
