package me.zoemartin.rubie.modules.embeds;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.google.gson.JsonSyntaxException;
import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.Check;
import me.zoemartin.rubie.core.util.Parser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class CustomEmbed implements GuildCommand {
    @NotNull
    @Override
    public Set<Command> subCommands() {
        return Set.of(new Bulk());
    }

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
                c = event.getChannel();
                if (args.get(0).matches(
                    "(http(s)?://.)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&/=]*)")) {

                    json = EmbedUtil.jsonFromUrl(args.get(0));
                } else {
                    json = event.getContent()
                               .substring(event.getContent().indexOf(event.getInvoked().getLast()) + event.getInvoked().getLast().length() + 1);
                }
            } else {
                c = tc;
                if (args.get(1).matches(
                    "(http(s)?://.)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&/=]*)")) {

                    json = EmbedUtil.jsonFromUrl(args.get(1));
                } else {
                    json = event.getContent()
                               .substring(event.getContent().indexOf(cRef) + cRef.length() + 1);
                }
            }
        } else {
            c = event.getChannel();
            if (args.get(0).matches(
                "(http(s)?://.)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&/=]*)")) {

                json = EmbedUtil.jsonFromUrl(args.get(0));
            } else {
                json = event.getContent()
                           .substring(event.getContent().indexOf(event.getInvoked().getLast()) + event.getInvoked().getLast().length() + 1);
            }
        }

        Check.check(event.getMember().hasPermission(c, Permission.MESSAGE_WRITE,
            Permission.MESSAGE_READ, Permission.MESSAGE_EMBED_LINKS),
            () -> new ReplyError("Error, looks like you don't have all the necessary permissions to post embeds in %s",
                c.getAsMention()));

        Check.check(event.getGuild().getSelfMember().hasPermission(c, Permission.MESSAGE_WRITE,
            Permission.MESSAGE_READ, Permission.MESSAGE_EMBED_LINKS),
            () -> new ReplyError("Error, looks like I don't have all the necessary permissions to post embeds in %s",
                c.getAsMention()));

        Embed e;
        try {
            e = Embed.fromJson(json);
        } catch (JsonSyntaxException ignored) {
            throw new ReplyError("Sorry, I cannot parse your input json!");
        }

        c.sendMessage(e.toDiscordEmbed()).queue();
        event.addCheckmark();
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

    private static class Bulk implements GuildCommand {

        @Override
        public void run(GuildCommandEvent event) {
            List<String> args = event.getArgs();
            Check.check(args.size() > 1, CommandArgumentException::new);
            TextChannel c = Parser.Channel.getTextChannel(event.getGuild(), args.get(0));
            Check.entityReferenceNotNull(c, TextChannel.class, args.get(0));

            Check.check(event.getMember().hasPermission(c, Permission.MESSAGE_WRITE,
                Permission.MESSAGE_READ, Permission.MESSAGE_EMBED_LINKS),
                () -> new ReplyError("Error, looks like you don't have all the necessary permissions to post embeds in %s",
                    c.getAsMention()));

            Check.check(event.getGuild().getSelfMember().hasPermission(c, Permission.MESSAGE_WRITE,
                Permission.MESSAGE_READ, Permission.MESSAGE_EMBED_LINKS),
                () -> new ReplyError("Error, looks like I don't have all the necessary permissions to post embeds in %s",
                    c.getAsMention()));


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

        @NotNull
        @Override
        public String name() {
            return "bulk";
        }

        @NotNull
        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @NotNull
        @Override
        public Collection<Permission> required() {
            return EnumSet.of(Permission.MANAGE_CHANNEL);
        }

        @NotNull
        @Override
        public String usage() {
            return "<channel> <urls...>";
        }

        @NotNull
        @Override
        public String description() {
            return "Send Many custom Embeds at once";
        }
    }
}
