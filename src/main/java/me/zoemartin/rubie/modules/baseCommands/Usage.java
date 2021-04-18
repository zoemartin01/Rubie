package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ConsoleError;
import me.zoemartin.rubie.core.interfaces.AbstractCommand;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.managers.CommandManager;
import me.zoemartin.rubie.core.util.Check;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Disabled
@Command
@CommandOptions(
    name = "usage",
    description = "Shows usage information for a command",
    usage = "<command>"
)
public class Usage extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);

        LinkedList<AbstractCommand> commands = new LinkedList<>();
        event.getArgs().forEach(s -> {
            if (commands.isEmpty()) commands.add(CommandManager.getCommands().stream()
                                                     .filter(c -> c.alias().contains(s.toLowerCase()))
                                                     .findFirst().orElseThrow(
                    () -> new ConsoleError("Command '%s' not found", event.getInvoked().getLast())));
            else commands.getLast().subCommands().stream()
                     .filter(sc -> sc.alias().contains(s.toLowerCase()))
                     .findFirst().ifPresent(commands::add);

        });

        String name = commands.stream().map(AbstractCommand::name).collect(Collectors.joining(" "));
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
}
