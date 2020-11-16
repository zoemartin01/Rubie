package me.zoemartin.rubie.modules.levels;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SubCommand(Config.class)
@CommandOptions(
    name = "blacklist",
    description = "Blacklist Config",
    perm = CommandPerm.BOT_ADMIN,
    alias = "bl"
)
class BlackList extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        throw new CommandArgumentException();
    }

    @SubCommand(BlackList.class)
    @CommandOptions(
        name = "list",
        description = "List all blacklistings",
        perm = CommandPerm.BOT_ADMIN,
        alias = "l"
    )
    private static class list extends GuildCommand {
        @SuppressWarnings("ConstantConditions")
        @Override
        public void run(GuildCommandEvent event) {
            Guild g = event.getGuild();
            LevelConfig config = Levels.getConfig(g);

            Collection<String> roles = config.getBlacklistedRoles();
            Collection<String> channels = config.getBlacklistedChannels();

            if (roles.isEmpty() && channels.isEmpty()) return;

            PagedEmbed p = new PagedEmbed(EmbedUtil.pagedDescription(
                new EmbedBuilder().setTitle("Blacklistings").build(),
                Stream.concat(roles.stream().filter(s -> !s.isEmpty()).map(s -> g.getRoleById(s) == null ? s :
                                                                                    g.getRoleById(s).getAsMention()),
                    channels.stream().filter(s -> !s.isEmpty()).map(s -> g.getTextChannelById(s) == null ? s :
                                                                             g.getTextChannelById(s).getAsMention()))
                    .map(s -> String.format("%s\n", s)).collect(Collectors.toList())), event);

            PageListener.add(p);
        }
    }

    @SubCommand(BlackList.class)
    @CommandOptions(
        name = "channel",
        description = "Blacklist a channel",
        usage = "<channel...>",
        perm = CommandPerm.BOT_ADMIN,
        alias = "c"
    )
    private static class Channel extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);
            Guild g = event.getGuild();
            List<TextChannel> channels = event.getArgs().stream().map(s -> Parser.Channel.getTextChannel(g, s))
                                             .collect(Collectors.toList());

            LevelConfig config = Levels.getConfig(event.getGuild());
            channels.forEach(c -> config.addBlacklistedChannel(c.getId()));
            DatabaseUtil.updateObject(config);
            event.addCheckmark();

            PagedEmbed p = new PagedEmbed(EmbedUtil.pagedDescription(
                new EmbedBuilder().setTitle("Blacklisted the following channels: " + channels.size()).build(),
                channels.stream().map(c -> String.format("%s\n", c.getAsMention())).collect(Collectors.toList())),
                event);

            PageListener.add(p);
        }

        @SubCommand(Channel.class)
        @CommandOptions(
            name = "remove",
            description = "Removes a blacklisted channel",
            usage = "<channel>",
            perm = CommandPerm.BOT_ADMIN,
            alias = "rm"
        )
        private static class Remove extends GuildCommand {
            @Override
            public void run(GuildCommandEvent event) {
                Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);
                String cRef = lastArg(0, event);
                TextChannel c = Parser.Channel.getTextChannel(event.getGuild(), cRef);

                Check.entityReferenceNotNull(c, TextChannel.class, cRef);
                LevelConfig config = Levels.getConfig(event.getGuild());
                if (!config.removeBlacklistedChannel(c.getId())) return;
                DatabaseUtil.updateObject(config);
                event.addCheckmark();
                embedReply(event, "Level Blacklist", "Unblacklisted %s",
                    c.getAsMention()).queue();
            }
        }
    }

    @SubCommand(BlackList.class)
    @CommandOptions(
        name = "role",
        description = "Blacklist a role",
        usage = "<role>",
        perm = CommandPerm.BOT_ADMIN,
        alias = "r"
    )
    private static class role extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);
            String rRef = lastArg(0, event);
            Role r = Parser.Role.getRole(event.getGuild(), rRef);

            Check.entityReferenceNotNull(r, Role.class, rRef);
            LevelConfig config = Levels.getConfig(event.getGuild());
            config.addBlacklistedRole(r.getId());
            DatabaseUtil.updateObject(config);
            event.addCheckmark();
            embedReply(event, "Level Blacklist", "Blacklisted %s",
                r.getAsMention()).queue();
        }

        @SubCommand(role.class)
        @CommandOptions(
            name = "remove",
            description = "Removes a blacklisted role",
            usage = "<role>",
            perm = CommandPerm.BOT_ADMIN,
            alias = "rm"
        )
        private static class Remove extends GuildCommand {
            @Override
            public void run(GuildCommandEvent event) {
                Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);
                String rRef = lastArg(0, event);
                Role r = Parser.Role.getRole(event.getGuild(), rRef);

                Check.entityReferenceNotNull(r, Role.class, rRef);
                LevelConfig config = Levels.getConfig(event.getGuild());
                if (!config.removeBlacklistedRole(r.getId())) return;
                DatabaseUtil.updateObject(config);
                event.addCheckmark();
                embedReply(event, "Level Blacklist", "Unblacklisted %s",
                    r.getAsMention()).queue();
            }
        }
    }
}
