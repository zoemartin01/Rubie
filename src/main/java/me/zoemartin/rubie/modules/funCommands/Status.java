package me.zoemartin.rubie.modules.funCommands;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.LoadModule;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.*;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.entities.*;

import java.util.*;

@LoadModule
public class Status implements Module, GuildCommand {
    @Override
    public Set<Command> subCommands() {
        return Collections.emptySet();
    }

    @Override
    public String name() {
        return "status";
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
        Check.check(args.size() >= 2, CommandArgumentException::new);

        int id = Parser.Int.parse(args.get(0));
        Check.notNull(id, CommandArgumentException::new);

        Activity.ActivityType type = Activity.ActivityType.fromKey(id);

        Bot.getJDA().getPresence().setActivity(Activity.of(type, lastArg(1, args, original)));
        channel.sendMessageFormat("Set bot status to `%s %s`", Bot.getJDA().getPresence().getActivity().getType(),
            Bot.getJDA().getPresence().getActivity()).queue();
    }

    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.BOT_ADMIN;
    }

    @Override
    public String usage() {
        return "status <type id> <status...>";
    }

    @Override
    public String description() {
        return "Sets the bot's status";
    }

    @Override
    public void init() {
        CommandManager.register(new Status());
        CommandManager.register(new Echo());
    }
}
