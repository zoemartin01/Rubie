package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.modules.commandProcessing.Prefixes;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "prefix",
    description = "Bot Prefix Management",
    perm = CommandPerm.BOT_ADMIN
)
public class Prefix extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        throw new CommandArgumentException();
    }

    @SubCommand(Prefix.class)
    @CommandOptions(
        name = "add",
        description = "Adds a Bot Prefix",
        perm = CommandPerm.BOT_ADMIN,
        usage = "<prefix>"
    )
    @SubCommand.AsBase(name = "addprefix")
    private static class Add extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);

            String prefix = lastArg(0, event);
            Prefixes.addPrefix(event.getGuild().getId(), prefix);
            event.reply(null, "Added `%s` as a prefix", prefix).queue();
        }
    }

    @SubCommand(Prefix.class)
    @CommandOptions(
        name = "remove",
        description = "Removes a Bot Prefix",
        perm = CommandPerm.BOT_ADMIN,
        usage = "<prefix>"
    )
    @SubCommand.AsBase(name = "removeprefix", alias = {"delprefix", "rmprefix", "deleteprefix"})
    private static class Remove extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().size() == 1, CommandArgumentException::new);

            String prefix = event.getArgs().get(0);
            Check.check(Prefixes.removePrefix(event.getGuild().getId(), prefix),
                () -> new ReplyError("Error, `%s` was not a bot prefix", prefix));
            event.reply(null, "Removed `%s` as a prefix", prefix).queue();
        }
    }

    @SubCommand(Prefix.class)
    @CommandOptions(
        name = "list",
        description = "Removes a Bot Prefix",
        perm = CommandPerm.BOT_ADMIN,
        usage = "<prefix>"
    )
    @SubCommand.AsBase(name = "listprefix", alias = "listprefixes")
    private static class list extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().isEmpty(), CommandArgumentException::new);

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(event.getGuild().getSelfMember().getColor());
            eb.setTitle("Bot Prefixes");

            String prefixes = Prefixes.getPrefixes(event.getGuild().getId()).stream()
                                  .map(s -> s.matches("<@!?\\d{17,19}>\\s*") ? s : "`" + s + "`")
                                  .collect(Collectors.joining(", "));

            eb.setDescription(prefixes);
            event.getChannel().sendMessage(eb.build()).queue();
        }
    }
}
