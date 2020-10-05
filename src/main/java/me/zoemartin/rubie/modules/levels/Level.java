package me.zoemartin.rubie.modules.levels;

import me.zoemartin.rubie.Bot;
import me.zoemartin.rubie.core.CommandPerm;
import me.zoemartin.rubie.core.interfaces.Command;
import me.zoemartin.rubie.core.interfaces.GuildCommand;
import me.zoemartin.rubie.core.util.*;
import me.zoemartin.rubie.modules.pagedEmbeds.PageListener;
import me.zoemartin.rubie.modules.pagedEmbeds.PagedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.util.*;
import java.util.stream.Collectors;

public class Level implements GuildCommand {
    @Override
    public String name() {
        return "level";
    }

    @Override
    public String regex() {
        return "level|lvl";
    }

    @Override
    public Set<Command> subCommands() {
        return Set.of(new Show(), new Config(), new Leaderboard());
    }

    @Override
    public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
        new Show().run(user, channel, args, original, invoked);
    }

    @Override
    public CommandPerm commandPerm() {
        return CommandPerm.BOT_USER;
    }

    @Override
    public String description() {
        return "Shows Levels";
    }

    private static class Leaderboard implements GuildCommand {

        @Override
        public String name() {
            return "leaderboard";
        }

        @Override
        public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
            List<UserLevel> levels;
            if (args.size() > 0 && args.get(0).equalsIgnoreCase("full")) {
                levels = Levels.getLevels(original.getGuild()).stream()
                             .sorted(Comparator.comparingInt(UserLevel::getExp).reversed())
                             .collect(Collectors.toList());
            } else {
                Guild g  = original.getGuild();
                levels = Levels.getLevels(original.getGuild()).stream()
                             .filter(userLevel -> Bot.getJDA().getUserById(userLevel.getUser_id()) != null)
                             .sorted(Comparator.comparingInt(UserLevel::getExp).reversed())
                             .collect(Collectors.toList());
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
                (TextChannel) channel, user);

            PageListener.add(p);
        }

        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_MANAGER;
        }

        @Override
        public String description() {
            return "Shows the current leaderboard";
        }
    }

    private static class Show implements GuildCommand {

        @Override
        public String name() {
            return "show";
        }

        @Override
        public void run(User user, MessageChannel channel, List<String> args, Message original, String invoked) {
            User u = null;
            if (args.isEmpty()) u = user;
            else if (Parser.User.isParsable(args.get(0))) u = CacheUtils.getUser(args.get(0));
            else if (Parser.User.tagIsParsable(args.get(0))) Bot.getJDA().getUserByTag(args.get(0));
            if (u == null) u = user;

            Member member = CacheUtils.getMember(original.getGuild(), u.getId());
            UserLevel level = Levels.getUserLevel(original.getGuild(), u);
            int exp = level.getExp();
            int lvl = Levels.calcLevel(exp);
            double expToNext = Levels.calcExp(lvl + 1);

            EmbedBuilder eb = new EmbedBuilder()
                                  .setThumbnail(u.getEffectiveAvatarUrl())
                                  .setAuthor(u.getAsTag(), null, u.getEffectiveAvatarUrl())
                                  .setTitle("Level " + lvl);

            if (member != null) eb.setColor(member.getColor());

            eb.addField( (int) ((exp - Levels.calcExp(lvl)) / (expToNext - Levels.calcExp(lvl)) * 100) + "%",
                String.format("%d/%dxp", exp, Levels.calcExp(lvl + 1)), true);

            channel.sendMessage(eb.build()).queue();

        }

        @Override
        public CommandPerm commandPerm() {
            return CommandPerm.BOT_USER;
        }

        @Override
        public String description() {
            return "Shows a users level";
        }
    }
}
