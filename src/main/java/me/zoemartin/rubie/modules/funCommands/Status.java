package me.zoemartin.rubie.modules.funCommands;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.*;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static me.zoemartin.rubie.core.managers.CommandManager.register;

@LoadModule
public class Status implements Module, Command {
    @Override
    public @NotNull Set<Command> subCommands() {
        return Collections.emptySet();
    }

    @Override
    public @NotNull String name() {
        return "status";
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void run(CommandEvent event) {
        Check.check(event.getArgs().size() >= 2, CommandArgumentException::new);

        int id = Parser.Int.parse(event.getArgs().get(0));
        Check.notNull(id, CommandArgumentException::new);

        Activity.ActivityType type = Activity.ActivityType.fromKey(id);

        Bot.getJDA().getPresence().setActivity(Activity.of(type, lastArg(1, event)));
        event.getChannel().sendMessageFormat("Set bot status to `%s %s`", Bot.getJDA().getPresence().getActivity().getType(),
            Bot.getJDA().getPresence().getActivity()).queue();
    }

    @Override
    public @NotNull CommandPerm commandPerm() {
        return CommandPerm.OWNER;
    }

    @Override
    public @NotNull String usage() {
        return "<type id> <status...>";
    }

    @Override
    public @NotNull String description() {
        return "Sets the bot's status";
    }

    @Override
    public void init() {
        register(new Status());
        register(new Echo());
    }
}
