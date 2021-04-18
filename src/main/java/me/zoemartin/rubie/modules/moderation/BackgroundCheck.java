package me.zoemartin.rubie.modules.moderation;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.Check;

@Command
@CommandOptions(
    name = "backgroundcheck",
    description = "Shows a users account info, notes and modlogs",
    alias = "bgc",
    usage = "<user>",
    perm = CommandPerm.BOT_MODERATOR
)
public class BackgroundCheck extends GuildCommand {
    private static AbstractCommand USERINFO = null;
    private static final AbstractCommand NOTE = new Note();
    private static final AbstractCommand MODLOGS = new ModLogs();

    @Override
    public void run(GuildCommandEvent event) {
        Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);
        if (USERINFO == null) USERINFO = CommandManager.getCommands().stream()
                                             .filter(command -> command.name().equals("userinfo")).findAny()
                                             .orElse(null);
        if (USERINFO != null) USERINFO.run(event);
        NOTE.run(event);
        MODLOGS.run(event);

    }
}
