package me.zoemartin.rubie.modules.funCommands;

import com.google.auto.service.AutoService;
import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandEvent;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.annotations.Command;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.entities.Activity;

@AutoService(Module.class)
@Command
@CommandOptions(
    name = "status",
    description = "Sets the Bot's status",
    usage = "<type id> <status...>",
    perm = CommandPerm.OWNER
)
public class Status extends AbstractCommand implements Module {
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
    public void init() {
    }
}
