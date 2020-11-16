package me.zoemartin.rubie.modules.funCommands;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.util.*;

@Command
@CommandOptions(
    name = "echo",
    description = "Send a message through the bot",
    usage = "<message...>",
    perm = CommandPerm.BOT_MANAGER
)
public class Echo extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);

        String echo = lastArg(0, event);

        if (event.getMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_MENTION_EVERYONE))
            event.getChannel().sendMessageFormat(echo).allowedMentions(EnumSet.allOf(Message.MentionType.class)).queue();
        else event.getChannel().sendMessageFormat(echo).queue();
        event.addCheckmark();
    }

    @SubCommand(Echo.class)
    @CommandOptions(
        name = "to",
        alias = ">>",
        description = "Sends a message to another channel through the bot",
        perm = CommandPerm.BOT_MANAGER
    )
    @Checks.Permissions.Guild(Permission.MESSAGE_MANAGE)
    @Checks.Permissions.Channel({Permission.MESSAGE_WRITE, Permission.MESSAGE_READ})
    private static class To extends GuildCommand {
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
    }

    @SubCommand(Echo.class)
    @CommandOptions(
        name = "edit",
        description = "Edits a message previously sent through the bot",
        usage = "<message id> [channel] <message...>",
        perm = CommandPerm.BOT_ADMIN
    )
    @Checks.Permissions.Channel(Permission.MESSAGE_MANAGE)
    private static class Edit extends GuildCommand {
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
            c = tc == null ? event.getTextChannel() : tc;

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
    }
}
