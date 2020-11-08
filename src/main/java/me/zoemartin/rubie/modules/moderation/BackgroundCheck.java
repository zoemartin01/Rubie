package me.zoemartin.rubie.modules.moderation;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.Check;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BackgroundCheck implements GuildCommand {
    private static final Command USERINFO = CommandManager.getCommands().stream()
                                                     .filter(command -> command.name().equals("userinfo")).findAny()
                                                     .orElse(null);
    private static final Command NOTE = new Note();
    private static final Command MODLOGS = new ModLogs();

    @Override
    public void run(GuildCommandEvent event) {
        Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);
        if (USERINFO != null) USERINFO.run(event);
        NOTE.run(event);
        MODLOGS.run(event);

    }

    @NotNull
    @Override
    public String name() {
        return "backgroundcheck";
    }

    @NotNull
    @Override
    public String regex() {
        return "backgroundcheck|bgc";
    }

    @NotNull
    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.BOT_MODERATOR;
    }

    @NotNull
    @Override
    public String usage() {
        return "<user>";
    }

    @NotNull
    @Override
    public String description() {
        return "Backgroundchecks a user";
    }
}
