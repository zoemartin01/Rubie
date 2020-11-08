package me.zoemartin.rubie.modules.funCommands;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.embeds.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Echo implements GuildCommand {
    @Override
    public @NotNull Set<Command> subCommands() {
        return Set.of(new To(), new Edit());
    }

    @Override
    public @NotNull String name() {
        return "echo";
    }

    @Override
    public void run(GuildCommandEvent event) {
        Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);

        String echo = lastArg(0, event);

        if (event.getMember().hasPermission(event.getChannel(), Permission.MESSAGE_MENTION_EVERYONE))
            event.getChannel().sendMessageFormat(echo).allowedMentions(EnumSet.allOf(Message.MentionType.class)).queue();
        else event.getChannel().sendMessageFormat(echo).queue();
        event.addCheckmark();
    }

    @Override
    public @NotNull CommandPerm commandPerm() {
        return CommandPerm.BOT_MANAGER;
    }

    @Override
    public @NotNull String usage() {
        return "<message...>";
    }

    @Override
    public @NotNull String description() {
        return "Makes the bot say stuff";
    }

    private static class To implements GuildCommand {
        @Override
        public @NotNull String name() {
            return "to";
        }

        @Override
        public @NotNull String regex() {
            return ">>|to";
        }

        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().size() > 1, CommandArgumentException::new);

            TextChannel c = Parser.Channel.getTextChannel(event.getGuild(), event.getArgs().get(0));
            Check.entityReferenceNotNull(c, TextChannel.class, event.getArgs().get(0));

            Check.check(event.getMember().hasPermission(c, Permission.MESSAGE_WRITE,
                Permission.MESSAGE_READ),
                () -> new ReplyError("Error, looks like you don't have all the necessary permissions to post embeds in %s",
                    c.getAsMention()));

            Check.check(event.getGuild().getSelfMember().hasPermission(c, Permission.MESSAGE_WRITE,
                Permission.MESSAGE_READ),
                () -> new ReplyError("Error, looks like I don't have all the necessary permissions to post embeds in %s",
                    c.getAsMention()));

            String echo = lastArg(1, event);

            if (event.getMember().hasPermission(c, Permission.MESSAGE_MENTION_EVERYONE))
                c.sendMessageFormat(echo).allowedMentions(EnumSet.allOf(Message.MentionType.class)).queue();
            else c.sendMessageFormat(echo).queue();
            event.addCheckmark();
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_MANAGER;
        }

        @Override
        public @NotNull Collection<Permission> required() {
            return Set.of(Permission.MESSAGE_MANAGE);
        }


        @Override
        public @NotNull String usage() {
            return "<#channel> <message...>";
        }

        @Override
        public @NotNull String description() {
            return "Makes the bot say stuff in a different channel";
        }
    }

    private static class Edit implements GuildCommand {
        @Override
        public @NotNull String name() {
            return "edit";
        }

        @Override
        public @NotNull String regex() {
            return "edit";
        }

        @Override
        public void run(GuildCommandEvent event) {
            List<String> args = event.getArgs();
            Check.check(args.size() > 1, CommandArgumentException::new);

            TextChannel c;
            String mRef = args.get(0);
            TextChannel tc = null;

            if (args.size() > 2) {
                String cRef = args.get(1);
                tc = Parser.Channel.getTextChannel(event.getGuild(), cRef);
            }
            c = tc == null ? event.getChannel() : tc;

            Check.check(event.getMember().hasPermission(c, Permission.MESSAGE_MANAGE),
                () -> new ConsoleError("Member '%s' doesn't have edit permissions in channel '%s'",
                    event.getMember().getId(), c.getId()));

            Message message;
            try {
                message = c.retrieveMessageById(mRef).complete();
            } catch (ErrorResponseException e) {
                if (e.getErrorResponse().getCode() == 10008)
                    throw new EntityNotFoundException("Error, message `%s` not found in %s!", mRef, c.getAsMention());
                else
                    throw e;
            }

            String echo = lastArg(tc == null ? 1 : 2, event);

            Check.check(message.getAuthor().getId().equals(Bot.getJDA().getSelfUser().getId()),
                () -> new ReplyError("Mhhh looks like that message wasn't sent by me!"));

            message.editMessage(echo).queue();
            event.addCheckmark();
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public @NotNull String usage() {
            return "<message id> [channel] <message...>";
        }

        @Override
        public @NotNull String description() {
            return "Edits an echoed message";
        }
    }
}
