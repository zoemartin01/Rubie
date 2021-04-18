package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.LinkedList;
import java.util.List;

@Disabled
@Command
@CommandOptions(
    name = "output",
    description = "Run a command and redirect the output",
    usage = "<channel> <command...>",
    perm = CommandPerm.OWNER
)
public class Output extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        List<String> args = event.getArgs();
        Check.check(!args.isEmpty(), CommandArgumentException::new);
        TextChannel outputChannel = Parser.Channel.getTextChannel(event.getGuild(), args.get(0));
        Check.entityReferenceNotNull(outputChannel, TextChannel.class, args.get(0));

        LinkedList<AbstractCommand> commands = new LinkedList<>();
        args.subList(1, args.size()).forEach(s -> {
            if (commands.isEmpty()) commands.add(CommandManager.getCommands().stream()
                                                     .filter(c -> c.alias().contains(s.toLowerCase()))
                                                     .findFirst().orElse(null));
            else if (commands.getLast() != null) commands.getLast().subCommands().stream()
                                                     .filter(sc -> sc.alias().contains(s.toLowerCase()))
                                                     .findFirst().ifPresent(commands::add);

        });

        List<String> arguments = args.subList(1 + commands.size(), args.size());
        AbstractCommand command = commands.getLast();

        command.run(new GuildCommandEvent(event.getMember(), outputChannel, event.getContent(), event.getJDA(),
            arguments, event.getInvoked(), event.getArgString()));
        event.addCheckmark();
    }
}
