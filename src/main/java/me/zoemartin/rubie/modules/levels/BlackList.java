package me.zoemartin.rubie.modules.levels;

import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.exceptions.CommandArgumentException;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class BlackList implements GuildCommand {

    @Override
    public @NotNull String name() {
        return "blacklist";
    }

    @Override
    public @NotNull Set<Command> subCommands() {
        return Set.of(new list(), new Channel(), new role());
    }

    @Override
    public @NotNull String regex() {
        return "bl|blacklist";
    }

    @Override
    public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
        help(user, channel, List.of("level", "config", name()), original);
    }

    @Override
    public @NotNull CommandPerm commandPerm() {
        return CommandPerm.BOT_ADMIN;
    }

    @Override
    public @NotNull String usage() {
        return "help";
    }

    @Override
    public @NotNull String description() {
        return "Blacklist config";
    }

    private static class list implements GuildCommand {
        @Override
        public @NotNull String name() {
            return "list";
        }

        @Override
        public @NotNull String regex() {
            return "l|list";
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
            Guild g = original.getGuild();
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
                    .map(s -> String.format("%s\n", s)).collect(Collectors.toList())),
                channel, user.getUser()
            );

            PageListener.add(p);
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public @NotNull String description() {
            return "Lists all blacklistings";
        }
    }

    private static class Channel implements GuildCommand {
        @Override
        public @NotNull Set<Command> subCommands() {
            return Set.of(new Channel.Remove());
        }

        @Override
        public @NotNull String name() {
            return "channel";
        }

        @Override
        public @NotNull String regex() {
            return "c|channel";
        }

        @Override
        public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
            Check.check(!args.isEmpty(), CommandArgumentException::new);
            String cRef = lastArg(0, args, original);
            TextChannel c = Parser.Channel.getTextChannel(original.getGuild(), cRef);

            Check.entityReferenceNotNull(c, TextChannel.class, cRef);
            LevelConfig config = Levels.getConfig(original.getGuild());
            config.addBlacklistedChannel(c.getId());
            DatabaseUtil.updateObject(config);
            addCheckmark(original);
            embedReply(original, channel, "Level Blacklist", "Blacklisted %s",
                c.getAsMention()).queue();
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public @NotNull String usage() {
            return "<channel>";
        }

        @Override
        public @NotNull String description() {
            return "Blacklist a channel";
        }

        private static class Remove implements GuildCommand {

            @Override
            public @NotNull String name() {
                return "remove";
            }

            @Override
            public @NotNull String regex() {
                return "rm|remove";
            }

            @Override
            public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
                Check.check(!args.isEmpty(), CommandArgumentException::new);
                String cRef = lastArg(0, args, original);
                TextChannel c = Parser.Channel.getTextChannel(original.getGuild(), cRef);

                Check.entityReferenceNotNull(c, TextChannel.class, cRef);
                LevelConfig config = Levels.getConfig(original.getGuild());
                if (!config.removeBlacklistedChannel(c.getId())) return;
                DatabaseUtil.updateObject(config);
                addCheckmark(original);
                embedReply(original, channel, "Level Blacklist", "Unblacklisted %s",
                    c.getAsMention()).queue();
            }

            @Override
            public @NotNull CommandPerm commandPerm() {
                return CommandPerm.BOT_ADMIN;
            }

            @Override
            public @NotNull String usage() {
                return "<channel>";
            }

            @Override
            public @NotNull String description() {
                return "Removes a blacklisted channel";
            }
        }
    }

    private static class role implements GuildCommand {
        @Override
        public @NotNull Set<Command> subCommands() {
            return Set.of(new role.Remove());
        }

        @Override
        public @NotNull String name() {
            return "role";
        }

        @Override
        public @NotNull String regex() {
            return "r|role";
        }

        @Override
        public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
            Check.check(!args.isEmpty(), CommandArgumentException::new);
            String rRef = lastArg(0, args, original);
            Role r = Parser.Role.getRole(original.getGuild(), rRef);

            Check.entityReferenceNotNull(r, Role.class, rRef);
            LevelConfig config = Levels.getConfig(original.getGuild());
            config.addBlacklistedRole(r.getId());
            DatabaseUtil.updateObject(config);
            addCheckmark(original);
            embedReply(original, channel, "Level Blacklist", "Blacklisted %s",
                r.getAsMention()).queue();
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public @NotNull String usage() {
            return "<role>";
        }

        @Override
        public @NotNull String description() {
            return "Blacklist a channel";
        }

        private static class Remove implements GuildCommand {

            @Override
            public @NotNull String name() {
                return "remove";
            }

            @Override
            public @NotNull String regex() {
                return "rm|remove";
            }

            @Override
            public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
                Check.check(!args.isEmpty(), CommandArgumentException::new);
                String rRef = lastArg(0, args, original);
                Role r = Parser.Role.getRole(original.getGuild(), rRef);

                Check.entityReferenceNotNull(r, Role.class, rRef);
                LevelConfig config = Levels.getConfig(original.getGuild());
                if (!config.removeBlacklistedRole(r.getId())) return;
                DatabaseUtil.updateObject(config);
                addCheckmark(original);
                embedReply(original, channel, "Level Blacklist", "Unblacklisted %s",
                    r.getAsMention()).queue();
            }

            @Override
            public @NotNull CommandPerm commandPerm() {
                return CommandPerm.BOT_ADMIN;
            }

            @Override
            public @NotNull String usage() {
                return "<role>";
            }

            @Override
            public @NotNull String description() {
                return "Removes a blacklisted role";
            }
        }
    }
}
