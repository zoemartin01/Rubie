package me.zoemartin.rubie.modules.levels;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.annotations.CommandOptions;
import me.zoemartin.rubie.core.annotations.SubCommand;
import me.zoemartin.rubie.core.exceptions.*;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.io.*;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SubCommand(Level.class)
@CommandOptions(
    name = "config",
    description = "Level Configuration",
    perm = CommandPerm.BOT_ADMIN,
    alias = "conf"
)
@SubCommand.AsBase(name = "levelconfig", alias = "lvlconf")
class Config extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        throw new CommandArgumentException();
    }

    @SubCommand(Config.class)
    @CommandOptions(
        name = "param",
        description = "Level Parameter Configuration",
        perm = CommandPerm.BOT_ADMIN,
        alias = "parameter",
        usage = "[key] [value]"
    )
    static class Param extends AutoConfig<LevelConfig> {
        @Override
        protected LevelConfig supply(GuildCommandEvent event) {
            return Levels.getConfig(event.getGuild());
        }
    }

    @SubCommand(Config.class)
    @CommandOptions(
        name = "setxp",
        description = "Sets a users exp",
        usage = "<xp> <user>",
        perm = CommandPerm.BOT_ADMIN
    )
    private static class SetExp extends GuildCommand {
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
            event.reply("Levels", "Set %s's xp to `%s`", u.getAsMention(), xp).queue();
        }
    }

    /**
     * Input file format: *.json
     * <p>
     * [ { "Userid": id, "Exp": exp }, ... ]
     */
    @SubCommand(Config.class)
    @CommandOptions(
        name = "import",
        description = "Import Levels from an Attachment",
        perm = CommandPerm.BOT_ADMIN

    )
    private static class Import extends GuildCommand {
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
                        )).collect(Collectors.toList())), event);

            PageListener.add(p);
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

    @SubCommand(Config.class)
    @CommandOptions(
        name = "clear",
        description = "WARNING: CLEARS ALL LEVELS FROM THE DATABASE",
        perm = CommandPerm.BOT_ADMIN
    )
    private static class Clear extends GuildCommand {
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
            event.reply("Level Config",
                "Successfully cleared all Levels\nTime taken: %s seconds",
                Duration.between(start, Instant.now()).toSeconds()).queue();
        }
    }

    @SubCommand(Config.class)
    @CommandOptions(
        name = "rewards",
        description = "Manage Role Rewards",
        perm = CommandPerm.BOT_ADMIN,
        alias = "rolerewards",
        botPerms = Permission.MANAGE_ROLES
    )
    private static class RoleRewards extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            throw new CommandArgumentException();
        }

        @SubCommand(RoleRewards.class)
        @CommandOptions(
            name = "add",
            description = "Adds a role reward",
            usage = "<level> <role>",
            perm = CommandPerm.BOT_ADMIN,
            botPerms = Permission.MANAGE_ROLES
        )
        private static class Add extends GuildCommand {
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
                event.reply("Level Rewards", "Added %s to Level %s",
                    r.getAsMention(), level).queue();
            }
        }

        @SubCommand(RoleRewards.class)
        @CommandOptions(
            name = "remove",
            description = "Removes a role reward",
            usage = "<level> <role>",
            perm = CommandPerm.BOT_ADMIN
        )
        private static class Remove extends GuildCommand {
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
                event.reply("Level Rewards", "Removed %s from Level %s",
                    r.getAsMention(), level).queue();
            }
        }

        @SubCommand(RoleRewards.class)
        @CommandOptions(
            name = "list",
            description = "Lists all role rewards",
            perm = CommandPerm.BOT_ADMIN
        )
        private static class list extends GuildCommand {
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
                        ).collect(Collectors.toList())), event);

                PageListener.add(p);
                event.addCheckmark();
            }
        }
    }
}
