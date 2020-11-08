package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ConsoleError;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.Check;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Usage implements GuildCommand {
    @Override
    public @NotNull String name() {
        return "usage";
    }

    @Override
    public void run(GuildCommandEvent event) {
        Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);

        LinkedList<Command> commands = new LinkedList<>();
        event.getArgs().forEach(s -> {
            if (commands.isEmpty()) commands.add(CommandManager.getCommands().stream()
                                                     .filter(c -> s.matches(c.regex().toLowerCase()))
                                                     .findFirst().orElseThrow(
                    () -> new ConsoleError("Command '%s' not found", event.getInvoked().getLast())));
            else commands.getLast().subCommands().stream()
                     .filter(sc -> s.matches(sc.regex().toLowerCase()))
                     .findFirst().ifPresent(commands::add);

        });

        String name = commands.stream().map(Command::name).collect(Collectors.joining(" "));
        EmbedBuilder eb = new EmbedBuilder()
                              .setTitle("`" + name.toUpperCase() + "` usage")
                              .setDescription(Stream.concat(
                                  Stream.of(commands.getLast()), commands.getLast().subCommands().stream())
                                                  .map(c -> {
                                                      if (commands.getLast().equals(c))
                                                          return c.name().equals(c.usage()) ?
                                                              String.format("`%s`", name) : String.format("`%s %s`", name, c.usage());
                                                      if (c.usage().equals(c.name()))
                                                          return String.format("`%s %s`", name, c.usage());
                                                      return String.format("`%s %s %s`", name, c.name(), c.usage());
                                                  })
                                                  .collect(Collectors.joining(" or\n")))
                              .setColor(0xdf136c);

        event.getChannel().sendMessage(eb.build()).queue();
    }

    @Override
    public @NotNull CommandPerm commandPerm() {
        return CommandPerm.EVERYONE;
    }

    @Override
    public @NotNull String usage() {
        return "<command>";
    }

    @Override
    public @NotNull String description() {
        return "Shows a commands usage page";
    }
}
