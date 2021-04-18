package me.zoemartin.rubie.modules.levels;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.annotations.SubCommand;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.interfaces.JobProcessorInterface;
import me.zoemartin.rubie.core.managers.JobManager;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.zoemartin.rubie.core.Job.CommonKeys.GUILD;
import static me.zoemartin.rubie.core.Job.CommonKeys.USER;

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
            var users = config.getBlockedUsers();

            if (roles.isEmpty() && channels.isEmpty()) return;

            PagedEmbed p = new PagedEmbed(EmbedUtil.pagedDescription(
                new EmbedBuilder().setTitle("Blacklistings").build(),
                Stream.of(roles.stream().filter(s -> !s.isEmpty()).map(s -> g.getRoleById(s) == null ? s :
                                                                                g.getRoleById(s).getAsMention()),
                    channels.stream().filter(s -> !s.isEmpty()).map(s -> g.getTextChannelById(s) == null ? s :
                                                                             g.getTextChannelById(s).getAsMention()),
                    users.stream().filter(s -> !s.isEmpty()).map(s -> CacheUtils.getUser(s) == null ? s :
                                                                          CacheUtils.getUser(s).getAsMention()))
                    .reduce(Stream::concat).orElseGet(Stream::empty)
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
                event.reply("Level Blacklist", "Unblacklisted %s",
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
            event.reply("Level Blacklist", "Blacklisted %s",
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
                event.reply("Level Blacklist", "Unblacklisted %s",
                    r.getAsMention()).queue();
            }
        }
    }

    @SubCommand(BlackList.class)
    @CommandOptions(
        name = "user",
        description = "Block a user from gaining levels",
        usage = "<user> [time]",
        perm = CommandPerm.BOT_MANAGER,
        alias = "u"
    )
    @SubCommand.AsBase(name = "nolevels")
    private static class user extends GuildCommand {
        private static final JobProcessorInterface processor = new TempBlockLevels();

        private static final PeriodFormatter formatter = new PeriodFormatterBuilder()
                                                             .appendYears()
                                                             .appendSuffix("y")
                                                             .appendSeparator(" ", " ", new String[]{",", ", "})
                                                             .appendMonths()
                                                             .appendSuffix("mon")
                                                             .appendSeparator(" ", " ", new String[]{",", ", "})
                                                             .appendWeeks()
                                                             .appendSuffix("w")
                                                             .appendSeparator(" ", " ", new String[]{",", ", "})
                                                             .appendDays()
                                                             .appendSuffix("d")
                                                             .appendSeparator(" ", " ", new String[]{",", ", "})
                                                             .appendHours()
                                                             .appendSuffix("h")
                                                             .appendSeparator(" ", " ", new String[]{",", ", "})
                                                             .appendMinutes()
                                                             .appendSuffix("m")
                                                             .appendSeparator(" ", " ", new String[]{",", ", "})
                                                             .appendSecondsWithOptionalMillis()
                                                             .appendSuffix("s")
                                                             .toFormatter();

        @Override
        public void run(GuildCommandEvent event) {
            var args = event.getArgs();
            Check.check(!args.isEmpty(), CommandArgumentException::new);
            var uRef = args.get(0);
            var u = CacheUtils.getUser(Parser.User.parse(uRef));

            var time = -1L;
            if (args.size() > 1)
                time = DateTime.now().plus(Period.parse(lastArg(1, event), formatter)).getMillis();

            Check.entityReferenceNotNull(u, User.class, uRef);
            LevelConfig config = Levels.getConfig(event.getGuild());
            config.blockUser(u.getId());
            if (time > Instant.now().toEpochMilli()) {
                var settings = new ConcurrentHashMap<String, String>();
                settings.put(GUILD, event.getGuild().getId());
                settings.put(USER, u.getId());
                JobManager.newJob(processor, time, settings);
            }
            DatabaseUtil.updateObject(config);
            event.addCheckmark();
            event.reply("Level Blacklist", "Blacklisted %s",
                u.getAsMention()).queue();
        }

        @SubCommand(user.class)
        @CommandOptions(
            name = "remove",
            description = "Unblocks a user from not gaining levels",
            usage = "<user>",
            perm = CommandPerm.BOT_MANAGER,
            alias = "rm"
        )
        private static class Remove extends GuildCommand {
            @Override
            public void run(GuildCommandEvent event) {
                Check.check(!event.getArgs().isEmpty(), CommandArgumentException::new);
                var uRef = lastArg(0, event);
                var u = CacheUtils.getUser(Parser.User.parse(uRef));

                Check.entityReferenceNotNull(u, User.class, uRef);
                LevelConfig config = Levels.getConfig(event.getGuild());
                if (!config.unblocksUser(u.getId())) return;
                DatabaseUtil.updateObject(config);
                event.addCheckmark();
                event.reply("Level Blacklist", "Unblacklisted %s",
                    u.getAsMention()).queue();
            }
        }

        @SubCommand(user.class)
        @CommandOptions(
            name = "list",
            description = "List users blocked from gaining levels",
            perm = CommandPerm.BOT_MANAGER,
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
                var users = config.getBlockedUsers();

                if (roles.isEmpty() && channels.isEmpty()) return;

                PagedEmbed p = new PagedEmbed(EmbedUtil.pagedDescription(
                    new EmbedBuilder().setTitle("Blacklistings").build(),
                    users.stream().filter(s -> !s.isEmpty()).map(s -> CacheUtils.getUser(s) == null ? s :
                                                                          CacheUtils.getUser(s).getAsMention())
                        .map(s -> String.format("%s\n", s)).collect(Collectors.toList())), event);

                PageListener.add(p);
            }
        }
    }
}
