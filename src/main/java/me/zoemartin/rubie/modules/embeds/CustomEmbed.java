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
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CustomEmbed implements GuildCommand {
    @Override
    public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
        Check.check(!args.isEmpty(), CommandArgumentException::new);

        TextChannel c;
        String json;
        if (args.size() > 1) {
            String cRef = args.get(0);
            c = Parser.Channel.getTextChannel(original.getGuild(), cRef);
            Check.entityReferenceNotNull(c, TextChannel.class, cRef);

            if (args.get(1).matches(
                "(http(s)?://.)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&//=]*)")) {

                json = EmbedUtil.jsonFromUrl(args.get(1));
            } else {
                json = original.getContentRaw()
                           .substring(original.getContentRaw().indexOf(cRef) + cRef.length() + 1);
            }
        } else {
            c = channel;
            if (args.get(0).matches(
                "(http(s)?://.)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&//=]*)")) {

                json = EmbedUtil.jsonFromUrl(args.get(0));
            } else {
                json = original.getContentRaw()
                           .substring(original.getContentRaw().indexOf(invoked) + invoked.length() + 1);
            }
        }

        Check.check(user.hasPermission(c, Permission.MESSAGE_WRITE),
            () -> new ConsoleError("Member '%s' doesn't have write permissions in channel '%s'",
                user.getId(), c.getId()));

        Embed e;
        try {
            e = Embed.fromJson(json);
        } catch (JsonSyntaxException ignored) {
            throw new ReplyError("Sorry, I cannot parse your input json!");
        }

        channel.sendMessage(e.toDiscordEmbed()).queue();
        addCheckmark(original);
    }

    @NotNull
    @Override
    public String regex() {
        return "customembed|cembed";
    }

    @NotNull
    @Override
    public String name() {
        return "customembed";
    }

    @NotNull
    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.BOT_MANAGER;
    }

    @NotNull
    @Override
    public String usage() {
        return "[channel] <json|url>";
    }

    @NotNull
    @Override
    public String description() {
        return "Creates a custom embed";
    }
}
