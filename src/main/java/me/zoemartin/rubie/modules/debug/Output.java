package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Output implements GuildCommand {
    @Override
    public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
        Check.check(!args.isEmpty(), CommandArgumentException::new);
        TextChannel outputChannel = Parser.Channel.getTextChannel(original.getGuild(), args.get(0));
        Check.entityReferenceNotNull(outputChannel, TextChannel.class, args.get(0));

        LinkedList<Command> commands = new LinkedList<>();
        args.subList(1, args.size()).forEach(s -> {
            if (commands.isEmpty()) commands.add(CommandManager.getCommands().stream()
                                                     .filter(c -> s.matches(c.regex().toLowerCase()))
                                                     .findFirst().orElse(null));
            else if (commands.getLast() != null) commands.getLast().subCommands().stream()
                                                     .filter(sc -> s.matches(sc.regex().toLowerCase()))
                                                     .findFirst().ifPresent(commands::add);

        });

        List<String> arguments = args.subList(1 + commands.size(), args.size());
        Command command = commands.getLast();

        if (command instanceof GuildCommand) {
            System.out.println(1);
            ((GuildCommand) command).run(user, outputChannel, arguments, original, args.get(commands.size()));
        } else {
            System.out.println(2);
            commands.getLast().run(user.getUser(), outputChannel, arguments, original, args.get(commands.size()));
        }
        addCheckmark(original);
    }

    @NotNull
    @Override
    public String name() {
        return "output";
    }

    @NotNull
    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.OWNER;
    }

    @NotNull
    @Override
    public String usage() {
        return "<channel> <command...>";
    }

    @NotNull
    @Override
    public String description() {
        return "Run a command and redirect the output";
    }
}
