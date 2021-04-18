package me.zoemartin.rubie.modules.embeds;

import com.google.gson.JsonSyntaxException;
import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "customembed",
    description = "Creates a custom embed",
    usage = "[channel] <json|url>",
    perm = CommandPerm.BOT_MANAGER,
    alias = "cembed",
    botPerms = {Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS}
)
@Checks.Permissions.Channel({Permission.MESSAGE_WRITE, Permission.MESSAGE_READ, Permission.MESSAGE_EMBED_LINKS})
public class CustomEmbed extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        List<String> args = event.getArgs();
        Check.check(!args.isEmpty(), CommandArgumentException::new);

        TextChannel c;
        String json;
        if (args.size() > 1) {
            String cRef = args.get(0);
            TextChannel tc = Parser.Channel.getTextChannel(event.getGuild(), cRef);

            if (tc == null) {
                c = event.getTextChannel();
                if (args.get(0).matches(
                    "(http(s)?://.)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&/=]*)")) {

                    json = EmbedUtil.jsonFromUrl(args.get(0));
                } else {
                    json = lastArg(0, event);
                }
            } else {
                c = tc;
                if (args.get(1).matches(
                    "(http(s)?://.)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&/=]*)")) {

                    json = EmbedUtil.jsonFromUrl(args.get(1));
                } else {
                    json = lastArg(1, event);
                }
            }
        } else {
            c = event.getTextChannel();
            if (args.get(0).matches(
                "(http(s)?://.)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&/=]*)")) {

                json = EmbedUtil.jsonFromUrl(args.get(0));
            } else {
                json = lastArg(0, event);
            }
        }

        if (c != event.getTextChannel()) {
            Check.check(this.checkChannelPerms(event, c),
                () -> new CommandPermissionException(
                    "Error, you are missing the necessary permissions in this channel for this command!"));
            Check.check(this.checkNecessaryPerms(event, c),
                () -> new CommandPermissionException(
                    "Error, I seem to be missing the necessary permissions to run this command!"));
        }

        Embed e;
        try {
            e = Embed.fromJson(json);
        } catch (JsonSyntaxException ignored) {
            throw new ReplyError("Sorry, I cannot parse your input json!");
        }

        c.sendMessage(e.toDiscordEmbed()).queue();
        event.addCheckmark();
    }

    @SubCommand(CustomEmbed.class)
    @CommandOptions(
        name = "bulk",
        description = "Send multiple custom embeds at once",
        usage = "<channel> <urls...>",
        perm = CommandPerm.BOT_ADMIN,
        botPerms = {Permission.MESSAGE_WRITE, Permission.MESSAGE_READ, Permission.MESSAGE_EMBED_LINKS}
    )
    @Checks.Permissions.Guild(Permission.MESSAGE_MANAGE)
    @Checks.Permissions.Channel({Permission.MESSAGE_WRITE, Permission.MESSAGE_READ,
        Permission.MANAGE_CHANNEL, Permission.MESSAGE_EMBED_LINKS})
    private static class Bulk extends GuildCommand {

        @Override
        public void run(GuildCommandEvent event) {
            List<String> args = event.getArgs();
            Check.check(args.size() > 1, CommandArgumentException::new);
            TextChannel c = Parser.Channel.getTextChannel(event.getGuild(), args.get(0));
            Check.entityReferenceNotNull(c, TextChannel.class, args.get(0));

            Check.check(this.checkChannelPerms(event, c),
                () -> new CommandPermissionException(
                    "Error, you are missing the necessary permissions in this channel for this command!"));
            Check.check(this.checkNecessaryPerms(event, c),
                () -> new CommandPermissionException(
                    "Error, I seem to be missing the necessary permissions to run this command!"));


            List<String> jsons = args.subList(1, args.size())
                                     .stream()
                                     .map(EmbedUtil::jsonFromUrl)
                                     .collect(Collectors.toList());

            List<Embed> embeds;

            try {
                embeds = jsons.stream().map(Embed::fromJson).collect(Collectors.toList());
            } catch (JsonSyntaxException e) {
                throw new ReplyError("An error occurred while parsing a json file!");
            }

            embeds.forEach(embed -> c.sendMessage(embed.toDiscordEmbed()).queue());
            event.addCheckmark();
        }
    }
}
