package me.zoemartin.rubie.modules.embeds;

import com.google.gson.JsonSyntaxException;
import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.util.List;

@Command
@CommandOptions(
    name = "editembed",
    description = "Edit a Custom Embed sent by me",
    usage = "<message id> [channel] <json|link>",
    perm = CommandPerm.BOT_MANAGER,
    alias = {"ecembed", "ee"}
)
@Checks.Permissions.Channel(Permission.MESSAGE_MANAGE)
public class EditEmbed extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        List<String> args = event.getArgs();
        Check.check(!args.isEmpty(), CommandArgumentException::new);

        String mRef = args.get(0);
        Check.check(mRef.matches("\\d{17,19}"), () -> new ReplyError("Error, `%s` is not a valid id", mRef));
        TextChannel c;

        String json;

        if (args.size() > 2) {
            String cRef = args.get(1);
            c = Parser.Channel.getTextChannel(event.getGuild(), cRef);
            Check.entityReferenceNotNull(c, TextChannel.class, cRef);

            if (args.get(2).matches(
                "(http(s)?://.)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&/=]*)")) {

                json = EmbedUtil.jsonFromUrl(args.get(2));
            } else {
                json = event.getContent()
                           .substring(event.getContent().indexOf(cRef) + cRef.length() + 1);
            }
        } else {
            c = event.getTextChannel();
            if (args.get(1).matches(
                "(http(s)?://.)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&/=]*)")) {

                json = EmbedUtil.jsonFromUrl(args.get(1));
            } else {
                json = event.getContent()
                           .substring(event.getContent().indexOf(mRef) + mRef.length() + 1);
            }
        }

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

        Embed e;
        try {
            e = Embed.fromJson(json);
        } catch (JsonSyntaxException ignored) {
            throw new ReplyError("Sorry, I cannot parse your input json!");
        }

        message.editMessage(e.toDiscordEmbed()).queue();
        event.addCheckmark();
    }
}
