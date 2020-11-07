package me.zoemartin.rubie.modules.embeds;

import com.google.gson.JsonSyntaxException;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.Embed;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EditEmbed implements GuildCommand {
    @Override
    public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
        Check.check(!args.isEmpty(), CommandArgumentException::new);

        String mRef = args.get(0);
        Check.check(mRef.matches("\\d{17,19}"), () -> new ReplyError("Error, `%s` is not a valid id", mRef));
        TextChannel c;

        String json;

        if (args.size() > 2) {
            String cRef = args.get(1);
            c = Parser.Channel.getTextChannel(original.getGuild(), cRef);
            Check.entityReferenceNotNull(c, TextChannel.class, cRef);

            if (args.get(2).matches(
                "(http(s)?://.)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&//=]*)")) {

                json = EmbedUtil.jsonFromUrl(args.get(2));
            } else {
                json = original.getContentRaw()
                           .substring(original.getContentRaw().indexOf(cRef) + cRef.length() + 1);
            }
        } else {
            c = channel;
            if (args.get(1).matches(
                "(http(s)?://.)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&//=]*)")) {

                json = EmbedUtil.jsonFromUrl(args.get(1));
            } else {
                json = original.getContentRaw()
                           .substring(original.getContentRaw().indexOf(mRef) + mRef.length() + 1);
            }
        }

        Check.check(user.hasPermission(c, Permission.MESSAGE_MANAGE),
            () -> new ConsoleError("Member '%s' doesn't have edit permissions in channel '%s'",
                user.getId(), c.getId()));

        Message message;
        try {
            message = c.retrieveMessageById(mRef).complete();
        } catch (ErrorResponseException e) {
            if (e.getErrorResponse().getCode() == 10008)
                throw new EntityNotFoundException("Error, message `%s` not found in %s!", mRef, c.getAsMention());
            else
                throw e;
        }

        Embed e;
        try {
            e = Embed.fromJson(json);
        } catch (JsonSyntaxException ignored) {
            throw new ReplyError("Sorry, I cannot parse your input json!");
        }

        message.editMessage(e.toDiscordEmbed()).queue();
        addCheckmark(original);
    }

    @NotNull
    @Override
    public String name() {
        return "editembed";
    }

    @NotNull
    @Override
    public String regex() {
        return "editembed|ee";
    }

    @NotNull
    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.BOT_MANAGER;
    }

    @NotNull
    @Override
    public String usage() {
        return "<message id> [channel] <json|link>";
    }

    @NotNull
    @Override
    public String description() {
        return "Edit a Custom Embed sent by me";
    }
}
