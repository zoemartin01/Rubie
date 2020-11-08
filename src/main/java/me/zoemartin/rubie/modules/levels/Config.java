package me.zoemartin.rubie.modules.levels;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.GuildCommandEvent;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Config implements GuildCommand {
    @Override
    public @NotNull Set<Command> subCommands() {
        return Set.of(new Enable(), new RoleRewards(), new Import(), new BlackList(), new Disable(), new Announce(),
            new SetExp(), new Clear());
    }

    @Override
    public @NotNull String name() {
        return "config";
    }

    @Override
    public @NotNull String regex() {
        return "config|conf";
    }

    @Override
    public void run(GuildCommandEvent event) {
        throw new CommandArgumentException();
    }

    @Override
    public @NotNull CommandPerm commandPerm() {
        return CommandPerm.BOT_ADMIN;
    }

    @Override
    public @NotNull String description() {
        return "Level Configuration";
    }

    private static class Enable implements GuildCommand {

        @Override
        public @NotNull String name() {
            return "enable";
        }

        @Override
        public void run(GuildCommandEvent event) {
            LevelConfig c = Levels.getConfig(event.getGuild());
            c.setEnabled(true);
            DatabaseUtil.updateObject(c);
            event.addCheckmark();
            embedReply(event, "Levels", "Enabled Leveling System").queue();
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public @NotNull String description() {
            return "Enabled the Leveling System";
        }
    }

    private static class Disable implements GuildCommand {

        @Override
        public @NotNull String name() {
            return "disable";
        }

        @Override
        public void run(GuildCommandEvent event) {
            LevelConfig c = Levels.getConfig(event.getGuild());
            c.setEnabled(false);
            DatabaseUtil.updateObject(c);
            event.addCheckmark();
            embedReply(event, "Levels", "Disabled Leveling System").queue();
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public @NotNull String description() {
            return "Disable the Leveling System";
        }
    }

    private static class Announce implements GuildCommand {

        @Override
        public @NotNull String name() {
            return "announce";
        }

        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().size() == 1 && event.getArgs().get(0).toLowerCase().matches("always|reward|never"),
                CommandArgumentException::new);

            LevelConfig config = Levels.getConfig(event.getGuild());

            switch (event.getArgs().get(0).toLowerCase()) {
                case "always" -> config.setAnnouncements(LevelConfig.Announcements.ALL);
                case "reward" -> config.setAnnouncements(LevelConfig.Announcements.REWARDS);
                case "never" -> config.setAnnouncements(LevelConfig.Announcements.NONE);
            }

            DatabaseUtil.updateObject(config);
            event.addCheckmark();
            embedReply(event, "Levels", "Level up announcements set to `%s`",
                config.getAnnouncements().toString()).queue();
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public @NotNull String description() {
            return "Announce level up ALWAYS/REWARD/NEVER";
        }
    }

    private static class SetExp implements GuildCommand {

        @Override
        public @NotNull String name() {
            return "setxp";
        }

        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().size() == 2 && Parser.Int.isParsable(event.getArgs().get(0))
                            && Parser.User.isParsable(event.getArgs().get(1)), CommandArgumentException::new);

            int xp = Parser.Int.parse(event.getArgs().get(0));
            User u = CacheUtils.getUserExplicit(event.getArgs().get(1));

            Check.check(xp >= 0, () -> new ReplyError("Xp must be above 0"));
            UserLevel level = Levels.getUserLevel(event.getGuild(), u);
            level.setExp(xp);
            DatabaseUtil.updateObject(level);
            event.addCheckmark();
            embedReply(event, "Levels", "Set %s's xp to `%s`", u.getAsMention(), xp).queue();
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public @NotNull String description() {
            return "Set's a users exp";
        }
    }

    /**
     * Input file format: *.json
     * <p>
     * [ { "Userid": id, "Exp": exp }, ... ]
     */
    private static class Import implements GuildCommand {

        @Override
        public @NotNull String name() {
            return "import";
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().isEmpty(), CommandArgumentException::new);
            Check.check(event.getAttachments().size() == 1, CommandArgumentException::new);
            Message m = event.getChannel().sendMessage("Okay... this might take a while").complete();
            Instant start = Instant.now();

            String guildId = event.getGuild().getId();
            Type listType = new TypeToken<ArrayList<ImportLevel>>() {
            }.getType();
            Map<String, Integer> levels;
            try (InputStreamReader ir = new InputStreamReader(event.getAttachments()
                                                                  .get(0)
                                                                  .retrieveInputStream()
                                                                  .get(1, TimeUnit.MINUTES))) {
                BufferedReader br = new BufferedReader(ir);
                levels = ((List<ImportLevel>) new Gson().fromJson(br, listType)).stream()
                             .collect(Collectors.toMap(ImportLevel::getUserId, ImportLevel::getExp));
            } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
                throw new UnexpectedError(e);
            }

            levels.forEach((s, integer) -> Levels.importLevel(new UserLevel(guildId, s, integer)));
            event.addCheckmark();
            m.delete().complete();

            PagedEmbed p = new PagedEmbed(EmbedUtil.pagedDescription(
                new EmbedBuilder().setTitle("Imported Levels: " + levels.size()).build(),
                Stream.concat(
                    Stream.of(String.format("Time taken: %s seconds\n",
                        Duration.between(start, Instant.now()).toSeconds())),
                levels.entrySet().stream()
                    .sorted(Comparator.comparingInt((ToIntFunction<Map.Entry<String, Integer>>) Map.Entry::getValue)
                                .reversed())
                    .map(e -> {
                            User u = Bot.getJDA().getUserById(e.getKey());
                            if (u == null) return String.format("User: `%s` - Level: `%s` - Exp: `%s`\n", e.getKey(),
                                Levels.calcLevel(e.getValue()), e.getValue());
                            return String.format("User: %s - Level: `%s` - Exp: `%s`\n", u.getAsMention(),
                                Levels.calcLevel(e.getValue()), e.getValue());
                        }
                    )).collect(Collectors.toList())),
                event.getChannel(), event.getUser());

            PageListener.add(p);
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public @NotNull String description() {
            return "Import Levels";
        }

        private static class ImportLevel implements Serializable {
            @SerializedName(value = "Userid")
            private final String userId;

            @SerializedName(value = "Exp")
            private final int exp;

            public ImportLevel(String userid, int exp) {
                this.userId = userid;
                this.exp = exp;
            }

            public String getUserId() {
                return userId;
            }

            public int getExp() {
                return exp;
            }
        }
    }

    private static class Clear implements GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            Check.check(event.getArgs().size() >= 1 && event.getArgs().get(0).matches("--agree"),
                () -> new ReplyError("This action will clear the entire level database for this server. " +
                                         "To confirm this action rerun this command with the argument `--agree`!"));
            Instant start = Instant.now();

            Message m = event.getChannel().sendMessage("Okay... this might take a while").complete();
            Levels.getLevels(event.getGuild()).forEach(DatabaseUtil::deleteObject);
            Levels.clearGuildCache(event.getGuild());
            event.addCheckmark();
            m.delete().complete();
            embedReply(event,"Level Config",
                "Successfully cleared all Levels\nTime taken: %s seconds",
                Duration.between(start, Instant.now()).toSeconds()).queue();
        }

        @NotNull
        @Override
        public String name() {
            return "clear";
        }

        @NotNull
        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @NotNull
        @Override
        public String description() {
            return "WARNING: CLEARS ALL LEVELS FROM THE DATABASE";
        }
    }

    private static class RoleRewards implements GuildCommand {

        @Override
        public @NotNull Set<Command> subCommands() {
            return Set.of(new RoleRewards.Add(), new RoleRewards.Remove(), new RoleRewards.list());
        }

        @Override
        public @NotNull String name() {
            return "rewards";
        }

        @Override
        public void run(GuildCommandEvent event) {
            throw new CommandArgumentException();
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_ADMIN;
        }

        @Override
        public @NotNull String description() {
            return "Manage Role Rewards";
        }

        private static class Add implements GuildCommand {

            @Override
            public @NotNull String name() {
                return "add";
            }

            @Override
            public void run(GuildCommandEvent event) {
                Check.check(event.getArgs().size() > 1 && Parser.Int.isParsable(event.getArgs().get(0)),
                    CommandArgumentException::new);

                String rRef = lastArg(1, event);
                Role r = Parser.Role.getRole(event.getGuild(), rRef);
                int level = Parser.Int.parse(event.getArgs().get(0));

                Check.entityReferenceNotNull(r, Role.class, rRef);
                Check.check(level > 0, () -> new ReplyError("Error, Level must be bigger than 0"));

                LevelConfig config = Levels.getConfig(event.getGuild());
                config.addRewardRole(r.getId(), level);

                DatabaseUtil.updateObject(config);
                event.addCheckmark();
                embedReply(event, "Level Rewards", "Added %s to Level %s",
                    r.getAsMention(), level).queue();
            }

            @Override
            public @NotNull CommandPerm commandPerm() {
                return CommandPerm.BOT_ADMIN;
            }

            @Override
            public @NotNull String usage() {
                return "<level> <role>";
            }

            @Override
            public @NotNull String description() {
                return "Adds Role Rewards";
            }
        }

        private static class Remove implements GuildCommand {

            @Override
            public @NotNull String name() {
                return "remove";
            }

            @Override
            public void run(GuildCommandEvent event) {
                Check.check(event.getArgs().size() > 1, CommandArgumentException::new);

                String rRef = lastArg(1, event);
                Role r = Parser.Role.getRole(event.getGuild(), rRef);
                int level = Parser.Int.parse(event.getArgs().get(0));
                Check.entityReferenceNotNull(r, Role.class, rRef);
                Check.check(level > 0, () -> new ReplyError("Error, Level must be bigger than 0"));

                LevelConfig config = Levels.getConfig(event.getGuild());
                if (!config.removeRewardRole(level, r.getId())) return;

                DatabaseUtil.updateObject(config);
                event.addCheckmark();
                embedReply(event, "Level Rewards", "Removed %s from Level %s",
                    r.getAsMention(), level).queue();
            }

            @Override
            public @NotNull String usage() {
                return "<level> <role>";
            }

            @Override
            public @NotNull CommandPerm commandPerm() {
                return CommandPerm.BOT_ADMIN;
            }

            @Override
            public @NotNull String description() {
                return "Removes Role Rewards";
            }
        }

        private static class list implements GuildCommand {

            @Override
            public @NotNull String name() {
                return "list";
            }

            @Override
            public void run(GuildCommandEvent event) {
                Check.check(event.getArgs().isEmpty(), CommandArgumentException::new);

                LevelConfig config = Levels.getConfig(event.getGuild());

                PagedEmbed p = new PagedEmbed(EmbedUtil.pagedDescription(
                    new EmbedBuilder().setTitle("Role Rewards").build(),
                    config.getRewardRoles().entrySet().stream()
                        .map(e ->
                                 String.format("Level: `%d` - Role(s): %s\n", e.getKey(),
                                     e.getValue().stream().map(s -> {
                                         Role r = event.getGuild().getRoleById(s);
                                         return r == null ? s : r.getAsMention();
                                     }).collect(Collectors.joining(", "))
                                 )
                        ).collect(Collectors.toList())),
                    event.getChannel(), event.getUser());

                PageListener.add(p);
                event.addCheckmark();
            }

            @Override
            public @NotNull CommandPerm commandPerm() {
                return CommandPerm.BOT_ADMIN;
            }

            @Override
            public @NotNull String description() {
                return "Lists Role Rewards";
            }
        }
    }
}
