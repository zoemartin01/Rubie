package me.zoemartin.rubie.modules.funCommands;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.*;
import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.*;

public class Echo implements GuildCommand {
    @Override
    public Set<Command> subCommands() {
        return Set.of(new To(), new Edit());
    }

    @Override
    public String name() {
        return "echo";
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
        Check.check(!args.isEmpty(), CommandArgumentException::new);

        String echo = lastArg(0, args, original);

        channel.sendMessageFormat(MessageUtils.cleanMessage(original.getMember(), echo)).queue();
        original.addReaction("U+2705").queue();
    }

    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.BOT_MANAGER;
    }

    @Override
    public String usage() {
        return "echo <message...>";
    }

    @Override
    public String description() {
        return "Makes the bot say stuff";
    }

    @SuppressWarnings("ConstantConditions")
    private static class To implements GuildCommand {
        @Override
        public String name() {
            return "to";
        }

        @Override
        public String regex() {
            return ">>|to";
        }

        @Override
        public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
            Check.check(args.size() > 1 && Parser.Channel.isParsable(args.get(0)),
                CommandArgumentException::new);

            TextChannel c = original.getGuild().getTextChannelById(Parser.Channel.parse(args.get(0)));
            Check.entityNotNull(c, TextChannel.class);
            Check.check(original.getMember().hasPermission(c, Permission.MESSAGE_WRITE),
                () -> new ConsoleError("Member '%s' doesn't have write permissions in channel '%s'",
                    original.getMember().getId(), c.getId()));

            StringBuilder sb = new StringBuilder();
            Check.notNull(c, () -> new ReplyError("Channel '%s' does not exist", args.get(1)));

            args.subList(1, args.size()).forEach(s -> sb.append(s).append(" "));

            c.sendMessageFormat(MessageUtils.cleanMessage(original.getMember(), sb.toString())).queue();
            original.addReaction("U+2705").queue();
        }

        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_MANAGER;
        }

        @Override
        public Collection<Permission> required() {
            return Set.of(Permission.MESSAGE_MANAGE);
        }


        @Override
        public String usage() {
            return "echo >> #channel <message...>";
        }

        @Override
        public String description() {
            return "Makes the bot say stuff in a different channel";
        }
    }

    private static class Edit implements GuildCommand {
        @Override
        public String name() {
            return "--edit";
        }

        @Override
        public String regex() {
            return "-e|--edit";
        }

        @Override
        public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
            Check.check(args.size() > 1 && Parser.Message.isParsable(args.get(0)), CommandArgumentException::new);

            String message, channelId;
            channelId = Parser.Message.parse(args.get(0)).getLeft();
            message = Parser.Message.parse(args.get(0)).getRight();

            TextChannel c = original.getGuild().getTextChannelById(channelId);
            Check.entityNotNull(c, TextChannel.class);
            Message m = c.retrieveMessageById(message).complete();
            Check.entityNotNull(m, Message.class);

            Check.check(m.getAuthor().getId().equals(Bot.getJDA().getSelfUser().getId()), UnexpectedError::new);

            StringBuilder sb = new StringBuilder();
            args.subList(1, args.size()).forEach(s -> sb.append(s).append(" "));

            m.editMessage(sb.toString()).queue();
            original.addReaction("U+2705").queue();
        }

        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public String usage() {
            return "echo -e <message_link> <message...>";
        }

        @Override
        public String description() {
            return "Edits an echoed message";
        }
    }
}
