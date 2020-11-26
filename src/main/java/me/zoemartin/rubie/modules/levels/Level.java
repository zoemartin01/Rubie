package me.zoemartin.rubie.modules.levels;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.*;
import me.zoemartin.rubie.core.annotations.*;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Command
@CommandOptions(
    name = "level",
    alias = "lvl",
    description = "Shows Levels",
    usage = "[user]",
    perm = CommandPerm.BOT_USER
)
public class Level extends GuildCommand {
    @Override
    public void run(GuildCommandEvent event) {
        new Show().run(event);
    }

    @SubCommand(Level.class)
    @CommandOptions(
        name = "leaderboard",
        description = "Shows the current leaderboard",
        usage = "[full]",
        perm = CommandPerm.BOT_MANAGER
    )
    @SubCommand.AsBase(name = "leaderboard")
    static class Leaderboard extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            List<String> args = event.getArgs();
            List<UserLevel> levels;
            int start;
            if (!args.isEmpty() && args.get(0).equalsIgnoreCase("full")) {
                levels = Levels.getLevels(event.getGuild()).stream()
                             .sorted(Comparator.comparingInt(UserLevel::getExp).reversed())
                             .collect(Collectors.toList());
                start = args.size() > 1 && Parser.Int.isParsable(args.get(1)) ? Parser.Int.parse(args.get(1)) : 1;
            } else {
                levels = Levels.getLevels(event.getGuild()).stream()
                             .filter(userLevel -> Bot.getJDA().getUserById(userLevel.getUser_id()) != null)
                             .sorted(Comparator.comparingInt(UserLevel::getExp).reversed())
                             .collect(Collectors.toList());
                start = !args.isEmpty() && Parser.Int.isParsable(args.get(0)) ? Parser.Int.parse(args.get(0)) : 1;
            }

            PagedEmbed p = new PagedEmbed(EmbedUtil.pagedDescription(
                new EmbedBuilder().setTitle("Leaderboard").build(),
                levels.stream()
                    .map(ul -> {
                            User u = Bot.getJDA().getUserById(ul.getUser_id());
                            if (u == null) return String.format("%d. `%s` - Level: `%s` - `%sxp`\n",
                                levels.indexOf(ul) + 1, ul.getUser_id(), Levels.calcLevel(ul.getExp()), ul.getExp());
                            return String.format("%d. %s - Level: `%s` - `%sxp`\n", levels.indexOf(ul) + 1,
                                u.getAsMention(), Levels.calcLevel(ul.getExp()), ul.getExp());
                        }
                    ).collect(Collectors.toList())), event, start);

            PageListener.add(p);
        }
    }

    @SubCommand(Level.class)
    @CommandOptions(
        name = "show",
        description = "Shows a users level",
        usage = "[user]",
        perm = CommandPerm.BOT_USER
    )
    private static class Show extends GuildCommand {
        @Override
        public void run(GuildCommandEvent event) {
            User u = null;
            String arg;
            if (event.getArgs().isEmpty()) u = event.getUser();
            else if (Parser.User.isParsable(arg = lastArg(0, event))) u = CacheUtils.getUser(arg);
            else if (Parser.User.tagIsParsable(arg)) u = event.getJDA().getUserByTag(Objects.requireNonNull(arg));
            if (u == null) u = event.getUser();

            Member member = CacheUtils.getMember(event.getGuild(), u.getId());
            UserLevel level = Levels.getUserLevel(event.getGuild(), u);
            var userConf = member == null ? null : Levels.getUserConfig(member);
            int exp = level.getExp();
            int lvl = Levels.calcLevel(exp);
            double expToNext = Levels.calcExp(lvl + 1);

            List<UserLevel> levels = Levels.getLevels(event.getGuild()).stream()
                                         .filter(userLevel -> Bot.getJDA().getUserById(userLevel.getUser_id()) != null)
                                         .sorted(Comparator.comparingInt(UserLevel::getExp).reversed())
                                         .collect(Collectors.toList());

            EmbedBuilder eb = new EmbedBuilder()
                                  .setThumbnail(u.getEffectiveAvatarUrl())
                                  .setFooter(u.getAsTag())
                                  .setTimestamp(Instant.now());

            if (userConf != null && userConf.getColor() != null) {
                eb.setColor(userConf.getColor());
            } else {
                if (member != null) eb.setColor(member.getColor());
            }

            if (member != null) eb.setTitle("Level " + lvl + " - Rank #" + (levels.indexOf(level) + 1));
            else eb.setTitle("Level " + lvl);

            eb.addField((int) ((exp - Levels.calcExp(lvl)) / (expToNext - Levels.calcExp(lvl)) * 100) + "%",
                String.format("%d/%dxp", exp, Levels.calcExp(lvl + 1)), true);

            event.getChannel().sendMessage(eb.build()).queue();

        }
    }

    @SubCommand(Level.class)
    @CommandOptions(
        name = "customise",
        description = "Customise your level card!",
        usage = "[key] [value]",
        alias = {"c", "custom"},
        perm = CommandPerm.BOT_USER
    )
    static class Custom extends AutoConfig<UserConfig> {
        @Override
        protected UserConfig supply(GuildCommandEvent event) {
            return Levels.getUserConfig(event.getMember());
        }
    }
}
