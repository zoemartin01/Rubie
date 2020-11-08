package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.modules.commandProcessing.Prefixes;
import me.zoemartin.rubie.core.util.Check;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class Prefix implements GuildCommand {
    @Override
    public @NotNull Set<Command> subCommands() {
        return Set.of(new list(), new Remove(), new Add());
    }

    @Override
    public @NotNull String name() {
        return "prefix";
    }

    @Override
    public void run(GuildCommandEvent event) {
        throw new CommandArgumentException();
    }

    @Override
    public @NotNull CommandPerm commandPerm() {
        return CommandPerm.BOT_ADMIN;
    }

    @Override
    public @NotNull String description() {
        return "Bot Prefix Management";
    }

    private static class Add implements GuildCommand {
        @Override
        public @NotNull String name() {
            return "add";
        }

        @Override
        public void run(GuildCommandEvent event) {
            Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);

            String prefix = lastArg(0, event);
            Prefixes.addPrefix(event.getGuild().getId(), prefix);
            embedReply(event, (String) null, "Added `%s` as a prefix", prefix).queue();
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public @NotNull String usage() {
            return "<prefix>";
        }

        @Override
        public @NotNull String description() {
            return "Adds a Bot Prefix";
        }
    }

    private static class Remove implements GuildCommand {
        @Override
        public @NotNull String name() {
            return "remove";
        }

        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().size() == 1, CommandArgumentException::new);

            String prefix = event.getArgs().get(0);
            Check.check(Prefixes.removePrefix(event.getGuild().getId(), prefix),
                () -> new ReplyError("Error, `%s` was not a bot prefix", prefix));
            embedReply(event, (String) null, "Removed `%s` as a prefix", prefix).queue();
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public @NotNull String usage() {
            return "<prefix>";
        }

        @Override
        public @NotNull String description() {
            return "Removes a Bot Prefix";
        }
    }

    private static class list implements GuildCommand {
        @Override
        public @NotNull String name() {
            return "list";
        }

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

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public @NotNull String description() {
            return "Lists the bot prefixes";
        }
    }
}
