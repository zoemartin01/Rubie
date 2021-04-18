package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandEvent;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import org.jetbrains.annotations.NotNull;

@Command
@CommandOptions(
    name = "shutdown",
    description = "Shut down the bot",
    perm = CommandPerm.OWNER
)
public class Shutdown extends AbstractCommand {
    private static final int EXIT_CODE_PROPER_SHUTDOWN = 0;
    private static final int EXIT_CODE_RESTART = 10;
    private static final int EXIT_CODE_UPGRADE = 20;

    @Override
    public void run(CommandEvent event) {
        event.getChannel().sendMessageFormat("Shutting down soon! :)").complete();
        Bot.shutdownWithCode(EXIT_CODE_PROPER_SHUTDOWN, false);
    }

    @SubCommand(Shutdown.class)
    @CommandOptions(
        name = "force",
        description = "Forces the bot to shut down and cancels RestActions",
        perm = CommandPerm.OWNER,
        alias = "now"
    )
    private static class Force extends AbstractCommand {
        @Override
        public void run(CommandEvent event) {
            event.getChannel().sendMessageFormat("Shutting down now!").complete();
            Bot.shutdownWithCode(EXIT_CODE_PROPER_SHUTDOWN, true);
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.OWNER;
        }
    }

    @SubCommand(Shutdown.class)
    @CommandOptions(
        name = "update",
        description = "Updates the bot to the newest version and restarts",
        perm = CommandPerm.OWNER,
        alias = "upgrade"
    )
    @SubCommand.AsBase(name = "update", alias = "upgrade")
    private static class Upgrade extends AbstractCommand {
        @Override
        public void run(CommandEvent event) {
            event.getChannel().sendMessageFormat("Upgrading the bot and rebooting!").complete();
            Bot.shutdownWithCode(EXIT_CODE_UPGRADE, true);
        }
    }

    @SubCommand(Shutdown.class)
    @CommandOptions(
        name = "restart",
        description = "Restarts the bot",
        perm = CommandPerm.OWNER,
        alias = "reboot"
    )
    @SubCommand.AsBase(name = "restart", alias = "reboot")
    private static class Restart extends AbstractCommand {
        @Override
        public void run(CommandEvent event) {
            event.getChannel().sendMessageFormat("Restarting the bot!").complete();
            Bot.shutdownWithCode(EXIT_CODE_RESTART, true);
        }
    }
}
