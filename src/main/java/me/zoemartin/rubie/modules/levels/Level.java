package me.zoemartin.rubie.modules.levels;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import me.zoemartin.rubie.core.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Level implements GuildCommand {
    @Override
    public @NotNull String name() {
        return "level";
    }

    @Override
    public @NotNull String regex() {
        return "level|lvl";
    }

    @Override
    public @NotNull Set<Command> subCommands() {
        return Set.of(new Show(), new Config(), new Leaderboard());
    }

    @Override
    public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
        new Show().run(user, channel, args, original, invoked);
    }

    @Override
    public @NotNull CommandPerm commandPerm() {
        return CommandPerm.BOT_USER;
    }

    @Override
    public @NotNull String description() {
        return "Shows Levels";
    }

    static class Leaderboard implements GuildCommand {

        @Override
        public @NotNull String name() {
            return "leaderboard";
        }

        @Override
        public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
            List<UserLevel> levels;
            int start;
            if (!args.isEmpty() && args.get(0).equalsIgnoreCase("full")) {
                levels = Levels.getLevels(original.getGuild()).stream()
                             .sorted(Comparator.comparingInt(UserLevel::getExp).reversed())
                             .collect(Collectors.toList());
                start = args.size() > 1 && Parser.Int.isParsable(args.get(1)) ? Parser.Int.parse(args.get(1)) : 1;
            } else {
                levels = Levels.getLevels(original.getGuild()).stream()
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
                    ).collect(Collectors.toList())),
                channel, user.getUser(), start);

            PageListener.add(p);
        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_MANAGER;
        }

        @Override
        public @NotNull String description() {
            return "Shows the current leaderboard";
        }
    }

    private static class Show implements GuildCommand {

        @Override
        public @NotNull String name() {
            return "show";
        }

        @Override
        public void run(Member user, TextChannel channel, List<String> args, Message original, String invoked) {
            User u = null;
            String arg;
            if (args.isEmpty()) u = user.getUser();
            else if (Parser.User.isParsable(arg = lastArg(0, args, original))) u = CacheUtils.getUser(arg);
            else if (Parser.User.tagIsParsable(arg)) u = Bot.getJDA().getUserByTag(arg);
            if (u == null) u = user.getUser();

            Member member = CacheUtils.getMember(original.getGuild(), u.getId());
            UserLevel level = Levels.getUserLevel(original.getGuild(), u);
            int exp = level.getExp();
            int lvl = Levels.calcLevel(exp);
            double expToNext = Levels.calcExp(lvl + 1);

            List<UserLevel> levels = Levels.getLevels(original.getGuild()).stream()
                                         .filter(userLevel -> Bot.getJDA().getUserById(userLevel.getUser_id()) != null)
                                         .sorted(Comparator.comparingInt(UserLevel::getExp).reversed())
                                         .collect(Collectors.toList());

            EmbedBuilder eb = new EmbedBuilder()
                                  .setThumbnail(u.getEffectiveAvatarUrl())
                                  .setAuthor(u.getAsTag(), null, u.getEffectiveAvatarUrl())
                                  .setFooter(Bot.getJDA().getSelfUser().getName(), Bot.getJDA().getSelfUser().getEffectiveAvatarUrl())
                                  .setTimestamp(Instant.now())
                                  .setTitle("Level " + lvl + " - Rank #" + (levels.indexOf(level) + 1));

            if (member != null) eb.setColor(member.getColor());

            eb.addField( (int) ((exp - Levels.calcExp(lvl)) / (expToNext - Levels.calcExp(lvl)) * 100) + "%",
                String.format("%d/%dxp", exp, Levels.calcExp(lvl + 1)), true);

            channel.sendMessage(eb.build()).queue();

        }

        @Override
        public @NotNull CommandPerm commandPerm() {
            return CommandPerm.BOT_USER;
        }

        @Override
        public @NotNull String description() {
            return "Shows a users level";
        }
    }
}
