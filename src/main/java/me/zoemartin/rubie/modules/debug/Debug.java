package me.zoemartin.rubie.modules.debug;

import me.zoemartin.rubie.core.LoadModule;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ConsoleError;
import me.zoemartin.rubie.core.interfaces.*;
import me.zoemartin.rubie.core.interfaces.Module;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.Check;
import net.dv8tion.jda.api.entities.*;

import java.util.*;

@LoadModule
public class Debug implements Module, GuildCommand {
    @Override
    public Set<Command> subCommands() {
        return Set.of(new Full());
    }

    @Override
    public void init() {
        CommandManager.register(new Shutdown());
        CommandManager.register(new Debug());
        CommandManager.register(new Dump());
        CommandManager.register(new ReadError());
    }

    @Override
    public String name() {
        return "debug";
    }

    @Override
    public String regex() {
        return "debug|dc";
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
        List<Object> list = help(args);
        Command command = (Command) list.get(0);

        try {
            command.run(user, channel, (List<String>) list.get(1), original, (String) list.get(2));
        } catch (Exception e) {
            channel.sendMessageFormat("Error while running command `%s`: `%s`", command.name(), e.getMessage()).queue();
        }
    }

    private static List<Object> help(List<String> args) {
        Check.check(!args.isEmpty(), CommandArgumentException::new);

        String commandString = args.get(0);
        String subCommandString = args.size() > 1 ? args.get(1) : null;

        Command command = CommandManager.getCommands().stream()
                              .filter(c -> commandString.matches(c.regex().toLowerCase()))
                              .findFirst().orElseThrow(() -> new ConsoleError("Command '%s' not found", commandString));

        boolean isSubCommand = false;

        if (!command.subCommands().isEmpty() && subCommandString != null) {
            Command subCommand = command.subCommands().stream()
                                     .filter(sc -> subCommandString.matches(sc.regex().toLowerCase()))
                                     .findFirst().orElse(null);

            command = subCommand == null ? command : subCommand;
            isSubCommand = subCommand != null;
        }

        List<String> arguments = isSubCommand ? Collections.unmodifiableList(args.subList(2, args.size())) :
                                     Collections.unmodifiableList(args.subList(1, args.size()));

        return List.of(command, arguments, isSubCommand ? subCommandString : commandString);
    }

    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.BOT_MANAGER;
    }

    @Override
    public String usage() {
        return "debug <command...>";
    }

    @Override
    public String description() {
        return "Debugs a command";
    }

    private static class Full implements GuildCommand {
        @Override
        public String name() {
            return "-f";
        }

        @Override
        public String regex() {
            return "-f|--full";
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
            List<Object> list = Debug.help(args);
            Command command = (Command) list.get(0);

            try {
                command.run(user, channel, (List<String>) list.get(1), original, (String) list.get(2));
            } catch (Exception e) {
                StringBuilder error = new StringBuilder("Exception in thread \"" + Thread.currentThread().getName()
                                                            + "\" " + e.getClass().getCanonicalName()
                                                            + ": " + e.getMessage() + "\n");

                for (StackTraceElement s : e.getStackTrace()) {
                    error.append("\tat ").append(s).append("\n");
                }

                channel.sendMessageFormat("Error while running command `%s`:```\n%s\n```",
                    command.name(), error.toString()).queue();
            }
        }

        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public String usage() {
            return "debug -f <command...>";
        }

        @Override
        public String description() {
            return "Debugs a command with the full error output";
        }
    }
}
