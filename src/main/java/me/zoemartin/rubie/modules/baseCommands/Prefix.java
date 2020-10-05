package me.zoemartin.rubie.modules.baseCommands;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.exceptions.ReplyError;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.commandProcessing.Prefixes;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Prefix implements GuildCommand {
    @Override
    public Set<Command> subCommands() {
        return Set.of(new list(), new Remove(), new Add());
    }

    @Override
    public String name() {
        return "prefix";
    }

    @Override
    public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
        Help.commandHelp(user, channel, args, original, name());
    }

    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.BOT_ADMIN;
    }

    @Override
    public String usage() {
        return "prefix";
    }

    @Override
    public String description() {
        return "Bot Prefix Management";
    }

    private static class Add implements GuildCommand {
        @Override
        public String name() {
            return "add";
        }

        @Override
        public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
            Check.check(!args.isEmpty(), CommandArgumentException::new);

            String prefix = lastArg(0, args, original);
            Prefixes.addPrefix(original.getGuild().getId(), prefix);
            embedReply(original, channel, null, "Added `%s` as a prefix", prefix).queue();
        }

        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public String usage() {
            return "prefix add <prefix>";
        }

        @Override
        public String description() {
            return "Adds a Bot Prefix";
        }
    }

    private static class Remove implements GuildCommand {
        @Override
        public String name() {
            return "remove";
        }

        @Override
        public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
            Check.check(args.size() == 1, CommandArgumentException::new);

            String prefix = args.get(0);
            Check.check(Prefixes.removePrefix(original.getGuild().getId(), prefix),
                () -> new ReplyError("Error, `%s` was not a bot prefix", prefix));
            embedReply(original, channel, null, "Removed `%s` as a prefix", prefix).queue();
        }

        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public String usage() {
            return "prefix remove prefix";
        }

        @Override
        public String description() {
            return "Removes a Bot Prefix";
        }
    }

    private static class list implements GuildCommand {
        @Override
        public String name() {
            return "list";
        }

        @Override
        public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
            Check.check(args.isEmpty(), CommandArgumentException::new);

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(original.getGuild().getSelfMember().getColor());
            eb.setTitle("Bot Prefixes");

            String prefixes = Prefixes.getPrefixes(original.getGuild().getId()).stream()
                                  .map(s -> s.matches("<@!?\\d{17,19}>\\s*") ? s : "`" + s + "`")
                                  .collect(Collectors.joining(", "));

            eb.setDescription(prefixes);
            channel.sendMessage(eb.build()).queue();
        }

        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public String usage() {
            return "prefix list";
        }

        @Override
        public String description() {
            return "Lists the bot prefixes";
        }
    }
}
