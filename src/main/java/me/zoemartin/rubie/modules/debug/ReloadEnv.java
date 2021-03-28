package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandEvent;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;

@Command
@CommandOptions(
    name = "rlenv",
    description = "Reload .env file",
    perm = CommandPerm.OWNER
)
public class ReloadEnv extends AbstractCommand {
    @Override
    public void run(CommandEvent event) {
        Bot.reloadConfig();
        event.addCheckmark();
    }
}
